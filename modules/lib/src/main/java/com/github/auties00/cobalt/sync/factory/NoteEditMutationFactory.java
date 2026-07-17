package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.media.NoteEditAction;
import com.github.auties00.cobalt.wire.linked.sync.action.media.NoteEditActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing note-edit sync mutations.
 *
 * <p>Backs the chat-scoped notes feature on the Business chat-info surface. A single call
 * produces the {@link SyncPendingMutation} that is consumed on receiving devices by
 * {@link com.github.auties00.cobalt.sync.handler.NoteEditHandler}.
 *
 * @implNote
 * This implementation mirrors {@code WAWebNoteSync.getNoteMutation} but adds support for
 * deletions: WA Web emits a separate deletion path that the receiver detects via a deletion flag
 * on the inbound action, so Cobalt surfaces a single builder with a boolean {@code deleted} flag
 * rather than two distinct methods.
 */
public final class NoteEditMutationFactory {
    /**
     * The logger for {@link NoteEditMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(NoteEditMutationFactory.class);

    /**
     * Constructs a note-edit mutation factory.
     *
     * <p>The factory keeps no state, so a single instance is sufficient per client.
     */
    public NoteEditMutationFactory() {

    }

    /**
     * Builds a pending outgoing mutation that creates, edits, or deletes a chat-scoped note.
     *
     * <p>Convenience overload that stamps {@link Instant#now()} on both the outer mutation
     * timestamp and the inner note creation field, delegating to
     * {@link #getNoteEditMutation(String, Jid, String, boolean, Instant)} for the actual build.
     *
     * @implNote
     * This implementation accepts {@code content == null} on the deletion path: the receiver still
     * reads both the unstructured content and the chat JID before branching on the deletion flag,
     * so the inner builder substitutes the empty string when the caller omits the content.
     *
     * @param noteId  the note identifier used as the mutation index
     * @param chatJid the chat the note is attached to
     * @param content the note text, may be {@code null} for deletions
     * @param deleted {@code true} when the mutation deletes the note
     * @return the pending mutation ready to be pushed for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebNoteSync", exports = "getNoteMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getNoteEditMutation(String noteId, Jid chatJid, String content, boolean deleted) {
        return getNoteEditMutation(noteId, chatJid, content, deleted, Instant.now());
    }

    /**
     * Builds a pending outgoing mutation that creates, edits, or deletes a chat-scoped note at an
     * explicit {@link Instant}.
     *
     * <p>Delegated to from {@link #getNoteEditMutation(String, Jid, String, boolean)} and from
     * tests that need a deterministic timestamp; the {@code timestamp} parameter is written both
     * to the outer mutation and to {@link NoteEditAction#createdAt()} so a single value covers
     * both fields.
     *
     * @implNote
     * This implementation populates {@link NoteEditAction#deleted()} unconditionally; WA Web's
     * {@code getNoteMutation} omits the deletion flag because it has a separate deletion path. The
     * index follows the standard {@code [actionName, noteId]} shape and writes into the
     * {@code RegularLow} collection alongside the other note-action mutations.
     *
     * @param noteId    the note identifier used as the mutation index
     * @param chatJid   the chat the note is attached to
     * @param content   the note text, may be {@code null} for deletions
     * @param deleted   {@code true} when the mutation deletes the note
     * @param timestamp the timestamp recorded on both the outer mutation and
     *                  {@link NoteEditAction#createdAt()}
     * @return the pending mutation
     */
    public SyncPendingMutation getNoteEditMutation(String noteId, Jid chatJid, String content, boolean deleted, Instant timestamp) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building note edit mutation note={0} chat={1} deleted={2}", noteId, chatJid, deleted);
        var action = new NoteEditActionBuilder()
                .type(NoteEditAction.NoteType.UNSTRUCTURED)
                .chatJid(chatJid)
                .createdAt(timestamp)
                .deleted(deleted)
                .unstructuredContent(content == null ? "" : content)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .noteEditAction(action)
                .build();
        var index = JSON.toJSONString(List.of(NoteEditAction.ACTION_NAME, noteId));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                NoteEditAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
