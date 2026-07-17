package com.github.auties00.cobalt.calls.transport.subscription;

import com.github.auties00.cobalt.calls.platform.VoipCryptoNative;
import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SenderSubscriptions;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SenderSubscriptionsSpec;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamDescriptors;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamDescriptorsSpec;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamSubscriptions;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamSubscriptionsSpec;

import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.github.auties00.cobalt.calls.transport.stun.StunAttributeType;
import com.github.auties00.cobalt.calls.transport.stun.StunIntegrity;
import com.github.auties00.cobalt.calls.transport.stun.StunMessage;
import com.github.auties00.cobalt.calls.transport.warp.WarpCodecSupport;
import com.github.auties00.cobalt.calls.transport.warp.WarpMessage;

/**
 * Builds the two WhatsApp Web connectivity plane STUN magic wire forms a call multiplexes over its one
 * SCTP data channel: the bare {@code 0x0801} keepalive ping and the {@code 0x0003} subscription envelope.
 *
 * <p>Both forms are STUN magic messages (the {@link StunMessage#MAGIC_COOKIE} cookie and a twelve byte
 * transaction id) carried as SCTP DATA, not RFC 8489 binding traffic. The
 * {@linkplain #keepalive(byte[]) keepalive} is a complete, header only message with no attributes and is
 * sent verbatim. The subscription envelope publishes which streams the leg wants forwarded and which
 * streams it sends, framed as the proprietary {@link StunAttributeType#WA_SUBSCRIPTION} attribute carrying
 * a {@link StreamSubscriptions}, {@link StreamDescriptors}, or {@link SenderSubscriptions} protobuf.
 *
 * <p>The {@code 0x0003} envelope is an ordered attribute list terminated by the
 * {@link StunAttributeType#MESSAGE_INTEGRITY} attribute. This class assembles two shapes:
 * <ul>
 * <li>a three attribute form: the subscription attribute ({@link StunAttributeType#WA_SUBSCRIPTION}
 * {@code 0x4024}, built by {@link #subscriptionAttribute(StreamSubscriptions)}), the relay reflexive
 * address ({@link StunAttributeType#WA_XOR_MAPPED_ADDRESS} {@code 0x0016}, built by
 * {@link #xorMappedAddressAttribute(InetSocketAddress, byte[])}), and the trailing {@code 0x0008} message
 * integrity;</li>
 * <li>a four attribute form that prepends the leading {@code 0x4000} relay token attribute (built by
 * {@link #relayTokenAttribute(byte[])}), the shape the relay requires: the relay binds the credential the
 * leading relay {@code <token>} references, then verifies the trailing {@code 0x0008} HMAC SHA1.</li>
 * </ul>
 * The trailing {@link StunAttributeType#MESSAGE_INTEGRITY} is HMAC SHA1 keyed by the relay {@code <key>} in
 * raw bytes, the same credential the {@code 0x0801} connectivity ping keys its own integrity with, not a
 * derived key.
 *
 * @implNote This implementation carries each SSRC in the {@code 0x4024} subscription value as a
 * {@code UINT64} varint, so a subscription SSRC with its high bit set encodes as a five byte unsigned varint
 * rather than the ten byte sign extended form a {@code UINT32} backed {@code Integer} would emit. The
 * keepalive matches the fixed {@code 0801 0000 2112a442 <txid>} header, and the envelope carries no
 * {@link StunAttributeType#FINGERPRINT}.
 */
public final class SubscriptionEnvelope {
    /**
     * The logger for {@link SubscriptionEnvelope}.
     */
    private static final System.Logger LOGGER = Log.get(SubscriptionEnvelope.class);

    /**
     * The fixed total length, in bytes, of a {@code 0x0801} keepalive message: the STUN header with a
     * zero length attribute section.
     */
    public static final int KEEPALIVE_LENGTH = StunMessage.HEADER_LENGTH;

    /**
     * The total on wire size, in bytes, of a {@link StunAttributeType#MESSAGE_INTEGRITY} attribute: a four
     * byte type length header plus the {@value StunIntegrity#MESSAGE_INTEGRITY_LENGTH} byte HMAC value.
     */
    private static final int MESSAGE_INTEGRITY_ATTR_SIZE = 4 + StunIntegrity.MESSAGE_INTEGRITY_LENGTH;

