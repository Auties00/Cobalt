package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.DeleteMessageForMeAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.DeleteMessageForMeActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.DeleteMessageForMeMutationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the {@link DeleteMessageForMeHandler} adapter for the
 * {@code deleteMessageForMe} app-state sync action across metadata, the SET
 * happy path, the orphan and malformed branches, the REMOVE rejection, the
 * deleteMedia-driven conflict matrix (intentionally timestamp-independent on
 * this handler), and the outbound builder helper.
 *
 * <p>Each test runs against a fresh in-memory {@link DeviceFixtures#temporaryStore}
 * via {@link TestWhatsAppClient}, so it starts from a clean single-device state.
 * The {@code seedMessage} helper installs a single
 * {@link com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo} keyed on a fixed
 * {@code MESSAGE_ID} so the orphan and removed paths can be exercised without
 * seeding history.
 */
@DisplayName("DeleteMessageForMeHandler")
class DeleteMessageForMeHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid PEER = Jid.of("1234567890@s.whatsapp.net");
    private static final String MESSAGE_ID = "msg-1";

    private TestWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private static DecryptedMutation.Trusted dmfmMutation(Jid chatJid, String messageId, String fromMe, String participant, Instant ts) {
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .deleteMessageForMeAction(new DeleteMessageForMeActionBuilder().deleteMedia(false).messageTimestamp(ts).build())
                .build();
        var index = JSON.toJSONString(List.of("deleteMessageForMe", chatJid.toString(), messageId, fromMe, participant));
        return new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, 3);
    }

    private void seedMessage(boolean fromMe) {
        var chat = client.store().chatStore().addNewChat(PEER);
        var key = new MessageKeyBuilder()
                .id(MESSAGE_ID).fromMe(fromMe).parentJid(PEER)
                .build();
        chat.addMessage(new ChatMessageInfoBuilder()
                .key(key)
                .message(LinkedMessageContainer.of("hello"))
                .timestamp(Instant.now())
                .build());
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName returns \"deleteMessageForMe\"")
        void actionName() {
            assertEquals("deleteMessageForMe", new DeleteMessageForMeHandler().actionName());
            assertEquals(DeleteMessageForMeAction.ACTION_NAME, new DeleteMessageForMeHandler().actionName());
        }

        @Test
        @DisplayName("collectionName resolves to REGULAR_HIGH")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_HIGH, new DeleteMessageForMeHandler().collectionName());
        }

        @Test
        @DisplayName("version returns the declared action version 3")
        void version() {
            assertEquals(3, new DeleteMessageForMeHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation SET - happy path")
    class HappySet {
        @Test
        @DisplayName("SET on an inbound (fromMe=false) message removes it from the chat")
        void removesInboundMessage() {
            seedMessage(false);
            assertEquals(1, client.store().chatStore().findChatByJid(PEER).orElseThrow().messageCount());

            var result = new DeleteMessageForMeHandler().applyMutation(client,
                    dmfmMutation(PEER, MESSAGE_ID, "0", "0", Instant.ofEpochSecond(1L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals(0, client.store().chatStore().findChatByJid(PEER).orElseThrow().messageCount());
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan")
    class Orphan {
        @Test
        @DisplayName("SET against an unknown chat JID returns ORPHAN with modelType=Msg")
        void orphanReturnsMsgModel() {
            var result = new DeleteMessageForMeHandler().applyMutation(client,
                    dmfmMutation(PEER, MESSAGE_ID, "0", "0", Instant.ofEpochSecond(1L)));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals("Msg", result.modelType());
            assertNotNull(result.modelId());
        }

        @Test
        @DisplayName("SET against a known chat but missing message returns ORPHAN with modelType=Msg")
        void missingMessageIsOrphan() {
            client.store().chatStore().addNewChat(PEER);

            var result = new DeleteMessageForMeHandler().applyMutation(client,
                    dmfmMutation(PEER, "absent-id", "0", "0", Instant.ofEpochSecond(1L)));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals("Msg", result.modelType());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value (n/a)")
    class MalformedValue {
        // The handler does not call malformedActionValue on the value type; it only checks the index
        // and then filters by fromMe/participant. A SyncActionValue carrying a different action is
        // not rejected - instead the chat-message lookup decides the outcome. The handler reads
        // deleteMedia in resolveConflicts() but not in applyMutation(), so a different action just
        // ends up as an Orphan or Success based on whether the message exists.
        @Test
        @DisplayName("a value carrying a pinAction still consults the index - outcome flows through the orphan path")
        void wrongActionTypeFallsThroughToOrphan() {
            // Index is valid, value has no deleteMessageForMeAction.
            // The handler doesn't reject - it just runs index-based lookups.
            var wrong = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    JSON.toJSONString(List.of("deleteMessageForMe", PEER.toString(), MESSAGE_ID, "0", "0")),
                    wrong, SyncdOperation.SET, Instant.ofEpochSecond(1L), 3);

            // Without a matching chat in the store, we get ORPHAN.
            var result = new DeleteMessageForMeHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.ORPHAN, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("a 4-element index missing the participant slot is MALFORMED")
        void shortIndexIsMalformed() {
            client.store().chatStore().addNewChat(PEER);
            var value = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .deleteMessageForMeAction(new DeleteMessageForMeActionBuilder().deleteMedia(false).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"deleteMessageForMe\",\"" + PEER + "\",\"" + MESSAGE_ID + "\",\"0\"]",
                    value, SyncdOperation.SET, Instant.ofEpochSecond(1L), 3);

            var result = new DeleteMessageForMeHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("an empty messageId at slot 2 is MALFORMED")
        void emptyMessageIdIsMalformed() {
            client.store().chatStore().addNewChat(PEER);
            var value = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .deleteMessageForMeAction(new DeleteMessageForMeActionBuilder().deleteMedia(false).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"deleteMessageForMe\",\"" + PEER + "\",\"\",\"0\",\"0\"]",
                    value, SyncdOperation.SET, Instant.ofEpochSecond(1L), 3);

            var result = new DeleteMessageForMeHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE")
    class RemoveOperation {
        @Test
        @DisplayName("REMOVE returns UNSUPPORTED")
        void removeReturnsUnsupported() {
            seedMessage(false);
            var mutation = new DecryptedMutation.Trusted(
                    JSON.toJSONString(List.of("deleteMessageForMe", PEER.toString(), MESSAGE_ID, "0", "0")),
                    new SyncActionValueBuilder()
                            .timestamp(Instant.ofEpochSecond(1L))
                            .deleteMessageForMeAction(new DeleteMessageForMeActionBuilder().deleteMedia(false).build())
                            .build(),
                    SyncdOperation.REMOVE, Instant.ofEpochSecond(1L), 3);

            var result = new DeleteMessageForMeHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            assertEquals(1, client.store().chatStore().findChatByJid(PEER).orElseThrow().messageCount(),
                    "REMOVE must not remove the message");
        }
    }

    @Nested
    @DisplayName("resolveConflicts - deleteMedia-based (NOT timestamp-based)")
    class ResolveConflicts {
        @Test
        @DisplayName("remote deleteMedia=false vs local deleteMedia=true -> SKIP_REMOTE (local wins, it is more aggressive)")
        void localDeleteMediaWinsOverRemote() {
            var local = mutationWithDeleteMedia(true, Instant.ofEpochSecond(100L));
            var remote = mutationWithDeleteMedia(false, Instant.ofEpochSecond(200L));

            var resolution = new DeleteMessageForMeHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE, resolution.state());
        }

        @Test
        @DisplayName("any other combination -> SKIP_REMOTE_DROP_LOCAL")
        void anyOtherCombinationDropsBoth() {
            // both true
            var a = new DeleteMessageForMeHandler().resolveConflicts(
                    mutationWithDeleteMedia(true, Instant.ofEpochSecond(100L)),
                    mutationWithDeleteMedia(true, Instant.ofEpochSecond(200L)));
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE_DROP_LOCAL, a.state());

            // both false
            var b = new DeleteMessageForMeHandler().resolveConflicts(
                    mutationWithDeleteMedia(false, Instant.ofEpochSecond(100L)),
                    mutationWithDeleteMedia(false, Instant.ofEpochSecond(200L)));
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE_DROP_LOCAL, b.state());

            // remote true, local false
            var c = new DeleteMessageForMeHandler().resolveConflicts(
                    mutationWithDeleteMedia(false, Instant.ofEpochSecond(100L)),
                    mutationWithDeleteMedia(true, Instant.ofEpochSecond(200L)));
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE_DROP_LOCAL, c.state());
        }

        private static DecryptedMutation.Trusted mutationWithDeleteMedia(boolean deleteMedia, Instant ts) {
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .deleteMessageForMeAction(new DeleteMessageForMeActionBuilder().deleteMedia(deleteMedia).build())
                    .build();
            return new DecryptedMutation.Trusted(
                    JSON.toJSONString(List.of("deleteMessageForMe", PEER.toString(), MESSAGE_ID, "0", "0")),
                    value, SyncdOperation.SET, ts, 3);
        }
    }

    @Nested
    @DisplayName("buildDeleteForMeMutation - builder helper")
    class BuilderHelpers {
        @Test
        @DisplayName("buildDeleteForMeMutation produces the 5-element index with the correct fromMe/participant encoding")
        void indexShape() {
            var now = Instant.ofEpochSecond(1_700_000_000L);
            var msgTs = Instant.ofEpochSecond(1_699_000_000L);

            // outgoing 1:1 message: fromMe=true, participant must serialise to "0"
            var pending = new DeleteMessageForMeMutationFactory().buildDeleteForMeMutation(
                    now, false, msgTs, PEER, "id-1", true, null);
            var trusted = pending.mutation();

            assertEquals(SyncdOperation.SET, trusted.operation());
            assertEquals(3, trusted.actionVersion());
            assertEquals(
                    JSON.toJSONString(List.of("deleteMessageForMe", PEER.toString(), "id-1", "1", "0")),
                    trusted.index());
            var action = trusted.value().flatMap(sav -> sav.action()).filter(a -> a instanceof DeleteMessageForMeAction).map(a -> (DeleteMessageForMeAction) a).orElseThrow();
            assertEquals(msgTs, action.messageTimestamp().orElseThrow());
        }

        @Test
        @DisplayName("incoming group message: fromMe=false + participant -> participant string at slot 4")
        void participantSlotForIncomingGroup() {
            var now = Instant.ofEpochSecond(1_700_000_000L);
            var msgTs = Instant.ofEpochSecond(1_699_000_000L);
            var sender = Jid.of("9999999999@s.whatsapp.net");

            var pending = new DeleteMessageForMeMutationFactory().buildDeleteForMeMutation(
                    now, false, msgTs, PEER, "id-2", false, sender);
            var trusted = pending.mutation();

            assertEquals(
                    JSON.toJSONString(List.of("deleteMessageForMe", PEER.toString(), "id-2", "0", sender.toString())),
                    trusted.index());
        }
    }

}
