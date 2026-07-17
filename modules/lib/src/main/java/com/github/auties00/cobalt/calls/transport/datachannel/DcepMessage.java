package com.github.auties00.cobalt.calls.transport.datachannel;

import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Models the two RFC 8832 Data Channel Establishment Protocol (DCEP) messages and their on the wire
 * codec for a Web P2P SCTP DataChannel.
 *
 * <p>DCEP is the handshake, carried in band, that opens a WebRTC DataChannel over an already established
 * SCTP association: one side sends a {@link Open} ({@code DATA_CHANNEL_OPEN}) on the channel's stream
 * carrying the channel label, subprotocol, priority, and reliability parameters, and the peer answers
 * with a single byte {@link Ack} ({@code DATA_CHANNEL_ACK}). Both ride the SCTP association on the DCEP
 * Payload Protocol Identifier {@link #PPID_DCEP}, distinct from the application data PPIDs the channel
 * uses once open. This sealed interface has exactly those two variants; {@link #encode()} produces the
 * bytes to send on {@link #PPID_DCEP} and {@link #decode(byte[])} parses the payload of an inbound DCEP
 * chunk back into the matching typed variant.
 *
 * <p>The {@code DATA_CHANNEL_OPEN} wire layout is big endian and laid out as follows:
 *
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  Message Type |  Channel Type |             Priority          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                    Reliability Parameter                      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |         Label Length          |       Protocol Length         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * /                             Label                             /
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * /                            Protocol                           /
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 *
 * <p>A {@code DATA_CHANNEL_ACK} is a single byte equal to {@link #MSG_ACK}.
 *
 * @implNote This implementation frames DCEP in Java over a directly bound usrsctp association. The wire
 * constants are the RFC 8831 SCTP PPID for WebRTC DCEP and the RFC 8832 message type and channel type
 * bytes.
 */
public sealed interface DcepMessage {
    /**
     * Holds the SCTP Payload Protocol Identifier that carries DCEP control messages.
     *
     * @implNote This implementation uses {@code 50}, the value assigned to "WebRTC DCEP" by the IANA SCTP
     * PPID registry per RFC 8831.
     */
    int PPID_DCEP = 50;

    /**
     * Holds the {@code Message Type} byte that marks a {@code DATA_CHANNEL_OPEN}.
     *
     * @implNote This implementation uses {@code 0x03}, the {@code DATA_CHANNEL_OPEN} message type defined
     * by RFC 8832.
     */
    byte MSG_OPEN = (byte) 0x03;

    /**
     * Holds the {@code Message Type} byte that marks a {@code DATA_CHANNEL_ACK}.
     *
     * @implNote This implementation uses {@code 0x02}, the {@code DATA_CHANNEL_ACK} message type defined
     * by RFC 8832.
     */
    byte MSG_ACK = (byte) 0x02;

    /**
     * Holds the {@code Channel Type} byte for a fully reliable, ordered channel.
     *
     * @implNote This implementation uses {@code 0x00}, the {@code DATA_CHANNEL_RELIABLE} channel type
     * defined by RFC 8832.
     */
    byte CHANNEL_RELIABLE = (byte) 0x00;

    /**
     * Holds the {@code Channel Type} byte for a channel partially reliable by retransmit count and
     * ordered.
     *
     * @implNote This implementation uses {@code 0x01}, the {@code DATA_CHANNEL_PARTIAL_RELIABLE_REXMIT}
     * channel type defined by RFC 8832.
     */
    byte CHANNEL_PARTIAL_RELIABLE_REXMIT = (byte) 0x01;

    /**
     * Holds the {@code Channel Type} byte for a channel partially reliable by message lifetime and
     * ordered.
     *
     * @implNote This implementation uses {@code 0x02}, the {@code DATA_CHANNEL_PARTIAL_RELIABLE_TIMED}
     * channel type defined by RFC 8832.
     */
    byte CHANNEL_PARTIAL_RELIABLE_TIMED = (byte) 0x02;

    /**
     * Holds the {@code Channel Type} byte for a fully reliable, unordered channel.
     *
     * @implNote This implementation uses {@code 0x80}, the {@code DATA_CHANNEL_RELIABLE_UNORDERED} channel
     * type defined by RFC 8832; the high bit flags unordered delivery.
     */
    byte CHANNEL_RELIABLE_UNORDERED = (byte) 0x80;

    /**
     * Holds the {@code Channel Type} byte for a channel partially reliable by retransmit count and
     * unordered.
     *
     * @implNote This implementation uses {@code 0x81}, the
     * {@code DATA_CHANNEL_PARTIAL_RELIABLE_REXMIT_UNORDERED} channel type defined by RFC 8832; the high bit
     * flags unordered delivery.
     */
    byte CHANNEL_PARTIAL_RELIABLE_REXMIT_UNORDERED = (byte) 0x81;

    /**
     * Holds the {@code Channel Type} byte for a channel partially reliable by message lifetime and
     * unordered.
     *
     * @implNote This implementation uses {@code 0x82}, the
     * {@code DATA_CHANNEL_PARTIAL_RELIABLE_TIMED_UNORDERED} channel type defined by RFC 8832; the high bit
     * flags unordered delivery.
     */
    byte CHANNEL_PARTIAL_RELIABLE_TIMED_UNORDERED = (byte) 0x82;

    /**
     * Holds the fixed size, in bytes, of the {@code DATA_CHANNEL_OPEN} header preceding the label and
     * protocol strings.
     *
     * @implNote This implementation uses {@code 12}: a message type byte, a channel type byte, a 16 bit
     * priority, a 32 bit reliability parameter, a 16 bit label length, and a 16 bit protocol length.
     */
    int OPEN_HEADER_SIZE = 12;

    /**
     * Holds the bit mask of the {@code Channel Type} byte that flags unordered delivery.
     *
     * @implNote This implementation uses {@code 0x80}; RFC 8832 reserves the high bit of the channel type
     * as the unordered flag.
     */
    int UNORDERED_FLAG = 0x80;

    /**
     * Encodes this message to its on the wire byte representation.
     *
     * <p>The returned array is ready to send on the SCTP association under the DCEP PPID
     * {@link #PPID_DCEP}. Each call returns a fresh array the caller owns.
     *
     * @return the encoded message bytes
     */
    byte[] encode();

    /**
     * Decodes an inbound DCEP chunk payload into its matching {@link DcepMessage} variant.
     *
     * <p>The first byte selects the variant: {@link #MSG_ACK} yields {@link Ack#INSTANCE} and
     * {@link #MSG_OPEN} yields a parsed {@link Open}. An empty payload, an unknown leading byte, or a
     * payload truncated relative to its declared label and protocol lengths is rejected.
     *
     * @param bytes the inbound bytes, typically the full payload of an SCTP DATA chunk that arrived on
     *              {@link #PPID_DCEP}
     * @return the parsed message
     * @throws NullPointerException              if {@code bytes} is {@code null}
     * @throws WhatsAppCallException.DataChannel if the payload is empty, carries an unknown message type,
     *                                           or is truncated relative to the declared field lengths
     */
    static DcepMessage decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes cannot be null");
        if (bytes.length == 0) {
            throw new WhatsAppCallException.DataChannel("DCEP payload is empty");
        }
        return switch (bytes[0]) {
            case MSG_ACK -> Ack.INSTANCE;
            case MSG_OPEN -> Open.decodeOpen(bytes);
            default -> throw new WhatsAppCallException.DataChannel(
                    "unknown DCEP message type: 0x" + Integer.toHexString(Byte.toUnsignedInt(bytes[0])));
        };
    }

    /**
     * Returns whether the given {@code Channel Type} byte denotes unordered delivery.
     *
     * @param channelType the {@code Channel Type} byte
     * @return {@code true} if the byte's high bit is set
     */
    static boolean isUnordered(byte channelType) {
        return (channelType & UNORDERED_FLAG) != 0;
    }

    /**
     * Represents the {@code DATA_CHANNEL_OPEN} variant.
     *
     * <p>An {@code Open} requests a new channel and carries everything the peer needs to mirror it: the
     * channel type byte, the priority, the reliability parameter whose meaning depends on the channel
     * type, and the {@code UTF-8} label and subprotocol strings. The compact constructor rejects an out of
     * range priority or reliability parameter and a label or protocol longer than the 16 bit length field
     * can express.
     *
     * @param channelType          the channel type byte, one of the {@code CHANNEL_*} constants
     * @param priority             the channel priority in the range {@code [0, 65535]}
     * @param reliabilityParameter the {@code Reliability Parameter} field; its meaning depends on
     *                             {@code channelType}, being {@code 0} for a reliable channel, the maximum
     *                             retransmits for a channel limited by retransmit count, or the maximum
     *                             lifetime in milliseconds for a timed channel
     * @param label                the channel label, {@code UTF-8} encoded
     * @param protocol             the application level subprotocol identifier, {@code UTF-8} encoded and
     *                             possibly empty
     */
    record Open(byte channelType,
                int priority,
                long reliabilityParameter,
                String label,
                String protocol) implements DcepMessage {
        /**
         * Validates the field values, rejecting null strings, an out of range priority or reliability
         * parameter, and a label or protocol whose {@code UTF-8} form exceeds the 16 bit length field.
         *
         * @throws NullPointerException     if {@code label} or {@code protocol} is {@code null}
         * @throws IllegalArgumentException if {@code priority} is outside {@code [0, 65535]}, if
         *                                  {@code reliabilityParameter} is outside {@code [0, 2^32)}, or if
         *                                  the label or protocol exceeds 65535 {@code UTF-8} bytes
         */
        public Open {
            Objects.requireNonNull(label, "label cannot be null");
            Objects.requireNonNull(protocol, "protocol cannot be null");
            if (priority < 0 || priority > 0xFFFF) {
                throw new IllegalArgumentException("priority out of range [0, 65535]: " + priority);
            }
            if (reliabilityParameter < 0 || reliabilityParameter > 0xFFFFFFFFL) {
                throw new IllegalArgumentException(
                        "reliabilityParameter out of range [0, 2^32): " + reliabilityParameter);
            }
            if (label.getBytes(StandardCharsets.UTF_8).length > 0xFFFF) {
                throw new IllegalArgumentException("label exceeds 65535 UTF-8 bytes");
            }
            if (protocol.getBytes(StandardCharsets.UTF_8).length > 0xFFFF) {
                throw new IllegalArgumentException("protocol exceeds 65535 UTF-8 bytes");
            }
        }

        /**
         * Returns whether this open requests unordered delivery.
         *
         * @return {@code true} if the channel type's high bit is set
         */
        public boolean unordered() {
            return DcepMessage.isUnordered(channelType);
        }

        /**
         * Returns the maximum retransmits encoded in this open, or empty when the channel type is not
         * limited by retransmit count.
         *
         * <p>The value is meaningful only for {@link #CHANNEL_PARTIAL_RELIABLE_REXMIT} and
         * {@link #CHANNEL_PARTIAL_RELIABLE_REXMIT_UNORDERED}, in which case the {@code reliabilityParameter}
         * is interpreted as a retransmit count.
         *
         * @return the maximum retransmits, or empty
         */
        public OptionalInt maxRetransmits() {
            return switch (channelType) {
                case CHANNEL_PARTIAL_RELIABLE_REXMIT, CHANNEL_PARTIAL_RELIABLE_REXMIT_UNORDERED ->
                        OptionalInt.of((int) reliabilityParameter);
                default -> OptionalInt.empty();
            };
        }

        /**
         * Returns the maximum message lifetime in milliseconds encoded in this open, or empty when the
         * channel type is not limited by message lifetime.
         *
         * <p>The value is meaningful only for {@link #CHANNEL_PARTIAL_RELIABLE_TIMED} and
         * {@link #CHANNEL_PARTIAL_RELIABLE_TIMED_UNORDERED}, in which case the {@code reliabilityParameter}
         * is interpreted as a millisecond lifetime.
         *
         * @return the maximum lifetime in milliseconds, or empty
         */
        public OptionalInt maxLifetimeMs() {
            return switch (channelType) {
                case CHANNEL_PARTIAL_RELIABLE_TIMED, CHANNEL_PARTIAL_RELIABLE_TIMED_UNORDERED ->
                        OptionalInt.of((int) reliabilityParameter);
                default -> OptionalInt.empty();
            };
        }

        /**
         * {@inheritDoc}
         *
         * <p>Serialises the {@code DATA_CHANNEL_OPEN} header followed by the {@code UTF-8} label and
         * protocol bytes, all big endian, into a freshly allocated array.
         *
         * @return the encoded {@code DATA_CHANNEL_OPEN} bytes
         */
        @Override
        public byte[] encode() {
            var labelBytes = label.getBytes(StandardCharsets.UTF_8);
            var protocolBytes = protocol.getBytes(StandardCharsets.UTF_8);
            var buf = ByteBuffer.allocate(OPEN_HEADER_SIZE + labelBytes.length + protocolBytes.length)
                    .order(ByteOrder.BIG_ENDIAN);
            buf.put(MSG_OPEN);
            buf.put(channelType);
            buf.putShort((short) priority);
            buf.putInt((int) reliabilityParameter);
            buf.putShort((short) labelBytes.length);
            buf.putShort((short) protocolBytes.length);
            buf.put(labelBytes);
            buf.put(protocolBytes);
            return buf.array();
        }

        /**
         * Parses the body of a {@code DATA_CHANNEL_OPEN} whose leading message type byte the caller has
         * already verified.
         *
         * <p>Reads the fixed header of {@link #OPEN_HEADER_SIZE} bytes, then the label and protocol strings
         * whose lengths it declares. The payload is rejected if it is shorter than the header or shorter
         * than the declared label plus protocol lengths.
         *
         * @param bytes the full payload, with byte 0 equal to {@link #MSG_OPEN}
         * @return the parsed {@code Open}
         * @throws WhatsAppCallException.DataChannel if the payload is shorter than the header or than its
         *                                           declared label and protocol lengths
         */
        private static Open decodeOpen(byte[] bytes) {
            if (bytes.length < OPEN_HEADER_SIZE) {
                throw new WhatsAppCallException.DataChannel(
                        "DATA_CHANNEL_OPEN truncated (need " + OPEN_HEADER_SIZE + " header bytes, have "
                                + bytes.length + ")");
            }
            var buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
            buf.get();
            var channelType = buf.get();
            var priority = Short.toUnsignedInt(buf.getShort());
            var reliability = Integer.toUnsignedLong(buf.getInt());
            var labelLen = Short.toUnsignedInt(buf.getShort());
            var protocolLen = Short.toUnsignedInt(buf.getShort());
            var expected = OPEN_HEADER_SIZE + labelLen + protocolLen;
            if (bytes.length < expected) {
                throw new WhatsAppCallException.DataChannel(
                        "DATA_CHANNEL_OPEN truncated: declared " + labelLen + "+" + protocolLen
                                + " bytes of label/protocol but payload is " + bytes.length);
            }
            var labelBytes = new byte[labelLen];
            buf.get(labelBytes);
            var protocolBytes = new byte[protocolLen];
            buf.get(protocolBytes);
            return new Open(channelType, priority, reliability,
                    new String(labelBytes, StandardCharsets.UTF_8),
                    new String(protocolBytes, StandardCharsets.UTF_8));
        }
    }

    /**
     * Represents the {@code DATA_CHANNEL_ACK} variant.
     *
     * <p>An {@code Ack} confirms a peer's {@link Open} and carries no payload beyond its leading message
     * type byte. The type has no state per instance; callers reuse the cached {@link #INSTANCE} rather than
     * allocating fresh values.
     */
    record Ack() implements DcepMessage {
        /**
         * Holds the single byte wire form of every {@code DATA_CHANNEL_ACK}, cloned on each
         * {@link #encode()} so callers cannot mutate the shared template.
         */
        private static final byte[] WIRE = {MSG_ACK};

        /**
         * Holds the canonical singleton instance.
         *
         * <p>Because {@code Ack} carries no state, every {@code Ack} compares equal to this instance, so
         * callers can match either by reference or by {@link #equals(Object)}.
         */
        public static final Ack INSTANCE = new Ack();

        /**
         * {@inheritDoc}
         *
         * @return a fresh copy of the single {@link #MSG_ACK} byte
         */
        @Override
        public byte[] encode() {
            return WIRE.clone();
        }
    }
}
