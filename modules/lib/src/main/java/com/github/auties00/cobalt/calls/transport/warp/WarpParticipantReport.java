package com.github.auties00.cobalt.calls.transport.warp;

/**
 * Holds the ten byte WARP participant rate control report block carried by the
 * {@link WarpAttributeFlag#PARTICIPANT_REPORT participant report} attribute.
 *
 * <p>The report tells the SFU one device's current rate control view: its round trip time, the
 * bandwidth it can receive and send, its observed loss, an auxiliary metric, and a bitset of which
 * media it currently has active. The block is fixed at {@value #BYTE_LENGTH} bytes; every multi byte
 * field is big endian and the layout is:
 *
 * {@snippet lang="text" :
 * offset  size  field            type   meaning
 * 0       2     rtt_ms           u16    round trip time in milliseconds
 * 2       2     dl_bw_kbps       u16    downlink bandwidth in kilobits per second
 * 4       1     packet_loss_q8   u8     loss rate scaled by 256, clamped to 255
 * 5       2     aux              u16    auxiliary rate control metric
 * 7       1     media_dir_flags  u8     active media bitset
 * 8       2     uplink_bw_kbps   u16    uplink bandwidth in kilobits per second
 * }
 *
 * <p>Every field holds an already scaled wire value: the millisecond and kilobit per second fields
 * are the rate control layer's raw measurements divided down, and {@code uplink_bw_kbps} is zero when
 * one side bandwidth estimation is inactive. The {@code aux} field is an opaque auxiliary rate control
 * metric.
 *
 * <p>The packet loss field is a Q8 fixed point fraction:
 * {@link #ofLossRate(int, int, int, int, double, int)} converts a {@code [0,1]} loss rate to it by
 * multiplying by {@code 256} and clamping to {@code 255}, while {@link #lossRate()} reverses the
 * conversion. The {@link #mediaDirFlags() media direction flags} are an opaque bitset whose low bit is
 * a base flag and whose higher bits mark individual enabled streams or features.
 *
 * @param rttMs         the round trip time in milliseconds, an unsigned sixteen bit value
 * @param dlBwKbps      the downlink bandwidth in kilobits per second, an unsigned sixteen bit value
 * @param packetLossQ8  the packet loss rate as a Q8 fraction, in the range {@code 0..255}
 * @param aux           the auxiliary rate control metric, an unsigned sixteen bit value
 * @param mediaDirFlags the active media bitset, an unsigned eight bit value
 * @param uplinkBwKbps  the uplink bandwidth in kilobits per second, an unsigned sixteen bit value
 */
