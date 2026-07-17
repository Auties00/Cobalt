package com.github.auties00.cobalt.wire.cloud.signup;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * The request to exchange an embedded-signup code for an access token.
 *
 * <p>At the end of the embedded-signup flow Meta hands the integrating app a short-lived code bound to
 * the redirect URI the flow was launched with. Exchanging that code, authenticated with the app's
 * {@link CloudAppCredentials credentials}, yields the access token the client uses to act on the newly
 * onboarded business. This model is the input to {@code CloudWhatsAppClient.exchangeSignupCode}; all three
 * fields are required.
 */
@ProtobufMessage
public final class CloudSignupCodeExchange {
    /**
     * The Meta app credentials used to authenticate the exchange.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final CloudAppCredentials credentials;

    /**
     * The redirect URI the embedded-signup flow was launched with.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String redirectUri;

    /**
     * The short-lived signup code to exchange.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String code;

    /**
     * Constructs a new signup-code exchange request.
     *
     * @param credentials the Meta app credentials used to authenticate the exchange
     * @param redirectUri the redirect URI the embedded-signup flow was launched with
     * @param code        the short-lived signup code to exchange
     * @throws NullPointerException if {@code credentials}, {@code redirectUri}, or {@code code} is
     *                              {@code null}
     */
    CloudSignupCodeExchange(CloudAppCredentials credentials, String redirectUri, String code) {
        this.credentials = Objects.requireNonNull(credentials, "credentials must not be null");
        this.redirectUri = Objects.requireNonNull(redirectUri, "redirectUri must not be null");
        this.code = Objects.requireNonNull(code, "code must not be null");
    }

    /**
     * Returns the Meta app credentials used to authenticate the exchange.
     *
     * @return the credentials
     */
    public CloudAppCredentials credentials() {
        return credentials;
    }

    /**
     * Returns the redirect URI the embedded-signup flow was launched with.
     *
     * @return the redirect URI
     */
    public String redirectUri() {
        return redirectUri;
    }

    /**
     * Returns the short-lived signup code to exchange.
     *
     * @return the code
     */
    public String code() {
        return code;
    }
}
