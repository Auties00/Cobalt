package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.MuteAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.MuteActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.MuteChatMutationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link MuteChatHandler} mute and unmute classification: a SET with a future
 * {@code muteEndTimestamp} stamps the chat mute, a SET with {@code muted=false} clears it, an
 * unknown chat JID surfaces as {@link SyncActionState#ORPHAN} with {@code modelType="Chat"}, a
 * wrong-typed value or missing end-timestamp and an empty chat JID at {@code indexParts[1]} surface
 * as {@link SyncActionState#MALFORMED}, and {@link SyncdOperation#REMOVE} surfaces as
 * {@link SyncActionState#UNSUPPORTED}.
 *
 * <p>Inbound mutations are built directly via the {@code muteMutation} helper. The
 * {@link MuteChatMutationFactory} smoke check uses {@code muteEndSeconds=0} so no AB-prop is
 * consulted; the group / mention-everyone branch is dead for user JIDs.
 */
@DisplayName("MuteChatHandler")
class MuteChatHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid PEER = Jid.of("1234567890@s.whatsapp.net");

    private TestWhatsAppClient client;
    private TestABPropsService props;
    private MuteChatHandler handler;
    private MuteChatMutationFactory factory;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        props = TestABPropsService.builder().build();
        client = TestWhatsAppClient.create().withStore(store).withAbPropsService(props);
        handler = new MuteChatHandler(props);
        factory = new MuteChatMutationFactory(props);
    }

    private static DecryptedMutation.Trusted muteMutation(boolean muted, Instant muteEnd, Jid jid, Instant ts) {
        var builder = new MuteActionBuilder().muted(muted);
        if (muteEnd != null) builder.muteEndTimestamp(muteEnd);
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .muteAction(builder.build())
                .build();
        var index = JSON.toJSONString(List.of("mute", jid.toString()));
        return new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, 2);
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName returns \"mute\"")
        void actionName() {
            assertEquals("mute", handler.actionName());
            assertEquals(MuteAction.ACTION_NAME, handler.actionName());
        }

        @Test
        @DisplayName("collectionName resolves to REGULAR_HIGH")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_HIGH, handler.collectionName());
        }

        @Test
        @DisplayName("version returns the declared action version 2")
        void version() {
            assertEquals(2, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation SET â€” happy path")
    class HappySet {
        @Test
        @DisplayName("muted=true with a future end timestamp stamps the chat mute")
        void mutesTheChat() {
            var chat = client.store().chatStore().addNewChat(PEER);
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var muteUntil = Instant.ofEpochMilli(System.currentTimeMillis() + 60_000L);

            var result = handler.applyMutation(client, muteMutation(true, muteUntil, PEER, ts));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(chat.mute().isPresent(), "chat must carry a mute state");
            assertTrue(chat.mute().orElseThrow().isMuted(), "chat must be reported as muted");
        }

        @Test
        @DisplayName("muted=false unmutes the chat (toEpochSecond returns 0)")
        void unmutesTheChat() {
            var chat = client.store().chatStore().addNewChat(PEER);

            var result = handler.applyMutation(client,
                    muteMutation(false, null, PEER, Instant.ofEpochSecond(1_700_000_000L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            // mute is set to mutedUntil(0) which is Disabled.
            assertTrue(chat.mute().isPresent());
            assertEquals(0L, chat.mute().orElseThrow().toEpochSecond());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” orphan")
    class Orphan {
        @Test
        @DisplayName("SET against an unknown chat JID returns ORPHAN with modelType=Chat")
        void orphanReturnsChatModel() {
            var muteUntil = Instant.ofEpochMilli(System.currentTimeMillis() + 60_000L);
            var result = handler.applyMutation(client,
                    muteMutation(true, muteUntil, PEER, Instant.ofEpochSecond(1L)));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals(PEER.toString(), result.modelId());
            assertEquals("Chat", result.modelType());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” malformed value")
    class MalformedValue {
        @Test
        @DisplayName("a SyncActionValue carrying a pinAction instead of muteAction is MALFORMED")
        void wrongActionTypeIsMalformed() {
            client.store().chatStore().addNewChat(PEER);
            var wrong = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    JSON.toJSONString(List.of("mute", PEER.toString())),
                    wrong, SyncdOperation.SET, Instant.ofEpochSecond(1L), 2);

            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("muted=true without a muteEndTimestamp is MALFORMED")
        void mutedWithoutEndTimestampIsMalformed() {
            client.store().chatStore().addNewChat(PEER);
            var value = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .muteAction(new MuteActionBuilder().muted(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    JSON.toJSONString(List.of("mute", PEER.toString())),
                    value, SyncdOperation.SET, Instant.ofEpochSecond(1L), 2);

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
            var muteUntil = Instant.ofEpochMilli(System.currentTimeMillis() + 60_000L);
            var value = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .muteAction(new MuteActionBuilder().muted(true).muteEndTimestamp(muteUntil).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"mute\",\"\"]",
                    value, SyncdOperation.SET, Instant.ofEpochSecond(1L), 2);

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
            client.store().chatStore().addNewChat(PEER);
            var mutation = new DecryptedMutation.Trusted(
                    JSON.toJSONString(List.of("mute", PEER.toString())),
                    new SyncActionValueBuilder()
                            .timestamp(Instant.ofEpochSecond(1L))
                            .muteAction(new MuteActionBuilder().muted(false).build())
                            .build(),
                    SyncdOperation.REMOVE, Instant.ofEpochSecond(1L), 2);

            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts â€” default timestamp-based behaviour")
    class ResolveConflicts {
        @Test
        @DisplayName("older local vs. newer remote â†’ APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteWins() {
            var local = muteMutation(true, Instant.ofEpochMilli(2_000L), PEER, Instant.ofEpochSecond(100L));
            var remote = muteMutation(false, null, PEER, Instant.ofEpochSecond(200L));

            var resolution = handler.resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }

        @Test
        @DisplayName("newer local vs. older remote â†’ SKIP_REMOTE")
        void newerLocalWins() {
            var local = muteMutation(true, Instant.ofEpochMilli(2_000L), PEER, Instant.ofEpochSecond(300L));
            var remote = muteMutation(false, null, PEER, Instant.ofEpochSecond(200L));

            var resolution = handler.resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE, resolution.state());
        }
    }

    @Nested
    @DisplayName("generateMuteMutation â€” builder helper (n/a)")
    class BuilderHelpers {
        // generateMuteMutation requires LinkedWhatsAppClient.abPropsService(), which TestWhatsAppClient
        // does not stub. We assert metadata invariants of the produced mutation using a
        // muteEndSeconds=0 unmute call which does not branch through abPropsService
        // (the AB-prop branch only runs for groups, never for user JIDs in this test).
        @Test
        @DisplayName("unmute via generateMuteMutation emits a SET mutation with muted=false")
        void unmuteEmitsMutedFalse() {
            var pending = factory.generateMuteMutation(client, PEER, 0L, null);

            var trusted = pending.mutation();
            assertEquals(SyncdOperation.SET, trusted.operation());
            assertEquals(2, trusted.actionVersion());
            assertEquals(JSON.toJSONString(List.of("mute", PEER.toString())), trusted.index());
            var mute = trusted.value().flatMap(sav -> sav.action()).filter(a -> a instanceof MuteAction).map(a -> (MuteAction) a).orElseThrow();
            assertEquals(false, mute.muted());
        }

        @Test
        @DisplayName("muteEndSeconds=-1 preserves the indefinite sentinel as Instant.ofEpochMilli(-1)")
        void indefiniteMuteUsesSentinel() {
            var pending = factory.generateMuteMutation(client, PEER, -1L, null);

            var mute = pending.mutation().value().flatMap(sav -> sav.action()).filter(a -> a instanceof MuteAction).map(a -> (MuteAction) a).orElseThrow();
            assertTrue(mute.muted());
            assertEquals(Instant.ofEpochMilli(-1L), mute.muteEndTimestamp().orElseThrow(),
                    "the -1 sentinel must be carried through as Instant.ofEpochMilli(-1) so the on-wire int64 stays -1");
        }
    }

}
