package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
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

    private static final Pattern ENTITY = Pattern.compile( "&(#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6}|[a-zA-Z]{2,8});" );

    private static final Pattern INTERNAL = Pattern.compile( "(content|media|image)://(?:(?:download|inline)/)?([^?#\"'\\s]+).*" );

    private static final Map<String, String> ENTITIES =
        Map.of( "amp", "&", "lt", "<", "gt", ">", "quot", "\"", "apos", "'", "nbsp", " ", "mdash", "\u2014", "ndash", "\u2013", "hellip",
                "\u2026" );

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
            link.put( "type", "image" );
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
