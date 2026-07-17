package com.github.auties00.cobalt.wire.linked.business;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import com.github.auties00.cobalt.wire.linked.sync.action.media.NoteEditAction.NoteType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A model representing a single chat-attached note in WhatsApp Business.
 *
 * <p>The "Add note to customer" feature lets a business user attach
 * private annotations to a customer chat — annotations only the business
 * team can see. Each note carries a stable {@linkplain #id() identifier},
 * the structural {@linkplain #type() type} of the payload (free-text vs.
 * structured), the {@linkplain #chatJid() chat JID} the note is attached
 * to, the {@linkplain #createdAt() creation instant}, a
 * {@linkplain #deleted() deletion flag}, and the
 * {@linkplain #unstructuredContent() free-text body} for unstructured
 * notes.
 *
 * <p>Cobalt persists each note independently so callers can resolve a
 * single note by id without scanning the entire annotation table.
 *
 * <p>This class is a local model only. Modifying its fields does not send
 * any request to the WhatsApp servers; it simply reflects the locally
 * cached state.
 */
@ProtobufMessage
public final class NoteState {
    /**
     * The non-{@code null} stable identifier of the note. Used as the
     * primary key by Cobalt's store.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * The structural category of the note payload, governing how
     * {@link #unstructuredContent} is interpreted, or {@code null} when
     * no type has been set.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    NoteType type;

    /**
     * The JID of the chat this note is attached to, or {@code null}
     * when no chat has been set.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    Jid chatJid;

    /**
     * The instant at which the note was created, or {@code null} when
     * no creation time has been recorded.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant createdAt;

    /**
     * The tombstone flag set to {@code true} when the note has been
     * deleted.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    boolean deleted;

    /**
     * The free-text body for {@link NoteType#UNSTRUCTURED} notes, or
     * {@code null} when the note is structured or empty.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String unstructuredContent;

    /**
     * Constructs a new note state with the given fields.
     *
     * @param id                  the non-{@code null} note identifier
     * @param type                the note type, or {@code null}
     * @param chatJid             the chat JID, or {@code null}
     * @param createdAt           the creation instant, or {@code null}
     * @param deleted             whether the note is tombstoned
     * @param unstructuredContent the free-text body, or {@code null}
     */
    NoteState(String id, NoteType type, Jid chatJid, Instant createdAt, boolean deleted, String unstructuredContent) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.type = type;
        this.chatJid = chatJid;
        this.createdAt = createdAt;
        this.deleted = deleted;
        this.unstructuredContent = unstructuredContent;
    }

    /**
     * Returns the non-{@code null} note identifier.
     *
     * @return the note identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns the structural category of the note payload.
     *
     * @return an {@code Optional} containing the note type, or empty if
     *         not set
     */
    public Optional<NoteType> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Updates the structural category of the note payload.
     *
     * @param type the new note type, or {@code null} to clear
     * @return this note state instance for method chaining
     */
    public NoteState setType(NoteType type) {
        this.type = type;
        return this;
    }

    /**
     * Returns the JID of the chat this note is attached to.
     *
     * @return an {@code Optional} containing the chat JID, or empty if
     *         not set
     */
    public Optional<Jid> chatJid() {
        return Optional.ofNullable(chatJid);
    }

    /**
     * Updates the chat JID this note is attached to.
     *
     * @param chatJid the new chat JID, or {@code null} to clear
     * @return this note state instance for method chaining
     */
    public NoteState setChatJid(Jid chatJid) {
        this.chatJid = chatJid;
        return this;
    }

    /**
     * Returns the instant at which the note was created.
     *
     * @return an {@code Optional} containing the creation instant, or
     *         empty if not set
     */
    public Optional<Instant> createdAt() {
        return Optional.ofNullable(createdAt);
    }

    /**
     * Updates the creation instant of the note.
     *
     * @param createdAt the new creation instant, or {@code null} to
     *                  clear
     * @return this note state instance for method chaining
     */
    public NoteState setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    /**
     * Returns whether the note is tombstoned.
     *
     * @return {@code true} if the note has been deleted
     */
    public boolean deleted() {
        return deleted;
    }

    /**
     * Updates the tombstone flag of this note.
     *
     * @param deleted the new flag
     * @return this note state instance for method chaining
     */
    public NoteState setDeleted(boolean deleted) {
        this.deleted = deleted;
        return this;
    }

    /**
     * Returns the free-text body of the note for
     * {@link NoteType#UNSTRUCTURED} payloads.
     *
     * @return an {@code Optional} containing the body, or empty if not
     *         set
     */
    public Optional<String> unstructuredContent() {
        return Optional.ofNullable(unstructuredContent);
    }

    /**
     * Updates the free-text body of the note.
     *
     * @param unstructuredContent the new body, or {@code null} to clear
     * @return this note state instance for method chaining
     */
    public NoteState setUnstructuredContent(String unstructuredContent) {
        this.unstructuredContent = unstructuredContent;
        return this;
    }

    /**
     * Returns a hash code derived from this note's
     * {@linkplain #id() identifier}.
     *
     * @return the hash code of the note identifier
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /**
     * Returns whether this note state is equal to the given object.
     *
     * <p>Two note states are considered equal when they share the same
     * {@linkplain #id() identifier}, regardless of the other fields.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a {@code NoteState}
     *         with the same identifier
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof NoteState that && Objects.equals(this.id, that.id);
    }
}
