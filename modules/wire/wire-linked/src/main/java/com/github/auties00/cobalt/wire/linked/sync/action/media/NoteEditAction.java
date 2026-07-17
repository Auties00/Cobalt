package com.github.auties00.cobalt.wire.linked.sync.action.media;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;

/**
 * App-state sync action that propagates the creation, editing, or
 * deletion of a chat-scoped note across linked devices.
 *
 * <p>Notes are short pieces of text the user attaches to a specific
 * chat as a personal aide-memoire (for example, "remember to ask about
 * the contract" on a colleague's chat). They live entirely on the user's
 * own devices and never reach other participants. Each mutation carries
 * either the new note body (for create or edit) or the deletion flag
 * (for removal); the target note is identified by the identifier
 * carried in the associated {@link NoteEditActionArgs}.
 *
 * <p>The action lives in the {@link SyncPatchType#REGULAR_LOW} bucket
 * so that bulky note edits do not stall higher-priority mutations.
 */
@ProtobufMessage(name = "SyncActionValue.NoteEditAction")
public final class NoteEditAction implements SyncAction<NoteEditActionArgs> {
    /**
     * Canonical app-state action name that identifies this action type
     * on the wire.
     */
    public static final String ACTION_NAME = "note_edit";

    /**
     * Schema version advertised by this action, used by sync handlers to
     * gate deserialisation and handling of newer payload shapes.
     */
    public static final int ACTION_VERSION = 7;

    /**
     * Collection this action belongs to, used by the sync protocol to
     * route the mutation into the correct replication stream.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

    /**
     * The structural category of the note payload (free-text vs.
     * structured), governing how {@link #unstructuredContent} is
     * interpreted.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    NoteType type;

    /**
     * The JID of the chat this note is attached to.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    Jid chatJid;

    /**
     * The instant at which the note was created. Encoded on the wire as
     * a 64-bit second epoch via {@link InstantSecondsMixin}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant createdAt;

    /**
     * Whether this action represents a deletion of the target note
     * rather than a create or update.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    Boolean deleted;

    /**
     * The free-text note body for {@link NoteType#UNSTRUCTURED} notes.
     * Ignored for {@link NoteType#STRUCTURED}.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String unstructuredContent;

    /**
     * Constructs a new {@code NoteEditAction} carrying the supplied
     * note fields.
     *
     * @param type                the structural category of the note
     * @param chatJid             the chat the note is attached to
     * @param createdAt           the creation instant of the note
     * @param deleted             whether the action deletes the note
     * @param unstructuredContent the free-text body for unstructured
     *                            notes
     */
    NoteEditAction(NoteType type, Jid chatJid, Instant createdAt, Boolean deleted, String unstructuredContent) {
        this.type = type;
        this.chatJid = chatJid;
        this.createdAt = createdAt;
        this.deleted = deleted;
        this.unstructuredContent = unstructuredContent;
    }

    /**
     * Returns the canonical action name for every
     * {@code NoteEditAction}.
     *
     * @return the constant {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version for every {@code NoteEditAction}.
     *
     * @return the constant {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }

    /**
     * Returns the structural category of the note payload.
     *
     * @return the note type, or {@link Optional#empty()} if unset
     */
    public Optional<NoteType> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns the JID of the chat the note is attached to.
     *
     * @return the chat JID, or {@link Optional#empty()} if unset
     */
    public Optional<Jid> chatJid() {
        return Optional.ofNullable(chatJid);
    }

    /**
     * Returns the instant at which the note was created.
     *
     * @return the creation instant, or {@link Optional#empty()} if
     *         unset
     */
    public Optional<Instant> createdAt() {
        return Optional.ofNullable(createdAt);
    }

    /**
     * Returns whether this action represents a deletion of the target
     * note.
     *
     * @return {@code true} if the note is being deleted, {@code false}
     *         otherwise
     */
    public boolean deleted() {
        return deleted != null && deleted;
    }

    /**
     * Returns the free-text body for {@link NoteType#UNSTRUCTURED}
     * notes.
     *
     * @return the note body, or {@link Optional#empty()} if unset
     */
    public Optional<String> unstructuredContent() {
        return Optional.ofNullable(unstructuredContent);
    }

    /**
     * Sets the structural category of the note payload.
     *
     * @param type the new note type, or {@code null} to clear it
     */
    public void setType(NoteType type) {
        this.type = type;
    }

    /**
     * Sets the JID of the chat the note is attached to.
     *
     * @param chatJid the new chat JID, or {@code null} to clear it
     */
    public void setChatJid(Jid chatJid) {
        this.chatJid = chatJid;
    }

    /**
     * Sets the instant at which the note was created.
     *
     * @param createdAt the new creation instant, or {@code null} to
     *                  clear it
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Sets whether this action represents a deletion of the target
     * note.
     *
     * @param deleted the new deletion flag, or {@code null} to clear it
     */
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * Sets the free-text body for {@link NoteType#UNSTRUCTURED} notes.
     *
     * @param unstructuredContent the new note body, or {@code null} to
     *                            clear it
     */
    public void setUnstructuredContent(String unstructuredContent) {
        this.unstructuredContent = unstructuredContent;
    }

    /**
     * The structural category of a chat note payload, distinguishing a
     * simple free-text body from a richer structured representation
     * defined outside this action.
     */
    @ProtobufEnum(name = "SyncActionValue.NoteEditAction.NoteType")
    public enum NoteType {
        /**
         * A note whose body is a single free-form text string carried
         * in {@link NoteEditAction#unstructuredContent()}.
         */
        UNSTRUCTURED(1),

        /**
         * A note with a richer, structured payload defined outside the
         * scope of this action.
         */
        STRUCTURED(2);

        /**
         * The protobuf wire index for this constant.
         */
        final int index;

        /**
         * Constructs a new {@code NoteType} constant with the supplied
         * wire index.
         *
         * @param index the protobuf wire index
         */
        NoteType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the protobuf wire index for this constant.
         *
         * @return the wire index
         */
        public int index() {
            return this.index;
        }
    }
}
