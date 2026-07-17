package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.linked.business.NoteStateBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.media.NoteEditAction;
import com.github.auties00.cobalt.wire.linked.sync.action.media.NoteEditAction.NoteType;
import com.github.auties00.cobalt.wire.linked.sync.action.media.NoteEditActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.NoteEditMutationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link NoteEditHandler}: a {@link SyncdOperation#SET} carrying type, {@code chatJid} and
 * content installs the note via {@link LinkedWhatsAppBusinessStore#putNoteState}, {@code deleted=true} on a SET
 * drops the entry, an unknown chat JID surfaces as {@link SyncActionState#ORPHAN} with
 * {@code modelType="Chat"}, a wrong-typed value or missing {@link NoteEditAction#type()},
 * {@link NoteEditAction#chatJid()}, note id slot or note id surface as
 * {@link SyncActionState#MALFORMED}, {@link SyncdOperation#REMOVE} surfaces as
 * {@link SyncActionState#UNSUPPORTED}, and the default {@code resolveConflicts} chooses the later
 * timestamp.
 *
 * <p>Inbound mutations are built directly via the
 * {@link #buildMutation(String, NoteEditAction, SyncdOperation, Instant)} helper.
 */
@DisplayName("NoteEditHandler")
class NoteEditHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid CHAT_JID = Jid.of("12345@s.whatsapp.net");

    private LinkedWhatsAppStore store;
    private TestWhatsAppClient client;
    private NoteEditHandler handler;
    private NoteEditMutationFactory factory;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
        handler = new NoteEditHandler();
        factory = new NoteEditMutationFactory();
    }

    // A null indexNoteId produces the singleton-index shape ["note_edit"] for the malformed-index
    // branch; a null action omits the noteEditAction field for the malformed-value branch.
    private DecryptedMutation.Trusted buildMutation(String indexNoteId, NoteEditAction action,
                                                    SyncdOperation operation, Instant ts) {
        var valueBuilder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) {
            valueBuilder.noteEditAction(action);
        }
        var indexParts = indexNoteId == null ? List.of(handler.actionName()) : List.of(handler.actionName(), indexNoteId);
        var index = JSON.toJSONString(indexParts);
        return new DecryptedMutation.Trusted(index, valueBuilder.build(), operation, ts, handler.version());
    }

    @Nested
    @DisplayName("metadata Ã¢â‚¬â€ wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the wire constant")
        void actionName() {
            assertEquals(NoteEditAction.ACTION_NAME, handler.actionName());
            assertEquals("note_edit", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR_LOW")
        void collectionName() {
            assertEquals(NoteEditAction.COLLECTION_NAME, handler.collectionName());
            assertEquals(SyncPatchType.REGULAR_LOW, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version")
        void version() {
            assertEquals(NoteEditAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation Ã¢â‚¬â€ SET upsert (create / edit)")
    class ApplySetUpsert {
        @Test
        @DisplayName("a SET with type + chatJid + content installs the note state")
        void installsNote() {
            store.chatStore().addNewChat(CHAT_JID);
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var action = new NoteEditActionBuilder()
                    .type(NoteType.UNSTRUCTURED)
                    .chatJid(CHAT_JID)
                    .createdAt(ts)
                    .deleted(false)
                    .unstructuredContent("remember to ask")
                    .build();

            var result = handler.applyMutation(client,
                    buildMutation("note-1", action, SyncdOperation.SET, ts));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var stored = store.businessStore().findNoteState("note-1").orElseThrow();
            assertEquals("note-1", stored.id());
            assertEquals("remember to ask", stored.unstructuredContent().orElseThrow());
            assertEquals(NoteType.UNSTRUCTURED, stored.type().orElseThrow());
        }

        @Test
        @DisplayName("deleted=true on a SET drops the note from the store")
        void deletedTrueRemovesNote() {
            store.chatStore().addNewChat(CHAT_JID);
            store.businessStore().putNoteState(new NoteStateBuilder()
                    .id("note-rm")
                    .type(NoteType.UNSTRUCTURED)
                    .chatJid(CHAT_JID)
                    .createdAt(Instant.now())
                    .deleted(false)
                    .unstructuredContent("body")
                    .build());

            var action = new NoteEditActionBuilder()
                    .type(NoteType.UNSTRUCTURED)
                    .chatJid(CHAT_JID)
                    .createdAt(Instant.now())
                    .deleted(true)
                    .unstructuredContent("")
                    .build();

            var result = handler.applyMutation(client,
                    buildMutation("note-rm", action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(store.businessStore().findNoteState("note-rm").isEmpty(),
                    "deleted=true must drop the stored note");
        }
    }

    @Nested
    @DisplayName("applyMutation Ã¢â‚¬â€ orphan when chat is unknown")
    class OrphanChat {
        @Test
        @DisplayName("a SET targeting an unknown chat returns ORPHAN with the chat JID")
        void orphanChat() {
            var action = new NoteEditActionBuilder()
                    .type(NoteType.UNSTRUCTURED)
                    .chatJid(CHAT_JID)
                    .createdAt(Instant.now())
                    .deleted(false)
                    .unstructuredContent("body")
                    .build();

            var result = handler.applyMutation(client,
                    buildMutation("note-orphan", action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals(CHAT_JID.toString(), result.modelId());
            assertEquals("Chat", result.modelType());
        }
    }

    @Nested
    @DisplayName("applyMutation Ã¢â‚¬â€ malformed value")
    class MalformedValue {
        @Test
        @DisplayName("a SET whose value carries the wrong action returns MALFORMED")
        void wrongActionType() {
            store.chatStore().addNewChat(CHAT_JID);
            var ts = Instant.now();
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var index = JSON.toJSONString(List.of(handler.actionName(), "note-x"));
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }

        @Test
        @DisplayName("a non-deletion action missing the type field returns MALFORMED")
        void missingType() {
            store.chatStore().addNewChat(CHAT_JID);
            var action = new NoteEditActionBuilder()
                    .chatJid(CHAT_JID)
                    .createdAt(Instant.now())
                    .deleted(false)
                    .unstructuredContent("body")
                    .build();

            var result = handler.applyMutation(client,
                    buildMutation("note-no-type", action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("a non-deletion action missing the chatJid returns MALFORMED")
        void missingChatJid() {
            var action = new NoteEditActionBuilder()
                    .type(NoteType.UNSTRUCTURED)
                    .createdAt(Instant.now())
                    .deleted(false)
                    .unstructuredContent("body")
                    .build();

            var result = handler.applyMutation(client,
                    buildMutation("note-no-jid", action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation Ã¢â‚¬â€ malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("a missing note id slot returns MALFORMED")
        void missingNoteIdSlot() {
            var action = new NoteEditActionBuilder()
                    .type(NoteType.UNSTRUCTURED)
                    .chatJid(CHAT_JID)
                    .deleted(false)
                    .build();

            var result = handler.applyMutation(client,
                    buildMutation(null, action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("an empty note id returns MALFORMED")
        void emptyNoteId() {
            var action = new NoteEditActionBuilder()
                    .type(NoteType.UNSTRUCTURED)
                    .chatJid(CHAT_JID)
                    .deleted(false)
                    .build();

            var result = handler.applyMutation(client,
                    buildMutation("", action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation Ã¢â‚¬â€ REMOVE")
    class ApplyRemove {
        @Test
        @DisplayName("REMOVE operation returns UNSUPPORTED")
        void removeUnsupported() {
            var action = new NoteEditActionBuilder()
                    .type(NoteType.UNSTRUCTURED)
                    .chatJid(CHAT_JID)
                    .deleted(false)
                    .build();

            var result = handler.applyMutation(client,
                    buildMutation("note-rm", action, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts Ã¢â‚¬â€ default timestamp comparison")
    class ResolveConflicts {
        @Test
        @DisplayName("newer remote Ã¢â‚¬â€ APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var local = mutationAt(Instant.ofEpochSecond(1_000));
            var remote = mutationAt(Instant.ofEpochSecond(2_000));
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("older remote Ã¢â‚¬â€ SKIP_REMOTE")
        void olderRemoteSkipped() {
            var local = mutationAt(Instant.ofEpochSecond(2_000));
            var remote = mutationAt(Instant.ofEpochSecond(1_000));
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    handler.resolveConflicts(local, remote).state());
        }

        private DecryptedMutation.Trusted mutationAt(Instant ts) {
            var action = new NoteEditActionBuilder()
                    .type(NoteType.UNSTRUCTURED)
                    .chatJid(CHAT_JID)
                    .deleted(false)
                    .build();
            return buildMutation("note-tie", action, SyncdOperation.SET, ts);
        }
    }

}
