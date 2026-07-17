package com.github.auties00.cobalt.wire.stanza.smax.waffle;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * Models the outbound Waffle generate-WAEntAC-user request.
 * <p>
 * This request bootstraps a fresh Waffle ("WAEnt-AC") user record on the Facebook side when the local user
 * agrees to a Facebook-linking disclosure flow. The body carries the encrypted payload inside
 * {@link SmaxWaffleRsaEncryptionMetadata} plus the four accepted-disclosure fields (the disclosure record's
 * numeric id and version, and the user's language-group and locale codes). The reply is parsed by
 * {@link SmaxWaffleGenerateWAEntACUserResponse} and returns fresh encryption metadata that decrypts to the
 * linked {@code fbid}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutWaffleGenerateWAEntACUserRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutWaffleBaseIQGetRequestMixin")
public final class SmaxWaffleGenerateWAEntACUserRequest implements SmaxStanza.Request {
    /**
     * Holds the RSA encryption metadata subtree.
     */
    private final SmaxWaffleRsaEncryptionMetadata encryptionMetadata;

    /**
     * Holds the client wall-clock value stamped at request time.
     */
    private final long timestamp;

    /**
     * Holds the numeric id of the legal-disclosure record the user accepted at link time.
     */
    private final int disclosureId;

    /**
     * Holds the version string of the accepted legal-disclosure record.
     */
    private final String disclosureVersion;

    /**
     * Holds the user's language-group code (for example {@code "en"}).
     */
    private final String disclosureLg;

    /**
     * Holds the user's locale code (for example {@code "US"}).
     */
    private final String disclosureLc;

    /**
     * Constructs a generate-WAEntAC-user request from the metadata, timestamp, and four disclosure fields.
     * <p>
     * The four disclosure fields encode the legal acceptance the user supplied in the linking flow.
     *
     * @param encryptionMetadata the RSA encryption metadata; never {@code null}
     * @param timestamp          the request timestamp
     * @param disclosureId       the accepted disclosure record id
     * @param disclosureVersion  the accepted disclosure version; never {@code null}
     * @param disclosureLg       the language-group code; never {@code null}
     * @param disclosureLc       the locale code; never {@code null}
     * @throws NullPointerException if {@code encryptionMetadata}, {@code disclosureVersion},
     *                              {@code disclosureLg}, or {@code disclosureLc} is {@code null}
     */
    public SmaxWaffleGenerateWAEntACUserRequest(SmaxWaffleRsaEncryptionMetadata encryptionMetadata, long timestamp,
                   int disclosureId, String disclosureVersion,
                   String disclosureLg, String disclosureLc) {
        this.encryptionMetadata = Objects.requireNonNull(encryptionMetadata, "encryptionMetadata cannot be null");
        this.timestamp = timestamp;
        this.disclosureId = disclosureId;
        this.disclosureVersion = Objects.requireNonNull(disclosureVersion, "disclosureVersion cannot be null");
        this.disclosureLg = Objects.requireNonNull(disclosureLg, "disclosureLg cannot be null");
        this.disclosureLc = Objects.requireNonNull(disclosureLc, "disclosureLc cannot be null");
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
     * Returns the accepted disclosure record id.
     *
     * @return the id as supplied at construction time
     */
    public int disclosureId() {
        return disclosureId;
    }

    /**
     * Returns the accepted disclosure version.
     *
     * @return the version as supplied at construction time; never {@code null}
     */
    public String disclosureVersion() {
        return disclosureVersion;
    }

    /**
     * Returns the language-group code.
     *
     * @return the code as supplied at construction time; never {@code null}
     */
    public String disclosureLg() {
        return disclosureLg;
    }

    /**
     * Returns the locale code.
     *
     * @return the code as supplied at construction time; never {@code null}
     */
    public String disclosureLc() {
        return disclosureLc;
    }

    /**
     * Builds the outbound IQ stanza ready for dispatch.
     * <p>
     * The result is an {@code <iq xmlns="waffle" smax_id="37" type="get" to="s.whatsapp.net">} envelope
     * carrying the encryption-metadata and timestamp children plus a {@code <disclosure>} child whose
     * attributes hold the four disclosure fields. The dispatch path stamps a fresh {@code id} attribute on
     * every outbound stanza so the reply parser can match it back to this request.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope; never {@code null}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutWaffleGenerateWAEntACUserRequest",
            exports = "makeGenerateWAEntACUserRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var encryptionMetadataNode = encryptionMetadata.toStanza();
        var timestampNode = new StanzaBuilder()
                .description("timestamp")
                .content(timestamp)
                .build();
        var disclosureNode = new StanzaBuilder()
                .description("disclosure")
                .attribute("id", disclosureId)
                .attribute("version", disclosureVersion)
                .attribute("lg", disclosureLg)
                .attribute("lc", disclosureLc)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "waffle")
                .attribute("smax_id", 37)
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(encryptionMetadataNode, timestampNode, disclosureNode);
    }

    /**
     * Returns whether the given object is a {@link SmaxWaffleGenerateWAEntACUserRequest} with equal payload fields.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when every payload field matches
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxWaffleGenerateWAEntACUserRequest) obj;
        return this.timestamp == that.timestamp
                && this.disclosureId == that.disclosureId
                && Objects.equals(this.encryptionMetadata, that.encryptionMetadata)
                && Objects.equals(this.disclosureVersion, that.disclosureVersion)
                && Objects.equals(this.disclosureLg, that.disclosureLg)
                && Objects.equals(this.disclosureLc, that.disclosureLc);
    }

    /**
     * Returns a hash code derived from the six payload fields.
     *
     * @return a content-based hash consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(encryptionMetadata, timestamp, disclosureId,
                disclosureVersion, disclosureLg, disclosureLc);
    }

    /**
     * Returns a debug rendering of this request.
     *
     * @return a human-readable summary; never {@code null}
     */
    @Override
    public String toString() {
        return "SmaxWaffleGenerateWAEntACUserRequest[timestamp=" + timestamp
                + ", disclosureId=" + disclosureId
                + ", disclosureVersion=" + disclosureVersion
                + ", disclosureLg=" + disclosureLg
                + ", disclosureLc=" + disclosureLc + ']';
    }
}
