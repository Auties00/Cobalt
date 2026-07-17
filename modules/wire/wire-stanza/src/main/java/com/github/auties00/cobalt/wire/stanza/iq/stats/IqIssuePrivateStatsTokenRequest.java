package com.github.auties00.cobalt.wire.stanza.iq.stats;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.Arrays;
import java.util.Objects;

/**
 * Models the outbound {@code <iq xmlns="privatestats" type="get">} stanza that asks the relay to
 * sign a blinded credential point.
 *
 * <p>This request drives the privacy-preserving analytics pipeline (WhatsApp "Private Stats").
 * A blinded elliptic-curve point and the project name that scopes the credential are carried to
 * the relay, which signs the point and returns the signed-credential bytes; the client then
 * unblinds that signature locally against the random blinding factor it retained, yielding an
 * unlinkable token. The token is later redeemed, one per project, when uploading anonymous
 * metrics, so the relay can confirm the upload came from a valid client without learning which
 * one. The matching reply variants are modelled by {@link IqIssuePrivateStatsTokenResponse}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutPrivatestatsSignCredentialRequest")
public final class IqIssuePrivateStatsTokenRequest implements IqStanza.Request {
    /**
     * Holds the protocol version advertised on the {@code <sign_credential>} tag.
     *
     * <p>The relay bumps this value when its signing scheme changes incompatibly; the current
     * WhatsApp Web bundle pins it to {@code "2"}.
     */
    private static final String SIGN_CREDENTIAL_VERSION = "2";

    /**
     * Holds the raw bytes of the blinded elliptic-curve point.
     *
     * <p>The relay signs this point and returns the signed-credential bytes; the client unblinds
     * the returned signature locally using the random blinding factor that was multiplied into
     * the point at request-build time, producing an unlinkable redeemable token.
     */
    private final byte[] blindedCredential;

    /**
     * Holds the UTF-8 project-name bytes that scope the minted credential to a particular
     * collector.
     *
     * <p>These bytes are routed verbatim into the {@code <project_name>} grandchild. The project
     * name maps one-to-one to the analytics surface the token will be redeemed against, so the
     * relay can mint per-project rate caps.
     */
    private final byte[] projectName;

    /**
     * Constructs a new issue-private-stats-token request from the given blinded point and project
     * name.
     *
     * <p>Both byte arrays are defensively cloned, so subsequent mutation by the caller does not
     * affect the dispatched stanza.
     *
     * @param blindedCredential the blinded credential bytes
     * @param projectName       the project-name bytes
     * @throws NullPointerException if either argument is {@code null}
     */
    public IqIssuePrivateStatsTokenRequest(byte[] blindedCredential, byte[] projectName) {
        this.blindedCredential = Objects.requireNonNull(blindedCredential, "blindedCredential cannot be null").clone();
        this.projectName = Objects.requireNonNull(projectName, "projectName cannot be null").clone();
    }

    /**
     * Returns a defensive copy of the blinded-credential bytes routed into the
     * {@code <blinded_credential>} child.
     *
     * @return a clone of the blinded-credential bytes, never {@code null}
     */
    public byte[] blindedCredential() {
        return blindedCredential.clone();
    }

    /**
     * Returns a defensive copy of the project-name bytes routed into the {@code <project_name>}
     * child.
     *
     * @return a clone of the project-name bytes, never {@code null}
     */
    public byte[] projectName() {
        return projectName.clone();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces an {@code <iq xmlns="privatestats" type="get">} envelope addressed to
     * {@link JidServer#user()} and wrapping a single {@code <sign_credential version="2">} child
     * that carries the {@code <blinded_credential>} and {@code <project_name>} grandchildren in
     * that order.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <iq>} envelope and the
     *         {@code <sign_credential>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutPrivatestatsSignCredentialRequest",
            exports = "makeSignCredentialRequest", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebIssuePrivateStatsToken",
            exports = "getToken", adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var blindedNode = new StanzaBuilder()
                .description("blinded_credential")
                .content(blindedCredential)
                .build();
        var projectNameNode = new StanzaBuilder()
                .description("project_name")
                .content(projectName)
                .build();
        var signCredentialNode = new StanzaBuilder()
                .description("sign_credential")
                .attribute("version", SIGN_CREDENTIAL_VERSION)
                .content(blindedNode, projectNameNode)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "privatestats")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(signCredentialNode);
    }

    /**
     * Compares this request with the given object for equality.
     *
     * <p>Two requests are equal when their blinded-credential and project-name byte arrays are
     * element-wise equal.
     *
     * @param obj the object to compare against
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqIssuePrivateStatsTokenRequest) obj;
        return Arrays.equals(this.blindedCredential, that.blindedCredential)
                && Arrays.equals(this.projectName, that.projectName);
    }

    /**
     * Returns a hash code derived from the blinded-credential and project-name byte arrays.
     *
     * @return the hash code consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(blindedCredential), Arrays.hashCode(projectName));
    }

    /**
     * Returns a diagnostic string carrying the lengths of the two byte arrays.
     *
     * <p>Only the array lengths are rendered, never the raw credential or project-name bytes, so
     * the value can be logged without leaking the blinded point.
     *
     * @return a string describing this request
     */
    @Override
    public String toString() {
        return "IqIssuePrivateStatsTokenRequest[blindedCredentialLength=" + blindedCredential.length
                + ", projectNameLength=" + projectName.length + ']';
    }
}
