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
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinAction;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.PinChatMutationFactory;
import com.github.auties00.cobalt.wam.LiveWamService;
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
 * Covers {@link PinChatHandler}: the pin and unpin happy paths, the pinned-chat cap and
 * its oldest-vs-incoming kick-out logic (including the unpin-then-queue path when the cap
 * is saturated and the incoming pin loses to the oldest), the orphan classification when
 * the chat is unknown to the store, the malformed classification when the index JID is
 * empty or unparseable, and the {@link PinChatMutationFactory} builder helpers. Mutations
 * are built via the local {@code pinMutation} helper and the handler is wired to a
 * {@link LiveWamService} so cap-eviction emissions are observable on the test client.
 */
@DisplayName("PinChatHandler")
class PinChatHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid PEER = Jid.of("1234567890@s.whatsapp.net");

    private TestWhatsAppClient client;
    private PinChatHandler handler;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
        var props = TestABPropsService.builder().build();
        var wam = new LiveWamService(client, props);
        handler = new PinChatHandler(wam);
    }

    private static DecryptedMutation.Trusted pinMutation(boolean pinned, Jid jid, Instant ts) {
        var action = new PinActionBuilder().pinned(pinned).build();
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .pinAction(action)
                .build();
        var index = JSON.toJSONString(List.of("pin_v1", jid.toString()));
        return new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, 5);
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName returns the WA wire name \"pin_v1\"")
        void actionName() {
            assertEquals("pin_v1", handler.actionName());
            assertEquals(PinAction.ACTION_NAME, handler.actionName());
        }

        @Test
        @DisplayName("collectionName resolves to REGULAR_LOW")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_LOW, handler.collectionName());
        }

        @Test
        @DisplayName("version returns the declared action version 5")
        void version() {
            assertEquals(5, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation SET â€” happy path")
    class HappySet {
        @Test
        @DisplayName("pinned=true on an existing chat stamps the pinnedTimestamp and clears archive")
        void pinsTheChat() {
            var chat = client.store().chatStore().addNewChat(PEER);
            chat.setArchived(true);

            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = handler.applyMutation(client, pinMutation(true, PEER, ts));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(chat.pinnedTimestamp().isPresent());
            assertEquals(ts, chat.pinnedTimestamp().orElseThrow());
            assertFalse(chat.archived(), "pinning a chat also unarchives it");
        }

        @Test
        @DisplayName("pinned=false clears the pinnedTimestamp")
        void unpinsTheChat() {
            var chat = client.store().chatStore().addNewChat(PEER);
            chat.setPinnedTimestamp(Instant.ofEpochSecond(1L));

            var result = handler.applyMutation(client, pinMutation(false, PEER, Instant.ofEpochSecond(2L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(chat.pinnedTimestamp().isEmpty(), "unpin clears the timestamp");
        }
    }

    @Nested
    @DisplayName("applyMutation â€” orphan")
    class Orphan {
        @Test
        @DisplayName("SET on an unknown chat JID returns ORPHAN with modelType=Chat")
        void orphanReturnsChatModel() {
            var result = handler.applyMutation(client, pinMutation(true, PEER, Instant.ofEpochSecond(1L)));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals(PEER.toString(), result.modelId());
            assertEquals("Chat", result.modelType());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” malformed value")
    class MalformedValue {
        @Test
        @DisplayName("a SyncActionValue carrying an archiveChatAction instead of pinAction is MALFORMED")
        void wrongActionTypeIsMalformed() {
            client.store().chatStore().addNewChat(PEER);
            var wrong = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .archiveChatAction(new ArchiveChatActionBuilder().archived(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    JSON.toJSONString(List.of("pin_v1", PEER.toString())),
                    wrong, SyncdOperation.SET, Instant.ofEpochSecond(1L), 5);

            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("an empty chat JID at slot 1 is MALFORMED")
        void emptyChatJidIsMalformed() {
            client.store().chatStore().addNewChat(PEER);
            var value = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            // PinChatHandler explicitly checks indexArray.size() > 1 first; either short or empty
            // produce the same malformedActionIndex result. Use the empty-slot form so the
            // result type does not change if fastjson behavior shifts.
            var mutation = new DecryptedMutation.Trusted(
                    "[\"pin_v1\",\"\"]",
                    value, SyncdOperation.SET, Instant.ofEpochSecond(1L), 5);

            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("an unparseable chat JID at slot 1 is MALFORMED")
        void unparseableJidIsMalformed() {
            client.store().chatStore().addNewChat(PEER);
            var value = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"pin_v1\",\"not-a-jid-without-server\"]",
                    value, SyncdOperation.SET, Instant.ofEpochSecond(1L), 5);

            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” REMOVE")
    class RemoveOperation {
        @Test
        @DisplayName("REMOVE returns UNSUPPORTED")
        void removeReturnsUnsupported() {
            var chat = client.store().chatStore().addNewChat(PEER);
            chat.setPinnedTimestamp(Instant.ofEpochSecond(1L));
            var mutation = new DecryptedMutation.Trusted(
                    JSON.toJSONString(List.of("pin_v1", PEER.toString())),
                    new SyncActionValueBuilder()
                            .timestamp(Instant.ofEpochSecond(2L))
                            .pinAction(new PinActionBuilder().pinned(false).build())
                            .build(),
                    SyncdOperation.REMOVE,
                    Instant.ofEpochSecond(2L), 5);

            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            // Side-effect not applied.
            assertTrue(chat.pinnedTimestamp().isPresent());
        }
    }

    @Nested
    @DisplayName("pin limit enforcement")
    class PinLimit {
        @Test
        @DisplayName("re-pinning an already-pinned chat refreshes the timestamp without invoking the limit")
        void alreadyPinnedRefreshesTimestamp() {
            var chat = client.store().chatStore().addNewChat(PEER);
            chat.setPinnedTimestamp(Instant.ofEpochSecond(1L));

            var newTs = Instant.ofEpochSecond(2L);
            var result = handler.applyMutation(client, pinMutation(true, PEER, newTs));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals(newTs, chat.pinnedTimestamp().orElseThrow());
        }

        @Test
        @DisplayName("when limit is reached and incoming is newer the oldest is kicked out and a pending unpin is queued")
        void kickOldestWhenIncomingIsNewer() {
            // Fill three slots with old timestamps.
            var a = client.store().chatStore().addNewChat(Jid.of("1@s.whatsapp.net"));
            a.setPinnedTimestamp(Instant.ofEpochSecond(100L)); // oldest
            var b = client.store().chatStore().addNewChat(Jid.of("2@s.whatsapp.net"));
            b.setPinnedTimestamp(Instant.ofEpochSecond(200L));
            var c = client.store().chatStore().addNewChat(Jid.of("3@s.whatsapp.net"));
            c.setPinnedTimestamp(Instant.ofEpochSecond(300L));

            var newcomer = Jid.of("4@s.whatsapp.net");
            client.store().chatStore().addNewChat(newcomer);
            var incoming = Instant.ofEpochSecond(400L);

            var result = handler.applyMutation(client, pinMutation(true, newcomer, incoming));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(a.pinnedTimestamp().isEmpty(), "oldest pin must be evicted");
            assertEquals(incoming, client.store().chatStore().findChatByJid(newcomer).orElseThrow().pinnedTimestamp().orElseThrow());

            var pending = client.store().syncStore().findPendingMutations(SyncPatchType.REGULAR_LOW);
            assertFalse(pending.isEmpty(), "a pending unpin must be queued for the evicted chat");
        }

        @Test
        @DisplayName("when limit is reached and incoming is older the incoming pin is rejected and an unpin is queued for it")
        void rejectIncomingWhenOlderThanOldest() {
            var a = client.store().chatStore().addNewChat(Jid.of("1@s.whatsapp.net"));
            a.setPinnedTimestamp(Instant.ofEpochSecond(1000L));
            var b = client.store().chatStore().addNewChat(Jid.of("2@s.whatsapp.net"));
            b.setPinnedTimestamp(Instant.ofEpochSecond(2000L));
            var c = client.store().chatStore().addNewChat(Jid.of("3@s.whatsapp.net"));
            c.setPinnedTimestamp(Instant.ofEpochSecond(3000L));

            var newcomer = Jid.of("4@s.whatsapp.net");
            var newcomerChat = client.store().chatStore().addNewChat(newcomer);
            var incomingTs = Instant.ofEpochSecond(500L); // older than the oldest current pin

            var result = handler.applyMutation(client, pinMutation(true, newcomer, incomingTs));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(newcomerChat.pinnedTimestamp().isEmpty(),
                    "newcomer is not pinned because it is older than the oldest current pin");
            assertTrue(a.pinnedTimestamp().isPresent(), "oldest pin remains");
            var pending = client.store().syncStore().findPendingMutations(SyncPatchType.REGULAR_LOW);
            assertFalse(pending.isEmpty(), "a pending unpin must be queued for the rejected newcomer");
        }
    }

    @Nested
    @DisplayName("resolveConflicts â€” default timestamp-based behaviour")
    class ResolveConflicts {
        // PinChatHandler does NOT override resolveConflicts; the interface default applies.
        @Test
        @DisplayName("older local vs. newer remote â†’ APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteWins() {
            var local = pinMutation(true, PEER, Instant.ofEpochSecond(100L));
            var remote = pinMutation(false, PEER, Instant.ofEpochSecond(200L));

            var resolution = handler.resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }

        @Test
        @DisplayName("newer local vs. older remote â†’ SKIP_REMOTE")
        void newerLocalWins() {
            var local = pinMutation(true, PEER, Instant.ofEpochSecond(300L));
            var remote = pinMutation(false, PEER, Instant.ofEpochSecond(200L));

            var resolution = handler.resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE, resolution.state());
        }
    }

    @Nested
    @DisplayName("getPinMutation â€” static builder helper")
    class BuilderHelpers {
        @Test
        @DisplayName("getPinMutation builds a SET mutation carrying the pinAction and the [\"pin_v1\", jid] index")
        void pinMutationCarriesActionAndIndex() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var pending = new PinChatMutationFactory(TestWamService.create(client)).getPinMutation(ts, true, PEER);

            var trusted = pending.mutation();
            assertEquals(SyncdOperation.SET, trusted.operation());
            assertEquals(5, trusted.actionVersion());
            assertEquals(ts, trusted.timestamp());
            assertEquals(JSON.toJSONString(List.of("pin_v1", PEER.toString())), trusted.index());
            assertTrue(trusted.value().flatMap(sav -> sav.action()).filter(a -> a instanceof PinAction).map(a -> (PinAction) a).orElseThrow().pinned());
        }

        @Test
        @DisplayName("getMutationsForPin returns exactly one pin mutation (Cobalt does not append the unarchive)")
        void getMutationsForPinReturnsOnlyTheMutation() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var mutations = PinChatHandler.getMutationsForPin(ts, true, PEER);
            assertEquals(1, mutations.size(),
                    "the unarchive is delegated to the caller; only the pin mutation is emitted here");
            assertTrue(mutations.get(0).mutation().value().flatMap(sav -> sav.action()).filter(a -> a instanceof PinAction).map(a -> (PinAction) a).orElseThrow().pinned());
        }
    }

}
