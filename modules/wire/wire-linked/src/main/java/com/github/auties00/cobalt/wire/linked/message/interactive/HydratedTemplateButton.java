package com.github.auties00.cobalt.wire.linked.message.interactive;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Represents a hydrated template button, where every placeholder has been replaced with a
 * literal value ready for display to the recipient.
 *
 * <p>A hydrated template button is one cell in the button grid of a
 * {@link TemplateMessage.HydratedFourRowTemplate}. Unlike the unhydrated
 * {@link TemplateButton}, whose display text is a highly structured template, the hydrated
 * variant stores plain strings so that clients can render it directly without further server
 * interaction.
 *
 * <p>Each button carries an optional ordering {@link #index()} and exactly one of three
 * concrete behaviours accessible through {@link #hydratedButton()}:
 * <ul>
 *   <li>{@link HydratedQuickReplyButton} sends a quick reply with a predefined identifier</li>
 *   <li>{@link HydratedURLButton} opens a URL, optionally inside an in-app web view</li>
 *   <li>{@link HydratedCallButton} dials a phone number</li>
 * </ul>
 */
@ProtobufMessage(name = "HydratedTemplateButton")
public final class HydratedTemplateButton {
    /**
     * The zero-based ordering index used to lay out this button within its parent grid.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
    Integer index;

    /**
     * The quick reply variant, when this button sends a predefined reply payload.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    HydratedQuickReplyButton quickReplyButton;

    /**
     * The URL variant, when this button opens a link.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    HydratedURLButton urlButton;

    /**
     * The call variant, when this button dials a phone number.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    HydratedCallButton callButton;


    /**
     * Constructs a new hydrated template button with the supplied index and variants.
     *
     * <p>At most one of {@code quickReplyButton}, {@code urlButton} and {@code callButton}
     * should be non-null to keep the oneof semantics well defined.
     *
     * @param index            the ordering index, possibly {@code null}
     * @param quickReplyButton the quick reply variant, possibly {@code null}
     * @param urlButton        the URL variant, possibly {@code null}
     * @param callButton       the call variant, possibly {@code null}
     */
    HydratedTemplateButton(Integer index, HydratedQuickReplyButton quickReplyButton, HydratedURLButton urlButton, HydratedCallButton callButton) {
        this.index = index;
        this.quickReplyButton = quickReplyButton;
        this.urlButton = urlButton;
        this.callButton = callButton;
    }

    /**
     * Returns the zero-based ordering index of this button within its parent button grid.
     *
     * @return an {@code OptionalInt} with the index, or empty if not set
     */
    public OptionalInt index() {
        return index == null ? OptionalInt.empty() : OptionalInt.of(index);
    }

    /**
     * Returns the concrete variant that describes the behaviour of this button.
     *
     * <p>Variants are checked in a fixed order: quick reply, URL, call. If multiple variants
     * are present on the same instance, only the first non-null one is returned.
     *
     * @return an {@code Optional} containing the matching variant, or empty if none is set
     */
    public Optional<? extends HydratedButtonVariant> hydratedButton() {
        if (quickReplyButton != null) return Optional.of(quickReplyButton);
        if (urlButton != null) return Optional.of(urlButton);
        if (callButton != null) return Optional.of(callButton);
        return Optional.empty();
    }

    /**
     * Updates the zero-based ordering index of this button.
     *
     * @param index the new ordering index, or {@code null} to clear the field
     */
    public void setIndex(Integer index) {
        this.index = index;
    }

    /**
     * Sets the quick reply variant, clearing the URL and call variants is the caller's
     * responsibility.
     *
     * @param quickReplyButton the new quick reply variant, or {@code null} to clear it
     */
    public void setQuickReplyButton(HydratedQuickReplyButton quickReplyButton) {
        this.quickReplyButton = quickReplyButton;
    }

    /**
     * Sets the URL variant, clearing the quick reply and call variants is the caller's
     * responsibility.
     *
     * @param urlButton the new URL variant, or {@code null} to clear it
     */
    public void setUrlButton(HydratedURLButton urlButton) {
        this.urlButton = urlButton;
    }

    /**
     * Sets the call variant, clearing the quick reply and URL variants is the caller's
     * responsibility.
     *
     * @param callButton the new call variant, or {@code null} to clear it
     */
    public void setCallButton(HydratedCallButton callButton) {
        this.callButton = callButton;
    }

    /**
     * Represents a hydrated button that initiates a phone call when tapped.
     *
     * <p>The display text is the user-visible label, while the phone number is the actual
     * destination dialed by the client when the button is pressed.
     */
    @ProtobufMessage(name = "HydratedTemplateButton.HydratedCallButton")
    public static final class HydratedCallButton implements HydratedButtonVariant {
        /**
         * The label shown to the recipient.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String displayText;

        /**
         * The phone number dialed when the button is tapped.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String phoneNumber;


        /**
         * Constructs a new hydrated call button with the supplied label and number.
         *
         * @param displayText the user-visible label, possibly {@code null}
         * @param phoneNumber the phone number to dial, possibly {@code null}
         */
        HydratedCallButton(String displayText, String phoneNumber) {
            this.displayText = displayText;
            this.phoneNumber = phoneNumber;
        }

        /**
         * Returns the user-visible label of this call button.
         *
         * @return an {@code Optional} with the display text, or empty if not set
         */
        public Optional<String> displayText() {
            return Optional.ofNullable(displayText);
        }

        /**
         * Returns the phone number dialed when the button is tapped.
         *
         * @return an {@code Optional} with the phone number, or empty if not set
         */
        public Optional<String> phoneNumber() {
            return Optional.ofNullable(phoneNumber);
        }

        /**
         * Updates the user-visible label of this call button.
         *
         * @param displayText the new display text, or {@code null} to clear the field
         */
        public void setDisplayText(String displayText) {
            this.displayText = displayText;
    }

        /**
         * Updates the phone number dialed when the button is tapped.
         *
         * @param phoneNumber the new phone number, or {@code null} to clear the field
         */
        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
    }
    }

    /**
     * Represents a hydrated button that sends a predefined reply when tapped.
     *
     * <p>When the recipient taps this button their client produces a
     * {@link TemplateButtonReplyMessage} carrying the button's {@link #id()} as the selected
     * identifier and its {@link #displayText()} as the selected label.
     */
    @ProtobufMessage(name = "HydratedTemplateButton.HydratedQuickReplyButton")
    public static final class HydratedQuickReplyButton implements HydratedButtonVariant {
        /**
         * The label shown to the recipient.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String displayText;

        /**
         * The identifier echoed back to the sender when the button is tapped.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String id;


        /**
         * Constructs a new hydrated quick reply button with the supplied label and
         * identifier.
         *
         * @param displayText the user-visible label, possibly {@code null}
         * @param id          the reply identifier, possibly {@code null}
         */
        HydratedQuickReplyButton(String displayText, String id) {
            this.displayText = displayText;
            this.id = id;
        }

        /**
         * Returns the user-visible label of this quick reply button.
         *
         * @return an {@code Optional} with the display text, or empty if not set
         */
        public Optional<String> displayText() {
            return Optional.ofNullable(displayText);
        }

        /**
         * Returns the identifier echoed back when the button is tapped.
         *
         * @return an {@code Optional} with the identifier, or empty if not set
         */
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }

        /**
         * Updates the user-visible label of this quick reply button.
         *
         * @param displayText the new display text, or {@code null} to clear the field
         */
        public void setDisplayText(String displayText) {
            this.displayText = displayText;
    }

        /**
         * Updates the identifier echoed back when the button is tapped.
         *
         * @param id the new identifier, or {@code null} to clear the field
         */
        public void setId(String id) {
            this.id = id;
    }
    }

    /**
     * Represents a hydrated button that opens a URL when tapped.
     *
     * <p>Two URL variants are supported: {@link #url()} is used when the recipient has not
     * opted into consent-gated navigation, while {@link #consentedUsersUrl()} is used for
     * users who have explicitly granted consent. The {@link #webviewPresentation()} field
     * allows the sender to request a specific in-app web view size, from a compact banner up
     * to a full-screen experience.
     */
    @ProtobufMessage(name = "HydratedTemplateButton.HydratedURLButton")
    public static final class HydratedURLButton implements HydratedButtonVariant {
        /**
         * The label shown to the recipient.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String displayText;

        /**
         * The URL opened for recipients without explicit consent for tracking.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String url;

        /**
         * The URL opened for recipients who have granted consent for tracking.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String consentedUsersUrl;

        /**
         * The requested presentation style for the in-app web view.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
        HydratedURLButton.WebviewPresentationType webviewPresentation;


        /**
         * Constructs a new hydrated URL button with the supplied fields.
         *
         * @param displayText         the user-visible label, possibly {@code null}
         * @param url                 the URL for unconsented users, possibly {@code null}
         * @param consentedUsersUrl   the URL for consented users, possibly {@code null}
         * @param webviewPresentation the requested web view size, possibly {@code null}
         */
        HydratedURLButton(String displayText, String url, String consentedUsersUrl, WebviewPresentationType webviewPresentation) {
            this.displayText = displayText;
            this.url = url;
            this.consentedUsersUrl = consentedUsersUrl;
            this.webviewPresentation = webviewPresentation;
        }

        /**
         * Returns the user-visible label of this URL button.
         *
         * @return an {@code Optional} with the display text, or empty if not set
         */
        public Optional<String> displayText() {
            return Optional.ofNullable(displayText);
        }

        /**
         * Returns the URL opened for recipients without explicit consent for tracking.
         *
         * @return an {@code Optional} with the URL, or empty if not set
         */
        public Optional<String> url() {
            return Optional.ofNullable(url);
        }

        /**
         * Returns the URL opened for recipients who have granted consent for tracking.
         *
         * @return an {@code Optional} with the consented URL, or empty if not set
         */
        public Optional<String> consentedUsersUrl() {
            return Optional.ofNullable(consentedUsersUrl);
        }

        /**
         * Returns the requested presentation style for the in-app web view.
         *
         * @return an {@code Optional} with the presentation style, or empty if not set
         */
        public Optional<WebviewPresentationType> webviewPresentation() {
            return Optional.ofNullable(webviewPresentation);
        }

        /**
         * Updates the user-visible label of this URL button.
         *
         * @param displayText the new display text, or {@code null} to clear the field
         */
        public void setDisplayText(String displayText) {
            this.displayText = displayText;
    }

        /**
         * Updates the URL opened for recipients without explicit consent for tracking.
         *
         * @param url the new URL, or {@code null} to clear the field
         */
        public void setUrl(String url) {
            this.url = url;
    }

        /**
         * Updates the URL opened for recipients who have granted consent for tracking.
         *
         * @param consentedUsersUrl the new consented URL, or {@code null} to clear the field
         */
        public void setConsentedUsersUrl(String consentedUsersUrl) {
            this.consentedUsersUrl = consentedUsersUrl;
    }

        /**
         * Updates the requested presentation style for the in-app web view.
         *
         * @param webviewPresentation the new presentation style, or {@code null} to clear the
         *                            field
         */
        public void setWebviewPresentation(WebviewPresentationType webviewPresentation) {
            this.webviewPresentation = webviewPresentation;
    }

        /**
         * Enumerates the possible sizes at which an in-app web view can be presented when a
         * hydrated URL button is tapped.
         *
         * <p>The chosen style is a hint to the client: full-screen, a tall sheet that leaves
         * a small strip of the chat visible, or a compact banner-sized sheet.
         */
        @ProtobufEnum(name = "HydratedTemplateButton.HydratedURLButton.WebviewPresentationType")
        public static enum WebviewPresentationType {
            /**
             * Present the web view as a full-screen experience.
             */
            FULL(1),
            /**
             * Present the web view as a tall sheet, leaving a small portion of the chat
             * visible at the top.
             */
            TALL(2),
            /**
             * Present the web view as a compact, banner-sized sheet near the bottom of the
             * screen.
             */
            COMPACT(3);

            /**
             * Constructs a new enum constant with the supplied protobuf index.
             *
             * @param index the numeric wire-format index
             */
            WebviewPresentationType(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The numeric wire-format index of this constant.
             */
            final int index;

            /**
             * Returns the numeric wire-format index of this constant.
             *
             * @return the protobuf enum index
             */
            public int index() {
                return this.index;
            }
        }
    }
}
