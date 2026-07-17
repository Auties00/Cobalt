package com.github.auties00.cobalt.wire.linked.message.interactive;

import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The reply produced when a recipient taps a quick-reply button on a
 * {@link TemplateMessage} or on an interactive carousel card.
 *
 * <p>When the user selects one of the template buttons, their client sends
 * back this message so the original sender can match the response to the
 * button that was pressed. The reply carries both the machine-readable
 * identifier of the selected button ({@link #selectedId()}) and the
 * human-readable label that was shown ({@link #selectedDisplayText()}),
 * along with optional positional hints that are useful when buttons are
 * arranged in a list or across multiple carousel cards.
 *
 * <p>This message is contextual: like any other quoted reply it can carry
 * a {@link ContextInfo} pointing back to the originating template message.
 */
@ProtobufMessage(name = "Message.TemplateButtonReplyMessage")
public final class TemplateButtonReplyMessage implements ContextualMessage {
    /**
     * The identifier of the button that the recipient selected, matching the
     * {@code id} field of the originating template button so the sender can
     * route the reply to the action associated with that button.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String selectedId;

    /**
     * The human-readable label shown on the selected button, useful for
     * rendering the reply as a chat bubble that mirrors the text the user
     * actually tapped.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String selectedDisplayText;

    /**
     * Contextual information that links this reply to the originating
     * template message, including the quoted-message reference and any
     * mention metadata.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * The zero-based index of the selected button within its parent list,
     * when buttons are ordered (for example, in a multi-button list). Useful
     * when matching the reply against the original button order without
     * relying on the identifier alone.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
    Integer selectedIndex;

    /**
     * The zero-based index of the carousel card that contained the selected
     * button, when the reply originates from an interactive carousel
     * message. Combined with {@link #selectedIndex} this pinpoints exactly
     * which card and which button on that card was tapped.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
    Integer selectedCarouselCardIndex;


    /**
     * Constructs a new template-button reply with the supplied selection metadata.
     *
     * @param selectedId                the identifier of the tapped button, or {@code null}
     * @param selectedDisplayText       the label shown on the tapped button, or {@code null}
     * @param contextInfo               the context linking this reply to the source message, or {@code null}
     * @param selectedIndex             the button's position within its list, or {@code null}
     * @param selectedCarouselCardIndex the card index when the button is inside a carousel, or {@code null}
     */
    TemplateButtonReplyMessage(String selectedId, String selectedDisplayText, ContextInfo contextInfo, Integer selectedIndex, Integer selectedCarouselCardIndex) {
        this.selectedId = selectedId;
        this.selectedDisplayText = selectedDisplayText;
        this.contextInfo = contextInfo;
        this.selectedIndex = selectedIndex;
        this.selectedCarouselCardIndex = selectedCarouselCardIndex;
    }

    /**
     * Returns the identifier of the button that the recipient selected.
     *
     * @return an {@link Optional} containing the button identifier, or empty if not set
     */
    public Optional<String> selectedId() {
        return Optional.ofNullable(selectedId);
    }

    /**
     * Returns the human-readable label shown on the selected button.
     *
     * @return an {@link Optional} containing the display text, or empty if not set
     */
    public Optional<String> selectedDisplayText() {
        return Optional.ofNullable(selectedDisplayText);
    }

    /**
     * Returns the contextual information that links this reply back to the
     * original template message.
     *
     * @return an {@link Optional} containing the context, or empty if not set
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Returns the zero-based position of the selected button inside its
     * parent list.
     *
     * @return an {@link OptionalInt} with the button index, or empty if not set
     */
    public OptionalInt selectedIndex() {
        return selectedIndex == null ? OptionalInt.empty() : OptionalInt.of(selectedIndex);
    }

    /**
     * Returns the zero-based index of the carousel card that contained the
     * selected button. Only present when the reply originates from an
     * interactive carousel message.
     *
     * @return an {@link OptionalInt} with the carousel card index, or empty if not set
     */
    public OptionalInt selectedCarouselCardIndex() {
        return selectedCarouselCardIndex == null ? OptionalInt.empty() : OptionalInt.of(selectedCarouselCardIndex);
    }

    /**
     * Sets the identifier of the selected button.
     *
     * @param selectedId the new button identifier, or {@code null} to clear the field
     */
    public void setSelectedId(String selectedId) {
        this.selectedId = selectedId;
    }

    /**
     * Sets the display text shown on the selected button.
     *
     * @param selectedDisplayText the new display text, or {@code null} to clear the field
     */
    public void setSelectedDisplayText(String selectedDisplayText) {
        this.selectedDisplayText = selectedDisplayText;
    }

    /**
     * Sets the contextual information attached to this reply.
     *
     * @param contextInfo the new context, or {@code null} to clear the field
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Sets the zero-based position of the selected button.
     *
     * @param selectedIndex the new index, or {@code null} to clear the field
     */
    public void setSelectedIndex(Integer selectedIndex) {
        this.selectedIndex = selectedIndex;
    }

    /**
     * Sets the zero-based index of the carousel card that contained the
     * selected button.
     *
     * @param selectedCarouselCardIndex the new carousel card index, or {@code null} to clear the field
     */
    public void setSelectedCarouselCardIndex(Integer selectedCarouselCardIndex) {
        this.selectedCarouselCardIndex = selectedCarouselCardIndex;
    }
}
