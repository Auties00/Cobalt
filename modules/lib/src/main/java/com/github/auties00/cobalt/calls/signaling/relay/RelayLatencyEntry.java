package com.github.auties00.cobalt.calls.signaling.relay;

import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Represents one {@code <te>} latency report inside a {@code <relaylatency>} message.
 *
 * <p>A relay latency report carries one such entry per relay the device probed. Each entry reports
 * the measured round trip latency to one relay together with the device's estimated bandwidth toward
 * it and an optional connection identifier. The latency value is named {@code latency} on the wire
 * when it is a plain measurement and {@code xlatency} when it is an encrypted round trip measurement;
 * the {@link #encryptedRtt() encrypted round trip flag} records which name applies. The {@code ul_bw}
 * and {@code dl_bw} attributes are the uplink and downlink bandwidth estimates and are omitted from
 * the wire when zero. The {@code is_favored} attribute marks the relay the device prefers. The
 * {@code conn_id} attribute, present only for a nonzero connection id, is the base64 encoding of an
 * eight byte connection identifier. An optional {@code <enc>} child carries an encrypted measurement
 * blob.
 *
 * <p>On the wire the entry is
 * {@snippet lang="xml" :
 * <te latency="42" ul_bw="500" dl_bw="800" is_favored="1" relay_name="mxp1c01" conn_id="AAAAAAAAAAA=">
 *   <enc>...bytes...</enc>
 * </te>
 * }
 * with {@code latency} replaced by {@code xlatency} when the measurement is an encrypted round trip.
 *
 * @param latency      the measured round trip latency
 * @param encryptedRtt whether the latency is an encrypted round trip measurement, selecting the
 *                     {@code xlatency} attribute name over {@code latency}
 * @param uplinkBw     the uplink bandwidth estimate, or {@code -1} when absent
 * @param downlinkBw   the downlink bandwidth estimate, or {@code -1} when absent
 * @param favored      whether the {@code is_favored} attribute marks this relay as preferred
 * @param relayName    the {@code relay_name} attribute, or {@code null} when absent
 * @param connectionId the eight byte connection identifier, or {@code null} when the entry carries no
 *                     {@code conn_id}
 * @param enc          the {@code <enc>} child measurement blob, or {@code null} when absent
 * @see RelayLatencyStanza
 */
public record RelayLatencyEntry(int latency,
                                boolean encryptedRtt,
                                int uplinkBw,
                                int downlinkBw,
                                boolean favored,
                                String relayName,
                                byte[] connectionId,
                                byte[] enc) {
    /**
     * The wire element tag for a relay latency entry.
     */
    public static final String ELEMENT = "te";

    /**
     * The wire attribute naming a plain round trip latency measurement.
     */
    private static final String LATENCY_ATTRIBUTE = "latency";

    /**
     * The wire attribute naming an encrypted round trip latency measurement.
     */
    private static final String XLATENCY_ATTRIBUTE = "xlatency";

    /**
     * The wire attribute naming the uplink bandwidth estimate.
     */
    private static final String UPLINK_BW_ATTRIBUTE = "ul_bw";

    /**
     * The wire attribute naming the downlink bandwidth estimate.
     */
    private static final String DOWNLINK_BW_ATTRIBUTE = "dl_bw";

    /**
     * The wire attribute marking the favored relay.
     */
    private static final String IS_FAVORED_ATTRIBUTE = "is_favored";

    /**
     * The wire attribute naming the short relay identifier.
     */
    private static final String RELAY_NAME_ATTRIBUTE = "relay_name";

    /**
     * The wire attribute carrying the base64 connection identifier.
     */
    private static final String CONN_ID_ATTRIBUTE = "conn_id";

    /**
     * The wire element tag for the encrypted measurement child.
     */
    private static final String ENC_ELEMENT = "enc";

    /**
     * The value of a boolean attribute that is set.
     */
    private static final String TRUE_LITERAL = "1";

    /**
     * The bit set in the high field of a packed latency to mark the relay measurement valid and the
     * relay reachable.
     *
     * <p>The wire {@code latency}/{@code xlatency} attribute is not a plain millisecond count: it packs
     * a high field, a flag bit, and a signed latency in the low 24 bits. A valid, reachable measurement
     * carries this marker at bit 25, so a decoded report reads as {@code 0x02000000 | latencyMillis}.
     */
    private static final int LATENCY_REACHABLE_MARKER = 1 << 25;

    /**
     * The mask isolating the latency millisecond count in the low 24 bits of a packed latency value.
     */
    private static final int LATENCY_VALUE_MASK = 0xffffff;

    /**
     * The packed latency value standing for an unreachable relay, the maximum positive 24 bit value.
     *
     * <p>This value is emitted for a relay that was probed but could not be reached; a report carrying
     * it marks the relay unreachable rather than fast.
     */
    public static final int LATENCY_UNREACHABLE = 0x7fffff;

    /**
     * Packs a millisecond latency into the wire {@code latency} value.
     *
     * <p>Sets the reachable marker in the high field and carries the latency in the low 24 bits,
     * producing the encoding that {@link #unpackLatencyMillis(int)} inverts on receipt. A negative or
     * oversized latency is masked to 24 bits.
     *
     * @param latencyMillis the round trip latency to encode, in milliseconds
     * @return the packed wire latency value
     */
    public static int packLatency(int latencyMillis) {
        return LATENCY_REACHABLE_MARKER | (latencyMillis & LATENCY_VALUE_MASK);
    }

    /**
     * Unpacks the millisecond latency from a wire {@code latency} value.
     *
     * <p>Recovers the signed latency from the low 24 bits, dropping the high field and flag bit. A
     * packed {@link #LATENCY_UNREACHABLE} value round trips to {@code 0x7fffff}, which
     * {@link #isReachable()} treats as unreachable.
     *
     * @param packed the packed wire latency value
     * @return the latency in milliseconds, sign extended from 24 bits
     */
    public static int unpackLatencyMillis(int packed) {
        var low = packed & LATENCY_VALUE_MASK;
        return (low & 0x800000) != 0 ? low | ~LATENCY_VALUE_MASK : low;
    }

    /**
     * Returns whether this entry reports the relay as reachable.
     *
     * <p>An entry whose packed latency unpacks to {@link #LATENCY_UNREACHABLE} reports a relay the peer
     * probed but could not reach; every other entry reports a reachable relay at the unpacked latency.
     * The relay election only counts a relay a peer can reach, so this gates folding the entry into the
     * reachability matrix.
     *
     * @return {@code true} when the peer can reach the reported relay
     */
    public boolean isReachable() {
        return unpackLatencyMillis(latency) != LATENCY_UNREACHABLE;
    }

    /**
     * Canonicalizes the record components, defensively copying the connection id and encrypted blob so
     * later mutation of a caller supplied array cannot alter this entry.
     */
    public RelayLatencyEntry {
        connectionId = connectionId == null ? null : connectionId.clone();
        enc = enc == null ? null : enc.clone();
    }

    /**
     * Returns the uplink bandwidth estimate, if present.
     *
     * @return an {@link OptionalInt} holding the {@code ul_bw}, or empty when absent
     */
    public OptionalInt uplinkBwValue() {
        return uplinkBw < 0 ? OptionalInt.empty() : OptionalInt.of(uplinkBw);
    }

    /**
     * Returns the downlink bandwidth estimate, if present.
     *
     * @return an {@link OptionalInt} holding the {@code dl_bw}, or empty when absent
     */
    public OptionalInt downlinkBwValue() {
        return downlinkBw < 0 ? OptionalInt.empty() : OptionalInt.of(downlinkBw);
    }

    /**
     * Returns the short relay identifier, if present.
     *
     * @return an {@link Optional} holding the {@code relay_name}, or empty when absent
     */
    public Optional<String> relayNameValue() {
        return Optional.ofNullable(relayName);
    }

    /**
     * Returns a defensive copy of the eight byte connection identifier, if present.
     *
     * @return an {@link Optional} holding a copy of the connection id, or empty when the entry carries
     *         no {@code conn_id}
     */
    public Optional<byte[]> connectionIdValue() {
        return connectionId == null ? Optional.empty() : Optional.of(connectionId.clone());
    }

    /**
     * Returns the connection identifier backing this entry.
     *
     * <p>This accessor overrides the implicit record accessor to return a defensive copy so the stored
     * array cannot be mutated through the returned reference.
     *
     * @return a copy of the connection id, or {@code null} when the entry carries no {@code conn_id}
     */
    @Override
    public byte[] connectionId() {
        return connectionId == null ? null : connectionId.clone();
    }

    /**
     * Returns a defensive copy of the {@code <enc>} child measurement blob, if present.
     *
     * @return an {@link Optional} holding a copy of the encrypted blob, or empty when absent
     */
    public Optional<byte[]> encValue() {
        return enc == null ? Optional.empty() : Optional.of(enc.clone());
    }

    /**
     * Returns the encrypted measurement blob backing this entry.
     *
     * <p>This accessor overrides the implicit record accessor to return a defensive copy so the stored
     * array cannot be mutated through the returned reference.
     *
     * @return a copy of the {@code <enc>} bytes, or {@code null} when absent
     */
    @Override
    public byte[] enc() {
        return enc == null ? null : enc.clone();
    }

    /**
     * Builds the {@code <te>} stanza for this latency entry.
     *
     * <p>The latency attribute is named {@code xlatency} when {@link #encryptedRtt()} is set and
     * {@code latency} otherwise. The bandwidth estimates are omitted when absent, {@code is_favored} is
     * written only when set, and {@code conn_id} is written as the base64 of the connection id only
     * when present. The encrypted blob, when present, becomes an {@code <enc>} child.
     *
     * @return the relay latency entry stanza
     */
    public Stanza toNode() {
        var builder = new StanzaBuilder()
                .description(ELEMENT)
                .attribute(encryptedRtt ? XLATENCY_ATTRIBUTE : LATENCY_ATTRIBUTE, latency)
                .attribute(UPLINK_BW_ATTRIBUTE, uplinkBw, uplinkBw >= 0)
                .attribute(DOWNLINK_BW_ATTRIBUTE, downlinkBw, downlinkBw >= 0)
                .attribute(IS_FAVORED_ATTRIBUTE, TRUE_LITERAL, favored)
                .attribute(RELAY_NAME_ATTRIBUTE, relayName)
                .attribute(CONN_ID_ATTRIBUTE, connectionId == null ? null : Base64.getEncoder().encodeToString(connectionId));
        if (enc != null) {
            builder.content(new StanzaBuilder()
                    .description(ENC_ELEMENT)
                    .content(enc)
                    .build());
        }
        return builder.build();
    }

    /**
     * Decodes a {@code <te>} stanza into a {@link RelayLatencyEntry}.
     *
     * <p>The latency name is resolved by probing {@code xlatency} first and then {@code latency}; the
     * presence of {@code xlatency} sets the encrypted round trip flag. A stanza that is not a
     * {@code <te>} element, or one that carries neither a {@code latency} nor an {@code xlatency}
     * attribute, yields an empty result so callers iterating a mixed child list can skip it. The
     * {@code conn_id} is decoded from base64 when present and parseable; an unparseable {@code conn_id}
     * is treated as absent.
     *
     * @param stanza the relay latency entry stanza
     * @return the decoded entry, or an empty result when the stanza is not a usable {@code <te>} element
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    public static Optional<RelayLatencyEntry> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription(ELEMENT)) {
            return Optional.empty();
        }
        var encryptedRtt = stanza.hasAttribute(XLATENCY_ATTRIBUTE);
        var latencyAttribute = encryptedRtt ? XLATENCY_ATTRIBUTE : LATENCY_ATTRIBUTE;
        var latency = stanza.getAttributeAsInt(latencyAttribute);
        if (latency.isEmpty()) {
            return Optional.empty();
        }
        var uplinkBw = stanza.getAttributeAsInt(UPLINK_BW_ATTRIBUTE, -1);
        var downlinkBw = stanza.getAttributeAsInt(DOWNLINK_BW_ATTRIBUTE, -1);
        var favored = TRUE_LITERAL.equals(stanza.getAttributeAsString(IS_FAVORED_ATTRIBUTE, null));
        var relayName = stanza.getAttributeAsString(RELAY_NAME_ATTRIBUTE, null);
        var connectionId = decodeConnectionId(stanza.getAttributeAsString(CONN_ID_ATTRIBUTE, null));
        var enc = stanza.getChild(ENC_ELEMENT)
                .flatMap(Stanza::toContentBytes)
                .orElse(null);
        return Optional.of(new RelayLatencyEntry(latency.getAsInt(), encryptedRtt, uplinkBw, downlinkBw,
                favored, relayName, connectionId, enc));
    }

    /**
     * Decodes the base64 {@code conn_id} attribute into the eight byte connection identifier.
     *
     * @param encoded the base64 connection id, or {@code null}
     * @return the decoded connection id, or {@code null} when the attribute is absent or not valid
     *         base64
     */
    private static byte[] decodeConnectionId(String encoded) {
        if (encoded == null) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException _) {
            return null;
        }
    }

    /**
     * Compares this entry with another for value equality.
     *
     * <p>This override replaces the record default so the {@code connectionId} and {@code enc} arrays
     * are compared by content rather than by reference; all other components are compared by value.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a {@link RelayLatencyEntry} with equal components
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof RelayLatencyEntry that
                && this.latency == that.latency
                && this.encryptedRtt == that.encryptedRtt
                && this.uplinkBw == that.uplinkBw
                && this.downlinkBw == that.downlinkBw
                && this.favored == that.favored
                && Objects.equals(this.relayName, that.relayName)
                && Arrays.equals(this.connectionId, that.connectionId)
                && Arrays.equals(this.enc, that.enc));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * <p>This override replaces the record default so the {@code connectionId} and {@code enc} arrays
     * contribute their content hashes rather than their identity hashes.
     *
     * @return the hash code for this entry
     */
    @Override
    public int hashCode() {
        return Objects.hash(latency, encryptedRtt, uplinkBw, downlinkBw, favored, relayName,
                Arrays.hashCode(connectionId), Arrays.hashCode(enc));
    }

    /**
     * Returns a textual representation of this entry.
     *
     * <p>The rendering names the latency component {@code xlatency} when {@link #encryptedRtt()} is set
     * and {@code latency} otherwise, and omits the connection id and encrypted blob.
     *
     * @return a string describing this entry
     */
    @Override
    public String toString() {
        return "RelayLatencyEntry[" + (encryptedRtt ? "xlatency" : "latency") + '=' + latency
                + ", ulBw=" + uplinkBw + ", dlBw=" + downlinkBw + ", favored=" + favored
                + ", relayName=" + relayName + ']';
    }
}
