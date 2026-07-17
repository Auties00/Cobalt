package com.github.auties00.cobalt.wire.cloud.template.library;

import com.github.auties00.cobalt.wire.cloud.template.CloudOtpType;

import java.util.Objects;

/**
 * The caller-bound value of one button when adopting a WhatsApp Cloud API Template Library entry.
 *
 * <p>This is the closed-type projection of the flat {@link CloudTemplateLibraryButtonInput} wire shape:
 * each button kind binds exactly one value, and the kind is the discriminator. A {@link Url} binds the
 * link a URL button opens, a {@link PhoneNumber} binds the number a phone-number button dials, an
 * {@link Otp} binds the one-time-passcode delivery type, and a {@link QuickReply} binds nothing. It is
 * obtained through {@link CloudTemplateLibraryButtonInput#value()} and is never serialized; the flat
 * fields on {@link CloudTemplateLibraryButtonInput} remain the wire representation.
 */
public sealed interface CloudTemplateLibraryButtonValue permits CloudTemplateLibraryButtonValue.Url,
        CloudTemplateLibraryButtonValue.PhoneNumber, CloudTemplateLibraryButtonValue.QuickReply,
        CloudTemplateLibraryButtonValue.Otp {
    /**
     * The bound value of a {@link CloudTemplateLibraryButtonType#URL} button.
     */
    final class Url implements CloudTemplateLibraryButtonValue {
        /**
         * The URL the button opens.
         */
        private final String url;

        /**
         * Constructs a new URL value.
         *
         * @param url the URL the button opens
         * @throws NullPointerException if {@code url} is {@code null}
         */
        public Url(String url) {
            this.url = Objects.requireNonNull(url, "url must not be null");
        }

        /**
         * Returns the URL the button opens.
         *
         * @return the URL
         */
        public String url() {
            return url;
        }
    }

    /**
     * The bound value of a {@link CloudTemplateLibraryButtonType#PHONE_NUMBER} button.
     */
    final class PhoneNumber implements CloudTemplateLibraryButtonValue {
        /**
         * The phone number the button dials.
         */
        private final String phoneNumber;

        /**
         * Constructs a new phone-number value.
         *
         * @param phoneNumber the phone number the button dials
         * @throws NullPointerException if {@code phoneNumber} is {@code null}
         */
        public PhoneNumber(String phoneNumber) {
            this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber must not be null");
        }

        /**
         * Returns the phone number the button dials.
         *
         * @return the phone number
         */
        public String phoneNumber() {
            return phoneNumber;
        }
    }

    /**
     * The bound value of a {@link CloudTemplateLibraryButtonType#QUICK_REPLY} button.
     *
     * <p>A quick-reply button binds no value; this variant is a marker for that case.
     */
    final class QuickReply implements CloudTemplateLibraryButtonValue {
        /**
         * Constructs a new quick-reply value.
         */
        public QuickReply() {
        }
    }

    /**
     * The bound value of a {@link CloudTemplateLibraryButtonType#OTP} button.
     */
    final class Otp implements CloudTemplateLibraryButtonValue {
        /**
         * The one-time-passcode delivery type.
         */
        private final CloudOtpType otpType;

        /**
         * Constructs a new OTP value.
         *
         * @param otpType the one-time-passcode delivery type
         * @throws NullPointerException if {@code otpType} is {@code null}
         */
        public Otp(CloudOtpType otpType) {
            this.otpType = Objects.requireNonNull(otpType, "otpType must not be null");
        }

        /**
         * Returns the one-time-passcode delivery type.
         *
         * @return the {@link CloudOtpType}
         */
        public CloudOtpType otpType() {
            return otpType;
        }
    }
}
