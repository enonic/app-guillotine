package com.enonic.app.guillotine.mcp.shape;

import java.util.function.LongSupplier;

/**
 * The time limit of one tool call that makes many backend calls. Tools check it between backend calls and stop with a partial
 * result; it cannot cut a backend call short. An interrupted thread counts as a passed deadline, and its interrupt flag is kept.
 */
public final class Deadline
{
    public static final long LIMIT_SECONDS = 20;

    private static final long LIMIT_NANOS = LIMIT_SECONDS * 1_000_000_000L;

    private final LongSupplier nanoTime;

    private final long start;

    private Deadline( final LongSupplier nanoTime )
    {
        this.nanoTime = nanoTime;
        this.start = nanoTime.getAsLong();
    }

    /**
     * A deadline {@link #LIMIT_SECONDS} from now on {@code nanoTime}, a monotonic clock such as {@code System::nanoTime}.
     */
    public static Deadline start( final LongSupplier nanoTime )
    {
        return new Deadline( nanoTime );
    }

    public boolean passed()
    {
        return Thread.currentThread().isInterrupted() || nanoTime.getAsLong() - start >= LIMIT_NANOS;
    }
}