    /**
     * The byte offset of the STUN header's sixteen bit attribute section length field.
     */
    private static final int STUN_LENGTH_FIELD_OFFSET = 2;

    /**
     * Prevents instantiation of this stateless wire form builder.
     */
    private SubscriptionEnvelope() {
        throw new AssertionError("SubscriptionEnvelope is not instantiable");
    }

    /**
     * Builds a {@code 0x0801} connectivity keepalive message with a fresh random transaction id.
     *
     * <p>The message is a bare STUN magic header with no attributes; it is sent verbatim as SCTP DATA on
     * the data channel on the keepalive cadence to keep the leg alive.
     *
     * @return the encoded {@value #KEEPALIVE_LENGTH} byte keepalive message
     */
    public static byte[] keepalive() {
        return keepalive(VoipCryptoNative.randomBytes(StunMessage.TRANSACTION_ID_LENGTH));
    }

    /**
     * Builds a {@code 0x0801} connectivity keepalive message with the given transaction id.
     *
     * <p>The message is a bare STUN magic header with no attributes: the {@link StunMessage#TYPE_KEEPALIVE}
     * type, the {@link StunMessage#MAGIC_COOKIE} cookie, the supplied transaction id, and a zero length
     * attribute section. It encodes to exactly {@value #KEEPALIVE_LENGTH} bytes.
     *
     * @param transactionId the {@value StunMessage#TRANSACTION_ID_LENGTH} byte transaction id
     * @return the encoded keepalive message
     * @throws NullPointerException     if {@code transactionId} is {@code null}
     * @throws IllegalArgumentException if {@code transactionId} is not the required length
     */
    public static byte[] keepalive(byte[] transactionId) {
        Objects.requireNonNull(transactionId, "transactionId cannot be null");
        if (Log.TRACE) LOGGER.log(Level.TRACE, "call keepalive built txid={0}", transactionId);
        return new StunMessage(StunMessage.TYPE_KEEPALIVE, StunMessage.MAGIC_COOKIE, transactionId, List.of())
                .encode();
    }

    /**
     * Encodes the cleartext value of the {@link StunAttributeType#WA_SUBSCRIPTION} attribute from a
     * subscription map.
     *
     * <p>The value is the {@link StreamSubscriptions} protobuf serialized through
     * {@link StreamSubscriptionsSpec}, unpadded; it is the exact byte sequence the {@code 0x4024} STUN
     * attribute length field reports, before the four byte attribute padding the STUN writer applies. This
     * is the decodable half of the {@code 0x0003} subscription envelope.
     *
     * @param subscriptions the combined per stream subscription map; never {@code null}
     * @return the serialized subscription protobuf bytes
     * @throws NullPointerException if {@code subscriptions} is {@code null}
     */
    public static byte[] subscriptionAttributeValue(StreamSubscriptions subscriptions) {
        Objects.requireNonNull(subscriptions, "subscriptions cannot be null");
        return StreamSubscriptionsSpec.encode(subscriptions);
    }

    /**
     * Frames the {@link StunAttributeType#WA_SUBSCRIPTION} attribute from a subscription map.
     *
     * <p>Pairs the {@code 0x4024} attribute type with the {@linkplain #subscriptionAttributeValue(StreamSubscriptions)
     * cleartext subscription value} so a STUN message writer can append it as a type length value entry. It is
     * the subscription attribute of the {@code 0x0003} envelope
     * {@link #subscriptionEnvelope(byte[], StreamSubscriptions, InetSocketAddress, byte[]) assembles}.
     *
     * @param subscriptions the combined per stream subscription map; never {@code null}
     * @return the framed {@link StunMessage.Attribute} carrying the subscription protobuf
     * @throws NullPointerException if {@code subscriptions} is {@code null}
     */
    public static StunMessage.Attribute subscriptionAttribute(StreamSubscriptions subscriptions) {
        return new StunMessage.Attribute(
                StunAttributeType.WA_SUBSCRIPTION, subscriptionAttributeValue(subscriptions));
    }

