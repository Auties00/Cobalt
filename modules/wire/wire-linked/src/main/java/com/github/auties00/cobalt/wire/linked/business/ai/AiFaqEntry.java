package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * One frequently-asked-question entry a WhatsApp Business AI agent answers from.
 *
 * <p>The merchant seeds the auto-reply assistant with example questions and the
 * answers it should give. Each such example is one entry, carrying the
 * {@link #question() question}, its {@link #answer() answer}, and, once the
 * server has stored it, the {@link #id() identifier} that names it.
 */
@ProtobufMessage(name = "AiFaqEntry")
public final class AiFaqEntry {
    /**
     * Example question the assistant should recognise. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String question;

    /**
     * Answer the assistant should give for the question. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String answer;

    /**
     * Server-assigned identifier of this entry. Empty for an entry the server
     * has not stored yet.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String id;

    /**
     * Constructs a new {@code AiFaqEntry}. Every argument may be {@code null}
     * to leave the corresponding field unset.
     *
     * @param question the example question, or {@code null}
     * @param answer   the answer, or {@code null}
     * @param id       the entry identifier, or {@code null} when not yet stored
     */
    AiFaqEntry(String question, String answer, String id) {
        this.question = question;
        this.answer = answer;
        this.id = id;
    }

    /**
     * Returns the example question the assistant should recognise.
     *
     * @return an {@link Optional} carrying the question, or empty when unset
     */
    public Optional<String> question() {
        return Optional.ofNullable(question);
    }

    /**
     * Returns the answer the assistant should give.
     *
     * @return an {@link Optional} carrying the answer, or empty when unset
     */
    public Optional<String> answer() {
        return Optional.ofNullable(answer);
    }

    /**
     * Returns the server-assigned identifier of this entry.
     *
     * @return an {@link Optional} carrying the identifier, or empty when unset
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }
}
