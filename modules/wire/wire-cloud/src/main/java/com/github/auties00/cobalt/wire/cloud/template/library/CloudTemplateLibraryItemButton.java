package com.github.auties00.cobalt.wire.cloud.template.library;

import com.github.auties00.cobalt.wire.cloud.template.CloudOtpType;

import java.util.Objects;
import java.util.Optional;

/**
 * A button declared by a {@link CloudTemplateLibraryItem}.
 *
 * <p>A library entry exposes its buttons abstractly so a business can adopt the entry and bind the
 * concrete values. The button kind is a closed four-variant union, and the variant itself is the
 * discriminator: a {@link Url} carries the link the button opens, a {@link PhoneNumber} carries the
 * number it dials, an {@link Otp} carries the one-time-passcode delivery type, and a {@link QuickReply}
 * carries only its label. Pattern matching on the variant recovers the kind-specific value; every
 * variant shares the optional {@linkplain #text() display text}.
 */
public sealed interface CloudTemplateLibraryItemButton permits CloudTemplateLibraryItemButton.Url,
        CloudTemplateLibraryItemButton.PhoneNumber, CloudTemplateLibraryItemButton.QuickReply,
        CloudTemplateLibraryItemButton.Otp {
    /**
     * Returns the button display text.
     *
     * @return an {@link Optional} carrying the text, or empty when unset
     */
    Optional<String> text();

    /**
     * A button that opens a URL.
     */
    final class Url implements CloudTemplateLibraryItemButton {
        /**
         * The button display text, or {@code null} when unset.
         */
        private final String text;

        /**
         * The URL the button opens.
         */
        private final String url;

        /**
         * Constructs a new URL button.
         *
         * @param text the display text, or {@code null} when unset
         * @param url  the URL the button opens
         * @throws NullPointerException if {@code url} is {@code null}
         */
        public Url(String text, String url) {
            this.text = text;
            this.url = Objects.requireNonNull(url, "url must not be null");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<String> text() {
            return Optional.ofNullable(text);
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
     * A button that dials a phone number.
     */
    final class PhoneNumber implements CloudTemplateLibraryItemButton {
        /**
         * The button display text, or {@code null} when unset.
         */
        private final String text;

        /**
         * The phone number the button dials.
         */
        private final String phoneNumber;

        /**
         * Constructs a new phone-number button.
         *
         * @param text        the display text, or {@code null} when unset
         * @param phoneNumber the phone number the button dials
         * @throws NullPointerException if {@code phoneNumber} is {@code null}
         */
        public PhoneNumber(String text, String phoneNumber) {
            this.text = text;
            this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber must not be null");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<String> text() {
            return Optional.ofNullable(text);
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
     * A button that sends a fixed quick-reply payload.
     *
     * <p>The button carries only its label; tapping it sends the label back as an inbound message.
     */
    final class QuickReply implements CloudTemplateLibraryItemButton {
        /**
         * The button display text, or {@code null} when unset.
         */
        private final String text;

        /**
         * Constructs a new quick-reply button.
         *
         * @param text the display text, or {@code null} when unset
         */
        public QuickReply(String text) {
            this.text = text;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<String> text() {
            return Optional.ofNullable(text);
        }
    }

    /**
     * A button that delivers a one-time passcode.
     */
    final class Otp implements CloudTemplateLibraryItemButton {
        /**
         * The button display text, or {@code null} when unset.
         */
        private final String text;

        /**
         * The one-time-passcode delivery type.
         */
        private final CloudOtpType otpType;

        /**
         * Constructs a new OTP button.
         *
         * @param text    the display text, or {@code null} when unset
         * @param otpType the one-time-passcode delivery type
         * @throws NullPointerException if {@code otpType} is {@code null}
         */
        public Otp(String text, CloudOtpType otpType) {
            this.text = text;
            this.otpType = Objects.requireNonNull(otpType, "otpType must not be null");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<String> text() {
            return Optional.ofNullable(text);
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
