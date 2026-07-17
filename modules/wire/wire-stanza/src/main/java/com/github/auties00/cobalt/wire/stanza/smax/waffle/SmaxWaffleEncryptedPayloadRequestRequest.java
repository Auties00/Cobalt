package com.github.auties00.cobalt.wire.stanza.smax.waffle;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Arrays;
import java.util.Objects;

/**
 * Models the outbound Waffle encrypted-payload-request IQ stanza.
 * <p>
 * This is the generic encrypted-action channel used to dispatch arbitrary linked-account payloads to the
 * Waffle backend; the relay forwards the encrypted action verbatim to the Facebook side and the reply is
 * parsed by {@link SmaxWaffleEncryptedPayloadRequestResponse}. The encrypted payload itself lives inside
 * {@link SmaxWaffleRsaEncryptionMetadata}; {@link #action()} is an opaque selector that the relay forwards
 * unchanged (the known WhatsApp Web selectors are {@code "waffle_100"} for account-linking mutations and
 * {@code "waffle_1"} for crossposting).
 */
@WhatsAppWebModule(moduleName = "WASmaxOutWaffleEncryptedPayloadRequestRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutWaffleBaseIQGetRequestMixin")
public final class SmaxWaffleEncryptedPayloadRequestRequest implements SmaxStanza.Request {
    /**
     * Holds the RSA encryption metadata subtree carrying the encrypted action payload.
     */
    private final SmaxWaffleRsaEncryptionMetadata encryptionMetadata;

    /**
     * Holds the client wall-clock value stamped at request time.
     */
    private final long timestamp;

    /**
     * Holds the linked Facebook account id as opaque bytes.
     */
    private final byte[] fbid;

    /**
     * Holds the opaque action selector bytes forwarded by the relay to the Facebook side.
     */
    private final byte[] action;

    /**
     * Constructs an encrypted-payload-request stanza from its four payload fields.
     *
     * @param encryptionMetadata the RSA encryption metadata; never {@code null}
     * @param timestamp          the request timestamp
     * @param fbid               the linked Facebook account id; never {@code null}
     * @param action             the opaque action selector bytes; never {@code null}
     * @throws NullPointerException if {@code encryptionMetadata}, {@code fbid}, or {@code action} is {@code null}
     */
    public SmaxWaffleEncryptedPayloadRequestRequest(SmaxWaffleRsaEncryptionMetadata encryptionMetadata, long timestamp,
                   byte[] fbid, byte[] action) {
        this.encryptionMetadata = Objects.requireNonNull(encryptionMetadata, "encryptionMetadata cannot be null");
        this.timestamp = timestamp;
        this.fbid = Objects.requireNonNull(fbid, "fbid cannot be null");
        this.action = Objects.requireNonNull(action, "action cannot be null");
    }

    /**
     * Returns the RSA encryption metadata.
     *
     * @return the metadata as supplied at construction time; never {@code null}
     */
    public SmaxWaffleRsaEncryptionMetadata encryptionMetadata() {
        return encryptionMetadata;
    }

    /**
     * Returns the request timestamp.
     *
     * @return the timestamp as supplied at construction time
     */
    public long timestamp() {
        return timestamp;
    }

    /**
     * Returns the linked Facebook account id.
     *
     * @return the id bytes as supplied at construction time; never {@code null}
     */
    public byte[] fbid() {
        return fbid;
    }

    /**
     * Returns the opaque action selector bytes.
     *
     * @return the action bytes as supplied at construction time; never {@code null}
     */
    public byte[] action() {
        return action;
    }

    /**
     * Builds the outbound IQ stanza ready for dispatch.
     * <p>
     * The result is an {@code <iq xmlns="waffle" smax_id="47" type="get" to="s.whatsapp.net">} envelope
     * carrying the encryption-metadata, timestamp, fbid, and action children. The dispatch path stamps a
     * fresh {@code id} attribute on every outbound stanza so the reply parser can match it back to this request.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the four payload children; never {@code null}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutWaffleEncryptedPayloadRequestRequest",
            exports = "makeEncryptedPayloadRequestRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var encryptionMetadataNode = encryptionMetadata.toStanza();
        var timestampNode = new StanzaBuilder()
                .description("timestamp")
                .content(timestamp)
                .build();
        var fbidNode = new StanzaBuilder()
                .description("fbid")
                .content(fbid)
                .build();
        var actionNode = new StanzaBuilder()
                .description("action")
                .content(action)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "waffle")
                .attribute("smax_id", 47)
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(encryptionMetadataNode, timestampNode, fbidNode, actionNode);
    }

    /**
     * Returns whether the given object is a {@link SmaxWaffleEncryptedPayloadRequestRequest} with equal payload fields.
     * <p>
     * Two instances are equal when their metadata, timestamp, fbid, and action all match; the byte-array
     * fields are compared element-wise.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when metadata, timestamp, fbid, and action all match
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxWaffleEncryptedPayloadRequestRequest) obj;
        return this.timestamp == that.timestamp
                && Objects.equals(this.encryptionMetadata, that.encryptionMetadata)
                && Arrays.equals(this.fbid, that.fbid)
                && Arrays.equals(this.action, that.action);
    }

    /**
     * Returns a hash code derived from the four payload fields.
     *
     * @return a content-based hash consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        var result = Objects.hash(encryptionMetadata, timestamp);
        result = 31 * result + Arrays.hashCode(fbid);
        result = 31 * result + Arrays.hashCode(action);
        return result;
    }

    /**
     * Returns a debug rendering of this request.
     * <p>
     * The fbid and action arrays are summarised as byte lengths rather than as raw content.
     *
     * @return a human-readable summary; never {@code null}
     */
    @Override
    public String toString() {
        return "SmaxWaffleEncryptedPayloadRequestRequest[encryptionMetadata=" + encryptionMetadata
                + ", timestamp=" + timestamp
                + ", fbid=" + (fbid != null ? fbid.length + " bytes" : "null")
                + ", action=" + (action != null ? action.length + " bytes" : "null") + ']';
    }
}
