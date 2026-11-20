package com.enonic.app.guillotine.mcp.shape;

/**
 * A failure the caller can fix by changing the arguments. It becomes an {@code isError} result whose text is the message followed by
 * the hint, so the message must say what was wrong and the hint what to do next.
 */
public final class ToolError
    extends RuntimeException
{
    private final String hint;

    public ToolError( final String message, final String hint )
    {
        super( message );
        this.hint = hint;
    }

    public String userMessage()
    {
        return hint == null || hint.isEmpty() ? getMessage() : getMessage() + " " + hint;
    }
}
