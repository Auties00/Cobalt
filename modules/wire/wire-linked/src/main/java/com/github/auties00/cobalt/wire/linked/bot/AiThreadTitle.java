package com.github.auties00.cobalt.wire.linked.bot;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * A model representing the user-set title of a single Meta AI conversation
 * thread.
 *
 * <p>The Meta AI assistant exposes multiple labelled conversation threads
 * (similar to ChatGPT-style chat tabs). Each thread is identified by a
 * stable id and carries an optional human-readable title that the user can
 * rename at will.
 *
 * <p>Cobalt persists each thread title independently so callers can resolve
 * the label of a single thread without iterating the entire map. Updates
 * arrive through the {@code AiThreadRenameAction} sync action.
 *
 * <p>This class is a local model only. Modifying its fields does not send any
 * request to the WhatsApp servers; it simply reflects the locally cached
 * state.
 */
@ProtobufMessage
public final class AiThreadTitle {
    /**
     * The non-{@code null} stable identifier of the AI thread. Used as the
     * primary key by Cobalt's store.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String threadId;

    /**
     * The user-set title of the thread, or {@code null} when no title has
     * been set or the title has been cleared.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String title;

    /**
     * Constructs a new AI thread title with the given identifier and title.
     *
     * @param threadId the non-{@code null} thread identifier
     * @param title    the title, or {@code null}
     */
    AiThreadTitle(String threadId, String title) {
        this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
        this.title = title;
    }

    /**
     * Returns the non-{@code null} thread identifier.
     *
     * @return the thread identifier
     */
    public String threadId() {
        return threadId;
    }

    /**
     * Returns the user-set title of the thread.
     *
     * @return an {@code Optional} containing the title, or empty when no
     *         title has been set
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Updates the title of the thread.
     *
     * @param title the new title, or {@code null} to clear it
     * @return this thread-title instance for method chaining
     */
    public AiThreadTitle setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Returns a hash code derived from this entry's
     * {@linkplain #threadId() identifier}.
     *
     * @return the hash code of the thread identifier
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(threadId);
    }

    /**
     * Returns whether this thread title is equal to the given object.
     *
     * <p>Two thread titles are considered equal when they share the same
     * {@linkplain #threadId() identifier}, regardless of the title string.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is an {@code AiThreadTitle}
     *         with the same identifier
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof AiThreadTitle that && Objects.equals(this.threadId, that.threadId);
    }
}
