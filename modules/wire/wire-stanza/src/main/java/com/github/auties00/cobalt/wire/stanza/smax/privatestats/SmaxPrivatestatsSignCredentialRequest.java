package com.github.auties00.cobalt.wire.stanza.smax.privatestats;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Arrays;
import java.util.Objects;

/**
 * Models the outbound {@code <iq xmlns="privatestats" type="get">} stanza that asks the relay to sign a blinded ACS credential.
 *
 * <p>This request backs the privatestats anonymous-credential pipeline: the local client mints a
 * blinded credential, the relay signs it with the per-project ACS key, and the signed credential is
 * later spent on a privatestats event submission so the relay can authenticate the report without
 * learning the device identity. The reply is parsed into one of the variants of
 * {@link SmaxPrivatestatsSignCredentialResponse}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutPrivatestatsSignCredentialRequest")
public final class SmaxPrivatestatsSignCredentialRequest implements SmaxStanza.Request {
    /**
     * Holds the blinded credential bytes generated locally by the client.
     */
    private final byte[] blindedCredentialElementValue;

    /**
     * Holds the project-name string identifying the privatestats project the credential is minted for.
     */
    private final String projectNameElementValue;

    /**
     * Constructs a new sign-credential request from the blinded credential bytes and the project name.
     *
     * <p>The pair {@code (projectName, blindedCredential)} carries the two values the relay needs to
     * sign the credential against the correct per-project ACS key.
     *
     * @param blindedCredentialElementValue the blinded credential bytes; never {@code null}
     * @param projectNameElementValue the project name; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public SmaxPrivatestatsSignCredentialRequest(byte[] blindedCredentialElementValue, String projectNameElementValue) {
        this.blindedCredentialElementValue = Objects.requireNonNull(blindedCredentialElementValue,
                "blindedCredentialElementValue cannot be null");
        this.projectNameElementValue = Objects.requireNonNull(projectNameElementValue,
                "projectNameElementValue cannot be null");
    }

    /**
     * Returns the blinded credential bytes.
     *
     * @return the bytes; never {@code null}
     */
    public byte[] blindedCredentialElementValue() {
        return blindedCredentialElementValue;
    }

    /**
     * Returns the project name.
     *
     * @return the project name; never {@code null}
     */
    public String projectNameElementValue() {
        return projectNameElementValue;
    }

    /**
     * Builds the outbound IQ stanza ready for dispatch.
     *
     * <p>The result carries the {@code privatestats} IQ envelope addressed to {@link Jid#userServer()}
     * with a {@code <sign_credential>} child wrapping the {@code <blinded_credential>} and
     * {@code <project_name>} payloads, as shown below; the envelope's {@code id} is stamped by the
     * dispatch path.
     * {@snippet lang="xml" :
     * <iq xmlns="privatestats" type="get" to="s.whatsapp.net">
     *   <sign_credential version="2">
     *     <blinded_credential>BYTES</blinded_credential>
     *     <project_name>STRING</project_name>
     *   </sign_credential>
     * </iq>
     * }
     *
     * @implNote
     * This implementation pins {@code version="2"} on the {@code <sign_credential>} child; the older
     * version-1 shape is not supported.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <sign_credential>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutPrivatestatsSignCredentialRequest",
            exports = "makeSignCredentialRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var blindedCredentialNode = new StanzaBuilder()
                .description("blinded_credential")
                .content(blindedCredentialElementValue)
                .build();
        var projectNameNode = new StanzaBuilder()
                .description("project_name")
                .content(projectNameElementValue)
                .build();
        var signCredentialNode = new StanzaBuilder()
                .description("sign_credential")
                .attribute("version", "2")
                .content(blindedCredentialNode, projectNameNode)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "privatestats")
                .attribute("to", Jid.userServer())
                .attribute("type", "get")
                .content(signCredentialNode);
    }

    /**
     * Returns whether the given object is a {@link SmaxPrivatestatsSignCredentialRequest} with equal blinded credential bytes and project name.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when both fields match
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxPrivatestatsSignCredentialRequest) obj;
        return Arrays.equals(this.blindedCredentialElementValue, that.blindedCredentialElementValue)
                && Objects.equals(this.projectNameElementValue, that.projectNameElementValue);
    }

    /**
     * Returns a hash code derived from the blinded credential bytes and the project name.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        var result = Objects.hash(projectNameElementValue);
        result = 31 * result + Arrays.hashCode(blindedCredentialElementValue);
        return result;
    }

    /**
     * Returns a debug-friendly textual representation of this request.
     *
     * @return the textual representation
     */
    @Override
    public String toString() {
        return "SmaxPrivatestatsSignCredentialRequest[blindedCredentialElementValue="
                + Arrays.toString(blindedCredentialElementValue)
                + ", projectNameElementValue=" + projectNameElementValue + ']';
    }
}
