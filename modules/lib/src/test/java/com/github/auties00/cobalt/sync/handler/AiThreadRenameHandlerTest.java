package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.wam.TestWamService;
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
import com.github.auties00.cobalt.wire.linked.sync.action.bot.AiThreadRenameAction;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.AiThreadRenameActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.AiThreadRenameMutationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers {@link AiThreadRenameHandler}, which renames an AI conversation thread in the flat
 * {@code aiThreadTitles} store, keyed by {@code "<botJid>|<threadId>"}. The thread feature is gated
 * on {@link DeviceCapabilities.AiThread.SupportLevel}, so the harness starts with the capability
 * unset and individual tests advertise the level they need.
 */
@DisplayName("AiThreadRenameHandler")
class AiThreadRenameHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final String BOT_JID_STRING = "867051314767696@bot";
    private static final String THREAD_ID = "thread-456";
    private static final String STORE_KEY = BOT_JID_STRING + "|" + THREAD_ID;
    private static final String INDEX = "[\"ai_thread_rename\",\"" + BOT_JID_STRING + "\",\"" + THREAD_ID + "\"]";

    private LinkedWhatsAppStore store;
    private TestWhatsAppClient client;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private void primaryAiThreadSupport(DeviceCapabilities.AiThread.SupportLevel level) {
        var aiThread = new DeviceCapabilitiesAiThreadBuilder().supportLevel(level).build();
        var caps = new DeviceCapabilitiesBuilder().aiThread(aiThread).build();
        store.contactStore().setPrimaryDeviceCapabilities(caps);
    }

    private static DecryptedMutation.Trusted setMutation(AiThreadRenameAction action, String index) {
        var ts = Instant.ofEpochSecond(1_700_000_000L);
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .aiThreadRenameAction(action)
                .build();
        return new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts,
                AiThreadRenameAction.ACTION_VERSION);
    }

    @Nested
    @DisplayName("metadata - wire constants")
    class Metadata {
        @Test
        @DisplayName("actionName() is ai_thread_rename")
        void actionName() {
            assertEquals("ai_thread_rename", new AiThreadRenameHandler().actionName());
            assertEquals(AiThreadRenameAction.ACTION_NAME, new AiThreadRenameHandler().actionName());
        }

        @Test
        @DisplayName("collectionName() is REGULAR_LOW")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_LOW, new AiThreadRenameHandler().collectionName());
            assertEquals(AiThreadRenameAction.COLLECTION_NAME, new AiThreadRenameHandler().collectionName());
        }

        @Test
        @DisplayName("version() is 7")
        void version() {
            assertEquals(7, new AiThreadRenameHandler().version());
            assertEquals(AiThreadRenameAction.ACTION_VERSION, new AiThreadRenameHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation - non-SET operation")
    class RemoveBranch {
        @Test
        @DisplayName("REMOVE operation is UNSUPPORTED")
        void removeUnsupported() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var action = new AiThreadRenameActionBuilder().newTitle("foo").build();
            var value = new SyncActionValueBuilder().timestamp(ts).aiThreadRenameAction(action).build();
            var mutation = new DecryptedMutation.Trusted(INDEX, value, SyncdOperation.REMOVE, ts, 7);
            assertEquals(SyncActionState.UNSUPPORTED,
                    new AiThreadRenameHandler().applyMutation(client, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("index shorter than 3 elements is MALFORMED")
        void shortIndex() {
            var action = new AiThreadRenameActionBuilder().newTitle("New").build();
            assertEquals(SyncActionState.MALFORMED,
                    new AiThreadRenameHandler().applyMutation(client,
                            setMutation(action, "[\"ai_thread_rename\",\"" + BOT_JID_STRING + "\"]")).actionState());
        }

        @Test
        @DisplayName("blank chatJid at index[1] is MALFORMED")
        void blankChatJid() {
            var action = new AiThreadRenameActionBuilder().newTitle("New").build();
            assertEquals(SyncActionState.MALFORMED,
                    new AiThreadRenameHandler().applyMutation(client,
                            setMutation(action, "[\"ai_thread_rename\",\"\",\"" + THREAD_ID + "\"]")).actionState());
        }

        @Test
        @DisplayName("non-bot JID at index[1] is MALFORMED")
        void nonBotJid() {
            primaryAiThreadSupport(DeviceCapabilities.AiThread.SupportLevel.FULL);
            var action = new AiThreadRenameActionBuilder().newTitle("New").build();
            var index = "[\"ai_thread_rename\",\"19255550100@s.whatsapp.net\",\"" + THREAD_ID + "\"]";
            assertEquals(SyncActionState.MALFORMED,
                    new AiThreadRenameHandler().applyMutation(client, setMutation(action, index)).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value")
    class MalformedValue {
        @Test
        @DisplayName("missing aiThreadRenameAction sub-message is MALFORMED")
        void missingActionMessage() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var value = new SyncActionValueBuilder().timestamp(ts).build();
            var mutation = new DecryptedMutation.Trusted(INDEX, value, SyncdOperation.SET, ts, 7);
            assertEquals(SyncActionState.MALFORMED,
                    new AiThreadRenameHandler().applyMutation(client, mutation).actionState());
        }

        @Test
        @DisplayName("present sub-message with null newTitle is MALFORMED")
        void missingNewTitle() {
            var action = new AiThreadRenameActionBuilder().build(); // no newTitle
            assertEquals(SyncActionState.MALFORMED,
                    new AiThreadRenameHandler().applyMutation(client, setMutation(action, INDEX)).actionState());
        }

        @Test
        @DisplayName("blank newTitle (whitespace only) is MALFORMED")
        void blankNewTitle() {
            var action = new AiThreadRenameActionBuilder().newTitle("   ").build();
            assertEquals(SyncActionState.MALFORMED,
                    new AiThreadRenameHandler().applyMutation(client, setMutation(action, INDEX)).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - feature gating")
    class FeatureGating {
        @Test
        @DisplayName("primaryDeviceCapabilities absent reports UNSUPPORTED")
        void unsupportedWhenCapabilityAbsent() {
            var action = new AiThreadRenameActionBuilder().newTitle("New").build();
            assertEquals(SyncActionState.UNSUPPORTED,
                    new AiThreadRenameHandler().applyMutation(client, setMutation(action, INDEX)).actionState());
        }

        @Test
        @DisplayName("AiThread.SupportLevel.NONE is treated as UNSUPPORTED")
        void unsupportedAtNoneLevel() {
            primaryAiThreadSupport(DeviceCapabilities.AiThread.SupportLevel.NONE);
            var action = new AiThreadRenameActionBuilder().newTitle("New").build();
            assertEquals(SyncActionState.UNSUPPORTED,
                    new AiThreadRenameHandler().applyMutation(client, setMutation(action, INDEX)).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan")
    class Orphan {
        @Test
        @DisplayName("no local thread title reports ORPHAN with botJid|threadId as model id")
        void orphanWhenMissingLocally() {
            primaryAiThreadSupport(DeviceCapabilities.AiThread.SupportLevel.FULL);
            var action = new AiThreadRenameActionBuilder().newTitle("New").build();
            var result = new AiThreadRenameHandler().applyMutation(client, setMutation(action, INDEX));
            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals(STORE_KEY, result.modelId());
            assertEquals("Thread", result.modelType());
        }
    }

    @Nested
    @DisplayName("applyMutation - happy SET path")
    class HappySet {
        @Test
        @DisplayName("existing thread title is renamed and SUCCESS is reported")
        void renamesExistingTitle() {
            primaryAiThreadSupport(DeviceCapabilities.AiThread.SupportLevel.FULL);
            store.businessStore().putAiThreadTitle(new AiThreadTitleBuilder()
                    .threadId(STORE_KEY).title("Old Title").build());

            var action = new AiThreadRenameActionBuilder().newTitle("New Title").build();
            var result = new AiThreadRenameHandler().applyMutation(client, setMutation(action, INDEX));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var updated = store.businessStore().findAiThreadTitle(STORE_KEY).orElseThrow();
            assertEquals("New Title", updated.title().orElseThrow(),
                    "the stored title must reflect the rename");
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp tiebreaker")
    class ResolveConflicts {
        @Test
        @DisplayName("remote with later timestamp wins (APPLY_REMOTE_DROP_LOCAL)")
        void remoteLater() {
            var local = setMutation(new AiThreadRenameActionBuilder().newTitle("A").build(), INDEX);
            var remoteTs = Instant.ofEpochSecond(1_700_000_010L);
            var remoteValue = new SyncActionValueBuilder()
                    .timestamp(remoteTs)
                    .aiThreadRenameAction(new AiThreadRenameActionBuilder().newTitle("B").build())
                    .build();
            var remote = new DecryptedMutation.Trusted(INDEX, remoteValue, SyncdOperation.SET, remoteTs, 7);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    new AiThreadRenameHandler().resolveConflicts(local, remote).state());
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
                    .threadId(STORE_KEY).title("Old").build());
            var action = new AiThreadRenameActionBuilder().newTitle("New").build();
            var results = new AiThreadRenameHandler().applyMutationBatch(client,
                    List.of(setMutation(action, INDEX)));
            assertEquals(1, results.size());
            assertEquals(SyncActionState.SUCCESS, results.get(0).actionState());
        }
    }

    @Nested
    @DisplayName("getAiThreadRenameMutation - pending mutation builder")
    class Builder {
        @Test
        @DisplayName("builder emits a SET pending mutation at the canonical index with the new title")
        void buildsCorrect() {
            var botJid = Jid.of(BOT_JID_STRING);
            var pending = new AiThreadRenameMutationFactory(client, TestWamService.create(client)).getAiThreadRenameMutation(botJid, THREAD_ID, "My Title");
            var mutation = pending.mutation();
            assertEquals(SyncdOperation.SET, mutation.operation());
            assertEquals(AiThreadRenameAction.ACTION_VERSION, mutation.actionVersion());
            assertEquals(INDEX, mutation.index());
            var action = mutation.value().flatMap(sav -> sav.action()).filter(a -> a instanceof AiThreadRenameAction).map(a -> (AiThreadRenameAction) a).orElseThrow();
            assertEquals("My Title", action.newTitle().orElseThrow());
        }

        @Test
        @DisplayName("attemptCount of a freshly built pending mutation is zero")
        void freshAttemptCount() {
            var botJid = Jid.of(BOT_JID_STRING);
            assertEquals(0,
                    new AiThreadRenameMutationFactory(client, TestWamService.create(client)).getAiThreadRenameMutation(botJid, THREAD_ID, "X").attemptCount());
        }
    }

}