    /**
     * Frames the {@link StunAttributeType#WA_XOR_MAPPED_ADDRESS} attribute carrying the relay's reflexive
     * transport address.
     *
     * <p>Pairs the {@code 0x0016} attribute type with the relay reflexive address encoded in the STUN
     * XOR MAPPED ADDRESS form ({@link StunMessage#encodeXorMappedAddress(InetSocketAddress, byte[])}), the
     * relay address the SFU echoes back inside the {@code 0x0003} subscription envelope. The
     * {@code transactionId} is the envelope's transaction id, which XORs the trailing twelve bytes of an
     * IPv6 address (and is unused for IPv4). It is the reflexive address attribute of the {@code 0x0003}
     * envelope {@link #subscriptionEnvelope(byte[], StreamSubscriptions, InetSocketAddress, byte[]) assembles}.
     *
     * @param reflexiveAddress the relay's reflexive transport address; never {@code null}
     * @param transactionId    the {@value StunMessage#TRANSACTION_ID_LENGTH} byte envelope transaction id
     * @return the framed {@link StunMessage.Attribute} carrying the XOR MAPPED ADDRESS value
     * @throws NullPointerException     if {@code reflexiveAddress} or {@code transactionId} is {@code null}
     * @throws IllegalArgumentException if {@code transactionId} is not the required length
     */
    public static StunMessage.Attribute xorMappedAddressAttribute(InetSocketAddress reflexiveAddress,
                                                                  byte[] transactionId) {
        Objects.requireNonNull(reflexiveAddress, "reflexiveAddress cannot be null");
        return new StunMessage.Attribute(StunAttributeType.WA_XOR_MAPPED_ADDRESS,
                StunMessage.encodeXorMappedAddress(reflexiveAddress, transactionId));
    }

    /**
     * Assembles a complete {@code 0x0003} subscription envelope, message integrity protected with the relay
     * {@code <key>}, with a fresh random transaction id.
     *
     * <p>The emitted message is {@code type 0x0003 + magic + txid + WA_SUBSCRIPTION(0x4024) +
     * WA_XOR_MAPPED_ADDRESS(0x0016) + MESSAGE_INTEGRITY(0x0008)}, the three attribute form, and is otherwise
     * identical to {@link #subscriptionEnvelope(byte[], StreamSubscriptions, InetSocketAddress, byte[])} with
     * a transaction id drawn from {@link VoipCryptoNative#randomBytes(int)}.
     *
     * @param relayKey         the relay {@code <key>} keying the {@code 0x0008} HMAC SHA1, in raw bytes
     * @param subscriptions    the combined per stream subscription map carried as {@code 0x4024}; never
     *                         {@code null}
     * @param reflexiveAddress the relay reflexive transport address carried as {@code 0x0016}; never
     *                         {@code null}
     * @return the encoded, message integrity protected {@code 0x0003} envelope bytes
     * @throws NullPointerException        if any argument is {@code null}
     * @throws WhatsAppCallException.Srtp  if the platform cannot compute the HMAC SHA1 message integrity
     */
    public static byte[] subscriptionEnvelope(byte[] relayKey,
                                              StreamSubscriptions subscriptions,
                                              InetSocketAddress reflexiveAddress) {
        return subscriptionEnvelope(relayKey, subscriptions, reflexiveAddress,
                VoipCryptoNative.randomBytes(StunMessage.TRANSACTION_ID_LENGTH));
    }

