package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.business.NoteStateBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.media.NoteEditAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

/**
 * Applies the {@code note_edit} app-state action that creates, edits, or
 * deletes per-chat business notes.
 *
 * <p>This handler backs the WhatsApp Business "Notes about this chat" surface:
 * each mutation either deletes a note by id or upserts a note record carrying
 * type, chat JID, content, and creation timestamp. The mutation index keys
 * each entry by the note id, formatted as
 * {@snippet :
 *     ["note_edit", noteId]
 * }
 *
 * @implNote
 * This implementation flattens WA Web's batch {@code applyMutations} into a
 * per-mutation call: WA Web's per-batch malformed counters and trailing log
 * traces are dropped, the frontend note dispatches are omitted (Cobalt has no
 * frontend bridge), and the note-id resolution helper is specialised to a
 * synchronous SHA-256 over the resolved chat JID. The chat JID is validated
 * via {@link Jid#of(String)} which is laxer than WA Web's chat-JID validator;
 * an unresolvable chat surfaces as
 * {@link MutationApplicationResult#orphan(String, String)} with
 * {@code modelType="Chat"}.
 */
@WhatsAppWebModule(moduleName = "WAWebNoteSync")
public final class NoteEditHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link NoteEditHandler}.
     *
     * @implNote
     * This implementation logs at {@link Level#WARNING} for missing
     * {@code createdAt}/{@code unstructuredContent} fields and for exceptions,
     * mirroring the WA Web warning surface.
     */
    private static final System.Logger LOGGER = Log.get(NoteEditHandler.class);

    /**
     * Constructs a stateless {@link NoteEditHandler} for registration in the
     * sync handler registry.
     */
    public NoteEditHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebNoteSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return NoteEditAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebNoteSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return NoteEditAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebNoteSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return NoteEditAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Only {@link SyncdOperation#SET} is accepted. A missing index slot or
     * empty note id is reported as
     * {@link SyncdIndexUtils#malformedActionIndex(String, String)}; a missing
     * {@link NoteEditAction} payload as
     * {@link SyncdIndexUtils#malformedActionValue(String)};
     * {@link NoteEditAction#deleted()} {@code == true} removes the note by id;
     * a missing {@link NoteEditAction#type()} or {@link NoteEditAction#chatJid()}
     * is malformed; an unresolvable chat is
     * {@link MutationApplicationResult#orphan(String, String)}; and the
     * resolved record is persisted via
     * {@link LinkedWhatsAppBusinessStore#putNoteState(com.github.auties00.cobalt.wire.linked.business.NoteState)}
     * keyed by the id from {@link #resolveNoteId(Jid, Jid, String)}. Exceptions
     * surface as {@link MutationApplicationResult#failed()}.
     *
     * @implNote
     * This implementation collapses the chat-table add-or-edit side effect into
     * the single store setter.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebNoteSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        try {
            if (mutation.operation() != SyncdOperation.SET) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "note edit: unsupported operation {0}", mutation.operation());
                return MutationApplicationResult.unsupported();
            }

            var indexArray = JSON.parseArray(mutation.index());
            if (indexArray.size() < 2) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "note edit mutation malformed: missing note id index");
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }
            var noteId = indexArray.getString(1);
            if (noteId == null || noteId.isEmpty()) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "note edit mutation malformed: empty note id");
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof NoteEditAction action)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "note edit mutation malformed: missing action value");
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }

            if (action.deleted()) {
                client.store().businessStore().removeNoteState(noteId);
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "note edit: removed note id={0}", noteId);
                return MutationApplicationResult.success();
            }

            var type = action.type().orElse(null);
            var rawChatJid = action.chatJid().orElse(null);
            var content = action.unstructuredContent().orElse(null);
            var createdAtOpt = action.createdAt();

            if (type == null) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "note edit mutation malformed: missing type");
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }
            if (rawChatJid == null) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "note edit mutation malformed: missing chat jid");
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }
            var validatedChatJid = rawChatJid;

            if (createdAtOpt.isEmpty()) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "note edit: noteEditAction.createdAt is empty");
            }
            if (content == null) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "note edit: noteEditAction.unstructuredContent is empty");
            }

            var chat = client.store().chatStore().findChatByJid(validatedChatJid);
            if (chat.isEmpty()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "note edit: orphan chat {0}", validatedChatJid);
                return MutationApplicationResult.orphan(validatedChatJid.toString(), "Chat");
            }

            var resolvedChatJid = chat.get().jid();

            var resolvedNoteId = resolveNoteId(validatedChatJid, resolvedChatJid, noteId);

            client.store().businessStore().putNoteState(new NoteStateBuilder()
                    .id(resolvedNoteId)
                    .type(type)
                    .chatJid(resolvedChatJid)
                    .createdAt(createdAtOpt.orElse(Instant.EPOCH))
                    .deleted(false)
                    .unstructuredContent(content != null ? content : "")
                    .build());

            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "note edit: upserted note for chat {0}", resolvedChatJid);
            return MutationApplicationResult.success();
        } catch (Exception e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "note edit mutation failed", e);
            return MutationApplicationResult.failed();
        }
    }

    /**
     * Resolves the canonical note id used as the store key for a given note
     * edit mutation.
     *
     * <p>Returns the original index id when the action's chat JID matches the
     * resolved canonical chat JID; otherwise derives a stable id from the
     * resolved chat JID via SHA-256 to keep notes addressable when the chat's
     * JID was remapped (for example after a LID/PN migration).
     *
     * @implNote
     * This implementation realises the {@code sha256Str} branch via
     * {@link #generateNoteId(String)}.
     *
     * @param actionChatJid   the validated chat JID from the action payload
     * @param resolvedChatJid the canonical chat JID resolved from the store
     * @param indexNoteId     the note id from the mutation index
     * @return the resolved note id used as the store key
     */
    @WhatsAppWebExport(moduleName = "WAWebNoteSync", exports = "resolveNoteId", adaptation = WhatsAppAdaptation.DIRECT)
    private String resolveNoteId(Jid actionChatJid, Jid resolvedChatJid, String indexNoteId) {
        if (resolvedChatJid.equals(actionChatJid)) {
            return indexNoteId;
        }
        return generateNoteId(resolvedChatJid.toString());
    }

    /**
     * Returns the base64-encoded SHA-256 hash of the given string.
     *
     * <p>This is the canonical way to derive a stable note id from a chat JID
     * when the action's JID and the resolved JID differ.
     *
     * @implNote
     * This implementation calls {@link MessageDigest#getInstance(String)} with
     * {@code "SHA-256"} and {@link Base64#getEncoder()}; the wire output is
     * identical to WA Web's because both pipelines use raw SHA-256 bytes and
     * standard base64. A missing SHA-256 provider surfaces as
     * {@link IllegalStateException}.
     *
     * @param input the string to hash
     * @return the base64-encoded SHA-256 hash of the input
     * @throws IllegalStateException if the SHA-256 algorithm is not available
     *                               on the JVM
     */
    @WhatsAppWebExport(moduleName = "WAWebNotesIdUtils", exports = "generateNoteId", adaptation = WhatsAppAdaptation.ADAPTED)
    private String generateNoteId(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

}