public record WarpParticipantReport(int rttMs,
                                    int dlBwKbps,
                                    int packetLossQ8,
                                    int aux,
                                    int mediaDirFlags,
                                    int uplinkBwKbps) {
    /**
     * The fixed length, in bytes, of a participant report block on the wire.
     */
    public static final int BYTE_LENGTH = 10;

    /**
     * The maximum value of the Q8 packet loss field, the clamp applied to {@code rate*256}.
     */
    public static final int MAX_PACKET_LOSS_Q8 = 255;

    /**
     * The Q8 fixed point scale, the factor a {@code [0,1]} loss rate is multiplied by.
     */
    private static final int Q8_SCALE = 256;

    /**
     * Canonicalizes the record components, validating each field fits its unsigned wire width.
     *
     * @throws IllegalArgumentException if {@code rttMs}, {@code dlBwKbps}, {@code aux}, or
     *                                  {@code uplinkBwKbps} is outside {@code 0..65535}, if
     *                                  {@code packetLossQ8} is outside {@code 0..255}, or if
     *                                  {@code mediaDirFlags} is outside {@code 0..255}
     */
    public WarpParticipantReport {
        requireU16(rttMs, "rttMs");
        requireU16(dlBwKbps, "dlBwKbps");
        requireRange(packetLossQ8, MAX_PACKET_LOSS_Q8, "packetLossQ8");
        requireU16(aux, "aux");
        requireRange(mediaDirFlags, 0xff, "mediaDirFlags");
        requireU16(uplinkBwKbps, "uplinkBwKbps");
    }

    /**
     * Creates a report from a {@code [0,1]} loss rate, converting it to the Q8 packet loss field.
     *
     * <p>The loss rate is multiplied by {@value #Q8_SCALE}, rounded toward zero, and clamped to
     * {@value #MAX_PACKET_LOSS_Q8}; the other fields are taken as already scaled wire values.
     *
     * @param rttMs         the round trip time in milliseconds
     * @param dlBwKbps      the downlink bandwidth in kilobits per second
     * @param uplinkBwKbps  the uplink bandwidth in kilobits per second
     * @param aux           the auxiliary rate control metric
     * @param lossRate      the packet loss rate in the range {@code [0, 1]}; values outside are clamped
     * @param mediaDirFlags the active media bitset
     * @return a participant report with {@code packetLossQ8} derived from {@code lossRate}
     */
    public static WarpParticipantReport ofLossRate(int rttMs,
                                                   int dlBwKbps,
                                                   int uplinkBwKbps,
                                                   int aux,
                                                   double lossRate,
                                                   int mediaDirFlags) {
        var scaled = (int) (Math.max(0.0, lossRate) * Q8_SCALE);
        var clamped = Math.min(MAX_PACKET_LOSS_Q8, scaled);
        return new WarpParticipantReport(rttMs, dlBwKbps, clamped, aux, mediaDirFlags, uplinkBwKbps);
    }

    /**
     * Returns the packet loss rate as a fraction in {@code [0, 1]}, reversing the Q8 conversion.
     *
     * @return the loss rate, {@code packetLossQ8 / 256.0}
     */
    public double lossRate() {
        return packetLossQ8 / (double) Q8_SCALE;
    }

    /**
     * Writes this report's ten bytes, big endian per field, into a buffer at the given offset.
     *
     * @param out    the destination buffer; must have at least {@value #BYTE_LENGTH} bytes free at
     *               {@code offset}
     * @param offset the index at which to write the first byte
     * @return the index immediately after the written block ({@code offset + }{@value #BYTE_LENGTH})
     * @throws NullPointerException           if {@code out} is {@code null}
     * @throws ArrayIndexOutOfBoundsException if the block does not fit at {@code offset}
     */
    public int writeTo(byte[] out, int offset) {
        var cursor = offset;
        cursor = WarpCodecSupport.putU16(out, cursor, rttMs);
        cursor = WarpCodecSupport.putU16(out, cursor, dlBwKbps);
        out[cursor++] = (byte) packetLossQ8;
        cursor = WarpCodecSupport.putU16(out, cursor, aux);
        out[cursor++] = (byte) mediaDirFlags;
        cursor = WarpCodecSupport.putU16(out, cursor, uplinkBwKbps);
        return cursor;
    }

    /**
     * Reads a ten byte report block, big endian per field, from a buffer at the given offset.
     *
     * @param in     the source buffer; must hold at least {@value #BYTE_LENGTH} bytes at {@code offset}
     * @param offset the index of the first byte of the block
     * @return the decoded participant report
     * @throws NullPointerException           if {@code in} is {@code null}
     * @throws ArrayIndexOutOfBoundsException if the block does not fit at {@code offset}
     */
    public static WarpParticipantReport readFrom(byte[] in, int offset) {
        var cursor = offset;
        var rttMs = WarpCodecSupport.readU16(in, cursor);
        cursor += 2;
        var dlBwKbps = WarpCodecSupport.readU16(in, cursor);
        cursor += 2;
        var packetLossQ8 = in[cursor++] & 0xff;
        var aux = WarpCodecSupport.readU16(in, cursor);
        cursor += 2;
        var mediaDirFlags = in[cursor++] & 0xff;
        var uplinkBwKbps = WarpCodecSupport.readU16(in, cursor);
        return new WarpParticipantReport(rttMs, dlBwKbps, packetLossQ8, aux, mediaDirFlags, uplinkBwKbps);
    }


    /**
     * Validates that a value fits an unsigned sixteen bit field.
     *
     * @param value the value to validate
     * @param name  the field name for the diagnostic
     * @throws IllegalArgumentException if {@code value} is outside {@code 0..65535}
     */
    private static void requireU16(int value, String name) {
        requireRange(value, 0xffff, name);
    }

    /**
     * Validates that a value lies in {@code 0..max}.
     *
     * @param value the value to validate
     * @param max   the inclusive upper bound
     * @param name  the field name for the diagnostic
     * @throws IllegalArgumentException if {@code value} is outside {@code 0..max}
     */
    private static void requireRange(int value, int max, String name) {
        if (value < 0 || value > max) {
            throw new IllegalArgumentException(name + " must be in [0, " + max + "], got " + value);
        }
    }
}