    /**
     * Assembles a complete {@code 0x0003} subscription envelope, message integrity protected with the relay
     * {@code <key>}, with the given transaction id.
     *
     * <p>The emitted message is {@code type 0x0003 + magic + txid + WA_SUBSCRIPTION(0x4024) +
     * WA_XOR_MAPPED_ADDRESS(0x0016) + MESSAGE_INTEGRITY(0x0008)}, the three attribute form. It frames the
     * subscription map as the {@code 0x4024} attribute and delegates to
     * {@link #subscriptionEnvelope(byte[], StunMessage.Attribute, InetSocketAddress, byte[])}.
     *
     * @param relayKey         the relay {@code <key>} keying the {@code 0x0008} HMAC SHA1, in raw bytes
     * @param subscriptions    the combined per stream subscription map carried as {@code 0x4024}; never
     *                         {@code null}
     * @param reflexiveAddress the relay reflexive transport address carried as {@code 0x0016}; never
     *                         {@code null}
     * @param transactionId    the {@value StunMessage#TRANSACTION_ID_LENGTH} byte envelope transaction id
     * @return the encoded, message integrity protected {@code 0x0003} envelope bytes
     * @throws NullPointerException        if any argument is {@code null}
     * @throws IllegalArgumentException    if {@code transactionId} is not the required length
     * @throws WhatsAppCallException.Srtp  if the platform cannot compute the HMAC SHA1 message integrity
     */
    public static byte[] subscriptionEnvelope(byte[] relayKey,
                                              StreamSubscriptions subscriptions,
                                              InetSocketAddress reflexiveAddress,
                                              byte[] transactionId) {
        Objects.requireNonNull(subscriptions, "subscriptions cannot be null");
        return subscriptionEnvelope(
                relayKey, subscriptionAttribute(subscriptions), reflexiveAddress, transactionId);
    }

    /**
     * Assembles a complete {@code 0x0003} subscription envelope around an already framed subscription attribute,
     * message integrity protected with the relay {@code <key>}.
     *
     * <p>This is the generic assembly core the {@link StreamSubscriptions} typed overloads delegate to: it
     * lays out the supplied subscription attribute, the {@code 0x0016} relay reflexive address, and the
     * trailing {@code 0x0008} HMAC SHA1 message integrity in order, with the header length field rewritten to
     * span the integrity attribute and the integrity computed over exactly the attributes assembled, keyed by
     * {@code relayKey}; the envelope carries no {@link StunAttributeType#FINGERPRINT}. The
     * {@code subscriptionAttribute} is framed by the caller, so the snapshot form receiver or sender
     * subscription attribute ({@link StunAttributeType#WA_RECEIVER_SUBSCRIPTION} /
     * {@link StunAttributeType#WA_SENDER_SUBSCRIPTIONS}) and the fused {@link StunAttributeType#WA_SUBSCRIPTION}
     * attribute both reach the wire through this core.
     *
     * @param relayKey             the relay {@code <key>} keying the {@code 0x0008} HMAC SHA1, in raw bytes
     * @param subscriptionAttribute the framed subscription attribute; never {@code null}
     * @param reflexiveAddress     the relay reflexive transport address carried as {@code 0x0016}; never
     *                             {@code null}
     * @param transactionId        the {@value StunMessage#TRANSACTION_ID_LENGTH} byte envelope transaction id
     * @return the encoded, message integrity protected {@code 0x0003} envelope bytes
     * @throws NullPointerException        if {@code relayKey}, {@code subscriptionAttribute},
     *                                     {@code reflexiveAddress}, or {@code transactionId} is {@code null}
     * @throws IllegalArgumentException    if {@code transactionId} is not the required length
     * @throws WhatsAppCallException.Srtp  if the platform cannot compute the HMAC SHA1 message integrity
     */
    public static byte[] subscriptionEnvelope(byte[] relayKey,
                                              StunMessage.Attribute subscriptionAttribute,
                                              InetSocketAddress reflexiveAddress,
                                              byte[] transactionId) {
        Objects.requireNonNull(relayKey, "relayKey cannot be null");
        Objects.requireNonNull(subscriptionAttribute, "subscriptionAttribute cannot be null");
        Objects.requireNonNull(reflexiveAddress, "reflexiveAddress cannot be null");
        Objects.requireNonNull(transactionId, "transactionId cannot be null");
        var attributes = new ArrayList<StunMessage.Attribute>(2);
        attributes.add(subscriptionAttribute);
        attributes.add(xorMappedAddressAttribute(reflexiveAddress, transactionId));
        return assembleEnvelope(relayKey, attributes, transactionId);
    }

