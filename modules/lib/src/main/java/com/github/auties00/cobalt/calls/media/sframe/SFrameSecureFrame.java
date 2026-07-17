package com.github.auties00.cobalt.calls.media.sframe;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Seals and opens one direction of SFrame end to end media frames, driving the cipher selected by
 * key id, framing each ciphertext with the SFrame trailer, and enforcing replay protection.
 *
 * <p>The wire frame this transform produces places the {@link SFrameCipher} body (ciphertext
 * followed by the truncated authentication tag) first, then the {@link SFrameHeaderCodec} trailer of
 * the LEB128 key id and counter closed by a final byte equal to the trailer's total length, so the
 * decoder reads the last byte to locate the trailer:
 *
 * {@snippet lang="text" :
 * [ ciphertext ][ auth tag ][ keyId varint ][ counter varint ][ length byte ]
 * }
 *
 * <p>The trailer bytes, including the length byte, are the associated data the cipher authenticates,
 * and the counter is the nonce selector for each frame.
 *
 * <p>An instance protects exactly one direction and expects a single writer. The seal path stamps a
 * monotonically increasing counter on each frame under a fixed key id; the open path reads the key id
 * and counter from the trailer, resolves the cipher for that key id through the
 * {@link SFrameKeyProvider}, rejects replays through the {@link SFrameReplayWindow}, and verifies the
 * tag before decrypting. Both paths update the cumulative {@link SFrameStats}.
 *
 * <p>The seal path resolves the cipher for the sealing key id, writes the trailer, encrypts with the
 * trailer as associated data, appends the trailer after the ciphertext and tag, and increments the
 * frame and byte counters. The open path reads the trailer length from the final byte, decodes the
 * key id then the counter, rejects a zero counter and any replay through the window, verifies the tag
 * then decrypts, and marks the counter consumed. A single key id per direction covers the 1:1 media
 * path; a group rekey installs a new chain key under a new key id through the provider.
 */
public final class SFrameSecureFrame {
    /**
     * The logger for {@link SFrameSecureFrame}.
     */
    private static final System.Logger LOGGER = Log.get(SFrameSecureFrame.class);

    /**
     * Holds the key provider resolving the cipher for each key id in this direction.
     */
    private final SFrameKeyProvider keyProvider;

    /**
     * Holds the key id stamped into every sealed frame for this direction.
     */
    private final long sealKeyId;

    /**
     * Holds the replay window guarding the open path against replayed or stale counters.
     */
    private final SFrameReplayWindow replayWindow = new SFrameReplayWindow();

    /**
     * Holds the next counter to stamp on a sealed frame.
     *
     * <p>The first sealed frame uses counter {@code 1}, since the decoder treats counter {@code 0} as
     * a malformed frame.
     */
    private long nextSealCounter = 1;

    /**
     * Holds the cumulative count of frames sealed or opened.
     */
    private long totalFrames;

    /**
     * Holds the cumulative count of plaintext bytes sealed or opened.
     */
    private long totalBytes;

    /**
     * Holds the cumulative count of frames that failed to seal or open.
     */
    private long errorFrames;

    /**
     * Holds the count of opened frames rejected as replays of a seen counter.
     */
    private long duplicateFrames;

    /**
     * Holds the count of opened frames whose key id had no installed key.
     */
    private long missingKeyFrames;

    /**
     * Holds the count of opened frames rejected for a malformed trailer or invalid parameter.
     */
    private long invalidParamFrames;

    /**
     * Constructs a secure frame transform for one direction.
     *
     * @param keyProvider the key provider resolving this direction's cipher
     * @param sealKeyId   the key id stamped into sealed frames
     * @throws NullPointerException if {@code keyProvider} is {@code null}
     */
    public SFrameSecureFrame(SFrameKeyProvider keyProvider, long sealKeyId) {
        this.keyProvider = Objects.requireNonNull(keyProvider, "keyProvider cannot be null");
        this.sealKeyId = sealKeyId;
    }

    /**
     * Returns the key provider this transform resolves ciphers through.
     *
     * @return the key provider
     */
    public SFrameKeyProvider keyProvider() {
        return keyProvider;
    }

