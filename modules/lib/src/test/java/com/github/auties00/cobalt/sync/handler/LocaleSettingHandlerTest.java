package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.LocaleSetting;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.LocaleSettingBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppAccountStore;
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
 * Covers the {@link LocaleSettingHandler} for the {@code setting_locale}
 * app-state sync action: metadata, the SET happy path that persists the new
 * locale, the malformed-value branch, the null-locale {@code SKIPPED} branch,
 * the REMOVE rejection and timestamp-based conflict resolution.
 *
 * <p>Tests run against a fresh in-memory {@link DeviceFixtures#temporaryStore}
 * through {@link TestWhatsAppClient} so the
 * {@link LinkedWhatsAppAccountStore#locale()} read-back can
 * be asserted directly.
 */
@DisplayName("LocaleSettingHandler")
class LocaleSettingHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private static DecryptedMutation.Trusted localeMutation(String locale, SyncdOperation op, Instant ts) {
        var builder = new LocaleSettingBuilder();
        if (locale != null) builder.locale(locale);
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .localeSetting(builder.build())
                .build();
        return new DecryptedMutation.Trusted("[\"setting_locale\"]", value, op, ts, LocaleSetting.ACTION_VERSION);
    }

    @Nested
    @DisplayName("metadata - wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the LocaleSetting wire constant")
        void actionName() {
            assertEquals(LocaleSetting.ACTION_NAME, new LocaleSettingHandler().actionName());
            assertEquals("setting_locale", new LocaleSettingHandler().actionName());
        }

        @Test
        @DisplayName("collectionName() returns CRITICAL_BLOCK")
        void collectionName() {
            assertEquals(LocaleSetting.COLLECTION_NAME, new LocaleSettingHandler().collectionName());
            assertEquals(SyncPatchType.CRITICAL_BLOCK, new LocaleSettingHandler().collectionName());
        }

        @Test
        @DisplayName("version() returns the declared LocaleSetting version (3)")
        void version() {
            assertEquals(LocaleSetting.ACTION_VERSION, new LocaleSettingHandler().version());
            assertEquals(3, new LocaleSettingHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation - happy SET")
    class ApplySetHappy {
        @Test
        @DisplayName("persists the new locale into the store and returns SUCCESS")
        void persistsLocale() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);

            var result = new LocaleSettingHandler().applyMutation(client, localeMutation("pt_BR", SyncdOperation.SET, ts));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals("pt_BR", client.store().accountStore().locale().orElseThrow(),
                    "WAWebLocaleSettingSync.applyMutations writes the new locale via setLocale");
        }

        @Test
        @DisplayName("a later SET overwrites an earlier locale value")
        void overwritesEarlier() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            new LocaleSettingHandler().applyMutation(client, localeMutation("en_US", SyncdOperation.SET, ts));
            new LocaleSettingHandler().applyMutation(client, localeMutation("fr_FR", SyncdOperation.SET, ts.plusSeconds(10)));

            assertEquals("fr_FR", client.store().accountStore().locale().orElseThrow());
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan dimension is n/a")
    class OrphanDimension {
        @Test
        @DisplayName("locale is a global setting, so there is no per-entity orphan path")
        void noOrphan() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = new LocaleSettingHandler().applyMutation(client, localeMutation("en_US", SyncdOperation.SET, ts));
            assertEquals(SyncActionState.SUCCESS, result.actionState(),
                    "WAWebLocaleSettingSync has no per-chat/per-contact target; the only outcomes are SUCCESS/UNSUPPORTED/SKIPPED/MALFORMED");
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed action value")
    class MalformedActionValue {
        @Test
        @DisplayName("a SyncActionValue carrying a different action returns MALFORMED")
        void wrongActionShapeIsMalformed() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .archiveChatAction(new ArchiveChatActionBuilder().archived(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted("[\"setting_locale\"]", value, SyncdOperation.SET, ts, 3);

            var result = new LocaleSettingHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState(),
                    "WAWebLocaleSettingSync.applyMutations returns malformedActionValue when localeSetting is absent");
        }

        @Test
        @DisplayName("a LocaleSetting with no locale field returns SKIPPED")
        void nullLocaleIsSkipped() {
            var result = new LocaleSettingHandler().applyMutation(client, localeMutation(null, SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.SKIPPED, result.actionState(),
                    "WA Web: if (s == null) { l++; return {actionState: Skipped}; }");
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed action index")
    class MalformedActionIndex {
        @Test
        @DisplayName("the locale handler ignores the index shape (global setting)")
        void indexShapeIgnored() {
            // The handler does not parse mutation.index() at all; even a wholly empty index applies cleanly.
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .localeSetting(new LocaleSettingBuilder().locale("en").build())
                    .build();
            var mutation = new DecryptedMutation.Trusted("", value, SyncdOperation.SET, ts, 3);

            var result = new LocaleSettingHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.SUCCESS, result.actionState(),
                    "WAWebLocaleSettingSync.applyMutations does not consult indexParts; only the action value is read");
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE returns UNSUPPORTED")
    class RemoveOperation {
        @Test
        @DisplayName("REMOVE is unsupported per the WA Web fall-through")
        void removeIsUnsupported() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = new LocaleSettingHandler().applyMutation(client, localeMutation("en", SyncdOperation.REMOVE, ts));
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - inherits default timestamp comparison")
    class ResolveConflicts {
        @Test
        @DisplayName("newer remote -> APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var local = localeMutation("en", SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            var remote = localeMutation("fr", SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    new LocaleSettingHandler().resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("equal timestamps -> APPLY_REMOTE_DROP_LOCAL (remote wins on tie)")
        void tieGoesToRemote() {
            var ts = Instant.ofEpochSecond(1_500);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    new LocaleSettingHandler().resolveConflicts(
                            localeMutation("en", SyncdOperation.SET, ts),
                            localeMutation("fr", SyncdOperation.SET, ts)
                    ).state());
        }

        @Test
        @DisplayName("older remote -> SKIP_REMOTE")
        void olderRemoteSkipped() {
            var local = localeMutation("en", SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            var remote = localeMutation("fr", SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    new LocaleSettingHandler().resolveConflicts(local, remote).state());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - inherits default sequential apply")
    class ApplyBatch {
        @Test
        @DisplayName("default batch path applies each mutation in order")
        void sequentialApply() {
            var ts1 = Instant.ofEpochSecond(1_000);
            var ts2 = Instant.ofEpochSecond(2_000);
            var results = new LocaleSettingHandler().applyMutationBatch(client, List.of(
                    localeMutation("en", SyncdOperation.SET, ts1),
                    localeMutation("fr", SyncdOperation.SET, ts2)
            ));
            assertEquals(2, results.size());
            assertEquals(SyncActionState.SUCCESS, results.get(0).actionState());
            assertEquals(SyncActionState.SUCCESS, results.get(1).actionState());
            assertEquals("fr", client.store().accountStore().locale().orElseThrow(),
                    "the default batch path leaves the last applied SET as the visible state");
        }
    }

}
