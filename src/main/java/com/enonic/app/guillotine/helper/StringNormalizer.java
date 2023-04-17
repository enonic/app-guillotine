package com.enonic.app.guillotine.helper;

import java.text.Normalizer;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableMap;

public class StringNormalizer
{
    private static final char[] REPLACE_WITH_UNDERSCORE_CHARS =
        {'$', '&', '|', ':', ';', '#', '/', '\\', '<', '>', '\"', '*', '+', ',', '=', '@', '%', '{', '}', '[', ']', '`', '~', '^', '-',
            '.'};

    private static final String NOT_ASCII = "[^\\p{ASCII}]";

    private static final String DEFAULT_REPLACE = "";

    private static final String DIACRITICAL = "[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+";

    private static final Pattern DIACRITICS = Pattern.compile( DIACRITICAL );

    private static final Map<Character, String> NON_DIACRITICS = buildNonDiacriticsMap();

    public static String create( final String value )
    {
        if ( value == null || value.isBlank() )
        {
            throw new IllegalArgumentException( "String cannot be empty or blank" );
        }

        String sanitizedString = value;

        sanitizedString = replaceWithUnderscore( sanitizedString );
        sanitizedString = replaceBlankSpaces( sanitizedString );
        sanitizedString = transcribe( sanitizedString );
        sanitizedString = sanitizedString.startsWith( "\\d+" ) ? "_" + sanitizedString : sanitizedString;

        return sanitizedString;
    }

    private static String replaceWithUnderscore( String sanitizedString )
    {
        if ( sanitizedString == null || sanitizedString.isBlank() )
        {
            return "";
        }

        for ( char toBeReplaced : REPLACE_WITH_UNDERSCORE_CHARS )
        {
            sanitizedString = sanitizedString.replace( toBeReplaced, '_' );
        }

        return sanitizedString;
    }

    private static String replaceBlankSpaces( String sanitizedString )
    {
        if ( sanitizedString == null || sanitizedString.isBlank() )
        {
            return "";
        }

        String trimmedName = sanitizedString.trim();

        trimmedName = trimmedName.replaceAll( "\\s+", "_" );

        return trimmedName;
    }

    private static String transcribe( final String string )
    {
        if ( string == null )
        {
            return null;
        }

        final StringBuilder stringBuilder = new StringBuilder();

        final int length = string.length();
        final char[] characters = new char[length];
        string.getChars( 0, length, characters, 0 );

        for ( final char character : characters )
        {
            final String replace = NON_DIACRITICS.get( character );
            final String toReplace = replace == null ? String.valueOf( character ) : replace;
            stringBuilder.append( toReplace );
        }

        final String normalized = Normalizer.normalize( stringBuilder, Normalizer.Form.NFD );
        final String diacriticsCleaned = DIACRITICS.matcher( normalized ).replaceAll( DEFAULT_REPLACE );
        final String nonAsciiCleaned = diacriticsCleaned.replaceAll( NOT_ASCII, DEFAULT_REPLACE );
        return nonAsciiCleaned;
    }

