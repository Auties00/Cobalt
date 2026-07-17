package com.github.auties00.cobalt.calls.transport.stun;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.Map;
import com.github.auties00.cobalt.calls.transport.warp.WarpMessage;

/**
 * Enumerates the STUN attribute types the call transport reads and writes, spanning the RFC 8489 and
 * RFC 8445 standard attributes and WhatsApp's proprietary {@code 0x40xx} attribute range.
 *
 * <p>A STUN message carries a sequence of type length value attributes after its twenty byte header.
 * Each attribute begins with a sixteen bit type, then a sixteen bit value length, then the value, then
 * zero padding to the next four byte boundary. This enum names every type the transport understands;
 * {@link StunMessage} keys its attribute table on these constants when parsing and stamps them when
 * building.
 *
 * <p>The standard attributes drive the RFC compliant Web P2P ICE path: {@link #USERNAME} carries the
 * short term credential username, {@link #MESSAGE_INTEGRITY} the {@code HMAC-SHA1} of the message,
 * {@link #FINGERPRINT} the CRC32 checksum, {@link #PRIORITY}, {@link #USE_CANDIDATE},
 * {@link #ICE_CONTROLLED}, and {@link #ICE_CONTROLLING} the ICE connectivity check fields, and
 * {@link #MAPPED_ADDRESS} / {@link #XOR_MAPPED_ADDRESS} the reflexive address reported back in a
 * binding response. The proprietary attributes drive the WhatsApp relay/SFU path:
 * {@link #WA_REFLEXIVE_PAYLOAD} and {@link #WA_RESPONSE_SIZE} appear in relay connectivity pings,
 * {@link #WA_RECEIVER_SUBSCRIPTION} / {@link #WA_SENDER_SUBSCRIPTIONS} carry the serialized
 * receive wish and send layout protobufs the SFU publish/subscribe layer embeds inside a binding
 * request, and the WhatsApp Web subscription envelope ({@link StunMessage#TYPE_SUBSCRIPTION}) carries
 * {@link #WA_WARP_MESSAGE} (a control WARP message whose body is sealed hop by hop with SRTP),
 * {@link #WA_SUBSCRIPTION} (the combined per stream subscription protobuf), and
 * {@link #WA_XOR_MAPPED_ADDRESS} (the relay's reflexive transport address) ahead of its trailing
 * {@link #MESSAGE_INTEGRITY}.
 *
 * <p>The standard values match RFC 8489 and RFC 8445; the {@code 0x40xx} values are the WhatsApp
 * private attribute range carried over the relay path.
 *
 * <p>Two attribute types are special in STUN message integrity processing: an attribute placed after
 * {@link #MESSAGE_INTEGRITY} is not covered by the integrity tag, and the integrity computation uses a
 * message length that counts up to and including the integrity attribute, while the fingerprint
 * computation uses a length that counts up to and including the fingerprint attribute.
 * {@link #isAfterIntegrity()} reports the two attributes ({@link #FINGERPRINT} and
 * {@link #MESSAGE_INTEGRITY} itself) that the integrity tag must not cover.
 */
public enum StunAttributeType {
    /**
     * {@code MAPPED-ADDRESS} (type {@code 0x0001}): a reflexive transport address in cleartext.
     *
     * <p>This is the reflexive address form that predates RFC 5389; modern STUN reports the reflexive
     * address through {@link #XOR_MAPPED_ADDRESS} instead, but the parser still recognizes this type.
     */
    MAPPED_ADDRESS(0x0001),

    /**
     * {@code USERNAME} (type {@code 0x0006}): the ICE short term credential username.
     *
     * <p>On an outbound binding request the value is {@code remoteUfrag:localUfrag} in ASCII, at most
     * 512 bytes, identifying the credential the receiver checks the {@link #MESSAGE_INTEGRITY} against.
     */
    USERNAME(0x0006),

    /**
     * {@code MESSAGE-INTEGRITY} (type {@code 0x0008}): the twenty byte {@code HMAC-SHA1} of the message.
     *
     * <p>The HMAC is keyed by the ICE password and computed over the STUN header and all preceding
     * attributes, with the header length field temporarily set to include this attribute. It is the
     * attribute before last, followed only by {@link #FINGERPRINT}.
     */
    MESSAGE_INTEGRITY(0x0008),

    /**
     * {@code XOR-MAPPED-ADDRESS} (type {@code 0x0020}): a reflexive transport address XORed with the
     * magic cookie.
     *
     * <p>The port is XORed with the high sixteen bits of the magic cookie and the address with the
     * magic cookie (and, for IPv6, the transaction id). A binding success response reports the
     * observed reflexive address through this attribute.
     */
    XOR_MAPPED_ADDRESS(0x0020),

