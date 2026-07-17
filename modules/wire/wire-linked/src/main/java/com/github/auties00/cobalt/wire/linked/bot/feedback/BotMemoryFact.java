package com.github.auties00.cobalt.wire.linked.bot.feedback;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents a single fact that the AI bot has memorized about the user.
 *
 * <p>Meta AI can remember user preferences and personal details across
 * conversations. Each remembered piece of information is represented as a
 * {@code BotMemoryFact} with a human-readable {@linkplain #fact() text
 * description} and a unique {@linkplain #factId() server-assigned identifier}
 * that can be used to reference or remove the fact later.
 *
 * @see BotMemoryMetadata
 */
@ProtobufMessage(name = "BotMemoryFact")
public final class BotMemoryFact {
    /**
     * The human-readable text of the memorized fact, for example
     * {@code "User prefers vegetarian recipes"}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String fact;

    /**
     * The unique identifier for this fact, for example {@code "fact_a1b2c3"}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String factId;


    /**
     * Constructs a new {@code BotMemoryFact} with the specified values.
     *
     * @param fact   the memorized fact text, or {@code null}
     * @param factId the unique fact identifier, or {@code null}
     */
    BotMemoryFact(String fact, String factId) {
        this.fact = fact;
        this.factId = factId;
    }

    /**
     * Returns the human-readable text of the memorized fact.
     *
     * @return an {@code Optional} describing the fact text, or an empty
     *         {@code Optional} if not set
     */
    public Optional<String> fact() {
        return Optional.ofNullable(fact);
    }

    /**
     * Returns the unique identifier for this fact.
     *
     * @return an {@code Optional} describing the fact identifier, or an empty
     *         {@code Optional} if not set
     */
    public Optional<String> factId() {
        return Optional.ofNullable(factId);
    }

    /**
     * Sets the human-readable text of the memorized fact.
     *
     * @param fact the new fact text, or {@code null}
     */
    public void setFact(String fact) {
        this.fact = fact;
    }

    /**
     * Sets the unique identifier for this fact.
     *
     * @param factId the new fact identifier, or {@code null}
     */
    public void setFactId(String factId) {
        this.factId = factId;
    }
}
