package com.github.auties00.cobalt.client.cloud;

import com.github.auties00.cobalt.wire.cloud.phone.CloudVerificationMethod;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A pluggable strategy for completing the phone-number verification ceremony of a
 * {@link CloudWhatsAppClient}.
 *
 * <p>Verifying a phone number for Cloud API use is a two-step exchange: the client requests a
 * one-time code over a delivery channel ({@link #method()}), the user receives it on the
 * phone number being verified, and the client submits it back ({@link #verificationCode()}). This
 * is the Cloud counterpart of the Linked
 * {@link com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientVerificationHandler.Mobile}
 * registration handler.
 *
 * @apiNote
 * Wire an implementation into
 * {@link CloudWhatsAppClient#verifyPhoneNumber(CloudWhatsAppClientVerificationHandler, java.util.Locale)},
 * or use the {@link #sms(Supplier)} and {@link #call(Supplier)} factories when the only decision
 * left to make is how the user supplies the received code.
 */
public interface CloudWhatsAppClientVerificationHandler {
    /**
     * Returns the delivery channel to request for the verification code.
     *
     * @return the delivery channel
     */
    CloudVerificationMethod method();

    /**
     * Returns the verification code supplied by the user.
     *
     * <p>Implementations typically block on user input (for example reading a console line) and
     * return the code once it has been entered.
     *
     * @return the verification code
     */
    String verificationCode();

    /**
     * Returns a verification handler that requests SMS delivery and reads the verification code
     * from the supplied supplier.
     *
     * @param supplier the supplier that produces the verification code once the user has received
     *                 it
     * @return the verification handler
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    static CloudWhatsAppClientVerificationHandler sms(Supplier<String> supplier) {
        Objects.requireNonNull(supplier, "supplier cannot be null");
        Objects.requireNonNull(CloudVerificationMethod.SMS, "method cannot be null");
        Objects.requireNonNull(supplier, "supplier cannot be null");
        return new CloudWhatsAppClientVerificationHandler() {
            @Override
            public CloudVerificationMethod method() {
                return CloudVerificationMethod.SMS;
            }

            @Override
            public String verificationCode() {
                var value = supplier.get();
                if (value == null) {
                    throw new IllegalArgumentException("Cannot send verification code: no value");
                }
                return value;
            }
        };
    }

    /**
     * Returns a verification handler that requests voice-call delivery and reads the verification
     * code from the supplied supplier.
     *
     * @param supplier the supplier that produces the verification code once the user has received
     *                 it
     * @return the verification handler
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    static CloudWhatsAppClientVerificationHandler call(Supplier<String> supplier) {
        Objects.requireNonNull(supplier, "supplier cannot be null");
        Objects.requireNonNull(CloudVerificationMethod.VOICE, "method cannot be null");
        Objects.requireNonNull(supplier, "supplier cannot be null");
        return new CloudWhatsAppClientVerificationHandler() {
            @Override
            public CloudVerificationMethod method() {
                return CloudVerificationMethod.VOICE;
            }

            @Override
            public String verificationCode() {
                var value = supplier.get();
                if (value == null) {
                    throw new IllegalArgumentException("Cannot send verification code: no value");
                }
                return value;
            }
        };
    }
}