    /**
     * Assembles a complete {@code 0x0003} subscription envelope carrying the leading {@code 0x4000}
     * RELAY TOKEN attribute, message integrity protected with the relay {@code <key>}.
     *
     * <p>The emitted message is {@code type 0x0003 + magic + txid + RELAY_TOKEN(0x4000) +
     * WA_SUBSCRIPTION(0x4024) + WA_XOR_MAPPED_ADDRESS(0x0016) + MESSAGE_INTEGRITY(0x0008)}, the four attribute
     * form the relay requires: the relay authenticates the envelope by binding the credential the leading
     * {@code 0x4000} relay {@code <token>} references, then verifying the trailing {@code 0x0008} HMAC SHA1
     * keyed by the relay {@code <key>} used verbatim. Omitting the {@code 0x4000} token makes the relay reject
     * the envelope with {@code "Integrity failure: Hmac mismatch"} even when the key is correct.
     *
     * @param relayKey         the relay {@code <key>} keying the {@code 0x0008} HMAC SHA1, used verbatim
     * @param relayToken       the relay {@code <token>} bytes carried as the leading {@code 0x4000}
     *                         RELAY TOKEN attribute; never {@code null}
     * @param subscriptions    the combined per stream subscription map carried as {@code 0x4024}; never
     *                         {@code null}
     * @param reflexiveAddress the relay reflexive transport address carried as {@code 0x0016}; never
     *                         {@code null}
     * @param transactionId    the {@value StunMessage#TRANSACTION_ID_LENGTH} byte envelope transaction id
     * @return the encoded, message integrity protected {@code 0x0003} envelope bytes
     * @throws NullPointerException        if any argument is {@code null}
     * @throws IllegalArgumentException    if {@code transactionId} is not the required length
     * @throws WhatsAppCallException.Srtp  if the platform cannot compute the HMAC SHA1 message integrity
     */
    public static byte[] subscriptionEnvelope(byte[] relayKey,
                                              byte[] relayToken,
                                              StreamSubscriptions subscriptions,
                                              InetSocketAddress reflexiveAddress,
                                              byte[] transactionId) {
        Objects.requireNonNull(relayKey, "relayKey cannot be null");
        Objects.requireNonNull(relayToken, "relayToken cannot be null");
        Objects.requireNonNull(subscriptions, "subscriptions cannot be null");
        Objects.requireNonNull(reflexiveAddress, "reflexiveAddress cannot be null");
        Objects.requireNonNull(transactionId, "transactionId cannot be null");
        var attributes = new ArrayList<StunMessage.Attribute>(3);
        attributes.add(relayTokenAttribute(relayToken));
        attributes.add(subscriptionAttribute(subscriptions));
        attributes.add(xorMappedAddressAttribute(reflexiveAddress, transactionId));
        return assembleEnvelope(relayKey, attributes, transactionId);
    }

    /**
     * Encodes the cleartext value of the {@link StunAttributeType#WA_SUBSCRIPTION} attribute from the local
     * stream descriptors.
     *
     * <p>The value is the {@link StreamDescriptors} protobuf serialized through {@link StreamDescriptorsSpec},
     * unpadded. This is the descriptor form of the {@code 0x4024} attribute: a flat descriptor list declaring
     * this client's own send streams, each a {@code (stream layer, payload type, SSRC)} entry, that the relay
     * reads to forward this client's media.
     *
     * @param descriptors the local send stream descriptors; never {@code null}
     * @return the serialized descriptor protobuf bytes
     * @throws NullPointerException if {@code descriptors} is {@code null}
     */
    public static byte[] streamDescriptorsAttributeValue(StreamDescriptors descriptors) {
        Objects.requireNonNull(descriptors, "descriptors cannot be null");
        return StreamDescriptorsSpec.encode(descriptors);
    }

    /**
     * Frames the {@link StunAttributeType#WA_SUBSCRIPTION} attribute from the local stream descriptors.
     *
     * <p>Pairs the {@code 0x4024} attribute type with the
     * {@linkplain #streamDescriptorsAttributeValue(StreamDescriptors) cleartext descriptor value} so a STUN
     * message writer can append it as a type length value entry.
     *
     * @param descriptors the local send stream descriptors; never {@code null}
     * @return the framed {@link StunMessage.Attribute} carrying the descriptor protobuf
     * @throws NullPointerException if {@code descriptors} is {@code null}
     */
    public static StunMessage.Attribute streamDescriptorsAttribute(StreamDescriptors descriptors) {
        return new StunMessage.Attribute(
                StunAttributeType.WA_SUBSCRIPTION, streamDescriptorsAttributeValue(descriptors));
    }

