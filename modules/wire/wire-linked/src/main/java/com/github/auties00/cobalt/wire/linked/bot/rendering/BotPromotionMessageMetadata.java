package com.github.auties00.cobalt.wire.linked.bot.rendering;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Metadata for a promotional message displayed within the AI bot chat interface.
 *
 * <p>Promotional messages are used by Meta to surface offers, feature announcements,
 * or feedback surveys to users interacting with the AI bot. Each promotion has a
 * {@linkplain BotPromotionType type} that identifies the campaign and a
 * {@linkplain #buttonTitle() button title} for the call-to-action element.
 *
 * <p>This type is referenced from
 * {@link com.github.auties00.cobalt.wire.linked.bot.BotMetadata BotMetadata} as the
 * {@code botPromotionMessageMetadata} field (protobuf index 21).
 */
@ProtobufMessage(name = "BotPromotionMessageMetadata")
public final class BotPromotionMessageMetadata {
    /**
     * The type of promotion campaign associated with this message.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    BotPromotionType promotionType;

    /**
     * The display text on the promotional call-to-action button, for example
     * {@code "Learn More"} or {@code "Take Survey"}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String buttonTitle;


    /**
     * Constructs a new {@code BotPromotionMessageMetadata} with the specified values.
     *
     * @param promotionType the promotion type, or {@code null}
     * @param buttonTitle   the button display text, or {@code null}
     */
    BotPromotionMessageMetadata(BotPromotionType promotionType, String buttonTitle) {
        this.promotionType = promotionType;
        this.buttonTitle = buttonTitle;
    }

    /**
     * Returns the type of promotion campaign.
     *
     * @return an {@code Optional} describing the promotion type, or an empty
     *         {@code Optional} if not set
     */
    public Optional<BotPromotionType> promotionType() {
        return Optional.ofNullable(promotionType);
    }

    /**
     * Returns the display text on the promotional button.
     *
     * @return an {@code Optional} describing the button title, or an empty
     *         {@code Optional} if not set
     */
    public Optional<String> buttonTitle() {
        return Optional.ofNullable(buttonTitle);
    }

    /**
     * Sets the type of promotion campaign.
     *
     * @param promotionType the new promotion type, or {@code null}
     */
    public void setPromotionType(BotPromotionType promotionType) {
        this.promotionType = promotionType;
    }

    /**
     * Sets the display text on the promotional button.
     *
     * @param buttonTitle the new button title, or {@code null}
     */
    public void setButtonTitle(String buttonTitle) {
        this.buttonTitle = buttonTitle;
    }

    /**
     * The type of promotional campaign associated with a bot promotion
     * message.
     */
    @ProtobufEnum(name = "BotPromotionMessageMetadata.BotPromotionType")
    public static enum BotPromotionType {
        /**
         * An unknown or unrecognized promotion type.
         */
        UNKNOWN_TYPE(0),

        /**
         * An internal Meta promotional campaign (campaign identifier "C50").
         */
        C50(1),

        /**
         * A promotion inviting the user to participate in a feedback survey
         * platform.
         */
        SURVEY_PLATFORM(2);

        /**
         * Constructs a new {@code BotPromotionType} with the given protobuf index.
         *
         * @param index the protobuf enum index
         */
        BotPromotionType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf enum index for this promotion type.
         */
        final int index;

        /**
         * Returns the protobuf index of this promotion type.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }
}
