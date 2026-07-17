package com.github.auties00.cobalt.wire.linked.bot.feedback;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Carries metadata describing changes to the AI bot's memory about the user.
 *
 * <p>When Meta AI learns new facts or forgets previously memorized information,
 * this metadata captures the delta: the list of {@linkplain #addedFacts() newly
 * added facts} and {@linkplain #removedFacts() removed facts}. A
 * {@linkplain #disclaimer() disclaimer} may accompany the memory update to
 * inform the user about how their data is being stored and used.
 *
 * <p>This metadata is attached to bot messages as part of the
 * {@code BotMetadata.memoryMetadata} field, allowing the client to render
 * memory change notifications inline within the conversation.
 *
 * @see BotMemoryFact
 */
@ProtobufMessage(name = "BotMemoryMetadata")
public final class BotMemoryMetadata {
    /**
     * The list of facts newly memorized by the AI bot in this update.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<BotMemoryFact> addedFacts;

    /**
     * The list of facts removed from the AI bot's memory in this update.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<BotMemoryFact> removedFacts;

    /**
     * A disclaimer text shown to the user about memory usage, for example
     * {@code "Meta AI uses memories to personalize your experience"}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String disclaimer;


    /**
     * Constructs a new {@code BotMemoryMetadata} with the specified values.
     *
     * @param addedFacts   the newly added facts, or {@code null}
     * @param removedFacts the removed facts, or {@code null}
     * @param disclaimer   the disclaimer text, or {@code null}
     */
    BotMemoryMetadata(List<BotMemoryFact> addedFacts, List<BotMemoryFact> removedFacts, String disclaimer) {
        this.addedFacts = addedFacts;
        this.removedFacts = removedFacts;
        this.disclaimer = disclaimer;
    }

    /**
     * Returns the list of facts newly memorized by the AI bot.
     *
     * @return an unmodifiable list of added facts, never {@code null}
     */
    public List<BotMemoryFact> addedFacts() {
        return addedFacts == null ? List.of() : Collections.unmodifiableList(addedFacts);
    }

    /**
     * Returns the list of facts removed from the AI bot's memory.
     *
     * @return an unmodifiable list of removed facts, never {@code null}
     */
    public List<BotMemoryFact> removedFacts() {
        return removedFacts == null ? List.of() : Collections.unmodifiableList(removedFacts);
    }

    /**
     * Returns the disclaimer text about memory usage.
     *
     * @return an {@code Optional} describing the disclaimer, or an empty
     *         {@code Optional} if not set
     */
    public Optional<String> disclaimer() {
        return Optional.ofNullable(disclaimer);
    }

    /**
     * Sets the list of facts newly memorized by the AI bot.
     *
     * @param addedFacts the new list of added facts, or {@code null}
     */
    public void setAddedFacts(List<BotMemoryFact> addedFacts) {
        this.addedFacts = addedFacts;
    }

    /**
     * Sets the list of facts removed from the AI bot's memory.
     *
     * @param removedFacts the new list of removed facts, or {@code null}
     */
    public void setRemovedFacts(List<BotMemoryFact> removedFacts) {
        this.removedFacts = removedFacts;
    }

    /**
     * Sets the disclaimer text about memory usage.
     *
     * @param disclaimer the new disclaimer text, or {@code null}
     */
    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
