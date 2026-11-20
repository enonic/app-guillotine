package com.enonic.app.guillotine.mcp.shape;

public record Page(int offset, int count)
{
    /**
     * Cursor for the item after the last one actually returned, or {@code null} when nothing is left or the next item is beyond
     * {@link Cursors#MAX_WINDOW}. {@code returned} may be lower than {@code count} when the result budget dropped trailing items.
     */
    public String nextCursor( final long total, final int returned )
    {
        if ( returned <= 0 )
        {
            return null;
        }
        final long next = (long) offset + returned;
        return next < total && next < Cursors.MAX_WINDOW ? Cursors.encode( (int) next ) : null;
    }

    /**
     * Rejects a cursor that points past the last result, e.g. after content was deleted between pages. Paged tools call it once the
     * total is known.
     */
    public void requireWithin( final long total )
    {
        if ( offset > 0 && offset >= total )
        {
            throw new ToolError( "Invalid cursor: it points past the end of the results.", Cursors.RESTART_HINT );
        }
    }
}
