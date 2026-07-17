package com.github.auties00.cobalt.emoji;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Reader for the generated binary emoji resources co-located with this package.
 *
 * <p>Every resource encodes integers as big-endian and strings as a 4-byte
 * big-endian byte length followed by the UTF-8 bytes (not Java modified UTF-8),
 * so supplementary-plane emoji round-trip through {@link DataInputStream#readInt()}
 * plus {@link DataInputStream#readNBytes(int)} cleanly. The resources are written
 * by {@code tools/web/emoji-codegen}.
 */
final class EmojiResources {
    /**
     * Prevents instantiation.
     */
    private EmojiResources() {
        throw new AssertionError();
    }

    /**
     * Opens a required binary resource from this package as a buffered data stream.
     *
     * @param name the resource file name, relative to this package
     * @return a buffered {@link DataInputStream} over the resource
     * @throws UncheckedIOException if the resource is absent
     */
    static DataInputStream open(String name) {
        var stream = EmojiResources.class.getResourceAsStream(name);
        if (stream == null) {
            throw new UncheckedIOException(new IOException("Missing emoji resource: " + name));
        }
        return new DataInputStream(new BufferedInputStream(stream));
    }

    /**
     * Opens an optional binary resource from this package, or returns {@code null}
     * when it is absent.
     *
     * @param name the resource file name, relative to this package
     * @return a buffered {@link DataInputStream} over the resource, or {@code null} when absent
     */
    static DataInputStream openOptional(String name) {
        var stream = EmojiResources.class.getResourceAsStream(name);
        return stream == null ? null : new DataInputStream(new BufferedInputStream(stream));
    }

    /**
     * Reads one length-prefixed UTF-8 string.
     *
     * @param in the stream positioned at a length-prefixed string
     * @return the decoded string
     * @throws IOException if the stream ends before the declared number of bytes is read
     */
    static String readString(DataInputStream in) throws IOException {
        var length = in.readInt();
        var bytes = in.readNBytes(length);
        if (bytes.length != length) {
            throw new IOException("Truncated emoji resource string: expected " + length + ", got " + bytes.length);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