    private static Map<Character, String> buildNonDiacriticsMap()
    {
        final ImmutableMap.Builder<Character, String> map = ImmutableMap.builder();

        //remove crap strings with no semantics
        map.put( '\"', "" );
        map.put( '\'', "" );

        //keep relevant characters as separation
        map.put( ' ', DEFAULT_REPLACE );
        map.put( ']', DEFAULT_REPLACE );
        map.put( '[', DEFAULT_REPLACE );
        map.put( ')', DEFAULT_REPLACE );
        map.put( '(', DEFAULT_REPLACE );
        map.put( '=', DEFAULT_REPLACE );
        map.put( '!', DEFAULT_REPLACE );
        map.put( '/', DEFAULT_REPLACE );
        map.put( '\\', DEFAULT_REPLACE );
        map.put( '&', DEFAULT_REPLACE );
        map.put( ',', DEFAULT_REPLACE );
        map.put( '?', DEFAULT_REPLACE );
        map.put( '\u00b0', DEFAULT_REPLACE );
        map.put( '|', DEFAULT_REPLACE );
        map.put( '<', DEFAULT_REPLACE );
        map.put( '>', DEFAULT_REPLACE );
        map.put( ';', DEFAULT_REPLACE );
        map.put( ':', DEFAULT_REPLACE );
        map.put( '#', DEFAULT_REPLACE );
        map.put( '~', DEFAULT_REPLACE );
        map.put( '+', DEFAULT_REPLACE );
        map.put( '*', DEFAULT_REPLACE );

        //replace non-diacritics as their equivalent chars
        map.put( '\u0141', "l" );    // BiaLystock
        map.put( '\u0142', "l" );    // Bialystock
        map.put( '\u00df', "ss" );
        map.put( '\u00e6', "ae" );
        map.put( '\u00f8', "o" );
        map.put( '\u00a9', "c" );
        map.put( '\u00D0', "d" );     // all \u00d0 \u00f0 from http://de.wikipedia.org/wiki/%C3%90
        map.put( '\u00F0', "d" );
        map.put( '\u0110', "d" );
        map.put( '\u0111', "d" );
        map.put( '\u0189', "d" );
        map.put( '\u0256', "d" );
        map.put( '\u00DE', "th" );   // thorn \u00de
        map.put( '\u00FE', "th" );   // thorn \u00fe

        // cyrillic letters transliteration
        // big letters
        map.put( '\u0410', "a" );
        map.put( '\u0411', "b" );
        map.put( '\u0412', "v" );
        map.put( '\u0413', "g" );
        map.put( '\u0414', "d" );
        map.put( '\u0415', "e" );
        map.put( '\u0401', "jo" );
        map.put( '\u0416', "zh" );
        map.put( '\u0417', "z" );
        map.put( '\u0418', "i" );
        map.put( '\u0419', "j" );
        map.put( '\u041a', "k" );
        map.put( '\u041b', "l" );
        map.put( '\u041c', "m" );
        map.put( '\u041d', "n" );
        map.put( '\u041e', "o" );
        map.put( '\u041f', "p" );
        map.put( '\u0420', "r" );
        map.put( '\u0421', "s" );
        map.put( '\u0422', "t" );
        map.put( '\u0423', "u" );
        map.put( '\u0424', "f" );
        map.put( '\u0425', "h" );
        map.put( '\u0426', "c" );
        map.put( '\u0427', "ch" );
        map.put( '\u0428', "sh" );
        map.put( '\u0429', "sch" );
        map.put( '\u042a', "" );
        map.put( '\u042b', "y" );
        map.put( '\u042c', "" );
        map.put( '\u042d', "eh" );
        map.put( '\u042e', "ju" );
        map.put( '\u042f', "ja" );

        // small letters
        map.put( '\u0430', "a" );
        map.put( '\u0431', "b" );
        map.put( '\u0432', "v" );
        map.put( '\u0433', "g" );
        map.put( '\u0434', "d" );
        map.put( '\u0435', "e" );
        map.put( '\u0451', "jo" );
        map.put( '\u0436', "zh" );
        map.put( '\u0437', "z" );
        map.put( '\u0438', "i" );
        map.put( '\u0439', "j" );
        map.put( '\u043a', "k" );
        map.put( '\u043b', "l" );
        map.put( '\u043c', "m" );
        map.put( '\u043d', "n" );
        map.put( '\u043e', "o" );
        map.put( '\u043f', "p" );
        map.put( '\u0440', "r" );
        map.put( '\u0441', "s" );
        map.put( '\u0442', "t" );
        map.put( '\u0443', "u" );
        map.put( '\u0444', "f" );
        map.put( '\u0445', "h" );
        map.put( '\u0446', "c" );
        map.put( '\u0447', "ch" );
        map.put( '\u0448', "sh" );
        map.put( '\u0449', "sch" );
        map.put( '\u044a', "" );
        map.put( '\u044b', "y" );
        map.put( '\u044c', "" );
        map.put( '\u044d', "eh" );
        map.put( '\u044e', "ju" );
        map.put( '\u044f', "ja" );

        // others
        map.put( '\u0406', "i" );
        map.put( '\u0472', "fh" );
        map.put( '\u0462', "je" );
        map.put( '\u0474', "yh" );
        map.put( '\u0490', "gj" );
        map.put( '\u0403', "gj" );
        map.put( '\u0404', "ye" );
        map.put( '\u0407', "yi" );
        map.put( '\u0405', "dz" );
        map.put( '\u0408', "jj" );
        map.put( '\u0409', "lj" );
        map.put( '\u040a', "nj" );
        map.put( '\u040c', "kj" );
        map.put( '\u040f', "dj" );
        map.put( '\u040e', "uj" );

        map.put( '\u0456', "i" );
        map.put( '\u0473', "fh" );
        map.put( '\u0463', "je" );
        map.put( '\u0475', "yh" );
        map.put( '\u0491', "gj" );
        map.put( '\u0453', "gj" );
        map.put( '\u0454', "ye" );
        map.put( '\u0457', "yi" );
        map.put( '\u0455', "dz" );
        map.put( '\u0458', "jj" );
        map.put( '\u0459', "lj" );
        map.put( '\u045a', "nj" );
        map.put( '\u045c', "kj" );
        map.put( '\u045f', "dj" );
        map.put( '\u045e', "uj" );

        // greek
        // big letters
        map.put( '\u03b1', "a" );
        map.put( '\u03b2', "b" );
        map.put( '\u03b3', "g" );
        map.put( '\u03b4', "d" );
        map.put( '\u03b5', "e" );
        map.put( '\u03b6', "z" );
        map.put( '\u03b7', "e" );
        map.put( '\u03b8', "th" );
        map.put( '\u03b9', "i" );
        map.put( '\u03ba', "c" );
        map.put( '\u03bb', "l" );
        map.put( '\u03bc', "m" );
        map.put( '\u03bd', "n" );
        map.put( '\u03be', "x" );
        map.put( '\u03bf', "o" );
        map.put( '\u03c0', "p" );
        map.put( '\u03c1', "r" );
        map.put( '\u03c3', "s" );
        map.put( '\u03c4', "t" );
        map.put( '\u03c5', "y" );
        map.put( '\u03c6', "ph" );
        map.put( '\u03c7', "ch" );
        map.put( '\u03c8', "ps" );
        map.put( '\u03c9', "o" );

        // small letters
        map.put( '\u0391', "a" );
        map.put( '\u0392', "b" );
        map.put( '\u0393', "g" );
        map.put( '\u0394', "d" );
        map.put( '\u0395', "e" );
        map.put( '\u0396', "z" );
        map.put( '\u0397', "e" );
        map.put( '\u0398', "th" );
        map.put( '\u0399', "i" );
        map.put( '\u039a', "c" );
        map.put( '\u039b', "l" );
        map.put( '\u039c', "m" );
        map.put( '\u039d', "n" );
        map.put( '\u039e', "x" );
        map.put( '\u039f', "o" );
        map.put( '\u03a0', "p" );
        map.put( '\u03a1', "r" );
        map.put( '\u03a3', "s" );
        map.put( '\u03a4', "t" );
        map.put( '\u03a5', "y" );
        map.put( '\u03a6', "ph" );
        map.put( '\u03a7', "ch" );
        map.put( '\u03a8', "ps" );
        map.put( '\u03a9', "o" );

        return map.build();
    }

}
