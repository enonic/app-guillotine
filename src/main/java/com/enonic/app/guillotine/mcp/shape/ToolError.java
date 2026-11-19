package com.enonic.app.guillotine.mcp.shape;

/**
 * A failure the caller can fix by changing the arguments. The message is shown to the model as is, so it must say what to change.
 */
public final class ToolError
    extends RuntimeException
{
    private final String hint;

    public ToolError( final String message )
    {
        this( message, null );
    }

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
