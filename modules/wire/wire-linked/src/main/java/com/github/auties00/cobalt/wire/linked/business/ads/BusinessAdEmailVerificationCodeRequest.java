package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Input model for dispatching the verification email that confirms the
 * contact email of a Click-to-WhatsApp advertising account.
 *
 * <p>While onboarding a Click-to-WhatsApp advertising account (an account
 * that funds paid promotions which open a chat with the business when
 * tapped), the merchant requests a verification code to be sent to the
 * email address they entered. This input carries the target email the code
 * is dispatched to.
 */
@ProtobufMessage(name = "BusinessAdEmailVerificationCodeRequest")
public final class BusinessAdEmailVerificationCodeRequest {
    /**
     * Email address the verification code is dispatched to. Required by
     * the onboarding backend; unset omits the field.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String email;

    /**
     * Constructs a new {@code BusinessAdEmailVerificationCodeRequest}. The
     * {@code email} may be {@code null} to omit the field from the request.
     *
     * @param email the target email address, or {@code null}
     */
    public BusinessAdEmailVerificationCodeRequest(String email) {
        this.email = email;
    }

    /**
     * Returns the target email address.
     *
     * @return an {@link Optional} carrying the email, or empty when unset
     */
    public Optional<String> email() {
        return Optional.ofNullable(email);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessAdEmailVerificationCodeRequest) obj;
        return Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    @Override
    public String toString() {
        return "BusinessAdEmailVerificationCodeRequest[" +
                "email=" + email + ']';
    }
}