    /**
     * Seals one media frame, returning the framed SFrame bytes.
     *
     * <p>The frame is encrypted under the next counter and the seal key id; the trailer is appended
     * after the ciphertext and tag, and its bytes are the cipher's associated data. The counter
     * advances even on failure so a transient cipher fault does not reuse a counter.
     *
     * @param plaintext the media payload to seal
     * @return the SFrame frame {@code [ciphertext][tag][keyId][counter][len]}
     * @throws NullPointerException  if {@code plaintext} is {@code null}
     * @throws IllegalStateException if no chain key has been installed in the key provider
     */
    public synchronized byte[] seal(byte[] plaintext) {
        Objects.requireNonNull(plaintext, "plaintext cannot be null");
        var counter = nextSealCounter++;
        var cipher = keyProvider.cipherForKeyId(sealKeyId, counter);
        if (cipher == null) {
            errorFrames++;
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sframe seal failed, no chain key for key id {0}", sealKeyId);
            throw new IllegalStateException("No SFrame chain key installed for key id " + sealKeyId);
        }
        var trailer = SFrameHeaderCodec.writeTrailer(sealKeyId, counter);
        var bodyLength = plaintext.length + cipher.tagLength();
        var frame = new byte[bodyLength + trailer.length];
        cipher.sealInto(trailer, plaintext, counter, frame, 0);
        System.arraycopy(trailer, 0, frame, bodyLength, trailer.length);
        totalFrames++;
        totalBytes += plaintext.length;
        if (Log.TRACE) LOGGER.log(Level.TRACE, "sframe frame sealed, keyId={0} counter={1} bytes={2}", sealKeyId, counter, plaintext.length);
        return frame;
    }

    /**
     * Opens one received SFrame frame, returning the recovered media payload.
     *
     * <p>Returns {@code null} when the frame is truncated, carries a malformed trailer, names a key id
     * with no installed key, replays an already seen counter, or fails authentication; the caller
     * drops the frame in those cases. Each rejection increments the matching error counter, and a
     * counter that authenticates is marked consumed in the replay window.
     *
     * @param frame the received SFrame bytes
     * @return the recovered plaintext, or {@code null} if the frame is invalid or rejected
     * @throws NullPointerException if {@code frame} is {@code null}
     */
    public synchronized byte[] open(byte[] frame) {
        Objects.requireNonNull(frame, "frame cannot be null");
        var trailerLength = SFrameHeaderCodec.readTrailerLength(frame, frame.length);
        if (trailerLength < 0) {
            invalidParamFrames++;
            errorFrames++;
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sframe open rejected, malformed trailer length, frame bytes={0}", frame.length);
            return null;
        }
        var trailerStart = frame.length - trailerLength;
        var trailer = SFrameHeaderCodec.readTrailer(frame, trailerStart, trailerLength);
        if (trailer == null) {
            invalidParamFrames++;
            errorFrames++;
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sframe open rejected, malformed trailer, frame bytes={0}", frame.length);
            return null;
        }
        var counter = trailer.counter();
        if (!replayWindow.isAcceptable(counter)) {
            duplicateFrames++;
            errorFrames++;
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sframe open rejected, replayed counter {0} for key id {1}", counter, trailer.keyId());
            return null;
        }
        var cipher = keyProvider.cipherForKeyId(trailer.keyId(), counter);
        if (cipher == null) {
            missingKeyFrames++;
            errorFrames++;
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sframe open rejected, no key for key id {0}", trailer.keyId());
            return null;
        }
        var plaintext = cipher.openFrame(frame, trailerStart, counter);
        if (plaintext == null) {
            errorFrames++;
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sframe open rejected, authentication failed for key id {0} counter {1}", trailer.keyId(), counter);
            return null;
        }
        replayWindow.accept(counter);
        totalFrames++;
        totalBytes += plaintext.length;
        if (Log.TRACE) LOGGER.log(Level.TRACE, "sframe frame opened, keyId={0} counter={1} bytes={2}", trailer.keyId(), counter, plaintext.length);
        return plaintext;
    }

    /**
     * Returns a snapshot of this transform's cumulative processing counters.
     *
     * @return the current {@link SFrameStats}
     */
    public synchronized SFrameStats stats() {
        return new SFrameStats(
                totalFrames, totalBytes, errorFrames, duplicateFrames, missingKeyFrames, invalidParamFrames);
    }

    /**
     * Resets this transform's cumulative processing counters to zero.
     */
    public synchronized void clearStats() {
        totalFrames = 0;
        totalBytes = 0;
        errorFrames = 0;
        duplicateFrames = 0;
        missingKeyFrames = 0;
        invalidParamFrames = 0;
    }
}
