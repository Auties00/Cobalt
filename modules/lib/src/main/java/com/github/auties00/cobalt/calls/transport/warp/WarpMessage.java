package com.github.auties00.cobalt.calls.transport.warp;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a WARP media control message, the SFU's per packet control channel.
 *
 * <p>A WARP message is a five byte fixed header followed by a flag selected sequence of
 * {@link WarpAttribute} payloads. The header is a single {@code type} byte (always {@value #WARP_TYPE})
 * at offset zero, a packed {@code length} byte at offset one, a big endian {@code u16} timestamp at
 * offsets two and three, and one mandatory attribute flag byte at offset four; an optional extension
 * flag byte follows at offset five when {@link WarpAttributeFlag#EXT_FLAG} is set. Each set flag bit
 * appends its attribute's payload in ascending bit order, and the payload (the flag bytes plus the
 * attributes) is padded to an even length. Two transports carry a WARP message: it travels
 * {@linkplain Piggybacked piggybacked} on the tail of an RTP packet, or it travels
 * {@linkplain Standalone standalone} as its own datagram. The two differ in what attributes they may
 * carry: a piggybacked message must not carry the {@link WarpAttributeFlag#BANDWIDTH_REPORT bandwidth
 * report} attribute (that one is standalone only), and a standalone message is the form used for BWE
 * configuration.
 *
 * <p>The header's first two bytes form a little endian {@code u16} whose low byte is the type
 * {@value #WARP_TYPE} and whose high byte (offset one) carries the packed length: the padded payload
 * length divided by two, {@code (paddedPayloadLength >> 1) & 0x7F}. Equivalently the word is built as
 * {@code 0x0009 | ((paddedPayloadLength << 7) & 0x7F00)}. The {@code u16} at offsets two and three is a
 * big endian timestamp, a sixteen bit sample of a rolling millisecond clock supplied by the caller at
 * send time. This codec reproduces both fields on {@link #encode(int)} and tolerates them on decode by
 * reading the type from byte zero, the flags from byte four, and walking the flags rather than trusting
 * the packed length.
 *
 * @implNote This implementation drives decode from the flag bits rather than the packed length byte: it
 *           reads the type from byte zero, the base flag byte from byte four, and, when
 *           {@link WarpAttributeFlag#EXT_FLAG} is set, the extension flag byte from byte five, then
 *           reads each present attribute in ascending bit order. It decodes whatever attributes are
 *           present; rejecting an inbound server bandwidth report on a client is enforced by the
 *           transport, not by this codec.
 */
public sealed interface WarpMessage permits WarpMessage.Piggybacked, WarpMessage.Standalone {
    /**
     * The constant WARP message type written to byte zero and verified there on decode.
     */
    int WARP_TYPE = 9;

    /**
     * The byte length of the WARP header before any attribute payload, when no extension flag byte is
     * present.
     *
     * <p>The five bytes are the type byte at offset zero, the packed length byte at offset one, the big
     * endian {@code u16} timestamp at offsets two and three, and the base attribute flag byte at offset
     * four. An extension flag byte at offset five, present only when {@link WarpAttributeFlag#EXT_FLAG}
     * is set, is not counted here.
     */
    int HEADER_LENGTH = 5;

    /**
     * The byte offset of the base attribute flag byte within the header.
     */
    int FLAGS_OFFSET = 4;

    /**
     * The byte offset of the big endian timestamp {@code u16} within the header.
     */
    int TIMESTAMP_OFFSET = 2;

    /**
     * Returns the attributes carried by this message, in ascending flag bit order.
     *
     * @return the message's attributes; never {@code null}, possibly empty
     */
    List<WarpAttribute> attributes();

    /**
     * Returns whether this message may carry the bandwidth report attribute.
     *
     * @return {@code true} for a {@link Standalone} message, {@code false} for a {@link Piggybacked}
     *         message
     */
    boolean allowsBandwidthReport();

    /**
     * Encodes this message to its wire bytes with a zero timestamp.
     *
     * <p>This is the {@link #encode(int)} form with a timestamp of zero, for callers that do not carry a
     * send time clock (round trip use and tests). The transport supplies a real rolling clock sample via
     * {@link #encode(int)}.
     *
     * @return the encoded message bytes with a zero timestamp field
     */
    default byte[] encode() {
        return encode(0);
    }

    /**
     * Encodes this message to its wire bytes with the given timestamp.
     *
     * <p>Byte zero is the type {@value #WARP_TYPE}; byte one is the packed length; bytes two and three
     * are the big endian {@code timestamp}; byte four is the base flag byte set from the present
     * attributes, followed by the extension flag byte when an extension attribute is present; the
     * attribute payloads are appended in ascending flag bit order; the payload (the flag bytes plus the
     * attributes) is padded to an even length; and byte one is set to {@code (paddedPayloadLength >> 1) &
     * 0x7F}. A piggybacked message that carries a bandwidth report attribute is rejected.
     *
     * @implNote This implementation tracks the padded payload length as the flag byte count (one, or two
     *           with an extension) plus the attribute byte count, rounded up to even, then folds it into
     *           byte one with {@code (paddedPayloadLength << 7) & 0x7F00} over a little endian leading
     *           {@code u16} whose low byte is the type. The timestamp is written big endian at offsets
     *           two and three.
     * @param timestamp the sixteen bit timestamp written big endian at offsets two and three; a sample
     *                  of the caller's rolling millisecond clock, masked to sixteen bits
     * @return the encoded message bytes
     */
    default byte[] encode(int timestamp) {
        var flags = 0;
        var attributeBytes = 0;
        var hasExtension = false;
        for (var attribute : attributes()) {
            var flag = attribute.flag();
            flags |= flag.mask();
            if (flag == WarpAttributeFlag.PARTICIPANT_REPORT) {
                flags |= WarpAttributeFlag.PARTICIPANT_REPORT_COMPANION.mask();
            }
            if (flag.extension()) {
                hasExtension = true;
            }
            attributeBytes += attribute.valueLength();
        }
        if (hasExtension) {
            flags |= WarpAttributeFlag.EXT_FLAG.mask();
        }

        var flagBytes = hasExtension ? 2 : 1;
        var payloadBytes = flagBytes + attributeBytes;
        var paddedPayloadBytes = payloadBytes + (payloadBytes & 1);
        var extensionByte = hasExtension ? 1 : 0;
        var pad = paddedPayloadBytes - payloadBytes;
        var out = new byte[HEADER_LENGTH + extensionByte + attributeBytes + pad];

        out[0] = (byte) WARP_TYPE;
        out[1] = (byte) ((paddedPayloadBytes >>> 1) & 0x7f);
        WarpCodecSupport.putU16(out, TIMESTAMP_OFFSET, timestamp & 0xffff);
        out[FLAGS_OFFSET] = (byte) (flags & 0xff);
        var cursor = HEADER_LENGTH;
        if (hasExtension) {
            out[HEADER_LENGTH] = (byte) ((flags >>> 8) & 0xff);
            cursor = HEADER_LENGTH + 1;
        }
        for (var attribute : attributes()) {
            cursor = attribute.writeValue(out, cursor);
        }
        return out;
    }

    /**
     * Decodes a WARP message from its wire bytes.
     *
     * <p>Byte zero must be {@value #WARP_TYPE}. The base flag byte at offset four is read, then the
     * extension flag byte at offset five when {@link WarpAttributeFlag#EXT_FLAG} is set, then each
     * present attribute in ascending flag bit order; the companion and extension marker bits carry no
     * payload and are skipped. The packed length byte at offset one and the timestamp at offsets two and
     * three are not interpreted (the flags drive the walk), and the trailing pad byte, when present, is
     * ignored. The result is a {@link Standalone} message when the bandwidth report attribute is present
     * and a {@link Piggybacked} message otherwise.
     *
     * @param data the message bytes
     * @return the decoded WARP message
     * @throws NullPointerException     if {@code data} is {@code null}
     * @throws IllegalArgumentException if {@code data} is too short to hold the header or an attribute,
     *                                  or if byte zero is not {@value #WARP_TYPE}
     */
    static WarpMessage decode(byte[] data) {
        Objects.requireNonNull(data, "data cannot be null");
        if (data.length < HEADER_LENGTH) {
            throw new IllegalArgumentException("WARP message is shorter than its header: " + data.length);
        }
        var type = data[0] & 0xff;
        if (type != WARP_TYPE) {
            throw new IllegalArgumentException("WARP message type must be " + WARP_TYPE + ", got " + type);
        }
        var baseFlags = data[FLAGS_OFFSET] & 0xff;
        var flags = baseFlags;
        var cursor = HEADER_LENGTH;
        if (WarpAttributeFlag.EXT_FLAG.isSet(baseFlags)) {
            if (data.length < HEADER_LENGTH + 1) {
                throw new IllegalArgumentException("WARP extension flag set but no extension byte present");
            }
            flags |= (data[HEADER_LENGTH] & 0xff) << 8;
            cursor = HEADER_LENGTH + 1;
        }

        var attributes = new ArrayList<WarpAttribute>();
        for (var flag : WarpAttributeFlag.VALUES) {
            if (flag == WarpAttributeFlag.PARTICIPANT_REPORT_COMPANION || flag == WarpAttributeFlag.EXT_FLAG) {
                continue;
            }
            if (!flag.isSet(flags)) {
                continue;
            }
            switch (flag) {
                case SEQUENCE_NUMBER -> {
                    requireRemaining(data, cursor, 2);
                    attributes.add(new WarpAttribute.SequenceNumber(WarpCodecSupport.readU16(data, cursor)));
                    cursor += 2;
                }
                case DOWNLINK_BW -> {
                    requireRemaining(data, cursor, 2);
                    attributes.add(new WarpAttribute.DownlinkBandwidth(WarpCodecSupport.readU16(data, cursor)));
                    cursor += 2;
                }
                case VIDEO_ENCODING -> {
                    requireRemaining(data, cursor, 1);
                    attributes.add(new WarpAttribute.VideoEncoding(data[cursor] & 0xff));
                    cursor += 1;
                }
                case PARTICIPANT_REPORT -> {
                    requireRemaining(data, cursor, WarpParticipantReport.BYTE_LENGTH);
                    attributes.add(new WarpAttribute.ParticipantReport(WarpParticipantReport.readFrom(data, cursor)));
                    cursor += WarpParticipantReport.BYTE_LENGTH;
                }
                case SENDER_BWA -> {
                    requireRemaining(data, cursor, 4);
                    attributes.add(new WarpAttribute.SenderBandwidthAllocation((int) WarpCodecSupport.readU32(data, cursor)));
                    cursor += 4;
                }
                case BANDWIDTH_REPORT -> {
                    requireRemaining(data, cursor, 4);
                    var version = data[cursor] & 0xff;
                    var index = data[cursor + 1] & 0xff;
                    var minRemoteBwe = WarpCodecSupport.readU16(data, cursor + 2);
                    attributes.add(new WarpAttribute.BandwidthReport(version, index, minRemoteBwe));
                    cursor += 4;
                }
                default -> {
                }
            }
        }

        var hasBandwidthReport = WarpAttributeFlag.BANDWIDTH_REPORT.isSet(flags);
        return hasBandwidthReport ? new Standalone(attributes) : new Piggybacked(attributes);
    }

    /**
     * Validates that a buffer has at least {@code needed} bytes available at {@code offset}.
     *
     * @param data   the buffer being decoded
     * @param offset the current read cursor
     * @param needed the number of bytes the next attribute requires
     * @throws IllegalArgumentException if fewer than {@code needed} bytes remain
     */
    private static void requireRemaining(byte[] data, int offset, int needed) {
        if (offset + needed > data.length) {
            throw new IllegalArgumentException(
                    "WARP message truncated: need " + needed + " bytes at " + offset + " of " + data.length);
        }
    }

    /**
     * A WARP message that rides on the tail of an RTP packet.
     *
     * <p>The piggybacked form is the common case for routine control: it appends WARP bytes after the
     * RTP payload so no separate datagram is needed. It must not carry the bandwidth report attribute,
     * which is reserved for the standalone form.
     *
     * @param attributes the attributes carried, in any order; reordered to ascending flag bit order on
     *                   encode
     */
    record Piggybacked(List<WarpAttribute> attributes) implements WarpMessage {
        /** The logger for {@link Piggybacked}. */
        private static final System.Logger LOGGER = Log.get(Piggybacked.class);

        /**
         * Canonicalizes the record component, copying the attribute list immutably and rejecting a
         * bandwidth report attribute.
         *
         * @throws NullPointerException     if {@code attributes} is {@code null}
         * @throws IllegalArgumentException if any attribute is a
         *                                  {@link WarpAttributeFlag#BANDWIDTH_REPORT} attribute
         */
        public Piggybacked {
            var sorted = new ArrayList<>(Objects.requireNonNull(attributes, "attributes cannot be null"));
            sorted.sort(WarpAttribute.FLAG_ORDER);
            attributes = List.copyOf(sorted);
            for (var attribute : attributes) {
                if (attribute.flag() == WarpAttributeFlag.BANDWIDTH_REPORT) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "piggybacked warp message rejected, carries a bandwidth report");
                    }
                    throw new IllegalArgumentException("a piggybacked WARP message cannot carry a bandwidth report");
                }
            }
        }

        @Override
        public boolean allowsBandwidthReport() {
            return false;
        }
    }

    /**
     * A WARP message that travels as its own datagram.
     *
     * <p>The standalone form is used for control that cannot piggyback, principally BWE configuration
     * carrying the bandwidth report attribute.
     *
     * @param attributes the attributes carried, in any order; reordered to ascending flag bit order on
     *                   encode
     */
    record Standalone(List<WarpAttribute> attributes) implements WarpMessage {
        /**
         * Canonicalizes the record component, copying the attribute list immutably.
         *
         * @throws NullPointerException if {@code attributes} is {@code null}
         */
        public Standalone {
            var sorted = new ArrayList<>(Objects.requireNonNull(attributes, "attributes cannot be null"));
            sorted.sort(WarpAttribute.FLAG_ORDER);
            attributes = List.copyOf(sorted);
        }

        @Override
        public boolean allowsBandwidthReport() {
            return true;
        }
    }
}
