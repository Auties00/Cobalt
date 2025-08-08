package it.auties.whatsapp.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

public final class Streams {
    private Streams() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static ByteArrayOutputStream newByteArrayOutputStream() {
        return new BytesOutputStream();
    }

    public static InputStream newInputStream(ByteBuffer buffer) {
        return new ByteBufferBackedInputStream(buffer);
    }

    public static InputStream newInputStream(byte[] buffer) {
        return new ByteArrayInputStream(buffer);
    }

    private static final class BytesOutputStream extends ByteArrayOutputStream {
        @Override
        public byte[] toByteArray() {
            return buf;
        }
    }

    private static final class ByteBufferBackedInputStream extends InputStream {
        private final ByteBuffer buffer;

        public ByteBufferBackedInputStream(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public int read() {
            if (!buffer.hasRemaining()) {
                return -1;
            }
            return buffer.get() & 0xFF;
        }

        @Override
        public int read(byte[] bytes, int off, int len) {
            if (!buffer.hasRemaining()) {
                return -1;
            }

            len = Math.min(len, buffer.remaining());
            buffer.get(bytes, off, len);
            return len;
        }

        @Override
        public int available() {
            return buffer.hasRemaining() ? buffer.remaining() : -1;
        }
    }
}
