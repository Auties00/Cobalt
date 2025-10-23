package com.github.auties00.cobalt.socket.message;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

final class InflaterByteBufferInputStream extends InputStream {
    private static final int BUFFER_SIZE = 8192;

    private final ByteBuffer source;
    private final Inflater inflater;
    private final byte[] buffer;
    private boolean closed;
    private boolean reachedEOF;

    InflaterByteBufferInputStream(ByteBuffer source) {
        this(source, new Inflater());
    }

    InflaterByteBufferInputStream(ByteBuffer source, Inflater inflater) {
        this.source = source;
        this.inflater = inflater;
        this.buffer = new byte[BUFFER_SIZE];
        this.closed = false;
        this.reachedEOF = false;
    }

    @Override
    public int read() throws IOException {
        ensureOpen();
        
        if (reachedEOF) {
            return -1;
        }
        
        try {
            byte[] singleByte = new byte[1];
            
            while (true) {
                if (inflater.finished()) {
                    reachedEOF = true;
                    return -1;
                }
                
                if (inflater.needsInput()) {
                    // Feed more compressed data to the inflater
                    int available = source.remaining();
                    if (available == 0) {
                        if (inflater.finished()) {
                            reachedEOF = true;
                            return -1;
                        }
                        // No more input and not finished - may be truncated data
                        throw new IOException("Unexpected end of compressed data");
                    }
                    
                    // Read up to buffer size from the ByteBuffer
                    int toRead = Math.min(available, buffer.length);
                    source.get(buffer, 0, toRead);
                    inflater.setInput(buffer, 0, toRead);
                }
                
                // Decompress data
                int bytesRead = inflater.inflate(singleByte, 0, 1);
                
                if (bytesRead == 1) {
                    return singleByte[0] & 0xFF;
                }
                
                if (inflater.finished()) {
                    reachedEOF = true;
                    return -1;
                }
                
                if (inflater.needsDictionary()) {
                    throw new IOException("Inflater needs dictionary");
                }
            }
            
        } catch (DataFormatException e) {
            throw new IOException("Invalid compressed data format", e);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        
        if (b == null) {
            throw new NullPointerException("Byte array is null");
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        
        if (reachedEOF) {
            return -1;
        }
        
        try {
            int bytesRead = 0;
            
            while (bytesRead == 0) {
                if (inflater.finished()) {
                    reachedEOF = true;
                    return -1;
                }
                
                if (inflater.needsInput()) {
                    // Feed more compressed data to the inflater
                    int available = source.remaining();
                    if (available == 0) {
                        if (inflater.finished()) {
                            reachedEOF = true;
                            return -1;
                        }
                        // No more input and not finished - may be truncated data
                        throw new IOException("Unexpected end of compressed data");
                    }
                    
                    // Read up to buffer size from the ByteBuffer
                    int toRead = Math.min(available, buffer.length);
                    source.get(buffer, 0, toRead);
                    inflater.setInput(buffer, 0, toRead);
                }
                
                // Decompress data
                bytesRead = inflater.inflate(b, off, len);
                
                if (bytesRead == 0) {
                    if (inflater.finished()) {
                        reachedEOF = true;
                        return -1;
                    }
                    if (inflater.needsDictionary()) {
                        throw new IOException("Inflater needs dictionary");
                    }
                }
            }
            
            return bytesRead;
            
        } catch (DataFormatException e) {
            throw new IOException("Invalid compressed data format", e);
        }
    }

    @Override
    public long skip(long n) throws IOException {
        if (n < 0) {
            throw new IllegalArgumentException("Skip value is negative");
        }
        ensureOpen();
        
        long remaining = n;
        byte[] skipBuffer = new byte[(int) Math.min(BUFFER_SIZE, n)];
        
        while (remaining > 0) {
            int toRead = (int) Math.min(skipBuffer.length, remaining);
            int bytesRead = read(skipBuffer, 0, toRead);
            if (bytesRead == -1) {
                break;
            }
            remaining -= bytesRead;
        }
        
        return n - remaining;
    }

    @Override
    public int available() throws IOException {
        ensureOpen();
        if (reachedEOF) {
            return 0;
        }
        return inflater.finished() ? 0 : 1;
    }

    @Override
    public void close() {
        if (!closed) {
            inflater.end();
            closed = true;
        }
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
    }
}