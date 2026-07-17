package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.wam.TestWamService;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.LockChatAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.LockChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinAction;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.ArchiveChatMutationFactory;
import com.github.auties00.cobalt.sync.factory.LockChatMutationFactory;
import com.github.auties00.cobalt.sync.factory.PinChatMutationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@link LockChatHandler} for the {@code lock} app-state sync action:
 * metadata, the SET happy path that locks (or unlocks) the chat and clears
 * archive and pin in the locking branch, the orphan branch when the chat is
 * unknown locally, the malformed-input fallbacks, the REMOVE rejection,
 * timestamp-based conflict resolution and the {@link LockChatMutationFactory}
 * builder paired with {@link ArchiveChatMutationFactory} and
 * {@link PinChatMutationFactory} for the multi-mutation lock payload.
 *
 * <p>Tests run against a fresh in-memory {@link DeviceFixtures#temporaryStore}
 * through {@link TestWhatsAppClient} so the
 * {@link com.github.auties00.cobalt.wire.linked.chat.Chat#locked()},
 * {@link com.github.auties00.cobalt.wire.linked.chat.Chat#archived()} and
 * {@link com.github.auties00.cobalt.wire.linked.chat.Chat#pinnedTimestamp()}
 * read-backs can be asserted directly.
 */
@DisplayName("LockChatHandler")
class LockChatHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid PEER = Jid.of("1234567890@s.whatsapp.net");

    private TestWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private static DecryptedMutation.Trusted lockMutation(boolean locked, Jid jid, Instant ts) {
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .lockChatAction(new LockChatActionBuilder().locked(locked).build())
                .build();
        var index = JSON.toJSONString(List.of("lock", jid.toString()));
        return new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, 7);
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName returns \"lock\"")
        void actionName() {
            assertEquals("lock", new LockChatHandler().actionName());
            assertEquals(LockChatAction.ACTION_NAME, new LockChatHandler().actionName());
        }

        @Test
        @DisplayName("collectionName resolves to REGULAR_LOW")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_LOW, new LockChatHandler().collectionName());
        }

        @Test
        @DisplayName("version returns the declared action version 7")
        void version() {
            assertEquals(7, new LockChatHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation SET - happy path")
    class HappySet {
        @Test
        @DisplayName("locked=true also unarchives and unpins the chat (mutual exclusion)")
        void lockingUnarchivesAndUnpins() {
            var chat = client.store().chatStore().addNewChat(PEER);
            chat.setArchived(true);
            chat.setPinnedTimestamp(Instant.ofEpochSecond(123L));

            var result = new LockChatHandler().applyMutation(client,
                    lockMutation(true, PEER, Instant.ofEpochSecond(1L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(chat.locked());
            assertFalse(chat.archived(), "locking must also unarchive the chat");
            assertTrue(chat.pinnedTimestamp().isEmpty(), "locking must also unpin the chat");
        }

        @Test
        @DisplayName("locked=false only flips the lock flag (does not touch archive/pin)")
        void unlockingLeavesOthersAlone() {
            var chat = client.store().chatStore().addNewChat(PEER);
            chat.setLocked(true);
            chat.setArchived(true);
            var pinTs = Instant.ofEpochSecond(123L);
            chat.setPinnedTimestamp(pinTs);

            var result = new LockChatHandler().applyMutation(client,
                    lockMutation(false, PEER, Instant.ofEpochSecond(2L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertFalse(chat.locked());
            assertTrue(chat.archived(), "unlock must not touch the archive flag");
            assertEquals(pinTs, chat.pinnedTimestamp().orElseThrow(),
                    "unlock must not touch the pinned timestamp");
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan")
    class Orphan {
        @Test
        @DisplayName("SET against an unknown chat JID returns ORPHAN with modelType=Chat")
        void orphanReturnsChatModel() {
            var result = new LockChatHandler().applyMutation(client,
                    lockMutation(true, PEER, Instant.ofEpochSecond(1L)));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals(PEER.toString(), result.modelId());
            assertEquals("Chat", result.modelType());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value")
    class MalformedValue {
        @Test
        @DisplayName("a SyncActionValue carrying a pinAction instead of lockChatAction is MALFORMED")
        void wrongActionTypeIsMalformed() {
            client.store().chatStore().addNewChat(PEER);
            var wrong = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    JSON.toJSONString(List.of("lock", PEER.toString())),
                    wrong, SyncdOperation.SET, Instant.ofEpochSecond(1L), 7);

            var result = new LockChatHandler().applyMutation(client, mutation);
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
                    .lockChatAction(new LockChatActionBuilder().locked(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"lock\",\"\"]",
                    value, SyncdOperation.SET, Instant.ofEpochSecond(1L), 7);

            var result = new LockChatHandler().applyMutation(client, mutation);
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
                    JSON.toJSONString(List.of("lock", PEER.toString())),
                    new SyncActionValueBuilder()
                            .timestamp(Instant.ofEpochSecond(1L))
                            .lockChatAction(new LockChatActionBuilder().locked(true).build())
                            .build(),
                    SyncdOperation.REMOVE, Instant.ofEpochSecond(1L), 7);

            var result = new LockChatHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based behaviour")
    class ResolveConflicts {
        // LockChatHandler does NOT override resolveConflicts; the interface default applies.
        @Test
        @DisplayName("older local vs. newer remote -> APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteWins() {
            var local = lockMutation(true, PEER, Instant.ofEpochSecond(100L));
            var remote = lockMutation(false, PEER, Instant.ofEpochSecond(200L));

            var resolution = new LockChatHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }

        @Test
        @DisplayName("newer local vs. older remote -> SKIP_REMOTE")
        void newerLocalWins() {
            var local = lockMutation(true, PEER, Instant.ofEpochSecond(300L));
            var remote = lockMutation(false, PEER, Instant.ofEpochSecond(200L));

            var resolution = new LockChatHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE, resolution.state());
        }
    }

    @Nested
    @DisplayName("getChatLockMutation / getMutationsForLock - builder helpers")
    class BuilderHelpers {
        @Test
        @DisplayName("getChatLockMutation carries the locked flag and the [\"lock\", jid] index")
        void chatLockMutationStructure() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var pinChatMutationFactory = new PinChatMutationFactory(TestWamService.create(client));
            var pending = new LockChatMutationFactory(new ArchiveChatMutationFactory(pinChatMutationFactory), pinChatMutationFactory).getChatLockMutation(ts, true, PEER);

            var trusted = pending.mutation();
            assertEquals(SyncdOperation.SET, trusted.operation());
            assertEquals(7, trusted.actionVersion());
            assertEquals(JSON.toJSONString(List.of("lock", PEER.toString())), trusted.index());
            assertTrue(trusted.value().flatMap(sav -> sav.action()).filter(a -> a instanceof LockChatAction).map(a -> (LockChatAction) a).orElseThrow().locked());
        }

        @Test
        @DisplayName("getMutationsForLock when locking emits (unarchive, unpin, lock) in that order")
        void lockingEmitsThreeMutations() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var pinChatMutationFactory = new PinChatMutationFactory(TestWamService.create(client));
            var mutations = new LockChatMutationFactory(new ArchiveChatMutationFactory(pinChatMutationFactory), pinChatMutationFactory).getMutationsForLock(ts, true, PEER, null);

            assertEquals(3, mutations.size(),
                    "locking emits the archive(false) + pin(false) + lock(true) triple");
            assertFalse(mutations.get(0).mutation().value().flatMap(sav -> sav.action()).filter(a -> a instanceof ArchiveChatAction).map(a -> (ArchiveChatAction) a).orElseThrow().archived());
            assertFalse(mutations.get(1).mutation().value().flatMap(sav -> sav.action()).filter(a -> a instanceof PinAction).map(a -> (PinAction) a).orElseThrow().pinned());
            assertTrue(mutations.get(2).mutation().value().flatMap(sav -> sav.action()).filter(a -> a instanceof LockChatAction).map(a -> (LockChatAction) a).orElseThrow().locked());
        }

        @Test
        @DisplayName("getMutationsForLock when unlocking emits only the lock mutation")
        void unlockingEmitsOnlyLock() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var pinChatMutationFactory = new PinChatMutationFactory(TestWamService.create(client));
            var mutations = new LockChatMutationFactory(new ArchiveChatMutationFactory(pinChatMutationFactory), pinChatMutationFactory).getMutationsForLock(ts, false, PEER, null);

            assertEquals(1, mutations.size(), "unlocking only emits the lock(false) mutation");
            assertFalse(mutations.get(0).mutation().value().flatMap(sav -> sav.action()).filter(a -> a instanceof LockChatAction).map(a -> (LockChatAction) a).orElseThrow().locked());
        }
    }

}
