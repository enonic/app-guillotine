package com.enonic.app.guillotine.mcp.shape;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeadlineTest
{
    private static final long SECOND = 1_000_000_000L;

    @Test
    public void testPassesTwentySecondsAfterTheStart()
    {
        final AtomicLong clock = new AtomicLong( -5 * SECOND );
        final Deadline deadline = Deadline.start( clock::get );

        clock.addAndGet( 20 * SECOND - 1 );
        assertFalse( deadline.passed() );
        clock.incrementAndGet();
        assertTrue( deadline.passed() );
    }

    @Test
    public void testAnInterruptedThreadHasPassedItAndKeepsTheFlag()
    {
        final Deadline deadline = Deadline.start( () -> 0 );
        Thread.currentThread().interrupt();
        try
        {
            assertTrue( deadline.passed() );
            assertTrue( Thread.currentThread().isInterrupted() );
        }
        finally
        {
            Thread.interrupted();
        }
        assertFalse( deadline.passed() );
    }
}
