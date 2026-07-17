package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessage;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageRange;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageRangeBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ClearChatAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ClearChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.ClearChatMutationFactory;
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
 * Tests for {@link ClearChatHandler}.
 */
@DisplayName("ClearChatHandler")
class ClearChatHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid PEER = Jid.of("1234567890@s.whatsapp.net");

    private TestWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private static DecryptedMutation.Trusted clearMutation(Jid jid, Instant ts, SyncActionMessageRange range,
                                                           String deleteStarred, String deleteMedia) {
        var builder = new ClearChatActionBuilder();
        if (range != null) builder.messageRange(range);
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .clearChatAction(builder.build())
                .build();
        var index = JSON.toJSONString(List.of("clearChat", jid.toString(), deleteStarred, deleteMedia));
        return new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, 6);
    }

    private static SyncActionMessageRange rangeWithLast(long epochSeconds) {
        return new SyncActionMessageRangeBuilder()
                .lastMessageTimestamp(Instant.ofEpochSecond(epochSeconds))
                .messages(List.of())
                .build();
    }

    private static SyncActionMessage msg(String id, long epochSeconds) {
        var key = new MessageKeyBuilder()
                .id(id).fromMe(false).parentJid(PEER)
                .build();
        return new SyncActionMessageBuilder()
                .key(key).timestamp(Instant.ofEpochSecond(epochSeconds))
                .build();
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName returns \"clearChat\"")
        void actionName() {
            assertEquals("clearChat", new ClearChatHandler().actionName());
            assertEquals(ClearChatAction.ACTION_NAME, new ClearChatHandler().actionName());
        }

        @Test
        @DisplayName("collectionName resolves to REGULAR_HIGH")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_HIGH, new ClearChatHandler().collectionName());
        }

        @Test
        @DisplayName("version returns the declared action version 6")
        void version() {
            assertEquals(6, new ClearChatHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation SET - happy path")
    class HappySet {
        @Test
        @DisplayName("SET clears the chat's messages")
        void clearsAllMessages() {
            var chat = client.store().chatStore().addNewChat(PEER);
            chat.addMessage(new ChatMessageInfoBuilder()
                    .key(new MessageKeyBuilder().id("m1").fromMe(false).parentJid(PEER).build())
                    .message(LinkedMessageContainer.of("hi"))
                    .timestamp(Instant.now())
                    .build());
            assertEquals(1, chat.messageCount());

            var range = rangeWithLast(1_700_000_000L);
            var result = new ClearChatHandler().applyMutation(client,
                    clearMutation(PEER, Instant.ofEpochSecond(1_700_000_001L), range, "0", "0"));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals(0, chat.messageCount(), "chat must have no messages after clear");
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan")
    class Orphan {
        @Test
        @DisplayName("SET against an unknown chat JID returns ORPHAN with modelType=Chat")
        void orphanReturnsChatModel() {
            var range = rangeWithLast(1L);
            var result = new ClearChatHandler().applyMutation(client,
                    clearMutation(PEER, Instant.ofEpochSecond(1L), range, "0", "0"));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals(PEER.toString(), result.modelId());
            assertEquals("Chat", result.modelType());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value")
    class MalformedValue {
        @Test
        @DisplayName("a SyncActionValue carrying a pinAction instead of clearChatAction is MALFORMED")
        void wrongActionTypeIsMalformed() {
            client.store().chatStore().addNewChat(PEER);
            var wrong = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    JSON.toJSONString(List.of("clearChat", PEER.toString(), "0", "0")),
                    wrong, SyncdOperation.SET, Instant.ofEpochSecond(1L), 6);

            var result = new ClearChatHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("a clearChatAction without a messageRange is MALFORMED")
        void missingMessageRangeIsMalformed() {
            client.store().chatStore().addNewChat(PEER);
            var mutation = clearMutation(PEER, Instant.ofEpochSecond(1L), null, "0", "0");
            var result = new ClearChatHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("an empty deleteMedia slot is MALFORMED")
        void emptyDeleteMediaIsMalformed() {
            client.store().chatStore().addNewChat(PEER);
            var value = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .clearChatAction(new ClearChatActionBuilder().messageRange(rangeWithLast(1L)).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"clearChat\",\"" + PEER + "\",\"0\",\"\"]",
                    value, SyncdOperation.SET, Instant.ofEpochSecond(1L), 6);

            var result = new ClearChatHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE")
    class RemoveOperation {
        @Test
        @DisplayName("REMOVE returns UNSUPPORTED")
        void removeReturnsUnsupported() {
            client.store().chatStore().addNewChat(PEER);
            var mutation = new DecryptedMutation.Trusted(
                    JSON.toJSONString(List.of("clearChat", PEER.toString(), "0", "0")),
                    new SyncActionValueBuilder()
                            .timestamp(Instant.ofEpochSecond(1L))
                            .clearChatAction(new ClearChatActionBuilder().messageRange(rangeWithLast(1L)).build())
                            .build(),
                    SyncdOperation.REMOVE, Instant.ofEpochSecond(1L), 6);

            var result = new ClearChatHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - message-range matrix")
    class ResolveConflicts {
        @Test
        @DisplayName("remote range encloses local range -> APPLY_REMOTE_DROP_LOCAL")
        void remoteEnclosesLocal() {
            var localRange = new SyncActionMessageRangeBuilder()
                    .lastMessageTimestamp(Instant.ofEpochSecond(50L)).messages(List.of()).build();
            var remoteRange = new SyncActionMessageRangeBuilder()
                    .lastMessageTimestamp(Instant.ofEpochSecond(100L))
                    .messages(List.of(msg("r-only", 60L))).build();
            var local = clearMutation(PEER, Instant.ofEpochSecond(100L), localRange, "0", "0");
            var remote = clearMutation(PEER, Instant.ofEpochSecond(200L), remoteRange, "0", "0");

            var resolution = new ClearChatHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }

        @Test
        @DisplayName("local range encloses remote range -> SKIP_REMOTE")
        void localEnclosesRemote() {
            var localRange = new SyncActionMessageRangeBuilder()
                    .lastMessageTimestamp(Instant.ofEpochSecond(100L))
                    .messages(List.of(msg("l-only", 60L))).build();
            var remoteRange = new SyncActionMessageRangeBuilder()
                    .lastMessageTimestamp(Instant.ofEpochSecond(50L)).messages(List.of()).build();
            var local = clearMutation(PEER, Instant.ofEpochSecond(200L), localRange, "0", "0");
            var remote = clearMutation(PEER, Instant.ofEpochSecond(100L), remoteRange, "0", "0");

            var resolution = new ClearChatHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE, resolution.state());
        }

        @Test
        @DisplayName("ranges equal with local <= remote -> APPLY_REMOTE_DROP_LOCAL")
        void rangesEqualLocalOlder() {
            var local = clearMutation(PEER, Instant.ofEpochSecond(100L), rangeWithLast(100L), "0", "0");
            var remote = clearMutation(PEER, Instant.ofEpochSecond(200L), rangeWithLast(100L), "0", "0");

            var resolution = new ClearChatHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }

        @Test
        @DisplayName("ranges equal with local strictly newer -> SKIP_REMOTE")
        void rangesEqualLocalNewer() {
            var local = clearMutation(PEER, Instant.ofEpochSecond(300L), rangeWithLast(100L), "0", "0");
            var remote = clearMutation(PEER, Instant.ofEpochSecond(200L), rangeWithLast(100L), "0", "0");

            var resolution = new ClearChatHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE, resolution.state());
        }

        @Test
        @DisplayName("ranges do not enclose each other -> SKIP_REMOTE_DROP_LOCAL with merged mutation")
        void rangesNotEnclosing() {
            // Each range carries a message whose timestamp >= the OTHER range's lastMessageTimestamp,
            // with disjoint key ids - forcing encloses() to return false in both directions.
            var localRange = new SyncActionMessageRangeBuilder()
                    .lastMessageTimestamp(Instant.ofEpochSecond(50L))
                    .messages(List.of(msg("local-1", 80L)))
                    .build();
            var remoteRange = new SyncActionMessageRangeBuilder()
                    .lastMessageTimestamp(Instant.ofEpochSecond(50L))
                    .messages(List.of(msg("remote-1", 90L)))
                    .build();
            var local = clearMutation(PEER, Instant.ofEpochSecond(100L), localRange, "0", "0");
            var remote = clearMutation(PEER, Instant.ofEpochSecond(200L), remoteRange, "0", "0");

            var resolution = new ClearChatHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE_DROP_LOCAL, resolution.state());
            assertNotNull(resolution.mergedMutation());
            assertTrue(resolution.mergedMutation().value().flatMap(sav -> sav.action()).filter(a -> a instanceof ClearChatAction).isPresent());
        }
    }

    @Nested
    @DisplayName("getClearChatMutation - builder helper")
    class BuilderHelpers {
        @Test
        @DisplayName("getClearChatMutation produces the [\"clearChat\", jid, deleteStarred, deleteMedia] index")
        void indexShape() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var pending = new ClearChatMutationFactory().getClearChatMutation(
                    ts, PEER, true, false, rangeWithLast(1_699_999_900L));

            var trusted = pending.mutation();
            assertEquals(SyncdOperation.SET, trusted.operation());
            assertEquals(6, trusted.actionVersion());
            assertEquals(
                    JSON.toJSONString(List.of("clearChat", PEER.toString(), "1", "0")),
                    trusted.index());
            assertTrue(trusted.value().flatMap(sav -> sav.action()).filter(a -> a instanceof ClearChatAction).map(a -> (ClearChatAction) a).orElseThrow().messageRange().isPresent());
        }
    }

}
