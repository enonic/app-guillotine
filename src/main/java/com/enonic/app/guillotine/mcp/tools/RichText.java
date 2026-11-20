package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns HtmlArea markup into plain text and the list of links and images it contains. Internal targets keep their content id
 * so that the caller can resolve them.
 */
final class RichText
{
    static final int MAX_LINKS = 50;

    // Bodies stop at the next opening of the same kind and tags at the next <, so an unclosed tag cannot make a scan quadratic.
    private static final Pattern DROPPED =
        Pattern.compile( "(?is)<(script|style)\\b[^<>]*>(?:(?!<\\1\\b).)*?</\\1\\s*>|<!--(?:(?!<!--).)*?-->" );

    private static final Pattern LINE_BREAK =
        Pattern.compile( "(?i)<br\\s*/?>|</(p|div|h[1-6]|li|tr|figure|figcaption|blockquote|pre)\\s*>" );

    private static final Pattern LIST_ITEM = Pattern.compile( "(?i)<li\\b[^<>]*>" );

    private static final Pattern TAG = Pattern.compile( "<[^<>]*>" );

    private static final Pattern ANCHOR = Pattern.compile( "(?is)<a\\b([^<>]*)>((?:(?!<a\\b).)*?)</a\\s*>" );

    private static final Pattern IMAGE = Pattern.compile( "(?is)<img\\b([^<>]*)>" );

    private static final Pattern ENTITY = Pattern.compile( "&(#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6}|[a-zA-Z][a-zA-Z0-9]{1,7});" );

    private static final Pattern INTERNAL = Pattern.compile( "(content|media|image)://(?:(?:download|inline)/)?([^?#\"'\\s]+).*" );

    /**
     * HTML 4 Latin-1 entity names, in code point order from U+00A0.
     */
    private static final String LATIN_1 = "nbsp iexcl cent pound curren yen brvbar sect uml copy ordf laquo not shy reg macr deg " +
        "plusmn sup2 sup3 acute micro para middot cedil sup1 ordm raquo frac14 frac12 frac34 iquest Agrave Aacute Acirc Atilde Auml " +
        "Aring AElig Ccedil Egrave Eacute Ecirc Euml Igrave Iacute Icirc Iuml ETH Ntilde Ograve Oacute Ocirc Otilde Ouml times Oslash " +
        "Ugrave Uacute Ucirc Uuml Yacute THORN szlig agrave aacute acirc atilde auml aring aelig ccedil egrave eacute ecirc euml igrave " +
        "iacute icirc iuml eth ntilde ograve oacute ocirc otilde ouml divide oslash ugrave uacute ucirc uuml yacute thorn yuml";

    private static final Map<String, String> ENTITIES = entities();

    private RichText()
    {
    }

