package com.enonic.app.guillotine.mcp.tools;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.ToolError;

/**
 * The index fields that hold user keys: {@code creator}, {@code modifier} and {@code owner}. Filters, sorts and aggregations on them
 * tell who changed what, so anonymous callers may not use them; signed-in callers may.
 */
final class PrincipalFields
{
    private static final Set<String> NAMES = Set.of( "creator", "modifier", "owner" );

    /**
     * The field itself and the index subfields XP keeps next to it, for matching wildcards.
     */
    private static final Set<String> SUBFIELDS = Set.of( "", "._orderby", "._analyzed", "._ngram" );

    private static final String REFUSAL = "filters, sorts and aggregations on creator, modifier and owner need a signed-in caller.";

    private static final String HINT = "Use other fields (data.<name> for a data field of that name), or ask the user to sign in to " +
        "Enonic XP.";

    private PrincipalFields()
    {
    }

    /**
     * Whether {@code field} names a principal field or a subfield of one ({@code modifier._orderby}), also with a boost
     * ({@code owner^2}) or with wildcards that match one ({@code mod*}). Data fields such as {@code data.owner} do not count. The name
     * is normalised as XP normalises an index path: boost split off, control characters and whitespace trimmed, lower case.
     */
    static boolean isPrincipal( final String field )
    {
        final String path = trim( field.split( "\\^", 2 )[0] );
        for ( final Locale locale : List.of( Locale.ROOT, Locale.getDefault() ) )
        {
            final String lower = path.toLowerCase( locale );
            if ( lower.contains( "*" ) || lower.contains( "?" ) )
            {
                final Pattern glob = Pattern.compile( Pattern.quote( lower ).replace( "*", "\\E.*\\Q" ).replace( "?", "\\E.\\Q" ) );
                if ( NAMES.stream()
                    .flatMap( name -> SUBFIELDS.stream().map( subfield -> name + subfield ) )
                    .anyMatch( name -> glob.matcher( name ).matches() ) )
                {
                    return true;
                }
            }
            else if ( NAMES.contains( trim( lower.split( "\\.", 2 )[0] ) ) )
            {
                return true;
            }
        }
        return false;
    }

    private static String trim( final String value )
    {
        String result = value;
        String previous;
        do
        {
            previous = result;
            result = result.trim().strip();
        }
        while ( !result.equals( previous ) );
        return result;
    }

    /**
     * Throws a {@link ToolError} when {@code anonymous} and {@code field}, given at {@code where}, is a principal field.
     */
    static void requireAllowed( final String field, final String where, final boolean anonymous )
    {
        if ( anonymous && isPrincipal( field ) )
        {
            throw new ToolError( "Invalid " + where + ": " + McpJson.preview( field ) + " holds user keys, and this call is anonymous; " +
                                     REFUSAL, HINT );
        }
    }
}
