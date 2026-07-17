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
import com.github.auties00.cobalt.wire.linked.sync.action.media.MusicUserIdAction;
import com.github.auties00.cobalt.wire.linked.sync.action.media.MusicUserIdActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link MusicUserIdHandler} for the protobuf-only {@code "music_user_id"} action: the
 * handler accepts only {@link SyncdOperation#SET} with at least one of
 * {@link MusicUserIdAction#musicUserId()} or {@link MusicUserIdAction#musicUserIdMap()} populated,
 * persists the action via {@link LinkedWhatsAppBusinessStore#setMusicUserIdState}, and rejects a wrong-typed
 * value or an empty payload as {@link SyncActionState#MALFORMED}.
 *
 * <p>No public outgoing-mutation factory exists for this action, so each test drives the handler
 * directly through {@link MusicUserIdHandler#applyMutation} with hand-built
 * {@link DecryptedMutation.Trusted} mutations.
 */
@DisplayName("MusicUserIdHandler")
class MusicUserIdHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppStore store;
    private LinkedWhatsAppClient client;
    private MusicUserIdHandler handler;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
        handler = new MusicUserIdHandler();
    }

    private DecryptedMutation.Trusted build(MusicUserIdAction action, SyncdOperation op, Instant ts) {
        var valueBuilder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) {
            valueBuilder.musicUserIdAction(action);
        }
        var index = JSON.toJSONString(List.of(handler.actionName()));
        return new DecryptedMutation.Trusted(index, valueBuilder.build(), op, ts, handler.version());
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName() returns 'music_user_id'")
        void actionName() {
            assertEquals(MusicUserIdAction.ACTION_NAME, handler.actionName());
            assertEquals("music_user_id", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version")
        void version() {
            assertEquals(MusicUserIdAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation - SET persists the music user id action")
    class ApplySetHappy {
        @Test
        @DisplayName("SET with a non-empty musicUserId is persisted as-is")
        void setMusicUserId() {
            var action = new MusicUserIdActionBuilder().musicUserId("spotify-user-123").build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var stored = store.businessStore().musicUserIdState().orElseThrow();
            assertEquals("spotify-user-123", stored.musicUserId().orElseThrow());
        }

        @Test
        @DisplayName("SET with a non-empty musicUserIdMap is persisted as-is")
        void setMusicUserIdMap() {
            var action = new MusicUserIdActionBuilder()
                    .musicUserIdMap(Map.of("spotify", "abc", "apple", "xyz"))
                    .build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var stored = store.businessStore().musicUserIdState().orElseThrow();
            assertEquals(2, stored.musicUserIdMap().size());
            assertEquals("abc", stored.musicUserIdMap().get("spotify"));
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
                    "[\"music_user_id\"]", value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }

        @Test
        @DisplayName("a SET whose musicUserId and musicUserIdMap are both empty returns MALFORMED")
        void emptyPayload() {
            var action = new MusicUserIdActionBuilder().build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
            assertTrue(store.businessStore().musicUserIdState().isEmpty());
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE is UNSUPPORTED")
    class RemoveOperation {
        @Test
        @DisplayName("REMOVE returns UNSUPPORTED without touching the store")
        void removeIsUnsupported() {
            var action = new MusicUserIdActionBuilder().musicUserId("foo").build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            assertTrue(store.businessStore().musicUserIdState().isEmpty());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based")
    class ConflictResolution {
        @Test
        @DisplayName("newer remote -> APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var action = new MusicUserIdActionBuilder().musicUserId("foo").build();
            var local = build(action, SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            var remote = build(action, SyncdOperation.SET, Instant.ofEpochSecond(2_000));

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("older remote -> SKIP_REMOTE")
        void olderRemoteSkipped() {
            var action = new MusicUserIdActionBuilder().musicUserId("foo").build();
            var local = build(action, SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            var remote = build(action, SyncdOperation.SET, Instant.ofEpochSecond(1_000));

            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    handler.resolveConflicts(local, remote).state());
        }
    }

}