    /**
     * Assembles a complete {@code 0x0003} subscription envelope carrying the leading {@code 0x4000}
     * RELAY TOKEN attribute and this client's stream descriptors as the {@code 0x4024} attribute,
     * message integrity protected with the relay {@code <key>}.
     *
     * <p>The emitted message is {@code type 0x0003 + magic + txid + RELAY_TOKEN(0x4000) +
     * WA_SUBSCRIPTION(0x4024) + WA_XOR_MAPPED_ADDRESS(0x0016) + MESSAGE_INTEGRITY(0x0008)}, identical to the
     * {@link #subscriptionEnvelope(byte[], byte[], StreamSubscriptions, InetSocketAddress, byte[]) token form}
     * except the {@code 0x4024} attribute carries the {@link StreamDescriptors} descriptor list (this
     * client's own send streams: audio plus both simulcast video layers, each a media, FEC, and NACK triple)
     * in place of the per (participant, stream) subscription map. The descriptor list is the form the relay
     * reads to forward this client's media.
     *
     * @param relayKey         the relay {@code <key>} keying the {@code 0x0008} HMAC SHA1, used verbatim
     * @param relayToken       the relay {@code <token>} bytes carried as the leading {@code 0x4000}
     *                         RELAY TOKEN attribute; never {@code null}
     * @param descriptors      the local send stream descriptors carried as {@code 0x4024}; never {@code null}
     * @param reflexiveAddress the relay reflexive transport address carried as {@code 0x0016}; never
     *                         {@code null}
     * @param transactionId    the {@value StunMessage#TRANSACTION_ID_LENGTH} byte envelope transaction id
     * @return the encoded, message integrity protected {@code 0x0003} envelope bytes
     * @throws NullPointerException        if any argument is {@code null}
     * @throws IllegalArgumentException    if {@code transactionId} is not the required length
     * @throws WhatsAppCallException.Srtp  if the platform cannot compute the HMAC SHA1 message integrity
     */
    public static byte[] subscriptionEnvelope(byte[] relayKey,
                                              byte[] relayToken,
                                              StreamDescriptors descriptors,
                                              InetSocketAddress reflexiveAddress,
                                              byte[] transactionId) {
        Objects.requireNonNull(relayKey, "relayKey cannot be null");
        Objects.requireNonNull(relayToken, "relayToken cannot be null");
        Objects.requireNonNull(descriptors, "descriptors cannot be null");
        Objects.requireNonNull(reflexiveAddress, "reflexiveAddress cannot be null");
        Objects.requireNonNull(transactionId, "transactionId cannot be null");
        var attributes = new ArrayList<StunMessage.Attribute>(3);
        attributes.add(relayTokenAttribute(relayToken));
        attributes.add(streamDescriptorsAttribute(descriptors));
        attributes.add(xorMappedAddressAttribute(reflexiveAddress, transactionId));
        return assembleEnvelope(relayKey, attributes, transactionId);
    }

    /**
     * Encodes the cleartext value of the {@link StunAttributeType#WA_SENDER_SUBSCRIPTIONS} attribute from this
     * client's sender subscriptions.
     *
     * <p>The value is the {@link SenderSubscriptions} protobuf serialized through {@link SenderSubscriptionsSpec},
     * unpadded. This is the {@code 0x4025} form of the subscription attribute: a list of SSRC to PID assignment
     * sources declaring this client's own send streams (the two simulcast video streams, the audio stream, and
     * the app data stream), each binding the streams' SSRCs to the relay assigned self participant id and the
     * SVC temporal layer, that the relay reads to map this client's forwarded media back to its sender and layer.
     *
     * @param senderSubscriptions this client's own send stream SSRC to PID assignments; never {@code null}
     * @return the serialized sender subscription protobuf bytes
     * @throws NullPointerException if {@code senderSubscriptions} is {@code null}
     */
    public static byte[] senderSubscriptionsAttributeValue(SenderSubscriptions senderSubscriptions) {
        Objects.requireNonNull(senderSubscriptions, "senderSubscriptions cannot be null");
        return SenderSubscriptionsSpec.encode(senderSubscriptions);
    }

