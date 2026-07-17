package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.wire.linked.setting.AppTheme;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.SettingsSyncAction;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.SettingsSyncActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.props.TestABPropsService;
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
 * Verifies {@link SettingsSyncHandler}: applying an incoming settings-sync
 * mutation and asserting the {@link LinkedWhatsAppStore} side-effect, across the
 * {@code settings_sync_enabled} primary-feature plus AB-prop gate, the
 * platform filter, the setting-key decode, and the batch dedup outcomes.
 * The fixture pre-opens both gates so gating-agnostic tests reach the
 * validation pipeline; gating tests disable one or both flags on their
 * local {@link LinkedWhatsAppStore} or {@link TestABPropsService} copy.
 */
@DisplayName("SettingsSyncHandler")
class SettingsSyncHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private static final int PLATFORM_WEB_INDEX = SettingsSyncAction.SettingPlatform.WEB.index();
    private static final int PLATFORM_MAC_INDEX = SettingsSyncAction.SettingPlatform.MAC.index();
    private static final int KEY_LANGUAGE_INDEX = SettingsSyncAction.SettingKey.LANGUAGE.index();
    private static final int KEY_DISABLE_LINK_PREVIEWS_INDEX = SettingsSyncAction.SettingKey.DISABLE_LINK_PREVIEWS.index();
    private static final int KEY_UNKNOWN_INDEX = SettingsSyncAction.SettingKey.SETTING_KEY_UNKNOWN.index();

    private LinkedWhatsAppStore store;
    private TestABPropsService props;
    private LinkedWhatsAppClient client;
    private SettingsSyncHandler handler;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        store.syncStore().setPrimaryFeatures(List.of("settings_sync_enabled"));
        props = TestABPropsService.builder()
                .with(ABProp.SETTINGS_SYNC_ENABLED, true)
                .build();
        client = TestWhatsAppClient.create().withStore(store).withAbPropsService(props);
        handler = new SettingsSyncHandler(props);
    }

    // Slot layout is [userIndex, platform, settingKey, scope]; the user-index
    // slot is a literal "u" because the handler never reads it.
    private static String index(int platformIdx, int keyIdx, String scope) {
        return "[\"u\",\"" + platformIdx + "\",\"" + keyIdx + "\",\"" + scope + "\"]";
    }

    private static DecryptedMutation.Trusted languageMutation(int platformIdx, String scope, String locale, Instant ts) {
        var action = new SettingsSyncActionBuilder().language(locale).build();
        var value = new SyncActionValueBuilder().timestamp(ts).settingsSyncAction(action).build();
        return new DecryptedMutation.Trusted(index(platformIdx, KEY_LANGUAGE_INDEX, scope), value, SyncdOperation.SET, ts, SettingsSyncAction.ACTION_VERSION);
    }

    @Nested
    @DisplayName("metadata - wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the SettingsSyncAction wire constant")
        void actionName() {
            assertEquals(SettingsSyncAction.ACTION_NAME, handler.actionName());
            assertEquals("settings_sync", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR_LOW")
        void collectionName() {
            assertEquals(SettingsSyncAction.COLLECTION_NAME, handler.collectionName());
            assertEquals(SyncPatchType.REGULAR_LOW, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version (1)")
        void version() {
            assertEquals(SettingsSyncAction.ACTION_VERSION, handler.version());
            assertEquals(1, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation - gating on settings_sync_enabled")
    class Gating {
        @Test
        @DisplayName("when the primary feature is absent, every mutation returns UNSUPPORTED")
        void primaryFeatureMissingReturnsUnsupported() {
            store.syncStore().setPrimaryFeatures(List.of()); // close the gate
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = handler.applyMutation(client,
                    languageMutation(PLATFORM_WEB_INDEX, "app", "en_US", ts));
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }

        @Test
        @DisplayName("when the AB-prop is false, every mutation returns UNSUPPORTED")
        void abPropFalseReturnsUnsupported() {
            props.set(ABProp.SETTINGS_SYNC_ENABLED, false);
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = handler.applyMutation(client,
                    languageMutation(PLATFORM_WEB_INDEX, "app", "en_US", ts));
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - happy SET (LANGUAGE on WEB platform)")
    class ApplySetHappy {
        @Test
        @DisplayName("LANGUAGE setting on WEB persists into the locale store field")
        void languageWritesLocale() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = handler.applyMutation(client,
                    languageMutation(PLATFORM_WEB_INDEX, "app", "pt_BR", ts));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals("pt_BR", store.accountStore().locale().orElseThrow(),
                    "WAWebSettingsSyncHelpers.applySettingUpdate(LANGUAGE,...) -> store.setLocale");
        }

        @Test
        @DisplayName("DISABLE_LINK_PREVIEWS setting on WEB persists into the store flag")
        void disableLinkPreviewsWritesFlag() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var action = new SettingsSyncActionBuilder().disableLinkPreviews(true).build();
            var value = new SyncActionValueBuilder().timestamp(ts).settingsSyncAction(action).build();
            var mutation = new DecryptedMutation.Trusted(
                    index(PLATFORM_WEB_INDEX, KEY_DISABLE_LINK_PREVIEWS_INDEX, "app"),
                    value, SyncdOperation.SET, ts, 1);
            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(store.settingsStore().disableLinkPreviews());
        }

        @Test
        @DisplayName("a setting key with no Cobalt store equivalent still resolves to SUCCESS (UI-only)")
        void unmappedKeyIsSuccess() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            // APP_THEME is on the WA Web shell - Cobalt has no store field, so it's a no-op success.
            var action = new SettingsSyncActionBuilder().appTheme(AppTheme.DARK).build();
            var value = new SyncActionValueBuilder().timestamp(ts).settingsSyncAction(action).build();
            var mutation = new DecryptedMutation.Trusted(
                    index(PLATFORM_WEB_INDEX, SettingsSyncAction.SettingKey.APP_THEME.index(), "app"),
                    value, SyncdOperation.SET, ts, 1);
            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan dimension is n/a")
    class OrphanDimension {
        @Test
        @DisplayName("settings sync writes to global store fields; no per-entity orphan path")
        void noOrphan() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = handler.applyMutation(client,
                    languageMutation(PLATFORM_WEB_INDEX, "app", "en_US", ts));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed action value")
    class MalformedActionValue {
        @Test
        @DisplayName("a SyncActionValue carrying a different action returns MALFORMED")
        void wrongActionIsMalformed() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .archiveChatAction(new ArchiveChatActionBuilder().archived(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    index(PLATFORM_WEB_INDEX, KEY_LANGUAGE_INDEX, "app"),
                    value, SyncdOperation.SET, ts, 1);
            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("a SettingsSyncAction missing the field for the selected key returns MALFORMED")
        void missingFieldIsMalformed() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            // LANGUAGE key but no language field present
            var action = new SettingsSyncActionBuilder().build();
            var value = new SyncActionValueBuilder().timestamp(ts).settingsSyncAction(action).build();
            var mutation = new DecryptedMutation.Trusted(
                    index(PLATFORM_WEB_INDEX, KEY_LANGUAGE_INDEX, "app"),
                    value, SyncdOperation.SET, ts, 1);
            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState(),
                    "WAWebSettingsSync.$SettingsSync$p_1: `if (b === void 0) return Malformed`");
        }

        @Test
        @DisplayName("the SETTING_KEY_UNKNOWN index returns MALFORMED")
        void unknownSettingKeyIsMalformed() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var action = new SettingsSyncActionBuilder().language("en").build();
            var value = new SyncActionValueBuilder().timestamp(ts).settingsSyncAction(action).build();
            var mutation = new DecryptedMutation.Trusted(
                    index(PLATFORM_WEB_INDEX, KEY_UNKNOWN_INDEX, "app"),
                    value, SyncdOperation.SET, ts, 1);
            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed action index")
    class MalformedActionIndex {
        @Test
        @DisplayName("an index with the wrong arity returns MALFORMED")
        void wrongArityIsMalformed() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var action = new SettingsSyncActionBuilder().language("en").build();
            var value = new SyncActionValueBuilder().timestamp(ts).settingsSyncAction(action).build();
            // only 3 elements instead of 4
            var mutation = new DecryptedMutation.Trusted("[\"u\",\"1\",\"3\"]", value, SyncdOperation.SET, ts, 1);
            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("a non-JSON index returns MALFORMED")
        void nonJsonIndexIsMalformed() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var action = new SettingsSyncActionBuilder().language("en").build();
            var value = new SyncActionValueBuilder().timestamp(ts).settingsSyncAction(action).build();
            var mutation = new DecryptedMutation.Trusted("not-json", value, SyncdOperation.SET, ts, 1);
            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("a non-numeric platform field is treated as null -> SKIPPED")
        void nonNumericPlatformIsSkipped() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var action = new SettingsSyncActionBuilder().language("en").build();
            var value = new SyncActionValueBuilder().timestamp(ts).settingsSyncAction(action).build();
            var mutation = new DecryptedMutation.Trusted("[\"u\",\"WEB\",\"3\",\"app\"]", value, SyncdOperation.SET, ts, 1);
            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.SKIPPED, result.actionState(),
                    "WA Web: parsePlatform fails -> appliesToCurrentPlatform false -> Skipped");
        }

        @Test
        @DisplayName("a non-numeric setting-key field returns MALFORMED")
        void nonNumericSettingKeyIsMalformed() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var action = new SettingsSyncActionBuilder().language("en").build();
            var value = new SyncActionValueBuilder().timestamp(ts).settingsSyncAction(action).build();
            var mutation = new DecryptedMutation.Trusted("[\"u\",\"1\",\"LANGUAGE\",\"app\"]", value, SyncdOperation.SET, ts, 1);
            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - non-WEB platforms are SKIPPED")
    class NonWebPlatformSkipped {
        @Test
        @DisplayName("MAC platform mutation is skipped on a non-Windows Cobalt client")
        void macIsSkipped() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = handler.applyMutation(client,
                    languageMutation(PLATFORM_MAC_INDEX, "app", "fr_FR", ts));
            assertEquals(SyncActionState.SKIPPED, result.actionState());
            assertTrue(store.accountStore().locale().isEmpty(), "no platform-MAC mutation should reach the store on a non-Windows client");
        }
    }

    @Nested
    @DisplayName("applyMutation - non-app scope is a no-op SUCCESS")
    class NonAppScope {
        @Test
        @DisplayName("per-chat scope mutations resolve to SUCCESS but write nothing")
        void perChatScopeIsNoOp() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = handler.applyMutation(client,
                    languageMutation(PLATFORM_WEB_INDEX, "1234@s.whatsapp.net", "fr_FR", ts));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(store.accountStore().locale().isEmpty(),
                    "Cobalt does not maintain a per-chat settings store; non-app scope is intentionally a no-op");
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE returns UNSUPPORTED")
    class RemoveOperation {
        @Test
        @DisplayName("REMOVE is unsupported per the WA Web fall-through")
        void removeIsUnsupported() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var action = new SettingsSyncActionBuilder().language("en").build();
            var value = new SyncActionValueBuilder().timestamp(ts).settingsSyncAction(action).build();
            var mutation = new DecryptedMutation.Trusted(
                    index(PLATFORM_WEB_INDEX, KEY_LANGUAGE_INDEX, "app"),
                    value, SyncdOperation.REMOVE, ts, 1);
            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - inherits default timestamp comparison")
    class ResolveConflicts {
        @Test
        @DisplayName("newer remote -> APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var local = languageMutation(PLATFORM_WEB_INDEX, "app", "en", Instant.ofEpochSecond(1_000));
            var remote = languageMutation(PLATFORM_WEB_INDEX, "app", "fr", Instant.ofEpochSecond(2_000));
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("older remote -> SKIP_REMOTE")
        void olderRemoteSkipped() {
            var local = languageMutation(PLATFORM_WEB_INDEX, "app", "en", Instant.ofEpochSecond(2_000));
            var remote = languageMutation(PLATFORM_WEB_INDEX, "app", "fr", Instant.ofEpochSecond(1_000));
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    handler.resolveConflicts(local, remote).state());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - latest-by-timestamp dedup per index")
    class ApplyBatchDedup {
        @Test
        @DisplayName("only the latest mutation for a given index is applied; earlier ones are SKIPPED")
        void latestByTimestampWins() {
            var indexA = index(PLATFORM_WEB_INDEX, KEY_LANGUAGE_INDEX, "app");
            var ts1 = Instant.ofEpochSecond(1_000);
            var ts2 = Instant.ofEpochSecond(2_000);

            var earlier = new DecryptedMutation.Trusted(indexA,
                    new SyncActionValueBuilder().timestamp(ts1).settingsSyncAction(
                            new SettingsSyncActionBuilder().language("en").build()).build(),
                    SyncdOperation.SET, ts1, 1);
            var later = new DecryptedMutation.Trusted(indexA,
                    new SyncActionValueBuilder().timestamp(ts2).settingsSyncAction(
                            new SettingsSyncActionBuilder().language("fr").build()).build(),
                    SyncdOperation.SET, ts2, 1);

            var results = handler.applyMutationBatch(client, List.of(earlier, later));

            assertEquals(2, results.size());
            assertEquals(SyncActionState.SKIPPED, results.get(0).actionState(),
                    "earlier mutation with the same index must be dropped by the dedup map");
            assertEquals(SyncActionState.SUCCESS, results.get(1).actionState());
            assertEquals("fr", store.accountStore().locale().orElseThrow());
        }

        @Test
        @DisplayName("when only non-SET ops cover an index, every entry reports MALFORMED")
        void noSetMutationForIndexMalformed() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var action = new SettingsSyncActionBuilder().language("en").build();
            var value = new SyncActionValueBuilder().timestamp(ts).settingsSyncAction(action).build();
            var indexA = index(PLATFORM_WEB_INDEX, KEY_LANGUAGE_INDEX, "app");
            var removeOnly = new DecryptedMutation.Trusted(indexA, value, SyncdOperation.REMOVE, ts, 1);

            var results = handler.applyMutationBatch(client, List.of(removeOnly));
            assertEquals(1, results.size());
            assertEquals(SyncActionState.MALFORMED, results.get(0).actionState(),
                    "WA Web: when no SET mutation lands in the latestByIndex map for a given index, the result is Malformed");
        }

        @Test
        @DisplayName("when settings_sync_enabled is off, every batch entry is UNSUPPORTED")
        void disabledBatchAllUnsupported() {
            props.set(ABProp.SETTINGS_SYNC_ENABLED, false);
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var results = handler.applyMutationBatch(client, List.of(
                    languageMutation(PLATFORM_WEB_INDEX, "app", "en", ts),
                    languageMutation(PLATFORM_WEB_INDEX, "app", "fr", ts.plusSeconds(1))
            ));
            assertEquals(2, results.size());
            for (var r : results) {
                assertEquals(SyncActionState.UNSUPPORTED, r.actionState());
            }
        }
    }

}
