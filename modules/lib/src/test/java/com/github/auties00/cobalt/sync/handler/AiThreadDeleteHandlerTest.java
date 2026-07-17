package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.linked.bot.AiThreadTitleBuilder;
import com.github.auties00.cobalt.wire.linked.device.capabilities.DeviceCapabilities;
import com.github.auties00.cobalt.wire.linked.device.capabilities.DeviceCapabilitiesAiThreadBuilder;
import com.github.auties00.cobalt.wire.linked.device.capabilities.DeviceCapabilitiesBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.AiThreadDeleteMutationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link AiThreadDeleteHandler}, which deletes an AI conversation thread keyed by
 * {@code "<botJid>|<threadId>"} from the flat {@code aiThreadTitles} store. The thread feature is
 * gated on {@link DeviceCapabilities.AiThread.SupportLevel}, so the harness starts with the
 * capability unset and individual tests advertise the level they need.
 */
@DisplayName("AiThreadDeleteHandler")
class AiThreadDeleteHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final String BOT_JID_STRING = "867051314767696@bot";
    private static final String THREAD_ID = "thread-123";
    private static final String STORE_KEY = BOT_JID_STRING + "|" + THREAD_ID;

    private LinkedWhatsAppStore store;
    private TestWhatsAppClient client;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private void primaryAiThreadSupport(DeviceCapabilities.AiThread.SupportLevel level) {
        var aiThread = new DeviceCapabilitiesAiThreadBuilder()
                .supportLevel(level)
                .build();
        var caps = new DeviceCapabilitiesBuilder()
                .aiThread(aiThread)
                .build();
        store.contactStore().setPrimaryDeviceCapabilities(caps);
    }

    private static DecryptedMutation.Trusted setMutation(String index) {
        var ts = Instant.ofEpochSecond(1_700_000_000L);
        var value = new SyncActionValueBuilder().timestamp(ts).build();
        return new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts,
                AiThreadDeleteHandler.ACTION_VERSION);
    }

    @Nested
    @DisplayName("metadata - wire constants")
    class Metadata {
        @Test
        @DisplayName("actionName() is ai_thread_delete")
        void actionName() {
            assertEquals("ai_thread_delete", new AiThreadDeleteHandler().actionName());
            assertEquals(AiThreadDeleteHandler.ACTION_NAME, new AiThreadDeleteHandler().actionName());
        }

        @Test
        @DisplayName("collectionName() is REGULAR_HIGH")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_HIGH, new AiThreadDeleteHandler().collectionName());
            assertEquals(AiThreadDeleteHandler.COLLECTION_NAME, new AiThreadDeleteHandler().collectionName());
        }

        @Test
        @DisplayName("version() is 7")
        void version() {
            assertEquals(7, new AiThreadDeleteHandler().version());
            assertEquals(AiThreadDeleteHandler.ACTION_VERSION, new AiThreadDeleteHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation - non-SET operation")
    class RemoveBranch {
        @Test
        @DisplayName("REMOVE operation is UNSUPPORTED")
        void removeUnsupported() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var value = new SyncActionValueBuilder().timestamp(ts).build();
            var index = "[\"ai_thread_delete\",\"" + BOT_JID_STRING + "\",\"" + THREAD_ID + "\"]";
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.REMOVE, ts, 7);
            assertEquals(SyncActionState.UNSUPPORTED,
                    new AiThreadDeleteHandler().applyMutation(client, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("index shorter than 3 elements is MALFORMED")
        void shortIndex() {
            var index = "[\"ai_thread_delete\",\"" + BOT_JID_STRING + "\"]";
            assertEquals(SyncActionState.MALFORMED,
                    new AiThreadDeleteHandler().applyMutation(client, setMutation(index)).actionState());
        }

        @Test
        @DisplayName("blank threadId at index[2] is MALFORMED")
        void blankThreadId() {
            var index = "[\"ai_thread_delete\",\"" + BOT_JID_STRING + "\",\"   \"]";
            assertEquals(SyncActionState.MALFORMED,
                    new AiThreadDeleteHandler().applyMutation(client, setMutation(index)).actionState());
        }

        @Test
        @DisplayName("non-bot JID at index[1] is MALFORMED")
        void nonBotJid() {
            // Plain user JID - not a bot
            var index = "[\"ai_thread_delete\",\"19255550100@s.whatsapp.net\",\"" + THREAD_ID + "\"]";
            assertEquals(SyncActionState.MALFORMED,
                    new AiThreadDeleteHandler().applyMutation(client, setMutation(index)).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - feature gating")
    class FeatureGating {
        @Test
        @DisplayName("primaryDeviceCapabilities absent reports UNSUPPORTED")
        void unsupportedWhenCapabilityAbsent() {
            var index = "[\"ai_thread_delete\",\"" + BOT_JID_STRING + "\",\"" + THREAD_ID + "\"]";
            assertEquals(SyncActionState.UNSUPPORTED,
                    new AiThreadDeleteHandler().applyMutation(client, setMutation(index)).actionState());
        }

        @Test
        @DisplayName("AiThread.SupportLevel.NONE is treated as UNSUPPORTED")
        void unsupportedAtNoneLevel() {
            primaryAiThreadSupport(DeviceCapabilities.AiThread.SupportLevel.NONE);
            var index = "[\"ai_thread_delete\",\"" + BOT_JID_STRING + "\",\"" + THREAD_ID + "\"]";
            assertEquals(SyncActionState.UNSUPPORTED,
                    new AiThreadDeleteHandler().applyMutation(client, setMutation(index)).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan")
    class Orphan {
        @Test
        @DisplayName("no local thread title reports ORPHAN with botJid|threadId as model id")
        void orphanWhenMissingLocally() {
            primaryAiThreadSupport(DeviceCapabilities.AiThread.SupportLevel.FULL);
            var index = "[\"ai_thread_delete\",\"" + BOT_JID_STRING + "\",\"" + THREAD_ID + "\"]";
            var result = new AiThreadDeleteHandler().applyMutation(client, setMutation(index));
            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals(STORE_KEY, result.modelId(),
                    "ORPHAN model id must be the bot+thread composite key");
            assertEquals("Thread", result.modelType(),
                    "ORPHAN model type must be Thread (WA Web SyncModelType.Thread)");
        }
    }

    @Nested
    @DisplayName("applyMutation - happy SET path")
    class HappySet {
        @Test
        @DisplayName("existing thread title is removed and SUCCESS is reported")
        void removesExistingTitle() {
            primaryAiThreadSupport(DeviceCapabilities.AiThread.SupportLevel.FULL);
            store.businessStore().putAiThreadTitle(new AiThreadTitleBuilder()
                    .threadId(STORE_KEY).title("My AI Thread").build());

            var index = "[\"ai_thread_delete\",\"" + BOT_JID_STRING + "\",\"" + THREAD_ID + "\"]";
            var result = new AiThreadDeleteHandler().applyMutation(client, setMutation(index));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(store.businessStore().findAiThreadTitle(STORE_KEY).isEmpty(),
                    "the matching title must be removed from the store");
        }

        @Test
        @DisplayName("INFRA support level is sufficient (any non-NONE level passes the gate)")
        void infraLevelAlsoPasses() {
            primaryAiThreadSupport(DeviceCapabilities.AiThread.SupportLevel.INFRA);
            store.businessStore().putAiThreadTitle(new AiThreadTitleBuilder()
                    .threadId(STORE_KEY).title("My AI Thread").build());

            var index = "[\"ai_thread_delete\",\"" + BOT_JID_STRING + "\",\"" + THREAD_ID + "\"]";
            assertEquals(SyncActionState.SUCCESS,
                    new AiThreadDeleteHandler().applyMutation(client, setMutation(index)).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value (n/a)")
    class MalformedValue {
        @Test
        @DisplayName("the handler ignores the SyncActionValue contents entirely")
        void notExercised() {
            // The index alone identifies the thread, so an empty SyncActionValue is still SUCCESS.
            primaryAiThreadSupport(DeviceCapabilities.AiThread.SupportLevel.FULL);
            store.businessStore().putAiThreadTitle(new AiThreadTitleBuilder()
                    .threadId(STORE_KEY).title("My AI Thread").build());
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var value = new SyncActionValueBuilder().timestamp(ts).build();
            var index = "[\"ai_thread_delete\",\"" + BOT_JID_STRING + "\",\"" + THREAD_ID + "\"]";
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, 7);
            assertEquals(SyncActionState.SUCCESS,
                    new AiThreadDeleteHandler().applyMutation(client, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp tiebreaker")
    class ResolveConflicts {
        @Test
        @DisplayName("remote with later timestamp wins (APPLY_REMOTE_DROP_LOCAL)")
        void remoteLater() {
            var index = "[\"ai_thread_delete\",\"" + BOT_JID_STRING + "\",\"" + THREAD_ID + "\"]";
            var local = setMutation(index);
            var remoteTs = Instant.ofEpochSecond(1_700_000_010L);
            var remoteValue = new SyncActionValueBuilder().timestamp(remoteTs).build();
            var remote = new DecryptedMutation.Trusted(index, remoteValue, SyncdOperation.SET, remoteTs, 7);
            var resolution = new AiThreadDeleteHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    resolution.state());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - default per-item dispatch (n/a override)")
    class BatchDispatch {
        @Test
        @DisplayName("the handler does not override applyMutationBatch")
        void defaultDispatchPreserved() {
            primaryAiThreadSupport(DeviceCapabilities.AiThread.SupportLevel.FULL);
            store.businessStore().putAiThreadTitle(new AiThreadTitleBuilder()
                    .threadId(STORE_KEY).title("My AI Thread").build());
            var index = "[\"ai_thread_delete\",\"" + BOT_JID_STRING + "\",\"" + THREAD_ID + "\"]";
            var results = new AiThreadDeleteHandler().applyMutationBatch(client, List.of(setMutation(index)));
            assertEquals(1, results.size());
            assertEquals(SyncActionState.SUCCESS, results.get(0).actionState());
        }
    }

    @Nested
    @DisplayName("getAiThreadDeleteMutation - pending mutation builder")
    class Builder {
        @Test
        @DisplayName("builder emits a SET pending mutation at the canonical index")
        void buildsCorrectIndex() {
            var botJid = Jid.of(BOT_JID_STRING);
            var pending = new AiThreadDeleteMutationFactory().getAiThreadDeleteMutation(botJid, THREAD_ID);
            var mutation = pending.mutation();
            assertEquals(SyncdOperation.SET, mutation.operation());
            assertEquals(AiThreadDeleteHandler.ACTION_VERSION, mutation.actionVersion());
            assertEquals("[\"ai_thread_delete\",\"" + BOT_JID_STRING + "\",\"" + THREAD_ID + "\"]",
                    mutation.index());
        }

        @Test
        @DisplayName("attemptCount of a freshly built pending mutation is zero")
        void freshAttemptCount() {
            var botJid = Jid.of(BOT_JID_STRING);
            var pending = new AiThreadDeleteMutationFactory().getAiThreadDeleteMutation(botJid, THREAD_ID);
            assertEquals(0, pending.attemptCount());
        }
    }

}