    /**
     * Frames the {@link StunAttributeType#WA_SENDER_SUBSCRIPTIONS} attribute from this client's sender
     * subscriptions.
     *
     * <p>Pairs the {@code 0x4025} attribute type with the
     * {@linkplain #senderSubscriptionsAttributeValue(SenderSubscriptions) cleartext sender subscription value} so
     * a STUN message writer can append it as a type length value entry. It is the subscription attribute of the
     * {@code 0x0003} envelope {@link #subscriptionEnvelope(byte[], byte[], SenderSubscriptions, InetSocketAddress,
     * byte[]) assembles}.
     *
     * @param senderSubscriptions this client's own send stream SSRC to PID assignments; never {@code null}
     * @return the framed {@link StunMessage.Attribute} carrying the sender subscription protobuf
     * @throws NullPointerException if {@code senderSubscriptions} is {@code null}
     */
    public static StunMessage.Attribute senderSubscriptionsAttribute(SenderSubscriptions senderSubscriptions) {
        return new StunMessage.Attribute(
                StunAttributeType.WA_SENDER_SUBSCRIPTIONS, senderSubscriptionsAttributeValue(senderSubscriptions));
    }

    /**
     * Assembles a complete {@code 0x0003} subscription envelope carrying the leading {@code 0x4000} RELAY TOKEN
     * attribute and this client's sender subscriptions as the {@code 0x4025} attribute, message integrity
     * protected with the relay {@code <key>}.
     *
     * <p>The emitted message is {@code type 0x0003 + magic + txid + RELAY_TOKEN(0x4000) +
     * WA_SENDER_SUBSCRIPTIONS(0x4025) + WA_XOR_MAPPED_ADDRESS(0x0016) + MESSAGE_INTEGRITY(0x0008)}, the sender
     * subscription form of the four attribute envelope: it is identical to the
     * {@link #subscriptionEnvelope(byte[], byte[], StreamDescriptors, InetSocketAddress, byte[]) descriptor form}
     * except the subscription attribute is the {@code 0x4025} {@link SenderSubscriptions} (SSRC to PID
     * assignments) in place of the {@code 0x4024} attribute. The relay authenticates the envelope by binding the
     * credential the leading {@code 0x4000} relay {@code <token>} references, then verifying the trailing
     * {@code 0x0008} HMAC SHA1 keyed by the relay {@code <key>} used verbatim.
     *
     * @param relayKey            the relay {@code <key>} keying the {@code 0x0008} HMAC SHA1, used verbatim
     * @param relayToken          the relay {@code <token>} bytes carried as the leading {@code 0x4000}
     *                            RELAY TOKEN attribute; never {@code null}
     * @param senderSubscriptions this client's own send stream SSRC to PID assignments carried as {@code 0x4025};
     *                            never {@code null}
     * @param reflexiveAddress    the relay reflexive transport address carried as {@code 0x0016}; never
     *                            {@code null}
     * @param transactionId       the {@value StunMessage#TRANSACTION_ID_LENGTH} byte envelope transaction id
     * @return the encoded, message integrity protected {@code 0x0003} envelope bytes
     * @throws NullPointerException        if any argument is {@code null}
     * @throws IllegalArgumentException    if {@code transactionId} is not the required length
     * @throws WhatsAppCallException.Srtp  if the platform cannot compute the HMAC SHA1 message integrity
     */
    public static byte[] subscriptionEnvelope(byte[] relayKey,
                                              byte[] relayToken,
                                              SenderSubscriptions senderSubscriptions,
                                              InetSocketAddress reflexiveAddress,
                                              byte[] transactionId) {
        Objects.requireNonNull(relayKey, "relayKey cannot be null");
        Objects.requireNonNull(relayToken, "relayToken cannot be null");
        Objects.requireNonNull(senderSubscriptions, "senderSubscriptions cannot be null");
        Objects.requireNonNull(reflexiveAddress, "reflexiveAddress cannot be null");
        Objects.requireNonNull(transactionId, "transactionId cannot be null");
        var attributes = new ArrayList<StunMessage.Attribute>(3);
        attributes.add(relayTokenAttribute(relayToken));
        attributes.add(senderSubscriptionsAttribute(senderSubscriptions));
        attributes.add(xorMappedAddressAttribute(reflexiveAddress, transactionId));
        return assembleEnvelope(relayKey, attributes, transactionId);
    }

