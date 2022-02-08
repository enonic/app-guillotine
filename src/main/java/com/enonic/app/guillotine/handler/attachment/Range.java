package com.enonic.app.guillotine.handler.attachment;

final class Range
{
    final long start;

    final long end;

    final long length;

    Range( final long start, final long end )
    {
        this.start = start;
        this.end = end;
        this.length = end - start + 1;
    }

}
