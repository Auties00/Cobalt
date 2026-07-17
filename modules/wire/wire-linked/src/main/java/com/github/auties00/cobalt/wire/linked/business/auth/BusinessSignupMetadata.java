package com.github.auties00.cobalt.wire.linked.business.auth;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * The onboarding metadata rendered during a WhatsApp Business signup flow.
 *
 * <p>While a new business onboards, WhatsApp must show the operator the
 * consent copy and a privacy-policy link tied to the in-progress signup
 * before completing it. This model is the metadata used to render that
 * consent screen: the server-issued {@linkplain #id() signup identifier},
 * the {@linkplain #signupMessage() consent message}, and the
 * {@linkplain #privacyPolicyUrl() privacy-policy URL} the message links to.
 */
@ProtobufMessage(name = "BusinessSignupMetadata")
public final class BusinessSignupMetadata {
    /**
     * Server-issued signup identifier, or {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Signup consent message rendered during onboarding, or {@code null} when
     * the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String signupMessage;

    /**
     * Privacy-policy URL linked from the signup message, or {@code null} when
     * the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String privacyPolicyUrl;

    /**
     * Constructs a new {@code BusinessSignupMetadata}. Any reference argument
     * may be {@code null} when the server omitted the corresponding field.
     *
     * @param id               the server-issued signup identifier, or
     *                         {@code null}
     * @param signupMessage    the signup consent message, or {@code null}
     * @param privacyPolicyUrl the privacy-policy URL, or {@code null}
     */
    BusinessSignupMetadata(String id, String signupMessage, String privacyPolicyUrl) {
        this.id = id;
        this.signupMessage = signupMessage;
        this.privacyPolicyUrl = privacyPolicyUrl;
    }

    /**
     * Returns the server-issued signup identifier.
     *
     * @return the signup identifier, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the signup consent message rendered during onboarding.
     *
     * @return the signup message, or empty when the server omitted it
     */
    public Optional<String> signupMessage() {
        return Optional.ofNullable(signupMessage);
    }

    /**
     * Returns the privacy-policy URL linked from the signup message.
     *
     * @return the privacy-policy URL, or empty when the server omitted it
     */
    public Optional<String> privacyPolicyUrl() {
        return Optional.ofNullable(privacyPolicyUrl);
    }
}
