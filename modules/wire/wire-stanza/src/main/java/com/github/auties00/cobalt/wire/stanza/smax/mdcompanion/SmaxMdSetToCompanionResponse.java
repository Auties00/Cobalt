package com.github.auties00.cobalt.wire.stanza.smax.mdcompanion;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the inbound
 * {@code <iq xmlns="md" type="set"><pair-device><ref/>...<ref/></pair-device></iq>} stanza pushed
 * to a companion at the start of a QR-code pair-device handshake.
 *
 * <p>The companion consumes this projection to receive the six rotating {@code <ref/>} byte
 * payloads it feeds into a rolling refresh timer: the first ref is rendered as a QR code on the
 * companion's pairing screen, and each rotation re-renders the code while the socket remains
 * unpaired. An inbound stanza is acknowledged with a
 * {@link SmaxMdSetToCompanionAcknowledgement} built from this projection.
 *
 * @implNote This implementation enforces the same shape as WA Web: the outer tag is {@code iq},
 * the {@code xmlns} is {@code md}, the {@code type} is {@code set}, the {@code from} is the
 * {@code s.whatsapp.net} domain, the {@code <pair-device/>} child is present, and the number of
 * {@code <ref/>} children whose content bytes resolve is exactly six. Empty-content children are
 * dropped before the size check, matching the upstream filter behaviour.
 *
 * @deprecated superseded by the inline companion-pairing flow in {@code IqStreamHandler}.
 */
@Deprecated
@WhatsAppWebModule(moduleName = "WASmaxInMdSetToCompanionRequest")
@WhatsAppWebModule(moduleName = "WASmaxInMdBaseIQSetRequestMixin")
public final class SmaxMdSetToCompanionResponse implements SmaxStanza.Response {
    /**
     * Holds the {@code id} attribute of the inbound IQ stanza.
     *
     * <p>Echoed back into the {@link SmaxMdSetToCompanionAcknowledgement} stanza's {@code id}
     * attribute by
     * {@link SmaxMdSetToCompanionAcknowledgement#from(SmaxMdSetToCompanionResponse)}.
     */
    private final String iqId;

    /**
     * Holds the {@code from} JID of the inbound IQ stanza, always the {@code s.whatsapp.net}
     * server domain.
     *
     * <p>Echoed back into the ack's {@code to} attribute by
     * {@link SmaxMdSetToCompanionAcknowledgement#from(SmaxMdSetToCompanionResponse)}.
     */
    private final Jid iqFrom;

    /**
     * Holds the six rotating pair-device reference byte payloads carried in the inner
     * {@code <pair-device><ref/>...<ref/></pair-device>}.
     *
     * <p>Each ref is surfaced as the active QR-code for one rotation interval before the next is
     * shown; the companion serialises each ref as a length-prefixed string before rendering.
     */
    private final List<byte[]> pairDeviceRefs;

    /**
     * Constructs the typed projection from already-validated component fields.
     *
     * <p>This is the target of {@link #of(Stanza)} after parsing has succeeded. Public visibility is
     * preserved so unit tests can construct fixtures.
     *
     * @param iqId           the IQ id; never {@code null}
     * @param iqFrom         the IQ sender JID; never {@code null}
     * @param pairDeviceRefs the six pair-device ref payloads; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public SmaxMdSetToCompanionResponse(String iqId, Jid iqFrom, List<byte[]> pairDeviceRefs) {
        this.iqId = Objects.requireNonNull(iqId, "iqId cannot be null");
        this.iqFrom = Objects.requireNonNull(iqFrom, "iqFrom cannot be null");
        this.pairDeviceRefs = List.copyOf(Objects.requireNonNull(pairDeviceRefs, "pairDeviceRefs cannot be null"));
    }

    /**
     * Returns the IQ id echoed back as the matching ack's {@code id} attribute.
     *
     * @return the id; never {@code null}
     */
    public String iqId() {
        return iqId;
    }

    /**
     * Returns the IQ sender JID, always the {@code s.whatsapp.net} server domain.
     *
     * <p>The domain is validated by {@link #of(Stanza)} during parsing, and the JID is echoed back
     * as the ack's {@code to} attribute.
     *
     * @return the JID; never {@code null}
     */
    public Jid iqFrom() {
        return iqFrom;
    }

