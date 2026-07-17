package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Per-stream bandwidth-allocation result for one simulcast layer.
 *
 * <p>Carries the BWA round's verdict for one sender stream: a target
 * {@linkplain #bitrate() bitrate} and a 128-bit bitmap (split across two
 * 64-bit halves) identifying which participant PIDs are subscribed to
 * this stream. Returned in pairs ({@code stream1}, {@code stream2}) by
 * {@link SimulcastBwaResults}, one per active simulcast layer.
 */
@ProtobufMessage(name = "BwaStream")
public final class BwaStream {
    /**
     * The allocated bitrate for this stream, in bits per second.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    final Integer bitrate;

    /**
     * The low half of the subscribed-PIDs bitmap, covering PIDs 0..63.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    final Long subscribedPidsBitmap0To63;

    /**
     * The high half of the subscribed-PIDs bitmap, covering PIDs 64..127.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
    final Long subscribedPidsBitmap64To127;

    /**
     * Constructs a new {@code BwaStream}.
     *
     * @param bitrate                     the allocated bitrate in bps
     * @param subscribedPidsBitmap0To63   the low bitmap half
     * @param subscribedPidsBitmap64To127 the high bitmap half
     */
    BwaStream(Integer bitrate, Long subscribedPidsBitmap0To63, Long subscribedPidsBitmap64To127) {
        this.bitrate = bitrate;
        this.subscribedPidsBitmap0To63 = subscribedPidsBitmap0To63;
        this.subscribedPidsBitmap64To127 = subscribedPidsBitmap64To127;
    }

    /**
     * Returns the allocated bitrate in bits per second.
     *
     * @return an {@link OptionalInt} carrying the bitrate, or empty
     */
    public OptionalInt bitrate() {
        return bitrate == null ? OptionalInt.empty() : OptionalInt.of(bitrate);
    }

    /**
     * Returns the low half of the subscribed-PIDs bitmap (PIDs 0..63).
     *
     * @return an {@link OptionalLong} carrying the bitmap half, or empty
     */
    public OptionalLong subscribedPidsBitmap0To63() {
        return subscribedPidsBitmap0To63 == null ? OptionalLong.empty() : OptionalLong.of(subscribedPidsBitmap0To63);
    }

    /**
     * Returns the high half of the subscribed-PIDs bitmap (PIDs 64..127).
     *
     * @return an {@link OptionalLong} carrying the bitmap half, or empty
     */
    public OptionalLong subscribedPidsBitmap64To127() {
        return subscribedPidsBitmap64To127 == null ? OptionalLong.empty() : OptionalLong.of(subscribedPidsBitmap64To127);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof BwaStream that
                && Objects.equals(this.bitrate, that.bitrate)
                && Objects.equals(this.subscribedPidsBitmap0To63, that.subscribedPidsBitmap0To63)
                && Objects.equals(this.subscribedPidsBitmap64To127, that.subscribedPidsBitmap64To127));
    }

    @Override
    public int hashCode() {
        return Objects.hash(bitrate, subscribedPidsBitmap0To63, subscribedPidsBitmap64To127);
    }

    @Override
    public String toString() {
        return "BwaStream[bitrate=" + bitrate
                + ", subscribedPidsBitmap0To63=" + subscribedPidsBitmap0To63
                + ", subscribedPidsBitmap64To127=" + subscribedPidsBitmap64To127 + ']';
    }
}
