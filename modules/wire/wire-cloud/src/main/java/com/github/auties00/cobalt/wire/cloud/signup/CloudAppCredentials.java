package com.github.auties00.cobalt.wire.cloud.signup;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * The Meta app identifier and secret pair used to authenticate app-level Graph operations.
 *
 * <p>Several embedded-signup and token operations are performed as the Meta app rather than as a business
 * user, and so require the app's own id and secret. This pair is the input to
 * {@code CloudWhatsAppClient.exchangeLongLivedToken} and is embedded in
 * {@link CloudSignupCodeExchange} for {@code CloudWhatsAppClient.exchangeSignupCode}. Both fields are
 * required.
 */
@ProtobufMessage
public final class CloudAppCredentials {
    /**
     * The Meta app id.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String appId;

    /**
     * The Meta app secret.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String appSecret;

    /**
     * Constructs a new app-credentials pair.
     *
     * @param appId     the Meta app id
     * @param appSecret the Meta app secret
     * @throws NullPointerException if {@code appId} or {@code appSecret} is {@code null}
     */
    CloudAppCredentials(String appId, String appSecret) {
        this.appId = Objects.requireNonNull(appId, "appId must not be null");
        this.appSecret = Objects.requireNonNull(appSecret, "appSecret must not be null");
    }

    /**
     * Returns the Meta app id.
     *
     * @return the app id
     */
    public String appId() {
        return appId;
    }

    /**
     * Returns the Meta app secret.
     *
     * @return the app secret
     */
    public String appSecret() {
        return appSecret;
    }
}