    /**
     * {@code PRIORITY} (type {@code 0x0024}): the RFC 8445 candidate pair priority, a thirty two bit
     * unsigned value.
     */
    PRIORITY(0x0024),

    /**
     * {@code USE-CANDIDATE} (type {@code 0x0025}): a zero length flag the controlling ICE agent adds to
     * nominate a candidate pair.
     */
    USE_CANDIDATE(0x0025),

    /**
     * {@code FINGERPRINT} (type {@code 0x8028}): the CRC32 of the message XORed with {@code 0x5354554E}.
     *
     * <p>It is always the last attribute, computed over the STUN header and all preceding attributes
     * with the header length field set to include this attribute.
     */
    FINGERPRINT(0x8028),

    /**
     * {@code ICE-CONTROLLED} (type {@code 0x8029}): the sixty four bit tiebreaker sent by the agent in
     * the controlled role.
     */
    ICE_CONTROLLED(0x8029),

    /**
     * {@code ICE-CONTROLLING} (type {@code 0x802A}): the sixty four bit tiebreaker sent by the agent in
     * the controlling role.
     */
    ICE_CONTROLLING(0x802A),

    /**
     * {@code WA_REFLEXIVE_PAYLOAD} (type {@code 0x4003}): the WhatsApp proprietary reflexive address
     * payload carried in a relay connectivity ping request.
     */
    WA_REFLEXIVE_PAYLOAD(0x4003),

    /**
     * {@code WA_RESPONSE_SIZE} (type {@code 0x4004}): the WhatsApp proprietary response size and padding
     * attribute carried in a relay connectivity ping request.
     */
    WA_RESPONSE_SIZE(0x4004),

    /**
     * {@code WA_RECEIVER_SUBSCRIPTION} (type {@code 0x4021}): a serialized {@code RxSubscriptions}
     * protobuf naming which streams and qualities the client wishes to receive from the SFU.
     *
     * <p>The value is the raw protobuf bytes, padded to a four byte boundary like every STUN attribute;
     * the SFU forwards the selected simulcast layers in response.
     */
    WA_RECEIVER_SUBSCRIPTION(0x4021),

    /**
     * {@code WA_SENDER_SUBSCRIPTIONS} (type {@code 0x4025}): a serialized {@code SenderSubscriptions}
     * protobuf publishing the client's send layout (SSRC to pid assignments and per pid temporal
     * layers) to the SFU.
     *
     * <p>The value is the raw protobuf bytes, padded to a four byte boundary.
     */
    WA_SENDER_SUBSCRIPTIONS(0x4025),

    /**
     * {@code WA_SUBSCRIPTION} (type {@code 0x4024}): a serialized
     * {@link com.github.auties00.cobalt.wire.linked.call.datachannel.StreamSubscriptions} protobuf, the WhatsApp
     * Web combined per stream subscription map carried inside the {@code 0x0003} subscription envelope.
     *
     * <p>The value is the raw protobuf bytes, padded to a four byte boundary. Unlike the separate
     * {@link #WA_RECEIVER_SUBSCRIPTION} and {@link #WA_SENDER_SUBSCRIPTIONS} attributes, this single
     * attribute fuses the client's own send layout (entries with no participant) and the remote streams it
     * subscribes to (entries naming a participant) into one flat {@code repeated} entry list.
     */
    WA_SUBSCRIPTION(0x4024),

    /**
     * {@code WA_WARP_MESSAGE} (type {@code 0x4000}): a WARP media control message (the type 9 WARP frame)
     * carried first in the {@code 0x0003} subscription envelope.
     *
     * <p>The value is a control {@link WarpMessage} whose cleartext header is byte zero {@code 0x09} (the
     * WARP type), byte one the packed length, and a big endian {@code u16} timestamp at offsets two and
     * three; a captured envelope's byte one is {@code 0x0f}, the packed length of a thirty byte padded
     * payload, not a flag byte. The flag byte at offset four and the attributes after it are sealed before
     * framing, so those bytes are ciphertext rather than cleartext attributes. The structural framing is
     * built by {@link WarpMessage}; the body sealing is the relay hop by hop layer, not the end to end
     * SFrame transform (which never engages on the relayed SFU send path). The SFU treats this attribute
     * as an optional piggybacked rate control report, so a subscription envelope that omits it is still
     * well formed and correctly authenticated.
     */
    // TODO: emit WA_WARP_MESSAGE; a byte correct sealed body is not yet reproducible because the
    //  counter mode IV inputs (the synthetic WARP SSRC and packet index the seal derives its keystream
    //  from) and a matching relay hop by hop key are not available, so the envelope is currently sent
    //  without this optional rate control report.
    WA_WARP_MESSAGE(0x4000),

