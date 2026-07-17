package com.github.auties00.cobalt.calls.transport.warp;

import java.util.Comparator;

/**
 * Represents one decoded attribute of a WARP media control message.
 *
 * <p>A {@link WarpMessage} carries a flag selected sequence of attributes; this sealed interface is
 * the typed union of those attributes, one permitted record per meaningful {@link WarpAttributeFlag}.
 * Each variant knows the flag that selects it ({@link #flag()}) and can write its own payload bytes
 * ({@link #writeValue(byte[], int)}); the message codec uses {@link #flag()} to set the right flag bit
 * and {@link #writeValue(byte[], int)} to append the payload in ascending bit order. Companion only
 * bits (the participant report companion and the extension marker) carry no payload of their own and
 * so have no variant here; the codec sets them implicitly.
 *
 * @implNote This implementation models one variant per payload bearing flag bit. Bits {@code 6} and
 *           {@code 7} of the base flag byte carry no payload and are not modeled as variants.
 */
public sealed interface WarpAttribute
        permits WarpAttribute.SequenceNumber,
        WarpAttribute.DownlinkBandwidth,
        WarpAttribute.VideoEncoding,
        WarpAttribute.ParticipantReport,
        WarpAttribute.SenderBandwidthAllocation,
        WarpAttribute.BandwidthReport {
    /**
     * Returns the flag bit that selects this attribute.
     *
     * @return the {@link WarpAttributeFlag} whose presence indicates this attribute is in the message
     */
    WarpAttributeFlag flag();

    /**
     * Orders attributes by ascending flag bit ordinal, the order the WARP serializer appends them in.
     *
     * <p>A {@link WarpMessage} sorts its attributes with this comparator once at construction so
     * {@link WarpMessage#encode(int)} can append them directly without sorting again on every packet;
     * the wire order is identical to sorting inside {@link WarpMessage#encode(int)}.
     */
    Comparator<WarpAttribute> FLAG_ORDER =
            Comparator.comparingInt(attribute -> attribute.flag().ordinal());

    /**
     * Writes this attribute's payload bytes into a buffer at the given offset.
     *
     * @param out    the destination buffer; must have room for {@link #valueLength()} bytes at
     *               {@code offset}
     * @param offset the index at which to write the first payload byte
     * @return the index immediately after the written payload
     * @throws NullPointerException           if {@code out} is {@code null}
     * @throws ArrayIndexOutOfBoundsException if the payload does not fit at {@code offset}
     */
    int writeValue(byte[] out, int offset);

    /**
     * Returns the number of payload bytes this attribute writes.
     *
     * @return the fixed payload length in bytes for this attribute's variant
     */
    int valueLength();

    /**
     * The sequence number attribute, a big endian {@code u16}.
     *
     * <p>It carries the monotonic WARP sequence number; the value is masked to sixteen bits on write.
     *
     * @param value the sequence number
     */
    record SequenceNumber(int value) implements WarpAttribute {
        @Override
        public WarpAttributeFlag flag() {
            return WarpAttributeFlag.SEQUENCE_NUMBER;
        }

        @Override
        public int valueLength() {
            return 2;
        }

        @Override
        public int writeValue(byte[] out, int offset) {
            return WarpCodecSupport.putU16(out, offset, value);
        }
    }

    /**
     * The downlink bandwidth attribute, a big endian {@code u16} in kilobits per second.
     *
     * @param kbps the downlink bandwidth in kilobits per second
     */
    record DownlinkBandwidth(int kbps) implements WarpAttribute {
        @Override
        public WarpAttributeFlag flag() {
            return WarpAttributeFlag.DOWNLINK_BW;
        }

        @Override
        public int valueLength() {
            return 2;
        }

        @Override
        public int writeValue(byte[] out, int offset) {
            return WarpCodecSupport.putU16(out, offset, kbps);
        }
    }

    /**
     * The video encoding attribute, a single byte of direction and encoding flags.
     *
     * @param flags the direction and encoding bitset: bit {@code 0} send, bit {@code 1} receive, bit
     *              {@code 3} ({@code 0x08}) screen share
     */
    record VideoEncoding(int flags) implements WarpAttribute {
        @Override
        public WarpAttributeFlag flag() {
            return WarpAttributeFlag.VIDEO_ENCODING;
        }

        @Override
        public int valueLength() {
            return 1;
        }

        @Override
        public int writeValue(byte[] out, int offset) {
            out[offset] = (byte) flags;
            return offset + 1;
        }
    }

    /**
     * The participant report attribute, the ten byte {@link WarpParticipantReport} block.
     *
     * <p>On the wire this attribute's flag combines with its companion bit so the message carries the
     * mask {@code 0x28}; the codec sets the companion implicitly.
     *
     * @param report the participant rate control report
     */
    record ParticipantReport(WarpParticipantReport report) implements WarpAttribute {
        /**
         * Validates the record component, rejecting a {@code null} report.
         *
         * @throws NullPointerException if {@code report} is {@code null}
         */
        public ParticipantReport {
            java.util.Objects.requireNonNull(report, "report cannot be null");
        }

        @Override
        public WarpAttributeFlag flag() {
            return WarpAttributeFlag.PARTICIPANT_REPORT;
        }

        @Override
        public int valueLength() {
            return WarpParticipantReport.BYTE_LENGTH;
        }

        @Override
        public int writeValue(byte[] out, int offset) {
            return report.writeTo(out, offset);
        }
    }

    /**
     * The sender bandwidth allocation attribute, also the SRTP authenticated feedback value, a big
     * endian {@code u32}.
     *
     * @param value the four byte sender bandwidth allocation value
     */
    record SenderBandwidthAllocation(int value) implements WarpAttribute {
        @Override
        public WarpAttributeFlag flag() {
            return WarpAttributeFlag.SENDER_BWA;
        }

        @Override
        public int valueLength() {
            return 4;
        }

        @Override
        public int writeValue(byte[] out, int offset) {
            return WarpCodecSupport.putU32(out, offset, value);
        }
    }

    /**
     * The bandwidth report attribute carried in the extension byte, legal only on a standalone
     * message.
     *
     * <p>It is the BWE configuration block: a {@code version} byte, an {@code index} byte, and a big
     * endian {@code min_remote_bwe} {@code u16} in kilobits per second.
     *
     * @param version          the report version byte (the engine writes {@code 2})
     * @param index            the report index byte
     * @param minRemoteBweKbps the minimum remote bandwidth estimate in kilobits per second
     */
    record BandwidthReport(int version, int index, int minRemoteBweKbps) implements WarpAttribute {
        @Override
        public WarpAttributeFlag flag() {
            return WarpAttributeFlag.BANDWIDTH_REPORT;
        }

        @Override
        public int valueLength() {
            return 4;
        }

        @Override
        public int writeValue(byte[] out, int offset) {
            out[offset] = (byte) version;
            out[offset + 1] = (byte) index;
            return WarpCodecSupport.putU16(out, offset + 2, minRemoteBweKbps);
        }
    }
}