    /**
     * {@code {text, links?, linksTruncated?}} with at most {@link #MAX_LINKS} links; each link is
     * {@code {type: content|media|image|external, id?, url?, text?}}.
     */
    static Map<String, Object> convert( final String html )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put( "text", text( html ) );
        final List<Map<String, Object>> links = links( html, MAX_LINKS + 1 );
        if ( links.size() > MAX_LINKS )
        {
            links.subList( MAX_LINKS, links.size() ).clear();
            result.put( "linksTruncated", true );
        }
        if ( !links.isEmpty() )
        {
            result.put( "links", links );
        }
        return result;
    }

    static String text( final String html )
    {
        String text = DROPPED.matcher( html ).replaceAll( "" );
        text = LIST_ITEM.matcher( text ).replaceAll( "- " );
        text = LINE_BREAK.matcher( text ).replaceAll( "\n" );
        text = TAG.matcher( text ).replaceAll( "" );
        text = decode( text );
        final StringBuilder result = new StringBuilder();
        for ( final String line : text.split( "\n" ) )
        {
            final String collapsed = line.replaceAll( "[ \\t\\x0B\\f\\r\\u00A0]+", " " ).strip();
            if ( !collapsed.isEmpty() )
            {
                result.append( collapsed ).append( '\n' );
            }
        }
        return result.toString().strip();
    }

    static List<Map<String, Object>> links( final String html )
    {
        return links( html, MAX_LINKS );
    }

    private static List<Map<String, Object>> links( final String html, final int max )
    {
        final List<Map<String, Object>> links = new ArrayList<>();
        final String markup = DROPPED.matcher( html ).replaceAll( "" );

        final Matcher anchor = ANCHOR.matcher( markup );
        while ( anchor.find() && links.size() < max )
        {
            final String href = attribute( anchor.group( 1 ), "href" );
            if ( href == null || href.isBlank() || href.startsWith( "#" ) )
            {
                continue;
            }
            final Map<String, Object> link = target( href );
            final String label = text( anchor.group( 2 ) );
            if ( !label.isEmpty() )
            {
                link.put( "text", label );
            }
            links.add( link );
        }

        final Matcher image = IMAGE.matcher( markup );
        while ( image.find() && links.size() < max )
        {
            final String src = attribute( image.group( 1 ), "src" );
            if ( src == null || src.isBlank() )
            {
                continue;
            }
            final Map<String, Object> link = target( src );
            if ( link.containsKey( "id" ) )
            {
                link.put( "type", "image" );
            }
            final String alt = attribute( image.group( 1 ), "alt" );
            if ( alt != null && !alt.isBlank() )
            {
                link.put( "text", alt );
            }
            links.add( link );
        }
        return links;
    }

    private static Map<String, Object> target( final String reference )
    {
        final Map<String, Object> link = new LinkedHashMap<>();
        final Matcher internal = INTERNAL.matcher( reference );
        if ( internal.matches() )
        {
            link.put( "type", internal.group( 1 ) );
            link.put( "id", internal.group( 2 ) );
        }
        else
        {
            link.put( "type", "external" );
            link.put( "url", reference );
        }
        return link;
    }

    private static String attribute( final String attributes, final String name )
    {
        final Matcher matcher =
            Pattern.compile( "(?i)(?:^|\\s)" + name + "\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))" ).matcher( attributes );
        if ( !matcher.find() )
        {
            return null;
        }
        final String value = matcher.group( 1 ) != null
            ? matcher.group( 1 )
            : matcher.group( 2 ) != null ? matcher.group( 2 ) : matcher.group( 3 );
        return decode( value ).strip();
    }

    private static String decode( final String text )
    {
        final Matcher matcher = ENTITY.matcher( text );
        final StringBuilder result = new StringBuilder();
        while ( matcher.find() )
        {
            matcher.appendReplacement( result, Matcher.quoteReplacement( entity( matcher.group( 1 ), matcher.group() ) ) );
        }
        matcher.appendTail( result );
        return result.toString();
    }

    private static Map<String, String> entities()
    {
        final Map<String, String> entities = new HashMap<>();
        final String[] latin1 = LATIN_1.split( " " );
        for ( int i = 0; i < latin1.length; i++ )
        {
            entities.put( latin1[i], String.valueOf( (char) ( 0xA0 + i ) ) );
        }
        entities.put( "nbsp", " " );
        final String[] other = {"amp", "&", "lt", "<", "gt", ">", "quot", "\"", "apos", "'", "ndash", "\u2013", "mdash", "\u2014",
            "lsquo", "\u2018", "rsquo", "\u2019", "sbquo", "\u201A", "ldquo", "\u201C", "rdquo", "\u201D", "bdquo", "\u201E", "bull",
            "\u2022", "hellip", "\u2026", "prime", "\u2032", "lsaquo", "\u2039", "rsaquo", "\u203A", "euro", "\u20AC", "trade",
            "\u2122", "larr", "\u2190", "rarr", "\u2192", "uarr", "\u2191", "darr", "\u2193", "minus", "\u2212", "le", "\u2264",
            "ge", "\u2265", "ne", "\u2260", "OElig", "\u0152", "oelig", "\u0153", "Scaron", "\u0160", "scaron", "\u0161", "Yuml",
            "\u0178", "zwnj", "\u200C", "zwj", "\u200D", "thinsp", "\u2009", "ensp", "\u2002", "emsp", "\u2003", "dagger",
            "\u2020", "Dagger", "\u2021", "permil", "\u2030"};
        for ( int i = 0; i < other.length; i += 2 )
        {
            entities.put( other[i], other[i + 1] );
        }
        return Map.copyOf( entities );
    }

    private static String entity( final String name, final String original )
    {
        if ( name.startsWith( "#" ) )
        {
            try
            {
                final int code = name.startsWith( "#x" ) || name.startsWith( "#X" )
                    ? Integer.parseInt( name.substring( 2 ), 16 )
                    : Integer.parseInt( name.substring( 1 ) );
                return Character.isValidCodePoint( code ) ? new String( Character.toChars( code ) ) : original;
            }
            catch ( NumberFormatException e )
            {
                return original;
            }
        }
        return ENTITIES.getOrDefault( name, original );
    }
}
