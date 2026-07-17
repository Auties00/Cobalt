package com.github.auties00.cobalt.wire.stanza.iq.biz;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;

import java.util.Arrays;
import java.util.Objects;

/**
 * Models the outbound legacy-IQ stanza that submits a re-signed business verified-name certificate.
 *
 * <p>When the local business account changes its display name the client re-issues a
 * {@code VerifiedNameCertificate}, signs it with the Signal identity key, and submits the serialised
 * certificate to the relay. Dispatching this request as an {@link IqStanza.Request} produces the
 * {@code <iq xmlns="w:biz" type="set">} envelope addressed to {@link Jid#userServer()} and wrapping a
 * single {@code <verified_name v="...">} child whose binary content is the encoded certificate. The
 * relay acknowledges the submission with one of the {@link IqSetVerifiedNameResponse} variants, whose
 * {@link IqSetVerifiedNameResponse.Success} carries the echoed verified-name id.
 *
 * <p>This operation adapts the legacy {@code w:biz} verified-name submit path; it carries no
 * {@code com.github.auties00.cobalt.meta.*} provenance annotation because the current WhatsApp Web
 * bundle exposes only the verified-name query (not the certificate submit) through a resolvable
 * module, so no accurate mapping can be declared.
 */
public final class IqSetVerifiedNameRequest implements IqStanza.Request {
    /**
     * Holds the certificate-format version stamped into the {@code v} attribute of the
     * {@code <verified_name>} child.
     *
     * <p>The current certificate format is version {@code 2}; the value is serialised verbatim into
     * the {@code v} attribute.
     */
    private final int version;

    /**
     * Holds the serialised {@code VerifiedNameCertificate} carried as the binary content of the
     * {@code <verified_name>} child.
     *
     * <p>The array is defensively copied on construction and on read so the wire payload cannot be
     * mutated after the request is built. Never {@code null}.
     */
    private final byte[] certificate;

    /**
     * Constructs a set-verified-name request bound to the given certificate-format version and
     * serialised certificate.
     *
     * <p>The certificate bytes are the output of encoding a signed {@code VerifiedNameCertificate};
     * they are copied defensively so a later mutation of the caller's array does not change the wire
     * payload.
     *
     * @param version     the certificate-format version stamped into the {@code v} attribute
     * @param certificate the serialised certificate; never {@code null}
     * @throws NullPointerException if {@code certificate} is {@code null}
     */
    public IqSetVerifiedNameRequest(int version, byte[] certificate) {
        this.version = version;
        Objects.requireNonNull(certificate, "certificate cannot be null");
        this.certificate = certificate.clone();
    }

    /**
     * Returns the certificate-format version stamped into the {@code v} attribute.
     *
     * @return the version
     */
    public int version() {
        return version;
    }

    /**
     * Returns a copy of the serialised certificate carried by this request.
     *
     * <p>A fresh copy is returned on every call so the internal wire payload stays immutable.
     *
     * @return a copy of the certificate bytes; never {@code null}
     */
    public byte[] certificate() {
        return certificate.clone();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces the {@code <iq xmlns="w:biz" type="set">} envelope addressed to
     * {@link Jid#userServer()} wrapping a single {@code <verified_name v="...">} child whose binary
     * content is the serialised certificate held by this request. The IQ {@code id} attribute is
     * assigned by the dispatch layer.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <iq>} envelope and the
     *         {@code <verified_name>} payload
     */
    @Override
    public StanzaBuilder toStanza() {
        var verifiedNameNode = new StanzaBuilder()
                .description("verified_name")
                .attribute("v", version)
                .content(certificate)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("to", Jid.userServer())
                .attribute("type", "set")
                .attribute("xmlns", "w:biz")
                .content(verifiedNameNode);
    }

    /**
     * Compares this request with another object for value equality on the version and certificate
     * bytes.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is an {@link IqSetVerifiedNameRequest} carrying an equal
     *         version and certificate, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqSetVerifiedNameRequest) obj;
        return this.version == that.version
                && Arrays.equals(this.certificate, that.certificate);
    }

    /**
     * Returns a hash code derived from the version and certificate bytes.
     *
     * @return the field-derived hash code
     */
    @Override
    public int hashCode() {
        return 31 * Integer.hashCode(version) + Arrays.hashCode(certificate);
    }

    /**
     * Returns a debug string rendering the version and certificate length.
     *
     * @return a string representation of this request
     */
    @Override
    public String toString() {
        return "IqSetVerifiedNameRequest[version=" + version
                + ", certificate=" + certificate.length + " bytes]";
    }
}
