package com.github.auties00.cobalt.wire.stanza.smax.mdcompanion;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the inbound {@code <notification type="link_code_companion_reg" stage="primary_hello">}
 * stanza pushed to a companion mid-flight in the link-code (alt device) pairing flow.
 *
 * <p>The companion consumes this projection to complete the link-code handshake against a
 * primary device that has just typed the eight-character code. The payload carries the wrapped
 * primary ephemeral public key, the primary identity public key, and the pairing reference; the
 * companion derives the shared secret from those three inputs before replying with a
 * {@link SmaxMdSetRegResponseClient} pair-device signature. An inbound notification is
 * acknowledged with a {@link SmaxMdPrimaryHelloNotifyCompanionAcknowledgement} built from this
 * projection.
 *
 * @implNote This implementation maps the WA Web schema field names verbatim onto Java fields so
 * the parser can be diffed line by line against the upstream source, and validates the
 * {@code from} attribute against the {@code s.whatsapp.net} server domain to mirror the
 * upstream domain-JID literal.
 *
 * @deprecated companion-pairing notify handled inline by {@code IqStreamHandler}.
 */
@Deprecated
@WhatsAppWebModule(moduleName = "WASmaxInMdPrimaryHelloNotifyCompanionRequest")
@WhatsAppWebModule(moduleName = "WASmaxInMdServerNotificationMixin")
public final class SmaxMdPrimaryHelloNotifyCompanionResponse implements SmaxStanza.Response {
    /**
     * Holds the {@code id} attribute of the inbound notification stanza.
     *
     * <p>Echoed back into the {@link SmaxMdPrimaryHelloNotifyCompanionAcknowledgement} stanza's
     * {@code id} attribute by
     * {@link SmaxMdPrimaryHelloNotifyCompanionAcknowledgement#from(SmaxMdPrimaryHelloNotifyCompanionResponse)}.
     */
    private final String notificationId;

    /**
     * Holds the {@code from} JID of the inbound notification, always the {@code s.whatsapp.net}
     * server domain.
     *
     * <p>Echoed back as the ack's {@code to} attribute by
     * {@link SmaxMdPrimaryHelloNotifyCompanionAcknowledgement#from(SmaxMdPrimaryHelloNotifyCompanionResponse)}.
     */
    private final Jid notificationFrom;

    /**
     * Holds the wrapped primary ephemeral public-key bytes carried in
     * {@code <link_code_pairing_wrapped_primary_ephemeral_pub/>}.
     *
     * <p>The companion unwraps these with the AES key derived from the eight-character code to
     * recover the primary device's X25519 ephemeral public key, which then feeds the
     * shared-secret derivation that produces the ADV signing key.
     */
    private final byte[] linkCodePairingWrappedPrimaryEphemeralPub;

    /**
     * Holds the primary device's identity public-key bytes carried in {@code <primary_identity_pub/>}.
     *
     * <p>Pinned by the companion as the trusted long-term identity of the primary device before
     * any pair-device-sign signature is sent.
     */
    private final byte[] primaryIdentityPub;

    /**
     * Holds the pairing-reference bytes carried in {@code <link_code_pairing_ref/>}.
     *
     * <p>Matches the {@code ref} the companion previously displayed in its link-code prompt; it
     * is the join-token for the rolling pairing window and is re-issued by
     * {@link SmaxMdRefreshCodeNotifyCompanionResponse} when the window expires.
     */
    private final byte[] linkCodePairingRef;

    /**
     * Constructs the typed projection from already-validated component fields.
     *
     * <p>This is the target of {@link #of(Stanza)} after parsing has succeeded. Public visibility
     * is preserved so unit tests can construct fixtures directly.
     *
     * @param notificationId                            the notification id; never {@code null}
     * @param notificationFrom                          the notification sender JID; never {@code null}
     * @param linkCodePairingWrappedPrimaryEphemeralPub the wrapped ephemeral pubkey bytes; never {@code null}
     * @param primaryIdentityPub                        the primary identity pubkey bytes; never {@code null}
     * @param linkCodePairingRef                        the pairing-reference bytes; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public SmaxMdPrimaryHelloNotifyCompanionResponse(String notificationId,
                   Jid notificationFrom,
                   byte[] linkCodePairingWrappedPrimaryEphemeralPub,
                   byte[] primaryIdentityPub,
                   byte[] linkCodePairingRef) {
        this.notificationId = Objects.requireNonNull(notificationId, "notificationId cannot be null");
        this.notificationFrom = Objects.requireNonNull(notificationFrom, "notificationFrom cannot be null");
        this.linkCodePairingWrappedPrimaryEphemeralPub = Objects.requireNonNull(linkCodePairingWrappedPrimaryEphemeralPub, "linkCodePairingWrappedPrimaryEphemeralPub cannot be null");
        this.primaryIdentityPub = Objects.requireNonNull(primaryIdentityPub, "primaryIdentityPub cannot be null");
        this.linkCodePairingRef = Objects.requireNonNull(linkCodePairingRef, "linkCodePairingRef cannot be null");
    }

    /**
     * Returns the notification id.
     *
     * <p>Used by
     * {@link SmaxMdPrimaryHelloNotifyCompanionAcknowledgement#from(SmaxMdPrimaryHelloNotifyCompanionResponse)}
     * to populate the matching {@code <ack id="..."/>}.
     *
     * @return the id; never {@code null}
     */
    public String notificationId() {
        return notificationId;
    }

    /**
     * Returns the notification sender JID, always the {@code s.whatsapp.net} server domain JID.
     *
     * <p>The domain is validated by {@link #of(Stanza)} during parsing.
     *
     * @return the JID; never {@code null}
     */
    public Jid notificationFrom() {
        return notificationFrom;
    }

