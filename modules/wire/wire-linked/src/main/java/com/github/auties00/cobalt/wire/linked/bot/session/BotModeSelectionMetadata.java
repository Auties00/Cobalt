package com.github.auties00.cobalt.wire.linked.bot.session;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * Metadata describing the AI processing modes the user has selected for a
 * bot interaction on WhatsApp.
 *
 * <p>The user can choose between different response modes, for example the
 * default fast mode or a deeper "Think Hard" reasoning mode that takes
 * longer but produces more thorough answers. The selected modes are sent to
 * the server as part of the bot request so the AI backend can adjust its
 * generation strategy accordingly.
 *
 * <p>This metadata is carried inside
 * {@link com.github.auties00.cobalt.wire.linked.bot.BotMetadata#botModeSelectionMetadata()}.
 */
@ProtobufMessage(name = "BotModeSelectionMetadata")
public final class BotModeSelectionMetadata {
    /**
     * The list of AI processing modes the user has selected for this
     * interaction.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    List<BotUserSelectionMode> mode;


    /**
     * Constructs a new {@code BotModeSelectionMetadata} with the specified
     * modes.
     *
     * @param mode the list of selected modes, or {@code null}
     */
    BotModeSelectionMetadata(List<BotUserSelectionMode> mode) {
        this.mode = mode;
    }

    /**
     * Returns the list of AI processing modes the user has selected.
     *
     * @return an unmodifiable list of selected modes, or an empty list if
     *         none were set
     */
    public List<BotUserSelectionMode> mode() {
        return mode == null ? List.of() : Collections.unmodifiableList(mode);
    }

    /**
     * Sets the list of AI processing modes the user has selected.
     *
     * @param mode the new list of selected modes, or {@code null}
     */
    public void setMode(List<BotUserSelectionMode> mode) {
        this.mode = mode;
    }

    /**
     * Enumerates the AI processing modes a user can select to control how
     * the bot generates its response.
     */
    @ProtobufEnum(name = "BotModeSelectionMetadata.BotUserSelectionMode")
    public static enum BotUserSelectionMode {
        /**
         * The standard response mode with balanced speed and quality.
         */
        DEFAULT_MODE(0),

        /**
         * A deeper reasoning mode (also known as "Think Hard") that
         * produces more thorough, step-by-step responses at the cost of
         * increased latency and quota consumption.
         */
        THINK_HARD_MODE(1);

        /**
         * Constructs a new user selection mode constant with the specified
         * protobuf index.
         *
         * @param index the protobuf enum index
         */
        BotUserSelectionMode(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        /**
         * Returns the protobuf enum index of this user selection mode.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }
}
