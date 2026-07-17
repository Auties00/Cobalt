package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.wam.TestWamService;
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
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinAction;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.ArchiveChatMutationFactory;
import com.github.auties00.cobalt.sync.factory.PinChatMutationFactory;
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
 * Covers {@link ArchiveChatHandler}, which owns archive/unarchive mutations including their
 * message-range based conflict resolution. The conflict matrix exercises the range-enclosure
 * outcomes (remote-encloses, local-encloses, equal ranges, non-enclosing merge) that drive the
 * {@link MutationConflictResolutionState} returned by {@code resolveConflicts}.
 */
@DisplayName("ArchiveChatHandler")
class ArchiveChatHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid PEER = Jid.of("1234567890@s.whatsapp.net");

    private TestWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private static DecryptedMutation.Trusted archiveMutation(boolean archived, Jid jid, Instant ts) {
        var action = new ArchiveChatActionBuilder()
                .archived(archived)
                .messageRange(rangeWithLast(ts.getEpochSecond()))
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .archiveChatAction(action)
                .build();
        var index = JSON.toJSONString(List.of("archive", jid.toString()));
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
                .id(id)
                .fromMe(false)
                .parentJid(PEER)
                .build();
        return new SyncActionMessageBuilder()
                .key(key)
                .timestamp(Instant.ofEpochSecond(epochSeconds))
                .build();
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName returns the WA wire name \"archive\"")
        void actionName() {
            assertEquals("archive", new ArchiveChatHandler().actionName());
            assertEquals(ArchiveChatAction.ACTION_NAME, new ArchiveChatHandler().actionName());
        }

        @Test
        @DisplayName("collectionName resolves to REGULAR_LOW")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_LOW, new ArchiveChatHandler().collectionName());
        }

        @Test
        @DisplayName("version returns the declared action version 3")
        void version() {
            assertEquals(3, new ArchiveChatHandler().version());
            assertEquals(ArchiveChatAction.ACTION_VERSION, new ArchiveChatHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation SET - happy path")
    class HappySet {
        @Test
        @DisplayName("SET archived=true on an existing chat archives it")
        void archivesTheChat() {
            var chat = client.store().chatStore().addNewChat(PEER);
            assertFalse(chat.archived());

            var result = new ArchiveChatHandler().applyMutation(
                    client, archiveMutation(true, PEER, Instant.ofEpochSecond(1_700_000_000L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(chat.archived());
        }

        @Test
        @DisplayName("SET archived=false on an archived chat unarchives it")
        void unarchivesTheChat() {
            var chat = client.store().chatStore().addNewChat(PEER);
            chat.setArchived(true);

            var result = new ArchiveChatHandler().applyMutation(
                    client, archiveMutation(false, PEER, Instant.ofEpochSecond(1_700_000_000L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertFalse(chat.archived());
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan")
    class Orphan {
        @Test
        @DisplayName("SET against an unknown chat JID returns ORPHAN with modelType=Chat")
        void orphanReturnsChatModel() {
            var result = new ArchiveChatHandler().applyMutation(
                    client, archiveMutation(true, PEER, Instant.ofEpochSecond(1_700_000_000L)));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertTrue(result.isOrphan());
            assertEquals(PEER.toString(), result.modelId());
            assertEquals("Chat", result.modelType());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value")
    class MalformedValue {
        @Test
        @DisplayName("a SyncActionValue carrying a pinAction instead of archiveChatAction is rejected as MALFORMED")
        void wrongActionTypeIsMalformed() {
            client.store().chatStore().addNewChat(PEER);
            var wrong = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1_700_000_000L))
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    JSON.toJSONString(List.of("archive", PEER.toString())),
                    wrong, SyncdOperation.SET,
                    Instant.ofEpochSecond(1_700_000_000L), 3);

            var result = new ArchiveChatHandler().applyMutation(client, mutation);
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
                    .archiveChatAction(new ArchiveChatActionBuilder().archived(true).messageRange(rangeWithLast(1L)).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"archive\",\"\"]",
                    value, SyncdOperation.SET,
                    Instant.ofEpochSecond(1L), 3);

            var result = new ArchiveChatHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("an empty index string is treated as failure (try/catch returns FAILED)")
        void emptyIndexIsFailed() {
            client.store().chatStore().addNewChat(PEER);
            var value = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .archiveChatAction(new ArchiveChatActionBuilder().archived(true).messageRange(rangeWithLast(1L)).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "", value, SyncdOperation.SET, Instant.ofEpochSecond(1L), 3);

            // JSON.parseArray("") throws; the handler's outer try/catch catches it as FAILED.
            var result = new ArchiveChatHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.FAILED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE")
    class RemoveOperation {
        @Test
        @DisplayName("a REMOVE operation returns UNSUPPORTED without touching the chat")
        void removeReturnsUnsupported() {
            var chat = client.store().chatStore().addNewChat(PEER);
            chat.setArchived(true);

            var value = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .archiveChatAction(new ArchiveChatActionBuilder().archived(false).messageRange(rangeWithLast(1L)).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    JSON.toJSONString(List.of("archive", PEER.toString())),
                    value, SyncdOperation.REMOVE, Instant.ofEpochSecond(1L), 3);

            var result = new ArchiveChatHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            assertTrue(chat.archived());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - message range matrix")
    class ResolveConflicts {
        @Test
        @DisplayName("remote range encloses local range -> APPLY_REMOTE_DROP_LOCAL")
        void remoteEnclosesLocal() {
            // local has no messages and a small lastTs; remote carries a message with ts >= local lastTs.
            // encloses(remote, local) = true (empty local messages); encloses(local, remote) = false.
            var local = mutationWithRangeAt(false, 50, List.of(), Instant.ofEpochSecond(100L));
            var remote = mutationWithRangeAt(true, 100, List.of(msg("r-only", 60L)), Instant.ofEpochSecond(200L));

            var resolution = new ArchiveChatHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }

        @Test
        @DisplayName("local range encloses remote range -> SKIP_REMOTE")
        void localEnclosesRemote() {
            // Mirror image of remoteEnclosesLocal.
            var local = mutationWithRangeAt(true, 100, List.of(msg("l-only", 60L)), Instant.ofEpochSecond(200L));
            var remote = mutationWithRangeAt(false, 50, List.of(), Instant.ofEpochSecond(100L));

            var resolution = new ArchiveChatHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE, resolution.state());
        }

        @Test
        @DisplayName("ranges are equal with local timestamp <= remote -> APPLY_REMOTE_DROP_LOCAL")
        void rangesEqualLocalOlder() {
            var local = mutationWithRangeAt(false, 100, List.of(), Instant.ofEpochSecond(100L));
            var remote = mutationWithRangeAt(true, 100, List.of(), Instant.ofEpochSecond(200L));

            var resolution = new ArchiveChatHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }

        @Test
        @DisplayName("ranges are equal with local timestamp strictly newer -> SKIP_REMOTE")
        void rangesEqualLocalNewer() {
            var local = mutationWithRangeAt(true, 100, List.of(), Instant.ofEpochSecond(300L));
            var remote = mutationWithRangeAt(false, 100, List.of(), Instant.ofEpochSecond(200L));

            var resolution = new ArchiveChatHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE, resolution.state());
        }

        @Test
        @DisplayName("ranges do not enclose each other -> SKIP_REMOTE_DROP_LOCAL with a merged mutation")
        void rangesNotEnclosingProducesMerge() {
            // Each range carries a message whose timestamp >= the OTHER range's lastMessageTimestamp,
            // with disjoint key ids - forcing encloses() to return false in both directions.
            var local = mutationWithRangeAt(false, 50L, List.of(msg("local-1", 80L)), Instant.ofEpochSecond(100L));
            var remote = mutationWithRangeAt(true, 50L, List.of(msg("remote-1", 90L)), Instant.ofEpochSecond(200L));

            var resolution = new ArchiveChatHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE_DROP_LOCAL, resolution.state());
            assertNotNull(resolution.mergedMutation(), "non-enclosing ranges must yield a merged mutation");
            // Newer mutation (remote) wins for archived flag.
            var mergedAction = resolution.mergedMutation().value().flatMap(sav -> sav.action()).filter(a -> a instanceof ArchiveChatAction).map(a -> (ArchiveChatAction) a).orElseThrow();
            assertTrue(mergedAction.archived(),
                    "merged action carries the archived flag from the newer (remote) mutation");
        }

        @Test
        @DisplayName("missing local message range falls back to APPLY_REMOTE_DROP_LOCAL")
        void missingLocalRangeFallback() {
            // local has no message range - handler treats this as defensive fallback.
            var localAction = new ArchiveChatActionBuilder().archived(false).build();
            var localValue = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(100L))
                    .archiveChatAction(localAction)
                    .build();
            var local = new DecryptedMutation.Trusted(
                    "[\"archive\",\"" + PEER + "\"]",
                    localValue, SyncdOperation.SET, Instant.ofEpochSecond(100L), 3);
            var remote = mutationWithRangeAt(true, 200, List.of(), Instant.ofEpochSecond(200L));

            var resolution = new ArchiveChatHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }
    }

    private static DecryptedMutation.Trusted mutationWithRange(boolean archived, long lastTsSeconds, List<SyncActionMessage> messages) {
        return mutationWithRangeAt(archived, lastTsSeconds, messages, Instant.ofEpochSecond(lastTsSeconds));
    }

    private static DecryptedMutation.Trusted mutationWithRangeAt(boolean archived, long lastTsSeconds, List<SyncActionMessage> messages, Instant mutationTimestamp) {
        var range = new SyncActionMessageRangeBuilder()
                .lastMessageTimestamp(Instant.ofEpochSecond(lastTsSeconds))
                .messages(messages)
                .build();
        var action = new ArchiveChatActionBuilder()
                .archived(archived)
                .messageRange(range)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(mutationTimestamp)
                .archiveChatAction(action)
                .build();
        return new DecryptedMutation.Trusted(
                JSON.toJSONString(List.of("archive", PEER.toString())),
                value, SyncdOperation.SET, mutationTimestamp, 3);
    }

    @Nested
    @DisplayName("getArchiveChatMutation - builder helper")
    class BuilderHelpers {
        @Test
        @DisplayName("getArchiveChatMutation carries the supplied archived flag and the action index")
        void singleArchiveMutation() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var range = rangeWithLast(1_699_999_900L);

            var pending = new ArchiveChatMutationFactory(new PinChatMutationFactory(TestWamService.create(client))).getArchiveChatMutation(ts, true, PEER, range);

            var trusted = pending.mutation();
            assertEquals(SyncdOperation.SET, trusted.operation());
            assertEquals(3, trusted.actionVersion());
            assertEquals(ts, trusted.timestamp());
            assertEquals(JSON.toJSONString(List.of("archive", PEER.toString())), trusted.index());
            var archiveAction = trusted.value().flatMap(sav -> sav.action()).filter(a -> a instanceof ArchiveChatAction).map(a -> (ArchiveChatAction) a).orElseThrow();
            assertTrue(archiveAction.archived());
            assertTrue(archiveAction.messageRange().isPresent());
        }

        @Test
        @DisplayName("getMutationsForArchive when archiving also enqueues an unpin")
        void archivingAlsoUnpins() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var range = rangeWithLast(1_699_999_900L);

            var mutations = new ArchiveChatMutationFactory(new PinChatMutationFactory(TestWamService.create(client))).getMutationsForArchive(ts, true, PEER, range);

            assertEquals(2, mutations.size(), "archiving must also queue an unpin mutation");
            var first = mutations.get(0).mutation().value().flatMap(sav -> sav.action()).filter(a -> a instanceof ArchiveChatAction).map(a -> (ArchiveChatAction) a).orElseThrow();
            assertTrue(first.archived());
            var second = mutations.get(1).mutation().value().flatMap(sav -> sav.action()).filter(a -> a instanceof PinAction).map(a -> (PinAction) a).orElseThrow();
            assertFalse(second.pinned(),
                    "the secondary mutation is an unpin (pinned=false), mirroring WA Web's getMutationsForArchive");
        }

        @Test
        @DisplayName("getMutationsForArchive when unarchiving does NOT enqueue an unpin")
        void unarchivingDoesNotUnpin() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var range = rangeWithLast(1_699_999_900L);

            var mutations = new ArchiveChatMutationFactory(new PinChatMutationFactory(TestWamService.create(client))).getMutationsForArchive(ts, false, PEER, range);
            assertEquals(1, mutations.size(), "unarchive only emits the archive mutation");
            var only = mutations.get(0).mutation().value().flatMap(sav -> sav.action()).filter(a -> a instanceof ArchiveChatAction).map(a -> (ArchiveChatAction) a).orElseThrow();
            assertFalse(only.archived());
        }
    }

}
