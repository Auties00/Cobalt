package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessage;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageRange;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageRangeBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.MarkChatAsReadAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.MarkChatAsReadActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.MarkChatAsReadMutationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link MarkChatAsReadHandler}: metadata, the SET happy path that flips a chat between
 * marked-as-read and marked-as-unread, the orphan branch for an unknown chat, the malformed-input
 * fallbacks, the REMOVE rejection, the four-way message-range conflict matrix and the
 * {@link MarkChatAsReadMutationFactory} builder.
 *
 * <p>The handler runs against an in-memory {@link DeviceFixtures#temporaryStore} via
 * {@link TestWhatsAppClient} so {@link com.github.auties00.cobalt.wire.linked.chat.Chat#markedAsUnread()}
 * and {@link com.github.auties00.cobalt.wire.linked.chat.Chat#unreadCount()} read-backs can be asserted
 * directly.
 */
@DisplayName("MarkChatAsReadHandler")
class MarkChatAsReadHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid PEER = Jid.of("1234567890@s.whatsapp.net");

    private TestWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private static DecryptedMutation.Trusted readMutation(boolean read, Jid jid, Instant ts) {
        return readMutation(read, jid, ts, rangeWithLast(ts.getEpochSecond()));
    }

    private static DecryptedMutation.Trusted readMutation(boolean read, Jid jid, Instant ts, SyncActionMessageRange range) {
        var action = new MarkChatAsReadActionBuilder()
                .read(read)
                .messageRange(range)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .markChatAsReadAction(action)
                .build();
        var index = JSON.toJSONString(List.of("markChatAsRead", jid.toString()));
        return new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, 3);
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
        @DisplayName("actionName returns \"markChatAsRead\"")
        void actionName() {
            assertEquals("markChatAsRead", new MarkChatAsReadHandler().actionName());
            assertEquals(MarkChatAsReadAction.ACTION_NAME, new MarkChatAsReadHandler().actionName());
        }

        @Test
        @DisplayName("collectionName resolves to REGULAR_LOW")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_LOW, new MarkChatAsReadHandler().collectionName());
        }

        @Test
        @DisplayName("version returns the declared action version 3")
        void version() {
            assertEquals(3, new MarkChatAsReadHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation SET - happy path")
    class HappySet {
        @Test
        @DisplayName("read=true marks the chat as read with unreadCount=0")
        void marksAsRead() {
            var chat = client.store().chatStore().addNewChat(PEER);
            chat.setMarkedAsUnread(true);
            chat.setUnreadCount(7);

            var result = new MarkChatAsReadHandler().applyMutation(
                    client, readMutation(true, PEER, Instant.ofEpochSecond(1L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertFalse(chat.markedAsUnread());
            assertEquals(0, chat.unreadCount().orElse(-1));
        }

        @Test
        @DisplayName("read=false marks the chat as unread with the MARKED_AS_UNREAD sentinel (-1)")
        void marksAsUnread() {
            var chat = client.store().chatStore().addNewChat(PEER);

            var result = new MarkChatAsReadHandler().applyMutation(
                    client, readMutation(false, PEER, Instant.ofEpochSecond(1L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(chat.markedAsUnread());
            assertEquals(-1, chat.unreadCount().orElse(0));
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan")
    class Orphan {
        @Test
        @DisplayName("SET against an unknown chat JID returns ORPHAN with modelType=Chat")
        void orphanReturnsChatModel() {
            var result = new MarkChatAsReadHandler().applyMutation(
                    client, readMutation(true, PEER, Instant.ofEpochSecond(1L)));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals(PEER.toString(), result.modelId());
            assertEquals("Chat", result.modelType());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value")
    class MalformedValue {
        @Test
        @DisplayName("a SyncActionValue carrying a pinAction instead of markChatAsReadAction is MALFORMED")
        void wrongActionTypeIsMalformed() {
            client.store().chatStore().addNewChat(PEER);
            var wrong = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    JSON.toJSONString(List.of("markChatAsRead", PEER.toString())),
                    wrong, SyncdOperation.SET, Instant.ofEpochSecond(1L), 3);

            var result = new MarkChatAsReadHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("an empty chat JID at slot 1 is MALFORMED")
        void emptyChatJidIsMalformed() {
            client.store().chatStore().addNewChat(PEER);
            var value = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .markChatAsReadAction(new MarkChatAsReadActionBuilder().read(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"markChatAsRead\",\"\"]",
                    value, SyncdOperation.SET, Instant.ofEpochSecond(1L), 3);

            var result = new MarkChatAsReadHandler().applyMutation(client, mutation);
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
                    JSON.toJSONString(List.of("markChatAsRead", PEER.toString())),
                    new SyncActionValueBuilder()
                            .timestamp(Instant.ofEpochSecond(1L))
                            .markChatAsReadAction(new MarkChatAsReadActionBuilder().read(true).build())
                            .build(),
                    SyncdOperation.REMOVE, Instant.ofEpochSecond(1L), 3);

            var result = new MarkChatAsReadHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - message-range matrix")
    class ResolveConflicts {
        @Test
        @DisplayName("remote range encloses local range -> APPLY_REMOTE_DROP_LOCAL")
        void remoteEnclosesLocal() {
            // local has no messages, small lastTs; remote carries a message past local's lastTs.
            var localRange = new SyncActionMessageRangeBuilder()
                    .lastMessageTimestamp(Instant.ofEpochSecond(50L)).messages(List.of()).build();
            var remoteRange = new SyncActionMessageRangeBuilder()
                    .lastMessageTimestamp(Instant.ofEpochSecond(100L))
                    .messages(List.of(msg("r-only", 60L))).build();
            var local = readMutation(false, PEER, Instant.ofEpochSecond(100L), localRange);
            var remote = readMutation(true, PEER, Instant.ofEpochSecond(200L), remoteRange);

            var resolution = new MarkChatAsReadHandler().resolveConflicts(local, remote);
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
            var local = readMutation(true, PEER, Instant.ofEpochSecond(200L), localRange);
            var remote = readMutation(false, PEER, Instant.ofEpochSecond(100L), remoteRange);

            var resolution = new MarkChatAsReadHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE, resolution.state());
        }

        @Test
        @DisplayName("ranges equal with local timestamp <= remote -> APPLY_REMOTE_DROP_LOCAL")
        void rangesEqualLocalOlder() {
            var local = readMutation(false, PEER, Instant.ofEpochSecond(100L), rangeWithLast(100L));
            var remote = readMutation(true, PEER, Instant.ofEpochSecond(200L), rangeWithLast(100L));

            var resolution = new MarkChatAsReadHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }

        @Test
        @DisplayName("ranges equal with local timestamp strictly newer -> SKIP_REMOTE")
        void rangesEqualLocalNewer() {
            var local = readMutation(true, PEER, Instant.ofEpochSecond(300L), rangeWithLast(100L));
            var remote = readMutation(false, PEER, Instant.ofEpochSecond(200L), rangeWithLast(100L));

            var resolution = new MarkChatAsReadHandler().resolveConflicts(local, remote);
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
            var local = readMutation(false, PEER, Instant.ofEpochSecond(100L), localRange);
            var remote = readMutation(true, PEER, Instant.ofEpochSecond(200L), remoteRange);

            var resolution = new MarkChatAsReadHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE_DROP_LOCAL, resolution.state());
            assertNotNull(resolution.mergedMutation());
            var merged = resolution.mergedMutation().value().flatMap(sav -> sav.action()).filter(a -> a instanceof MarkChatAsReadAction).map(a -> (MarkChatAsReadAction) a).orElseThrow();
            assertTrue(merged.read(),
                    "merged action carries the read flag from the newer (remote) mutation");
        }
    }

    @Nested
    @DisplayName("getMarkChatAsReadMutation - builder helper")
    class BuilderHelpers {
        @Test
        @DisplayName("getMarkChatAsReadMutation carries the read flag and the action index")
        void readMutationStructure() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var range = rangeWithLast(1_699_999_900L);

            var pending = new MarkChatAsReadMutationFactory().getMarkChatAsReadMutation(ts, true, PEER, range);

            var trusted = pending.mutation();
            assertEquals(SyncdOperation.SET, trusted.operation());
            assertEquals(3, trusted.actionVersion());
            assertEquals(JSON.toJSONString(List.of("markChatAsRead", PEER.toString())), trusted.index());
            assertTrue(trusted.value().flatMap(sav -> sav.action()).filter(a -> a instanceof MarkChatAsReadAction).map(a -> (MarkChatAsReadAction) a).orElseThrow().read());
        }
    }

}
