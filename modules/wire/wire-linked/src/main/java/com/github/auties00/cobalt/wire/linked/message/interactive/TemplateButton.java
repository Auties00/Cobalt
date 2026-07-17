package com.github.auties00.cobalt.wire.linked.message.interactive;

import com.github.auties00.cobalt.wire.linked.message.text.HighlyStructuredMessage;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Represents an unhydrated template button whose display text is still a
 * {@link HighlyStructuredMessage}.
 *
 * <p>A template button defines a tappable element attached to a
 * {@link TemplateMessage.FourRowTemplate}. Because the label and other textual fields are
 * highly structured messages, they may contain placeholders (for example {@code %1$s}) that
 * the server resolves into literal strings before delivering a
 * {@link TemplateMessage.HydratedFourRowTemplate} with matching
 * {@link HydratedTemplateButton} entries to the recipient.
 *
 * <p>Each button carries an optional ordering {@link #index()} and exactly one of three
 * concrete behaviours accessible through {@link #button()}:
 * <ul>
 *   <li>{@link QuickReplyButton} sends a predefined quick reply payload</li>
 *   <li>{@link URLButton} opens a URL</li>
 *   <li>{@link CallButton} dials a phone number</li>
 * </ul>
 */
@ProtobufMessage(name = "TemplateButton")
public final class TemplateButton {
    /**
     * The zero-based ordering index used to lay out this button within its parent grid.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
    Integer index;

    /**
     * The quick reply variant, when this button sends a predefined reply payload.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    QuickReplyButton quickReplyButton;

    /**
     * The URL variant, when this button opens a link.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    URLButton urlButton;

    /**
     * The call variant, when this button dials a phone number.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    CallButton callButton;


    /**
     * Constructs a new template button with the supplied index and variants.
     *
     * <p>At most one of {@code quickReplyButton}, {@code urlButton} and {@code callButton}
     * should be non-null to keep the oneof semantics well defined.
     *
     * @param index            the ordering index, possibly {@code null}
     * @param quickReplyButton the quick reply variant, possibly {@code null}
     * @param urlButton        the URL variant, possibly {@code null}
     * @param callButton       the call variant, possibly {@code null}
     */
    TemplateButton(Integer index, QuickReplyButton quickReplyButton, URLButton urlButton, CallButton callButton) {
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
    public Optional<? extends TemplateButtonVariant> button() {
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
    public void setQuickReplyButton(QuickReplyButton quickReplyButton) {
        this.quickReplyButton = quickReplyButton;
    }

    /**
     * Sets the URL variant, clearing the quick reply and call variants is the caller's
     * responsibility.
     *
     * @param urlButton the new URL variant, or {@code null} to clear it
     */
    public void setUrlButton(URLButton urlButton) {
        this.urlButton = urlButton;
    }

    /**
     * Sets the call variant, clearing the quick reply and URL variants is the caller's
     * responsibility.
     *
     * @param callButton the new call variant, or {@code null} to clear it
     */
    public void setCallButton(CallButton callButton) {
        this.callButton = callButton;
    }

    /**
     * Represents a template button that initiates a phone call when tapped.
     *
     * <p>Both the display text and the phone number are expressed as
     * {@link HighlyStructuredMessage} values so that placeholders can be resolved
     * server-side before the button is rendered to the recipient.
     */
    @ProtobufMessage(name = "TemplateButton.CallButton")
    public static final class CallButton implements TemplateButtonVariant {
        /**
         * The structured display text shown on the button before hydration.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        HighlyStructuredMessage displayText;

        /**
         * The structured phone number dialed when the button is tapped.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        HighlyStructuredMessage phoneNumber;


        /**
         * Constructs a new call button with the supplied structured label and number.
         *
         * @param displayText the structured button label, possibly {@code null}
         * @param phoneNumber the structured phone number, possibly {@code null}
         */
        CallButton(HighlyStructuredMessage displayText, HighlyStructuredMessage phoneNumber) {
            this.displayText = displayText;
            this.phoneNumber = phoneNumber;
        }

        /**
         * Returns the structured display text shown on this call button.
         *
         * @return an {@code Optional} with the display text template, or empty if not set
         */
        public Optional<HighlyStructuredMessage> displayText() {
            return Optional.ofNullable(displayText);
        }

        /**
         * Returns the structured phone number dialed when the button is tapped.
         *
         * @return an {@code Optional} with the phone number template, or empty if not set
         */
        public Optional<HighlyStructuredMessage> phoneNumber() {
            return Optional.ofNullable(phoneNumber);
        }

        /**
         * Updates the structured display text of this call button.
         *
         * @param displayText the new display text template, or {@code null} to clear the
         *                    field
         */
        public void setDisplayText(HighlyStructuredMessage displayText) {
            this.displayText = displayText;
    }

        /**
         * Updates the structured phone number dialed when the button is tapped.
         *
         * @param phoneNumber the new phone number template, or {@code null} to clear the
         *                    field
         */
        public void setPhoneNumber(HighlyStructuredMessage phoneNumber) {
            this.phoneNumber = phoneNumber;
    }
    }

    /**
     * Represents a template button that sends a predefined reply when tapped.
     *
     * <p>The display text is a {@link HighlyStructuredMessage} template resolved at
     * hydration time. The identifier is a plain string echoed back to the sender inside the
     * resulting {@link TemplateButtonReplyMessage}.
     */
    @ProtobufMessage(name = "TemplateButton.QuickReplyButton")
    public static final class QuickReplyButton implements TemplateButtonVariant {
        /**
         * The structured display text shown on the button before hydration.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        HighlyStructuredMessage displayText;

        /**
         * The identifier echoed back to the sender when the button is tapped.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String id;


        /**
         * Constructs a new quick reply button with the supplied structured label and reply
         * identifier.
         *
         * @param displayText the structured button label, possibly {@code null}
         * @param id          the reply identifier, possibly {@code null}
         */
        QuickReplyButton(HighlyStructuredMessage displayText, String id) {
            this.displayText = displayText;
            this.id = id;
        }

        /**
         * Returns the structured display text shown on this quick reply button.
         *
         * @return an {@code Optional} with the display text template, or empty if not set
         */
        public Optional<HighlyStructuredMessage> displayText() {
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
         * Updates the structured display text of this quick reply button.
         *
         * @param displayText the new display text template, or {@code null} to clear the
         *                    field
         */
        public void setDisplayText(HighlyStructuredMessage displayText) {
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
     * Represents a template button that opens a URL when tapped.
     *
     * <p>Both the display text and the URL are expressed as {@link HighlyStructuredMessage}
     * values so that placeholders can be resolved server-side before the button is rendered
     * to the recipient.
     */
    @ProtobufMessage(name = "TemplateButton.URLButton")
    public static final class URLButton implements TemplateButtonVariant {
        /**
         * The structured display text shown on the button before hydration.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        HighlyStructuredMessage displayText;

        /**
         * The structured URL opened when the button is tapped.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        HighlyStructuredMessage url;


        /**
         * Constructs a new URL button with the supplied structured label and URL.
         *
         * @param displayText the structured button label, possibly {@code null}
         * @param url         the structured target URL, possibly {@code null}
         */
        URLButton(HighlyStructuredMessage displayText, HighlyStructuredMessage url) {
            this.displayText = displayText;
            this.url = url;
        }

        /**
         * Returns the structured display text shown on this URL button.
         *
         * @return an {@code Optional} with the display text template, or empty if not set
         */
        public Optional<HighlyStructuredMessage> displayText() {
            return Optional.ofNullable(displayText);
        }

        /**
         * Returns the structured URL opened when the button is tapped.
         *
         * @return an {@code Optional} with the URL template, or empty if not set
         */
        public Optional<HighlyStructuredMessage> url() {
            return Optional.ofNullable(url);
        }

        /**
         * Updates the structured display text of this URL button.
         *
         * @param displayText the new display text template, or {@code null} to clear the
         *                    field
         */
        public void setDisplayText(HighlyStructuredMessage displayText) {
            this.displayText = displayText;
    }

        /**
         * Updates the structured URL opened when the button is tapped.
         *
         * @param url the new URL template, or {@code null} to clear the field
         */
        public void setUrl(HighlyStructuredMessage url) {
            this.url = url;
    }
    }
}