    /**
     * {@code WA_XOR_MAPPED_ADDRESS} (type {@code 0x0016}): the relay's reflexive transport address, in the
     * STUN XOR MAPPED ADDRESS form, carried between the {@link #WA_SUBSCRIPTION} attribute and the trailing
     * {@link #MESSAGE_INTEGRITY} in the {@code 0x0003} subscription envelope.
     *
     * <p>The eight byte value is the WhatsApp private XOR MAPPED ADDRESS encoding (a reserved byte, the
     * family byte, the port XORed with the high cookie bits, and the IPv4 address XORed with the magic
     * cookie). For example, the value {@code 00 01 2c 84 3e 1f f2 7d} decodes to family IPv4, port 3478,
     * address 31.13.86.63, a Meta relay reflexive address the SFU echoed back. WhatsApp uses attribute
     * type {@code 0x0016} for this XOR MAPPED ADDRESS form; the RFC standard type is the separate
     * {@link #XOR_MAPPED_ADDRESS} {@code 0x0020}. The on wire header bytes {@code 00 16 00 08} decode to
     * type {@code 0x0016}, length eight. Build the value with
     * {@link StunMessage#encodeXorMappedAddress(java.net.InetSocketAddress, byte[])}.
     */
    WA_XOR_MAPPED_ADDRESS(0x0016);

    /**
     * The logger for {@link StunAttributeType}.
     */
    private static final System.Logger LOGGER = Log.get(StunAttributeType.class);

    /**
     * Caches the constant array so the per attribute {@link #ofValue(int)} decode scan does not pay the
     * defensive clone cost of {@link #values()} on every STUN attribute parsed.
     */
    private static final StunAttributeType[] VALUES = values();

    /**
     * Maps each sixteen bit wire value to its attribute type constant so {@link #ofValue(int)} resolves a
     * parsed attribute in constant time rather than scanning {@link #VALUES}.
     *
     * <p>Built once from {@link #VALUES}, so the enum remains the single source of truth for the value
     * mapping; the STUN attribute values are too sparse across {@code 0..0xFFFF} for a dense array.
     */
    private static final Map<Integer, StunAttributeType> BY_VALUE = buildByValue();

    /**
     * Holds the sixteen bit STUN attribute type value as it appears on the wire.
     */
    private final int value;

    /**
     * Constructs an attribute type constant bound to its sixteen bit wire value.
     *
     * @param value the STUN attribute type value, in {@code 0..0xFFFF}
     */
    StunAttributeType(int value) {
        this.value = value;
    }

    /**
     * Returns the sixteen bit STUN attribute type value as it appears on the wire.
     *
     * @return the attribute type value, in {@code 0..0xFFFF}
     */
    public int value() {
        return value;
    }

    /**
     * Returns whether this attribute is one of the two that the {@link #MESSAGE_INTEGRITY} HMAC must
     * not cover.
     *
     * <p>The integrity tag is computed over the message up to but not including itself, so neither
     * {@link #MESSAGE_INTEGRITY} nor the trailing {@link #FINGERPRINT} contributes to the HMAC input.
     * Callers building the integrity input stop accumulating attribute bytes when they reach an
     * attribute for which this returns {@code true}.
     *
     * @return {@code true} for {@link #MESSAGE_INTEGRITY} and {@link #FINGERPRINT}, {@code false}
     *         otherwise
     */
    public boolean isAfterIntegrity() {
        return this == MESSAGE_INTEGRITY || this == FINGERPRINT;
    }

    /**
     * Returns the attribute type constant for a sixteen bit wire value, or {@code null} if no known
     * type matches.
     *
     * <p>A {@code null} result is the expected outcome for an attribute the transport does not model;
     * the parser keeps such an attribute as an unknown type value pair rather than failing, tolerating
     * unknown comprehension optional attributes.
     *
     * @param value the sixteen bit attribute type read from the wire
     * @return the matching {@link StunAttributeType}, or {@code null} if none is known
     */
    public static StunAttributeType ofValue(int value) {
        var type = BY_VALUE.get(value & 0xFFFF);
        if (type == null && Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "unknown stun attribute type=0x{0}", Integer.toHexString(value & 0xFFFF));
        }
        return type;
    }

    /**
     * Builds the {@link #BY_VALUE} wire value lookup from the enum constants.
     *
     * @return a map from each constant's sixteen bit wire value to the constant
     */
    private static Map<Integer, StunAttributeType> buildByValue() {
        var byValue = new HashMap<Integer, StunAttributeType>();
        for (var type : VALUES) {
            byValue.put(type.value, type);
        }
        return byValue;
    }
}
