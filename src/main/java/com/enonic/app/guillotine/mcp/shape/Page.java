package com.enonic.app.guillotine.mcp.shape;

public record Page(int offset, int count, String queryHash)
{
    /**
     * Cursor for the item after the last one actually returned, or {@code null} when nothing is left. {@code returned} may be lower
     * than {@code count} when the result budget dropped trailing items.
     */
    public String nextCursor( final long total, final int returned )
    {
        if ( returned <= 0 )
        {
            return null;
        }
        final long next = (long) offset + returned;
        return next < total && next <= Integer.MAX_VALUE ? Cursors.encode( (int) next, queryHash ) : null;
    }

    /**
     * Rejects a cursor that points past the last result, e.g. after content was deleted between pages. Paged tools call it once the
     * total is known.
     */
    public void requireWithin( final long total )
    {
        if ( offset > 0 && offset >= total )
        {
            throw new ToolError( "Invalid cursor: cursor is past the end of the results; repeat the call without cursor." );
        }
    }
}
