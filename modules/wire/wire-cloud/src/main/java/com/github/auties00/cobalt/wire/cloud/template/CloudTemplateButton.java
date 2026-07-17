package com.github.auties00.cobalt.wire.cloud.template;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * One button of a WhatsApp Cloud API message-template {@link CloudTemplateComponent.Buttons} component.
 *
 * <p>A buttons component carries an ordered list of these. Each button is a distinct kind selected by the
 * concrete variant, so this is a sealed hierarchy rather than one wide type: a {@link QuickReply} sends a
 * fixed reply payload, a {@link Url} opens a link, a {@link PhoneNumber} dials a number, a {@link CopyCode}
 * copies a one-time code, an {@link Otp} delivers a one-time passcode, and a {@link Flow} opens a Flow.
 * Matching on the sealed type recovers the variant-specific fields.
 *
 * <p>These buttons are caller-built request data passed inside a {@link CloudMessageTemplate} to
 * {@code CloudWhatsAppClient.createMessageTemplate}; each concrete variant is constructed through its
 * generated builder.
 */
public sealed interface CloudTemplateButton permits CloudTemplateButton.QuickReply, CloudTemplateButton.Url,
        CloudTemplateButton.PhoneNumber, CloudTemplateButton.CopyCode, CloudTemplateButton.Otp,
        CloudTemplateButton.Flow {
    /**
     * A button that sends a fixed quick-reply payload back to the business.
     *
     * <p>The button shows {@link #text() text} and, when tapped, sends that same text back as an inbound
     * message so the business can branch on it.
     */
    @ProtobufMessage
    final class QuickReply implements CloudTemplateButton {
        /**
         * The button label, also the payload sent back when the button is tapped.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String text;

        /**
         * Constructs a new quick-reply button.
         *
         * @param text the button label and reply payload
         * @throws NullPointerException if {@code text} is {@code null}
         */
        QuickReply(String text) {
            this.text = Objects.requireNonNull(text, "text must not be null");
        }

        /**
         * Returns the button label.
         *
         * @return the label, also the reply payload sent when the button is tapped
         */
        public String text() {
            return text;
        }
    }

    /**
     * A button that opens a URL.
     *
     * <p>The button shows {@link #text() text} and opens {@link #url() url} when tapped; the URL may carry
     * a trailing placeholder bound at send time.
     */
    @ProtobufMessage
    final class Url implements CloudTemplateButton {
        /**
         * The button label.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String text;

        /**
         * The URL the button opens, optionally carrying a trailing placeholder.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        final String url;

        /**
         * Constructs a new URL button.
         *
         * @param text the button label
         * @param url  the URL the button opens
         * @throws NullPointerException if {@code text} or {@code url} is {@code null}
         */
        Url(String text, String url) {
            this.text = Objects.requireNonNull(text, "text must not be null");
            this.url = Objects.requireNonNull(url, "url must not be null");
        }

        /**
         * Returns the button label.
         *
         * @return the label
         */
        public String text() {
            return text;
        }

        /**
         * Returns the URL the button opens.
         *
         * @return the URL, optionally carrying a trailing placeholder
         */
        public String url() {
            return url;
        }
    }

    /**
     * A button that dials a phone number.
     *
     * <p>The button shows {@link #text() text} and dials {@link #phoneNumber() phoneNumber} when tapped.
     */
    @ProtobufMessage
    final class PhoneNumber implements CloudTemplateButton {
        /**
         * The button label.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String text;

        /**
         * The phone number the button dials, in E.164 form.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        final String phoneNumber;

        /**
         * Constructs a new phone-number button.
         *
         * @param text        the button label
         * @param phoneNumber the phone number the button dials, in E.164 form
         * @throws NullPointerException if {@code text} or {@code phoneNumber} is {@code null}
         */
        PhoneNumber(String text, String phoneNumber) {
            this.text = Objects.requireNonNull(text, "text must not be null");
            this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber must not be null");
        }

        /**
         * Returns the button label.
         *
         * @return the label
         */
        public String text() {
            return text;
        }

        /**
         * Returns the phone number the button dials.
         *
         * @return the phone number in E.164 form
         */
        public String phoneNumber() {
            return phoneNumber;
        }
    }

    /**
     * A button that copies a one-time code to the clipboard.
     *
     * <p>The button copies the code carried by the message; {@link #example() example} supplies a sample
     * code that drives the template-review preview.
     */
    @ProtobufMessage
    final class CopyCode implements CloudTemplateButton {
        /**
         * The sample code shown in the template-review preview, or {@code null} when none is supplied.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String example;

        /**
         * Constructs a new copy-code button.
         *
         * @param example the sample code for the review preview, or {@code null} when none is supplied
         */
        CopyCode(String example) {
            this.example = example;
        }

        /**
         * Returns the sample code shown in the template-review preview.
         *
         * @return an {@link Optional} carrying the sample code, or empty when none was supplied
         */
        public Optional<String> example() {
            return Optional.ofNullable(example);
        }
    }

    /**
     * A button that delivers a one-time passcode for an authentication template.
     *
     * <p>The button copies or autofills the verification code carried by the message. The
     * {@link #otpType() otpType} selects the delivery mode; {@link #text() text} is the optional
     * copy-code button label.
     */
    @ProtobufMessage
    final class Otp implements CloudTemplateButton {
        /**
         * The OTP delivery mode, or {@code null} to let the server pick its default.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        final CloudOtpType otpType;

        /**
         * The copy-code button label, or {@code null} to use the server default.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        final String text;

        /**
         * Constructs a new OTP button.
         *
         * @param otpType the OTP delivery mode, or {@code null} to use the server default
         * @param text    the copy-code button label, or {@code null} to use the server default
         */
        Otp(CloudOtpType otpType, String text) {
            this.otpType = otpType;
            this.text = text;
        }

        /**
         * Returns the OTP delivery mode.
         *
         * @return an {@link Optional} carrying the {@link CloudOtpType}, or empty to use the server
         *         default
         */
        public Optional<CloudOtpType> otpType() {
            return Optional.ofNullable(otpType);
        }

        /**
         * Returns the copy-code button label.
         *
         * @return an {@link Optional} carrying the label, or empty to use the server default
         */
        public Optional<String> text() {
            return Optional.ofNullable(text);
        }
    }

    /**
     * A button that opens a WhatsApp Flow.
     *
     * <p>The button shows {@link #text() text} and opens the Flow identified by {@link #flowId() flowId}
     * when tapped; {@link #flowAction() flowAction} selects how the Flow is launched and
     * {@link #navigateScreen() navigateScreen} names the entry screen for a navigate action.
     */
    @ProtobufMessage
    final class Flow implements CloudTemplateButton {
        /**
         * The button label.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String text;

        /**
         * The id of the Flow the button opens, or {@code null} when it is named instead.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        final String flowId;

        /**
         * The Flow launch action ({@code navigate}, {@code data_exchange}), or {@code null} for the
         * server default.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        final String flowAction;

        /**
         * The entry screen for a {@code navigate} action, or {@code null} when not applicable.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        final String navigateScreen;

        /**
         * Constructs a new Flow button.
         *
         * @param text           the button label
         * @param flowId         the id of the Flow the button opens, or {@code null}
         * @param flowAction     the Flow launch action, or {@code null} for the server default
         * @param navigateScreen the entry screen for a navigate action, or {@code null}
         * @throws NullPointerException if {@code text} is {@code null}
         */
        Flow(String text, String flowId, String flowAction, String navigateScreen) {
            this.text = Objects.requireNonNull(text, "text must not be null");
            this.flowId = flowId;
            this.flowAction = flowAction;
            this.navigateScreen = navigateScreen;
        }

        /**
         * Returns the button label.
         *
         * @return the label
         */
        public String text() {
            return text;
        }

        /**
         * Returns the id of the Flow the button opens.
         *
         * @return an {@link Optional} carrying the Flow id, or empty when the Flow is named instead
         */
        public Optional<String> flowId() {
            return Optional.ofNullable(flowId);
        }

        /**
         * Returns the Flow launch action.
         *
         * @return an {@link Optional} carrying the action, or empty for the server default
         */
        public Optional<String> flowAction() {
            return Optional.ofNullable(flowAction);
        }

        /**
         * Returns the entry screen for a navigate action.
         *
         * @return an {@link Optional} carrying the screen, or empty when not applicable
         */
        public Optional<String> navigateScreen() {
            return Optional.ofNullable(navigateScreen);
        }
    }
}
