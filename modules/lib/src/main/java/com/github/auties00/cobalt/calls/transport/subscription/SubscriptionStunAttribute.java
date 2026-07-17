package com.github.auties00.cobalt.calls.transport.subscription;

import com.github.auties00.cobalt.wire.linked.call.datachannel.RxSubscriptions;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SenderSubscriptions;

import java.util.Arrays;
import java.util.Objects;

/**
 * Pairs one serialized subscription protobuf with the proprietary STUN attribute type that frames it.
 *
 * <p>The selective forwarding unit learns each client's send layout and receive wishes from protobufs
 * carried inside STUN binding request attributes rather than from a separate control channel. A
 * {@code SubscriptionStunAttribute} binds the proprietary {@link #attributeType()} to the serialized
 * protobuf {@link #value()} so the STUN message writer can append it as one typed, length prefixed
 * entry. Two attribute types are emitted: {@link #SENDER_SUBSCRIPTIONS_TYPE} carries a serialized
 * {@link SenderSubscriptions} and {@link #RECEIVER_SUBSCRIPTION_TYPE} carries a serialized
 * {@link RxSubscriptions}.
 *
 * <p>The {@link #value()} held here is the unpadded protobuf encoding: it is the exact byte sequence
 * whose length the STUN attribute length field reports. Proprietary STUN attributes are padded to a
 * four byte boundary on the wire, but the padding is applied uniformly by the STUN writer across every
 * attribute, so it is not folded into this value; {@link #paddedLength()} exposes the on wire footprint
 * for buffer sizing without mutating the value.
 *
 * @param attributeType the proprietary STUN attribute type that frames the value
 * @param value         the unpadded serialized subscription protobuf; never {@code null}
 * @implNote This implementation leaves the four byte padding to the STUN writer, which rounds every
 * binary attribute up to a four byte boundary uniformly; folding the padding into this value would
 * double pad it. The attribute type integers are held as constants on this record rather than drawn
 * from a shared STUN attribute type enumeration so the subscription layer carries no compile time
 * dependency on the STUN codec it feeds.
 */
public record SubscriptionStunAttribute(int attributeType, byte[] value) {
    /**
     * The proprietary STUN attribute type framing a serialized {@link SenderSubscriptions}.
     *
     * <p>Identifies the attribute in which a client publishes the SSRC to PID layout of the media it
     * sends, so receivers and the selective forwarding unit can map each forwarded SSRC back to its
     * sender and layer.
     */
    public static final int SENDER_SUBSCRIPTIONS_TYPE = 0x4025;

    /**
     * The proprietary STUN attribute type framing a serialized {@link RxSubscriptions}.
     *
     * <p>Identifies the attribute in which a client publishes which participants and video qualities it
     * wishes to receive, which the selective forwarding unit honours when choosing the simulcast layers
     * to forward.
     */
    public static final int RECEIVER_SUBSCRIPTION_TYPE = 0x4021;

    /**
     * The boundary, in bytes, that a proprietary STUN attribute value is padded up to.
     *
     * <p>The on wire attribute value is zero padded so the next attribute starts on a four byte
     * boundary; {@link #paddedLength()} applies this boundary to report the padded footprint.
     */
    private static final int STUN_ATTRIBUTE_PADDING = 4;

    /**
     * Canonicalizes the record, rejecting a {@code null} value and copying it defensively.
     *
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public SubscriptionStunAttribute {
        Objects.requireNonNull(value, "value cannot be null");
        value = value.clone();
    }

    /**
     * Returns the unpadded serialized subscription protobuf backing this attribute.
     *
     * <p>Overrides the implicit record accessor to return a defensive copy so the stored array cannot be
     * mutated through the returned reference. The returned length is the value the STUN attribute length
     * field reports.
     *
     * @return a copy of the unpadded value bytes; never {@code null}
     */
    @Override
    public byte[] value() {
        return value.clone();
    }

    /**
     * Returns the on wire footprint of this attribute value rounded up to the STUN four byte padding
     * boundary.
     *
     * <p>The STUN writer reports {@link #value()}{@code .length} in the length field and then pads the
     * value with zero bytes up to this rounded length so the following attribute begins aligned. The
     * four byte header carrying the type and length that precedes the value is not included.
     *
     * @return the padded value length in bytes, a multiple of four
     */
    public int paddedLength() {
        return (value.length + STUN_ATTRIBUTE_PADDING - 1) & -STUN_ATTRIBUTE_PADDING;
    }

    /**
     * Compares this attribute with another for equality by attribute type and value content.
     *
     * <p>Two instances are equal when their {@link #attributeType()} matches and their backing value
     * arrays hold the same bytes, compared by content through {@link Arrays#equals(byte[], byte[])}
     * rather than by reference.
     *
     * @param obj the object to compare against
     * @return {@code true} if {@code obj} is a {@code SubscriptionStunAttribute} with an equal type and
     * value content
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof SubscriptionStunAttribute that
                && this.attributeType == that.attributeType
                && Arrays.equals(this.value, that.value));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}, derived from the attribute type and
     * the value content.
     *
     * <p>The value contributes its content hash through {@link Arrays#hashCode(byte[])} so equal byte
     * sequences yield equal codes regardless of array identity.
     *
     * @return the combined hash of the attribute type and the value content
     */
    @Override
    public int hashCode() {
        return Objects.hash(attributeType, Arrays.hashCode(value));
    }

    /**
     * Returns a diagnostic string naming the attribute type in hexadecimal and the unpadded value length.
     *
     * <p>The value bytes themselves are omitted; only their length is reported, so the rendering stays
     * compact and does not expose the serialized subscription payload.
     *
     * @return a human readable summary of this attribute
     */
    @Override
    public String toString() {
        return "SubscriptionStunAttribute[attributeType=0x" + Integer.toHexString(attributeType)
                + ", length=" + value.length + ']';
    }
}
