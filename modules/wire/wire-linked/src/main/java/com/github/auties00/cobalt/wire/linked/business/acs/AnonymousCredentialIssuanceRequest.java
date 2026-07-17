package com.github.auties00.cobalt.wire.linked.business.acs;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Input model for a batch issuance request against the WhatsApp
 * anonymous-credential service.
 *
 * <p>The anonymous-credential service mints short-lived unlinkable tokens
 * the WhatsApp client redeems against rate-limited backend operations
 * (anti-abuse votes, abuse reporting, and the like) without revealing the
 * caller's identity. The client first blinds a batch of fresh tokens
 * locally; this input asks the server to sign the blinded batch so the
 * client can unblind the result into usable anonymous credentials.
 *
 * <p>{@link #projectName()} selects the credential-service project (each
 * project hosts an independent issuer with its own cipher suite and
 * usage limits). {@link #configurationId()} pins the per-project
 * configuration the {@link #blindedTokens() tokens} were blinded against.
 * {@link #blindedTokens()} is the batch of base64url-encoded blinded
 * tokens to issue. {@link #requestProof()} is the zero-knowledge proof
 * attesting the client did blind the tokens correctly under the named
 * configuration.
 */
@ProtobufMessage(name = "AnonymousCredentialIssuanceRequest")
public final class AnonymousCredentialIssuanceRequest {
    /**
     * Credential-service project name. Unset omits the variable.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String projectName;

    /**
     * Per-project configuration the tokens were blinded against. Unset
     * omits the variable.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String configurationId;

    /**
     * Batch of base64url-encoded blinded tokens to issue. Defaults to
     * {@link List#of()} when unset.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final List<String> blindedTokens;

    /**
     * Zero-knowledge proof attesting the client did blind the tokens
     * correctly under the named configuration. Unset omits the variable.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String requestProof;

    /**
     * Constructs a new {@code AnonymousCredentialIssuanceRequest}. The
     * list argument may be {@code null} to default to {@link List#of()};
     * the scalar arguments may be {@code null} to omit the corresponding
     * variable from the request.
     *
     * @param projectName     the credential-service project name, or
     *                        {@code null}
     * @param configurationId the per-project configuration identifier, or
     *                        {@code null}
     * @param blindedTokens   the base64url-encoded blinded tokens, or
     *                        {@code null} to default to empty
     * @param requestProof    the zero-knowledge blinding proof, or
     *                        {@code null}
     */
    public AnonymousCredentialIssuanceRequest(String projectName, String configurationId,
                                              List<String> blindedTokens, String requestProof) {
        this.projectName = projectName;
        this.configurationId = configurationId;
        this.blindedTokens = blindedTokens == null ? List.of() : List.copyOf(blindedTokens);
        this.requestProof = requestProof;
    }

    /**
     * Returns the credential-service project name.
     *
     * @return an {@link Optional} carrying the project name, or empty when
     *         unset
     */
    public Optional<String> projectName() {
        return Optional.ofNullable(projectName);
    }

    /**
     * Returns the per-project configuration identifier.
     *
     * @return an {@link Optional} carrying the identifier, or empty when
     *         unset
     */
    public Optional<String> configurationId() {
        return Optional.ofNullable(configurationId);
    }

    /**
     * Returns the batch of base64url-encoded blinded tokens.
     *
     * @return an unmodifiable view of the tokens; never {@code null},
     *         possibly empty
     */
    public List<String> blindedTokens() {
        return blindedTokens;
    }

    /**
     * Returns the zero-knowledge blinding proof.
     *
     * @return an {@link Optional} carrying the proof, or empty when unset
     */
    public Optional<String> requestProof() {
        return Optional.ofNullable(requestProof);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AnonymousCredentialIssuanceRequest) obj;
        return Objects.equals(projectName, that.projectName)
                && Objects.equals(configurationId, that.configurationId)
                && Objects.equals(blindedTokens, that.blindedTokens)
                && Objects.equals(requestProof, that.requestProof);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectName, configurationId, blindedTokens, requestProof);
    }

    @Override
    public String toString() {
        return "AnonymousCredentialIssuanceRequest[" +
                "projectName=" + projectName + ", " +
                "configurationId=" + configurationId + ", " +
                "blindedTokens=" + blindedTokens + ", " +
                "requestProof=" + requestProof + ']';
    }
}
