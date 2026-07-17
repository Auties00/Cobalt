package com.github.auties00.cobalt.media;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Internal handle to a transcoded media payload that is about to be
 * uploaded to the WhatsApp CDN.
 *
 * <p>Bridges the per-format transcoder pipelines (image, sticker, audio,
 * voice note, video, document) and the upload layer
 * ({@link MediaConnectionService#upload(com.github.auties00.cobalt.wire.linked.media.MediaProvider, MediaPayload)}).
 * Every payload reports its plaintext {@link #length()} up-front so the
 * upload layer can build a known-length {@link java.net.http.HttpRequest.BodyPublisher}
 * for the HTTP POST and runs a streaming encrypt-and-hash pass without ever
 * holding the full ciphertext in memory or on disk. The plaintext is
 * replayable through {@link #openPlaintext()} so the same payload can be
 * encrypted once to compute the hashes that go into the upload URL, then
 * encrypted again to stream into each HTTP attempt of the retry loop.
 * Extending {@link AutoCloseable} lets callers release any owned temp file
 * in a try-with-resources block.
 *
 * @implNote
 * This implementation provides two concrete variants covering every
 * pipeline output shape. {@link OfBytes} wraps a small heap array for
 * pipelines whose encoded output fits comfortably in memory (image JPEG,
 * sticker WebP). {@link OfPath} wraps a file the upload layer reads
 * sequentially; the {@code ownsFile} flag decides whether {@link #close()}
 * deletes the file (transcoder-owned temp file) or leaves it alone
 * (caller-provided file in the {@link Path}-input upload entry point).
 */
public sealed interface MediaPayload extends AutoCloseable {
    /**
     * Returns the plaintext length in bytes.
     *
     * <p>Known up-front for every variant so the upload layer can compute
     * the encrypted content length deterministically
     * ({@code ((length / 16) + 1) * 16 + 10} for the AES-CBC plus truncated
     * HMAC wire format) and build a known-length HTTP body publisher. The
     * returned value is never negative.
     *
     * @return the plaintext length in bytes
     */
    long length();

    /**
     * Opens a fresh {@link InputStream} over the plaintext content.
     *
     * <p>Callable multiple times: the upload layer reads the plaintext
     * twice during normal operation (pass 1 computes hashes by encrypting
     * and discarding, pass 2 encrypts and streams to the HTTP socket) and
     * again on every retry attempt. Each call returns a fresh independent
     * stream positioned at byte 0.
     *
     * @return a new input stream over the plaintext
     * @throws IOException if the underlying file cannot be opened
     */
    InputStream openPlaintext() throws IOException;

    /**
     * Releases any resources owned by this payload.
     *
     * <p>Idempotent. For {@link OfBytes} this is a no-op; for
     * {@link OfPath} this deletes the underlying file when {@code ownsFile}
     * is {@code true}.
     */
    @Override
    void close();

    /**
     * A payload backed by an in-memory byte array.
     *
     * <p>Used by pipelines whose encoded output fits comfortably in heap
     * and never needs a temp file: the image pipeline (single MJPEG packet,
     * sub-MB) and the sticker pipeline (single libwebp packet, sub-MB). The
     * supplied array is not copied, so the caller must not mutate it after
     * construction.
     *
     * @param bytes the plaintext bytes
     */
    record OfBytes(byte[] bytes) implements MediaPayload {
        /**
         * Constructs a fresh bytes-backed payload.
         *
         * @param bytes the plaintext bytes; must not be {@code null}
         * @throws NullPointerException if {@code bytes} is {@code null}
         */
        public OfBytes {
            Objects.requireNonNull(bytes, "bytes");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long length() {
            return bytes.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream openPlaintext() {
            return new ByteArrayInputStream(bytes);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation is a no-op because a heap array owns no
         * external resource.
         */
        @Override
        public void close() {
        }
    }

    /**
     * A payload backed by a filesystem path.
     *
     * <p>Used by pipelines that mux through libavformat to a temp file
     * (audio M4A, voice note OGG, video MP4 with {@code +faststart}), by
     * the document pipeline's pass-through (the caller's file is the upload
     * payload), and by the {@link InputStream} entry point after the source
     * has been spilled to a temp file. The {@code ownsFile} flag
     * distinguishes transcoder-owned temp files (deleted on
     * {@link #close()}) from caller-provided files (left alone).
     */
    final class OfPath implements MediaPayload {
        /** The logger for {@link OfPath}. */
        private static final System.Logger LOGGER = Log.get(OfPath.class);

        /**
         * The path to the plaintext content.
         */
        private final Path path;

        /**
         * The plaintext length in bytes, captured at construction so the
         * upload layer never has to syscall for the size during the hot
         * path.
         */
        private final long length;

        /**
         * Whether {@link #close()} should delete the file.
         */
        private final boolean ownsFile;

        /**
         * Guards against double-close.
         */
        private boolean closed;

        /**
         * Constructs a fresh path-backed payload.
         *
         * <p>The {@code ownsFile} flag is {@code true} for transcoder-owned
         * temp files, in which case {@link #close()} deletes the file, and
         * {@code false} for caller-provided files (the {@link Path}-input
         * upload entry point), in which case {@link #close()} leaves the
         * file alone.
         *
         * @param path     the path to the plaintext content; must not be
         *                 {@code null}
         * @param length   the plaintext length in bytes; must not be
         *                 negative
         * @param ownsFile whether {@link #close()} should delete the file
         * @throws NullPointerException     if {@code path} is {@code null}
         * @throws IllegalArgumentException if {@code length} is negative
         */
        public OfPath(Path path, long length, boolean ownsFile) {
            this.path = Objects.requireNonNull(path, "path");
            if (length < 0L) {
                throw new IllegalArgumentException("length must be non-negative: " + length);
            }
            this.length = length;
            this.ownsFile = ownsFile;
        }

        /**
         * Returns the underlying path.
         *
         * @return the path to the plaintext content
         */
        public Path path() {
            return path;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long length() {
            return length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream openPlaintext() throws IOException {
            return Files.newInputStream(path, StandardOpenOption.READ);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation deletes the underlying file when
         * {@code ownsFile} is {@code true} and otherwise leaves it alone.
         * Errors during delete are swallowed; the file system's eventual
         * cleanup is acceptable for orphaned temp files.
         */
        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (ownsFile) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to delete temp media file", e);
                }
            }
        }
    }
}
