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
 * Models the outbound Waffle keep-alive ping for a linked Facebook account.
 * <p>
 * This request keeps the linked account session alive on the Waffle backend and learns the relay-chosen
 * next-ping cadence from the reply. The body carries the encrypted payload inside
 * {@link SmaxWaffleRsaEncryptionMetadata} plus the timestamp and {@code fbid}. The reply is parsed by
 * {@link SmaxWaffleWFPingResponse}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutWaffleWFPingRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutWaffleBaseIQGetRequestMixin")
public final class SmaxWaffleWFPingRequest implements SmaxStanza.Request {
    /**
     * Holds the RSA encryption metadata subtree carrying the encrypted ping payload.
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
     * Constructs a Waffle ping request from the metadata, timestamp, and account id.
     *
     * @param encryptionMetadata the RSA encryption metadata; never {@code null}
     * @param timestamp          the request timestamp
     * @param fbid               the linked Facebook account id; never {@code null}
     * @throws NullPointerException if {@code encryptionMetadata} or {@code fbid} is {@code null}
     */
    public SmaxWaffleWFPingRequest(SmaxWaffleRsaEncryptionMetadata encryptionMetadata, long timestamp, byte[] fbid) {
        this.encryptionMetadata = Objects.requireNonNull(encryptionMetadata, "encryptionMetadata cannot be null");
        this.timestamp = timestamp;
        this.fbid = Objects.requireNonNull(fbid, "fbid cannot be null");
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
     * Builds the outbound IQ stanza ready for dispatch.
     * <p>
     * The result is an {@code <iq xmlns="waffle" smax_id="83" type="get" to="s.whatsapp.net">} envelope
     * carrying the encryption-metadata, timestamp, and fbid children. The dispatch path stamps a fresh
     * {@code id} attribute on every outbound stanza so the reply parser can match it back to this request.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the encrypted ping payload; never {@code null}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutWaffleWFPingRequest",
            exports = "makeWFPingRequest", adaptation = WhatsAppAdaptation.DIRECT)
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
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "waffle")
                .attribute("smax_id", 83)
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(encryptionMetadataNode, timestampNode, fbidNode);
    }

    /**
     * Returns whether the given object is a {@link SmaxWaffleWFPingRequest} with equal payload fields.
     * <p>
     * The fbid array is compared element-wise.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when metadata, timestamp, and fbid all match
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxWaffleWFPingRequest) obj;
        return this.timestamp == that.timestamp
                && Objects.equals(this.encryptionMetadata, that.encryptionMetadata)
                && Arrays.equals(this.fbid, that.fbid);
    }

    /**
     * Returns a hash code derived from the three payload fields.
     *
     * @return a content-based hash consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        var result = Objects.hash(encryptionMetadata, timestamp);
        result = 31 * result + Arrays.hashCode(fbid);
        return result;
    }

    /**
     * Returns a debug rendering of this request.
     * <p>
     * The fbid array is summarised as a length rather than as raw bytes.
     *
     * @return a human-readable summary; never {@code null}
     */
    @Override
    public String toString() {
        return "SmaxWaffleWFPingRequest[encryptionMetadata=" + encryptionMetadata
                + ", timestamp=" + timestamp
                + ", fbid=" + (fbid != null ? fbid.length + " bytes" : "null") + ']';
    }
}
