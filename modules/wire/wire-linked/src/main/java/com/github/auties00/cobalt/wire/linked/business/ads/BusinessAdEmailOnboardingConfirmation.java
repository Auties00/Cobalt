package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Input model for the email confirmation step of Click-to-WhatsApp ad
 * account onboarding.
 *
 * <p>While onboarding a Click-to-WhatsApp advertising account (an account
 * that funds paid promotions which open a chat with the business when
 * tapped), the merchant supplies a contact email and pastes the
 * verification code they received in their inbox. This input carries the
 * verification code the merchant pasted, the email being verified, and the
 * silent nonce the server returned from the send-code step to tie the
 * confirmation back to it.
 */
@ProtobufMessage(name = "BusinessAdEmailOnboardingConfirmation")
public final class BusinessAdEmailOnboardingConfirmation {
    /**
     * Verification code the merchant pasted from the confirmation email
     * the server sent. Required by the onboarding backend; unset omits the
     * field.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String code;

    /**
     * Email address being verified. Carries the contact email the merchant
     * supplied earlier in the onboarding flow so the server can match the
     * code against the right address. Unset omits the field.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String email;

    /**
     * Silent nonce the server returned from the send-verification-code step,
     * tying this confirmation back to it. Unset omits the field.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String silentNonce;

    /**
     * Constructs a new {@code BusinessAdEmailOnboardingConfirmation}. Every
     * argument may be {@code null} to omit the corresponding field from
     * the request.
     *
     * @param code        the verification code from the email, or {@code null}
     * @param email       the email address being verified, or {@code null}
     * @param silentNonce the silent nonce from the send-code step, or
     *                    {@code null}
     */
    public BusinessAdEmailOnboardingConfirmation(String code, String email, String silentNonce) {
        this.code = code;
        this.email = email;
        this.silentNonce = silentNonce;
    }

    /**
     * Returns the verification code the merchant pasted from the email.
     *
     * @return an {@link Optional} carrying the code, or empty when unset
     */
    public Optional<String> code() {
        return Optional.ofNullable(code);
    }

    /**
     * Returns the email address being verified.
     *
     * @return an {@link Optional} carrying the email, or empty when unset
     */
    public Optional<String> email() {
        return Optional.ofNullable(email);
    }

    /**
     * Returns the silent nonce from the send-verification-code step.
     *
     * @return an {@link Optional} carrying the nonce, or empty when unset
     */
    public Optional<String> silentNonce() {
        return Optional.ofNullable(silentNonce);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessAdEmailOnboardingConfirmation) obj;
        return Objects.equals(code, that.code)
                && Objects.equals(email, that.email)
                && Objects.equals(silentNonce, that.silentNonce);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, email, silentNonce);
    }

    @Override
    public String toString() {
        return "BusinessAdEmailOnboardingConfirmation[" +
                "code=" + code + ", " +
                "email=" + email + ", " +
                "silentNonce=" + silentNonce + ']';
    }
}
