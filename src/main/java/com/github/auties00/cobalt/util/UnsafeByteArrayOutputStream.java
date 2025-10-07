package com.github.auties00.cobalt.util;

import java.io.ByteArrayOutputStream;

/**
 * An optimized extension of {@link ByteArrayOutputStream} that provides direct access to the internal byte array buffer without creating a defensive copy.
 */
public final class UnsafeByteArrayOutputStream extends ByteArrayOutputStream {
    /**
     * Returns the internal byte array buffer directly without creating a copy.
     * <p>
     * <b>This method is unsafe</b> because:
     * <ul>
     *   <li>It exposes the internal buffer, allowing external modification</li>
     *   <li>Further writes to this stream will modify the returned array</li>
     * </ul>
     *
     * Use this method only when you understand these implications and need the performance
     * benefit of avoiding array copying.
     *
     * @return the internal byte array buffer (not a copy)
     */
    @Override
    public byte[] toByteArray() {
        return buf;
    }
}