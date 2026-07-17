package com.github.auties00.cobalt.wire.stanza.smax.waffle;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Models the outbound Waffle get-certificate request.
 * <p>
 * This request fetches the Waffle backend's public certificate set before any encrypted Waffle RPC runs; the
 * certificate public keys are used to wrap outbound action payloads and the password PEM is used when
 * bootstrapping a new linked account. The two boolean flags toggle the optional
 * {@code <payload_enc_certificates/>} and {@code <password_pem/>} markers, telling the relay which PEM subset
 * to return. The reply is parsed by {@link SmaxWaffleGetCertificateResponse}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutWaffleGetCertificateRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutWaffleBaseIQGetRequestMixin")
public final class SmaxWaffleGetCertificateRequest implements SmaxStanza.Request {
    /**
     * Holds the client wall-clock value stamped at request time, in seconds since the Unix epoch.
     */
    private final long timestamp;

    /**
     * Holds whether the {@code <payload_enc_certificates/>} marker is present; when set, the relay also
     * returns the encryption PEM and signature PEM children.
     */
    private final boolean hasPayloadEncCertificates;

    /**
     * Holds whether the {@code <password_pem/>} marker is present; when set, the relay also returns the
     * password PEM child.
     */
    private final boolean hasPasswordPem;

    /**
     * Constructs a get-certificate request from the timestamp and the two PEM-selector flags.
     * <p>
     * The two flags are independent: a request may set both to fetch the full encryption, signature, and
     * password PEM trio, or clear both to fetch the bare timestamp envelope.
     *
     * @param timestamp                 the request timestamp
     * @param hasPayloadEncCertificates whether the {@code <payload_enc_certificates/>} marker is present
     * @param hasPasswordPem            whether the {@code <password_pem/>} marker is present
     */
    public SmaxWaffleGetCertificateRequest(long timestamp, boolean hasPayloadEncCertificates, boolean hasPasswordPem) {
        this.timestamp = timestamp;
        this.hasPayloadEncCertificates = hasPayloadEncCertificates;
        this.hasPasswordPem = hasPasswordPem;
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
     * Reports whether the {@code <payload_enc_certificates/>} marker is present.
     *
     * @return {@code true} when the marker is present
     */
    public boolean hasPayloadEncCertificates() {
        return hasPayloadEncCertificates;
    }

    /**
     * Reports whether the {@code <password_pem/>} marker is present.
     *
     * @return {@code true} when the marker is present
     */
    public boolean hasPasswordPem() {
        return hasPasswordPem;
    }

    /**
     * Builds the outbound IQ stanza ready for dispatch.
     * <p>
     * The result is an {@code <iq xmlns="waffle" smax_id="51" type="get" to="s.whatsapp.net">} envelope
     * carrying a {@code <timestamp/>} child and, conditionally, the {@code <payload_enc_certificates/>} and
     * {@code <password_pem/>} markers; each marker is appended only when its corresponding flag is set. The
     * dispatch path stamps a fresh {@code id} attribute on every outbound stanza so the reply parser can
     * match it back to this request.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope; never {@code null}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutWaffleGetCertificateRequest",
            exports = "makeGetCertificateRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var children = new ArrayList<Stanza>();
        children.add(new StanzaBuilder()
                .description("timestamp")
                .content(timestamp)
                .build());
        if (hasPayloadEncCertificates) {
            children.add(new StanzaBuilder()
                    .description("payload_enc_certificates")
                    .build());
        }
        if (hasPasswordPem) {
            children.add(new StanzaBuilder()
                    .description("password_pem")
                    .build());
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "waffle")
                .attribute("smax_id", 51)
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(children);
    }

    /**
     * Returns whether the given object is a {@link SmaxWaffleGetCertificateRequest} with equal payload fields.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when timestamp and both flags match
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxWaffleGetCertificateRequest) obj;
        return this.timestamp == that.timestamp
                && this.hasPayloadEncCertificates == that.hasPayloadEncCertificates
                && this.hasPasswordPem == that.hasPasswordPem;
    }

    /**
     * Returns a hash code derived from the timestamp and the two flags.
     *
     * @return a content-based hash consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(timestamp, hasPayloadEncCertificates, hasPasswordPem);
    }

    /**
     * Returns a debug rendering of this request.
     *
     * @return a human-readable summary; never {@code null}
     */
    @Override
    public String toString() {
        return "SmaxWaffleGetCertificateRequest[timestamp=" + timestamp
                + ", hasPayloadEncCertificates=" + hasPayloadEncCertificates
                + ", hasPasswordPem=" + hasPasswordPem + ']';
    }
}