    /**
     * Returns the wrapped primary ephemeral public-key bytes.
     *
     * <p>These feed the link-code AES unwrap step that recovers the primary's X25519 ephemeral
     * public key.
     *
     * @return the wrapped pubkey bytes; never {@code null}
     */
    public byte[] linkCodePairingWrappedPrimaryEphemeralPub() {
        return linkCodePairingWrappedPrimaryEphemeralPub;
    }

    /**
     * Returns the primary device's identity public-key bytes.
     *
     * <p>Pinned by the companion as the long-term identity of the primary device before the
     * pair-device-sign signature is generated.
     *
     * @return the identity pubkey bytes; never {@code null}
     */
    public byte[] primaryIdentityPub() {
        return primaryIdentityPub;
    }

    /**
     * Returns the pairing-reference bytes the companion previously displayed.
     *
     * <p>Compare against {@link SmaxMdRefreshCodeNotifyCompanionResponse#linkCodePairingRef()}
     * when a refresh notification arrives to detect a stale ref versus the current ref.
     *
     * @return the pairing-reference bytes; never {@code null}
     */
    public byte[] linkCodePairingRef() {
        return linkCodePairingRef;
    }

    /**
     * Parses a {@code <notification type="link_code_companion_reg" stage="primary_hello"/>}
     * stanza into a typed projection.
     *
     * <p>The companion calls this once per inbound notification of that type to unwrap the
     * primary's pairing payload and continue the link-code handshake. The result is
     * {@link Optional#empty()} when the stanza shape diverges from the documented schema, rather
     * than an exception.
     *
     * @implNote This implementation enforces six structural checks: the tag is
     * {@code notification}, the {@code type} is {@code link_code_companion_reg}, the {@code from}
     * is the {@code s.whatsapp.net} domain, the nested {@code <link_code_companion_reg/>} child
     * carries {@code stage="primary_hello"}, and the three byte-payload children
     * {@code link_code_pairing_wrapped_primary_ephemeral_pub}, {@code primary_identity_pub}, and
     * {@code link_code_pairing_ref} each resolve to non-empty content bytes. Any failure surfaces
     * as {@link Optional#empty()} to mirror the WA Web parser swallowing parsing failures.
     *
     * @param stanza the inbound notification stanza
     * @return an {@link Optional} carrying the typed projection, or empty when the stanza shape
     *         diverges from the schema
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInMdPrimaryHelloNotifyCompanionRequest",
            exports = "parsePrimaryHelloNotifyCompanionRequest",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxMdPrimaryHelloNotifyCompanionResponse> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription("notification")) {
            return Optional.empty();
        }
        if (!stanza.hasAttribute("type", "link_code_companion_reg")) {
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
        var reg = stanza.getChild("link_code_companion_reg").orElse(null);
        if (reg == null || !reg.hasAttribute("stage", "primary_hello")) {
            return Optional.empty();
        }
        var wrappedEphemeral = reg.getChild("link_code_pairing_wrapped_primary_ephemeral_pub")
                .flatMap(Stanza::toContentBytes)
                .orElse(null);
        if (wrappedEphemeral == null) {
            return Optional.empty();
        }
        var identityPub = reg.getChild("primary_identity_pub")
                .flatMap(Stanza::toContentBytes)
                .orElse(null);
        if (identityPub == null) {
            return Optional.empty();
        }
        var pairingRef = reg.getChild("link_code_pairing_ref")
                .flatMap(Stanza::toContentBytes)
                .orElse(null);
        if (pairingRef == null) {
            return Optional.empty();
        }
        return Optional.of(new SmaxMdPrimaryHelloNotifyCompanionResponse(id, from, wrappedEphemeral, identityPub, pairingRef));
    }

    /**
     * Compares this projection to another object for value equality.
     *
     * <p>Two projections are equal when their notification id, sender JID, and all three
     * byte-payload fields match element by element.
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
        var that = (SmaxMdPrimaryHelloNotifyCompanionResponse) obj;
        return Objects.equals(this.notificationId, that.notificationId)
                && Objects.equals(this.notificationFrom, that.notificationFrom)
                && Arrays.equals(this.linkCodePairingWrappedPrimaryEphemeralPub, that.linkCodePairingWrappedPrimaryEphemeralPub)
                && Arrays.equals(this.primaryIdentityPub, that.primaryIdentityPub)
                && Arrays.equals(this.linkCodePairingRef, that.linkCodePairingRef);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * <p>The byte-payload fields contribute through {@link Arrays#hashCode(byte[])} so equal
     * contents yield equal codes.
     *
     * @return the hash code derived from the id, sender JID, and byte payloads
     */
    @Override
    public int hashCode() {
        var result = Objects.hash(notificationId, notificationFrom);
        result = 31 * result + Arrays.hashCode(linkCodePairingWrappedPrimaryEphemeralPub);
        result = 31 * result + Arrays.hashCode(primaryIdentityPub);
        result = 31 * result + Arrays.hashCode(linkCodePairingRef);
        return result;
    }

    /**
     * Returns a debug string listing the id, sender JID, and byte payloads.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxMdPrimaryHelloNotifyCompanionResponse[notificationId=" + notificationId
                + ", notificationFrom=" + notificationFrom
                + ", linkCodePairingWrappedPrimaryEphemeralPub=" + Arrays.toString(linkCodePairingWrappedPrimaryEphemeralPub)
                + ", primaryIdentityPub=" + Arrays.toString(primaryIdentityPub)
                + ", linkCodePairingRef=" + Arrays.toString(linkCodePairingRef) + ']';
    }
}
