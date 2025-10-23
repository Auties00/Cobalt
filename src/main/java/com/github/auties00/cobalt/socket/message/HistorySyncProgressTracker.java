package com.github.auties00.cobalt.socket.message;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

final class HistorySyncProgressTracker {
    private final BitSet chunksMarker;
    private final AtomicInteger chunkEnd;

    HistorySyncProgressTracker() {
        this.chunksMarker = new BitSet();
        this.chunkEnd = new AtomicInteger(0);
    }

    boolean isDone() {
        var chunkEnd = this.chunkEnd.get();
        return chunkEnd > 0 && IntStream.range(0, chunkEnd)
                .allMatch(chunksMarker::get);
    }

    void commit(int chunk, boolean finished) {
        if (finished) {
            chunkEnd.set(chunk);
        }

        chunksMarker.set(chunk);
    }

    void clear() {
        chunksMarker.clear();
        chunkEnd.set(0);
    }
}
