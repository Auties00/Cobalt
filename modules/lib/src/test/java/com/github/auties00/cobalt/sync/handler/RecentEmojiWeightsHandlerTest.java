package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.media.RecentEmojiWeight;
import com.github.auties00.cobalt.wire.linked.sync.action.media.RecentEmojiWeightBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.media.RecentEmojiWeightsAction;
import com.github.auties00.cobalt.wire.linked.sync.action.media.RecentEmojiWeightsActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link RecentEmojiWeightsHandler}, which accepts only
 * {@link SyncdOperation#SET}, persists the full {@link RecentEmojiWeight} snapshot via
 * {@link LinkedWhatsAppSettingsStore#setRecentEmojiWeights} (an empty list is still
 * {@link SyncActionState#SUCCESS}), rejects a wrong-typed value as
 * {@link SyncActionState#MALFORMED}, reports {@link SyncActionState#UNSUPPORTED} for
 * {@link SyncdOperation#REMOVE}, and resolves conflicts by timestamp.
 */
@DisplayName("RecentEmojiWeightsHandler")
class RecentEmojiWeightsHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppStore store;
    private LinkedWhatsAppClient client;
    private RecentEmojiWeightsHandler handler;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
        handler = new RecentEmojiWeightsHandler();
    }

    private DecryptedMutation.Trusted build(RecentEmojiWeightsAction action, SyncdOperation op, Instant ts) {
        var valueBuilder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) {
            valueBuilder.recentEmojiWeightsAction(action);
        }
        var index = JSON.toJSONString(List.of(handler.actionName()));
        return new DecryptedMutation.Trusted(index, valueBuilder.build(), op, ts, handler.version());
    }

    private RecentEmojiWeight weight(String emoji, float w) {
        return new RecentEmojiWeightBuilder().emoji(emoji).weight(w).build();
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName() returns 'recent_emoji_weights_action'")
        void actionName() {
            assertEquals(RecentEmojiWeightsAction.ACTION_NAME, handler.actionName());
            assertEquals("recent_emoji_weights_action", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR_LOW")
        void collectionName() {
            assertEquals(RecentEmojiWeightsAction.COLLECTION_NAME, handler.collectionName());
            assertEquals(SyncPatchType.REGULAR_LOW, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version")
        void version() {
            assertEquals(RecentEmojiWeightsAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation - SET persists the weights snapshot")
    class ApplySetHappy {
        @Test
        @DisplayName("SET with a non-empty weights list replaces the store snapshot")
        void setsWeights() {
            assertTrue(store.settingsStore().recentEmojiWeights().isEmpty(), "precondition: weights are unset");
            var action = new RecentEmojiWeightsActionBuilder()
                    .weights(List.of(weight("smile", 0.9f), weight("heart", 0.5f)))
                    .build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var stored = store.settingsStore().recentEmojiWeights();
            assertEquals(2, stored.size());
            assertEquals("smile", stored.get(0).emoji().orElseThrow());
        }

        @Test
        @DisplayName("SET with an empty weights list still returns SUCCESS")
        void setsEmpty() {
            var action = new RecentEmojiWeightsActionBuilder().weights(List.of()).build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(store.settingsStore().recentEmojiWeights().isEmpty());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value")
    class MalformedActionValue {
        @Test
        @DisplayName("a SET with the wrong action type returns MALFORMED")
        void wrongActionType() {
            var ts = Instant.now();
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"recent_emoji_weights_action\"]", value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE is UNSUPPORTED")
    class RemoveOperation {
        @Test
        @DisplayName("REMOVE returns UNSUPPORTED without touching the store")
        void removeIsUnsupported() {
            var action = new RecentEmojiWeightsActionBuilder()
                    .weights(List.of(weight("smile", 0.9f))).build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            assertTrue(store.settingsStore().recentEmojiWeights().isEmpty());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based")
    class ConflictResolution {
        @Test
        @DisplayName("newer remote -> APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var action = new RecentEmojiWeightsActionBuilder()
                    .weights(List.of(weight("smile", 0.9f))).build();
            var local = build(action, SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            var remote = build(action, SyncdOperation.SET, Instant.ofEpochSecond(2_000));

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("older remote -> SKIP_REMOTE")
        void olderRemoteSkipped() {
            var action = new RecentEmojiWeightsActionBuilder()
                    .weights(List.of(weight("smile", 0.9f))).build();
            var local = build(action, SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            var remote = build(action, SyncdOperation.SET, Instant.ofEpochSecond(1_000));

            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    handler.resolveConflicts(local, remote).state());
        }
    }

}