    /**
     * Frames the leading {@code 0x4000} RELAY TOKEN attribute carrying the relay {@code <token>} bytes.
     *
     * <p>The relay {@code <token>} is the server issued credential the relay binds before verifying the
     * envelope's message integrity; it is carried verbatim as the attribute value.
     *
     * @implNote This implementation writes the raw attribute type {@code 0x4000} rather than a named
     * {@link StunAttributeType} constant: the {@code 0x4000} constant is named {@code WA_WARP_MESSAGE},
     * which does not describe the relay token role this attribute plays here.
     *
     * @param relayToken the relay {@code <token>} bytes; never {@code null}
     * @return the framed {@link StunMessage.Attribute} carrying the relay token
     * @throws NullPointerException if {@code relayToken} is {@code null}
     */
    public static StunMessage.Attribute relayTokenAttribute(byte[] relayToken) {
        Objects.requireNonNull(relayToken, "relayToken cannot be null");
        return new StunMessage.Attribute(0x4000, relayToken);
    }

    /**
     * Encodes a {@code 0x0003} subscription envelope from its ordered attributes, appending the trailing
     * {@code 0x0008} HMAC SHA1 message integrity keyed by the relay {@code <key>}.
     *
     * <p>The attributes are laid out in the given order, the header length field is rewritten to span the
     * integrity attribute, and the integrity is computed over exactly the attributes assembled; the envelope
     * carries no {@link StunAttributeType#FINGERPRINT}.
     *
     * @param relayKey     the relay {@code <key>} keying the {@code 0x0008} HMAC SHA1, used verbatim
     * @param attributes   the ordered envelope attributes preceding the message integrity; never {@code null}
     * @param transactionId the {@value StunMessage#TRANSACTION_ID_LENGTH} byte envelope transaction id
     * @return the encoded, message integrity protected {@code 0x0003} envelope bytes
     * @throws WhatsAppCallException.Srtp if the platform cannot compute the HMAC SHA1 message integrity
     */
    private static byte[] assembleEnvelope(byte[] relayKey,
                                           List<StunMessage.Attribute> attributes,
                                           byte[] transactionId) {
        var prefix = new StunMessage(
                StunMessage.TYPE_SUBSCRIPTION, StunMessage.MAGIC_COOKIE, transactionId, attributes)
                .encode();
        var integrity = StunIntegrity.computeMessageIntegrity(prefix, relayKey);
        var out = new byte[prefix.length + MESSAGE_INTEGRITY_ATTR_SIZE];
        System.arraycopy(prefix, 0, out, 0, prefix.length);
        writeMessageIntegrityAttribute(out, prefix.length, integrity);
        var attributeSectionLength = out.length - StunMessage.HEADER_LENGTH;
        WarpCodecSupport.putU16(out, STUN_LENGTH_FIELD_OFFSET, attributeSectionLength);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "subscription envelope assembled attributes={0} bytes={1}",
                    attributes.size(), out.length);
        }
        return out;
    }

    /**
     * Writes a {@link StunAttributeType#MESSAGE_INTEGRITY} attribute into a buffer at the given offset.
     *
     * <p>The attribute is the four byte type length header ({@code 0x0008}, length
     * {@value StunIntegrity#MESSAGE_INTEGRITY_LENGTH}) followed by the twenty byte HMAC SHA1 value; the value
     * is a multiple of four bytes, so no trailing padding is written.
     *
     * @param out       the destination buffer, sized to hold the attribute at {@code offset}
     * @param offset    the byte offset at which to write the attribute header
     * @param integrity the {@value StunIntegrity#MESSAGE_INTEGRITY_LENGTH} byte HMAC SHA1 value
     */
    private static void writeMessageIntegrityAttribute(byte[] out, int offset, byte[] integrity) {
        var type = StunAttributeType.MESSAGE_INTEGRITY.value();
        WarpCodecSupport.putU16(out, offset, type);
        WarpCodecSupport.putU16(out, offset + 2, StunIntegrity.MESSAGE_INTEGRITY_LENGTH);
        System.arraycopy(integrity, 0, out, offset + 4, StunIntegrity.MESSAGE_INTEGRITY_LENGTH);
    }
}