    /**
     * Returns the list of pair-device reference byte payloads.
     *
     * <p>Contains exactly six entries when the projection was parsed by {@link #of(Stanza)};
     * fixtures built through the constructor may carry a different number.
     *
     * @return an unmodifiable list of byte arrays; never {@code null}
     */
    public List<byte[]> pairDeviceRefs() {
        return pairDeviceRefs;
    }

    /**
     * Parses an inbound {@code <iq><pair-device/></iq>} stanza into the typed projection.
     *
     * <p>The companion calls this on every inbound IQ-set whose first child is
     * {@code <pair-device/>}; the result is {@link Optional#empty()} when the stanza shape diverges
     * from the documented schema, rather than an exception.
     *
     * @implNote This implementation enforces tag equality on {@code iq}, attribute literals on
     * {@code xmlns="md"} and {@code type="set"}, a domain-JID literal on {@code from}, presence of
     * an {@code id} attribute, presence of a {@code <pair-device/>} child, and an exact count of
     * six {@code <ref/>} children with non-empty content bytes. Empty-content children are dropped
     * before the size check.
     *
     * @param stanza the inbound IQ stanza
     * @return an {@link Optional} carrying the projection, or empty when the stanza shape diverges
     *         from the schema
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInMdSetToCompanionRequest",
            exports = "parseSetToCompanionRequest",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxMdSetToCompanionResponse> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription("iq")) {
            return Optional.empty();
        }
        if (!stanza.hasAttribute("xmlns", "md")) {
            return Optional.empty();
        }
        if (!stanza.hasAttribute("type", "set")) {
            return Optional.empty();
        }
        var from = stanza.getAttributeAsJid("from").orElse(null);
        if (from == null || !"s.whatsapp.net".equals(from.server().toString())) {
            return Optional.empty();
        }
        var id = stanza.getAttributeAsString("id").orElse(null);
        if (id == null) {
            return Optional.empty();
        }
        var pairDevice = stanza.getChild("pair-device").orElse(null);
        if (pairDevice == null) {
            return Optional.empty();
        }
        var refs = pairDevice.streamChildren("ref")
                .map(ref -> ref.toContentBytes().orElse(null))
                .filter(Objects::nonNull)
                .toList();
        if (refs.size() != 6) {
            return Optional.empty();
        }
        return Optional.of(new SmaxMdSetToCompanionResponse(id, from, refs));
    }

    /**
     * Compares this projection to another object for value equality.
     *
     * <p>Two projections are equal when their IQ id, sender JID, and ref list match, with each
     * ref compared element by element in order.
     *
     * @param obj the object to compare against
     * @return {@code true} if {@code obj} is an equal projection
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxMdSetToCompanionResponse) obj;
        if (!Objects.equals(this.iqId, that.iqId) || !Objects.equals(this.iqFrom, that.iqFrom)) {
            return false;
        }
        if (this.pairDeviceRefs.size() != that.pairDeviceRefs.size()) {
            return false;
        }
        for (var i = 0; i < this.pairDeviceRefs.size(); i++) {
            if (!Arrays.equals(this.pairDeviceRefs.get(i), that.pairDeviceRefs.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * <p>Each ref contributes through {@link Arrays#hashCode(byte[])} so equal contents yield
     * equal codes.
     *
     * @return the hash code derived from the IQ id, sender JID, and ref list
     */
    @Override
    public int hashCode() {
        var result = Objects.hash(iqId, iqFrom);
        for (var ref : pairDeviceRefs) {
            result = 31 * result + Arrays.hashCode(ref);
        }
        return result;
    }

    /**
     * Returns a debug string listing the IQ id, sender JID, and ref list.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        var refsStr = new StringBuilder("[");
        for (var i = 0; i < pairDeviceRefs.size(); i++) {
            if (i > 0) {
                refsStr.append(", ");
            }
            refsStr.append(Arrays.toString(pairDeviceRefs.get(i)));
        }
        refsStr.append(']');
        return "SmaxMdSetToCompanionResponse[iqId=" + iqId
                + ", iqFrom=" + iqFrom
                + ", pairDeviceRefs=" + refsStr + ']';
    }
}
