package com.github.auties00.cobalt.wire.linked.bot.ai;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents the cached state of the Meta AI home screen within WhatsApp.
 *
 * <p>The AI home screen is the entry point for interacting with Meta AI. It displays
 * two categories of interactive cards that are periodically fetched from the server:
 * <ul>
 *   <li>{@linkplain #capabilityOptions() capability options}: cards that showcase specific
 *       AI features such as image generation, photo animation, and file analysis
 *   <li>{@linkplain #conversationOptions() conversation options}: suggested prompts the user
 *       can tap to start a new conversation with the AI
 * </ul>
 *
 * <p>The {@linkplain #lastFetchTime() last fetch time} records when these cards were
 * last retrieved from the server, allowing the client to determine whether the cached
 * state is stale and should be refreshed.
 */
@ProtobufMessage(name = "AIHomeState")
public final class AIHomeState {
    /**
     * The timestamp, in seconds since the Unix epoch, at which the home screen
     * options were last fetched from the server.
     *
     * <p>The client uses this value to determine whether the cached cards are stale
     * and need to be re-fetched. A {@code null} value indicates that no fetch has
     * been recorded.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant lastFetchTime;

    /**
     * The list of capability-oriented cards displayed on the AI home screen.
     *
     * <p>Each {@link AIHomeOption} in this list highlights a specific AI feature
     * (such as image creation, photo animation, or file analysis) and may include
     * a pre-filled prompt that is sent to the AI when the user taps the card.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<AIHomeOption> capabilityOptions;

    /**
     * The list of conversation-starter cards displayed on the AI home screen.
     *
     * <p>Each {@code AIHomeOption} in this list provides a pre-written prompt that
     * the user can tap to instantly begin a new conversation with Meta AI, without
     * having to type a query manually.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    List<AIHomeOption> conversationOptions;


    /**
     * Constructs a new {@code AIHomeState} with the specified values.
     *
     * @param lastFetchTime       the timestamp of the last server fetch, or {@code null} if unknown
     * @param capabilityOptions   the capability cards to display, or {@code null} for none
     * @param conversationOptions the conversation-starter cards to display, or {@code null} for none
     */
    AIHomeState(Instant lastFetchTime, List<AIHomeOption> capabilityOptions, List<AIHomeOption> conversationOptions) {
        this.lastFetchTime = lastFetchTime;
        this.capabilityOptions = capabilityOptions;
        this.conversationOptions = conversationOptions;
    }

    /**
     * Returns the timestamp at which the home screen options were last fetched
     * from the server.
     *
     * @return an {@code Optional} containing the last fetch time, or an empty
     *         {@code Optional} if no fetch has been recorded
     */
    public Optional<Instant> lastFetchTime() {
        return Optional.ofNullable(lastFetchTime);
    }

    /**
     * Returns the list of capability-oriented cards.
     *
     * @return an unmodifiable list of capability options, never {@code null}
     */
    public List<AIHomeOption> capabilityOptions() {
        return capabilityOptions == null ? List.of() : Collections.unmodifiableList(capabilityOptions);
    }

    /**
     * Returns the list of conversation-starter cards.
     *
     * @return an unmodifiable list of conversation options, never {@code null}
     */
    public List<AIHomeOption> conversationOptions() {
        return conversationOptions == null ? List.of() : Collections.unmodifiableList(conversationOptions);
    }

    /**
     * Sets the timestamp at which the home screen options were last fetched
     * from the server.
     *
     * @param lastFetchTime the new fetch timestamp, or {@code null} to clear
     */
    public void setLastFetchTime(Instant lastFetchTime) {
        this.lastFetchTime = lastFetchTime;
    }

    /**
     * Sets the list of capability-oriented cards displayed on the AI home screen.
     *
     * @param capabilityOptions the new capability options list, or {@code null} for none
     */
    public void setCapabilityOptions(List<AIHomeOption> capabilityOptions) {
        this.capabilityOptions = capabilityOptions;
    }

    /**
     * Sets the list of conversation-starter cards displayed on the AI home screen.
     *
     * @param conversationOptions the new conversation options list, or {@code null} for none
     */
    public void setConversationOptions(List<AIHomeOption> conversationOptions) {
        this.conversationOptions = conversationOptions;
    }

    /**
     * Represents an interactive card displayed on the Meta AI home screen.
     *
     * <p>Each option card has a {@linkplain #type() type} that determines the action
     * performed when the user taps it (for example, sending a text prompt or opening
     * the image generation flow), a {@linkplain #title() display title}, and optional
     * visual styling properties for its icon including a
     * {@linkplain #imageAssetIdentifier() WhatsApp Design System asset identifier},
     * a {@linkplain #imageTintColor() tint color}, and a
     * {@linkplain #imageBackgroundColor() background color}.
     *
     * <p>When the user taps a card, the {@linkplain #promptText() prompt text} is sent
     * to the AI within the {@linkplain #sessionId() session} associated with this option.
     */
    @ProtobufMessage(name = "AIHomeState.AIHomeOption")
    public static final class AIHomeOption {
        /**
         * The action type that determines what happens when the user taps this card.
         * For example, {@link AIHomeActionType#PROMPT PROMPT} sends a text query,
         * while {@link AIHomeActionType#CREATE_IMAGE CREATE_IMAGE} opens the image
         * generation flow.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        AIHomeOption.AIHomeActionType type;

        /**
         * The display title shown on the card, for example {@code "Create an image"}.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String title;

        /**
         * The prompt text sent to the AI when this option is tapped, for example
         * {@code "Draw a sunset over the ocean"}.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String promptText;

        /**
         * The identifier of the AI session to associate with this option, for example
         * {@code "abc123-def456"}.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        String sessionId;

        /**
         * The WhatsApp Design System (WDS) asset identifier for the icon image
         * displayed on this card, for example {@code "ai_create_image_icon"}.
         *
         * <p>This identifier is resolved by the client's asset pipeline to load
         * the corresponding icon graphic.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        String imageAssetIdentifier;

        /**
         * The tint color applied to the icon image, as a CSS-compatible color string,
         * for example {@code "#FFFFFF"}.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        String imageTintColor;

        /**
         * The background color behind the icon image, as a CSS-compatible color string,
         * for example {@code "#1A73E8"}.
         */
        @ProtobufProperty(index = 7, type = ProtobufType.STRING)
        String imageBackgroundColor;

        /**
         * An identifier for the card layout template used for rendering this option,
         * for example {@code "capability_card"} or {@code "conversation_card"}.
         *
         * <p>The client uses this identifier to select the appropriate visual layout
         * and styling for the card.
         */
        @ProtobufProperty(index = 8, type = ProtobufType.STRING)
        String cardTypeId;


        /**
         * Constructs a new {@code AIHomeOption} with the specified values.
         *
         * @param type                 the action type for this card, or {@code null}
         * @param title                the display title shown on the card, or {@code null}
         * @param promptText           the prompt text sent to the AI on tap, or {@code null}
         * @param sessionId            the AI session identifier, or {@code null}
         * @param imageAssetIdentifier the WDS icon asset identifier, or {@code null}
         * @param imageTintColor       the icon tint color string, or {@code null}
         * @param imageBackgroundColor the icon background color string, or {@code null}
         * @param cardTypeId           the card layout template identifier, or {@code null}
         */
        AIHomeOption(AIHomeActionType type, String title, String promptText, String sessionId, String imageAssetIdentifier, String imageTintColor, String imageBackgroundColor, String cardTypeId) {
            this.type = type;
            this.title = title;
            this.promptText = promptText;
            this.sessionId = sessionId;
            this.imageAssetIdentifier = imageAssetIdentifier;
            this.imageTintColor = imageTintColor;
            this.imageBackgroundColor = imageBackgroundColor;
            this.cardTypeId = cardTypeId;
        }

        /**
         * Returns the action type that determines what happens when this card is tapped.
         *
         * @return an {@code Optional} containing the action type, or an empty
         *         {@code Optional} if not set
         */
        public Optional<AIHomeActionType> type() {
            return Optional.ofNullable(type);
        }

        /**
         * Returns the display title shown on the card.
         *
         * @return an {@code Optional} describing the title, or an empty
         *         {@code Optional} if not set
         */
        public Optional<String> title() {
            return Optional.ofNullable(title);
        }

        /**
         * Returns the prompt text sent to the AI when this option is tapped.
         *
         * @return an {@code Optional} describing the prompt text, or an empty
         *         {@code Optional} if not set
         */
        public Optional<String> promptText() {
            return Optional.ofNullable(promptText);
        }

        /**
         * Returns the AI session identifier associated with this option.
         *
         * @return an {@code Optional} describing the session identifier, or an empty
         *         {@code Optional} if not set
         */
        public Optional<String> sessionId() {
            return Optional.ofNullable(sessionId);
        }

        /**
         * Returns the WhatsApp Design System asset identifier for the icon image.
         *
         * @return an {@code Optional} containing the WDS asset identifier, or an empty
         *         {@code Optional} if not set
         */
        public Optional<String> imageAssetIdentifier() {
            return Optional.ofNullable(imageAssetIdentifier);
        }

        /**
         * Returns the tint color applied to the icon image.
         *
         * @return an {@code Optional} describing the tint color, or an empty
         *         {@code Optional} if not set
         */
        public Optional<String> imageTintColor() {
            return Optional.ofNullable(imageTintColor);
        }

        /**
         * Returns the background color behind the icon image.
         *
         * @return an {@code Optional} describing the background color, or an empty
         *         {@code Optional} if not set
         */
        public Optional<String> imageBackgroundColor() {
            return Optional.ofNullable(imageBackgroundColor);
        }

        /**
         * Returns the card layout template identifier used for rendering.
         *
         * @return an {@code Optional} containing the card type identifier, or an empty
         *         {@code Optional} if not set
         */
        public Optional<String> cardTypeId() {
            return Optional.ofNullable(cardTypeId);
        }

        /**
         * Sets the action type that determines what happens when this card is tapped.
         *
         * @param type the new action type, or {@code null} to clear
         */
        public void setType(AIHomeActionType type) {
            this.type = type;
    }

        /**
         * Sets the display title shown on the card.
         *
         * @param title the new title, or {@code null} to clear
         */
        public void setTitle(String title) {
            this.title = title;
    }

        /**
         * Sets the prompt text sent to the AI when this option is tapped.
         *
         * @param promptText the new prompt text, or {@code null} to clear
         */
        public void setPromptText(String promptText) {
            this.promptText = promptText;
    }

        /**
         * Sets the AI session identifier associated with this option.
         *
         * @param sessionId the new session identifier, or {@code null} to clear
         */
        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
    }

        /**
         * Sets the WhatsApp Design System asset identifier for the icon image.
         *
         * @param imageAssetIdentifier the new WDS asset identifier, or {@code null} to clear
         */
        public void setImageAssetIdentifier(String imageAssetIdentifier) {
            this.imageAssetIdentifier = imageAssetIdentifier;
    }

        /**
         * Sets the tint color applied to the icon image.
         *
         * @param imageTintColor the new tint color, or {@code null} to clear
         */
        public void setImageTintColor(String imageTintColor) {
            this.imageTintColor = imageTintColor;
    }

        /**
         * Sets the background color behind the icon image.
         *
         * @param imageBackgroundColor the new background color, or {@code null} to clear
         */
        public void setImageBackgroundColor(String imageBackgroundColor) {
            this.imageBackgroundColor = imageBackgroundColor;
    }

        /**
         * Sets the card layout template identifier used for rendering.
         *
         * @param cardTypeId the new card type identifier, or {@code null} to clear
         */
        public void setCardTypeId(String cardTypeId) {
            this.cardTypeId = cardTypeId;
    }

        /**
         * Enumerates the types of actions that can be triggered when a user taps
         * an {@link AIHomeOption} card on the Meta AI home screen.
         *
         * <p>Each constant corresponds to a distinct AI feature flow that the
         * client opens or initiates on behalf of the user.
         */
        @ProtobufEnum(name = "AIHomeState.AIHomeOption.AIHomeActionType")
        public static enum AIHomeActionType {
            /**
             * Sends a free-text prompt to Meta AI and displays the response
             * as a standard chat message.
             */
            PROMPT(0),

            /**
             * Opens the AI image generation flow, where the user can describe
             * an image and the AI creates it.
             */
            CREATE_IMAGE(1),

            /**
             * Opens the photo animation flow, where the user can select a
             * static photo and the AI animates it.
             */
            ANIMATE_PHOTO(2),

            /**
             * Opens the file analysis flow, where the user can upload a
             * document and the AI provides a summary or answers questions
             * about its content.
             */
            ANALYZE_FILE(3);

            /**
             * Constructs an {@code AIHomeActionType} with the given protobuf index.
             *
             * @param index the protobuf index value
             */
            AIHomeActionType(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The protobuf index value associated with this enum constant.
             */
            final int index;

            /**
             * Returns the protobuf index value associated with this enum constant.
             *
             * @return the protobuf index
             */
            public int index() {
                return this.index;
            }
        }
    }
}
