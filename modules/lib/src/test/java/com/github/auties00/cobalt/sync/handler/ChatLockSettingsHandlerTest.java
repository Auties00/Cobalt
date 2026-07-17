package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.setting.ChatLockSettings;
import com.github.auties00.cobalt.wire.linked.setting.ChatLockSettingsBuilder;
import com.github.auties00.cobalt.wire.linked.setting.TransformerArgUserPasswordValueBuilder;
import com.github.auties00.cobalt.wire.linked.setting.UserPassword;
import com.github.auties00.cobalt.wire.linked.setting.UserPasswordBuilder;
import com.github.auties00.cobalt.wire.linked.setting.UserPasswordTransformerArgBuilder;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
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
 * Tests for {@link ChatLockSettingsHandler} - Cobalt's adapter for
 * {@code WAWebChatLockSettingsSync}.
 */
@DisplayName("ChatLockSettingsHandler")
class ChatLockSettingsHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private static UserPassword validSecretCode() {
        var saltArg = new UserPasswordTransformerArgBuilder()
                .key("salt")
                .value(new TransformerArgUserPasswordValueBuilder().asBlob(new byte[]{1, 2, 3, 4}).build())
                .build();
        var iterArg = new UserPasswordTransformerArgBuilder()
                .key("iterations")
                .value(new TransformerArgUserPasswordValueBuilder().asUnsignedInteger(100_000).build())
                .build();
        return new UserPasswordBuilder()
                .encoding(UserPassword.Encoding.UTF8)
                .transformer(UserPassword.Transformer.PBKDF2_HMAC_SHA512)
                .transformerArg(List.of(saltArg, iterArg))
                .transformedData(new byte[]{9, 9, 9})
                .build();
    }

    private static DecryptedMutation.Trusted mutation(Boolean hide, UserPassword secret, SyncdOperation op, Instant ts) {
        var builder = new ChatLockSettingsBuilder();
        if (hide != null) builder.hideLockedChats(hide);
        if (secret != null) builder.secretCode(secret);
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .chatLockSettings(builder.build())
                .build();
        return new DecryptedMutation.Trusted("[\"setting_chatLock\"]", value, op, ts, ChatLockSettings.ACTION_VERSION);
    }

    @Nested
    @DisplayName("metadata - wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the ChatLockSettings wire constant")
        void actionName() {
            assertEquals(ChatLockSettings.ACTION_NAME, new ChatLockSettingsHandler().actionName());
            assertEquals("setting_chatLock", new ChatLockSettingsHandler().actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR_LOW")
        void collectionName() {
            assertEquals(ChatLockSettings.COLLECTION_NAME, new ChatLockSettingsHandler().collectionName());
            assertEquals(SyncPatchType.REGULAR_LOW, new ChatLockSettingsHandler().collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version (7)")
        void version() {
            assertEquals(ChatLockSettings.ACTION_VERSION, new ChatLockSettingsHandler().version());
            assertEquals(7, new ChatLockSettingsHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation - happy SET")
    class ApplySetHappy {
        @Test
        @DisplayName("SET with hideLockedChats=true and no secret code persists the setting")
        void setsHideOnly() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = new ChatLockSettingsHandler().applyMutation(client, mutation(true, null, SyncdOperation.SET, ts));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var stored = client.store().settingsStore().chatLockSettings().orElseThrow();
            assertTrue(stored.hideLockedChats());
            assertTrue(stored.secretCode().isEmpty());
        }

        @Test
        @DisplayName("SET with a well-formed secret code persists hideLockedChats and the secret")
        void setsWithSecret() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = new ChatLockSettingsHandler().applyMutation(client, mutation(true, validSecretCode(), SyncdOperation.SET, ts));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var stored = client.store().settingsStore().chatLockSettings().orElseThrow();
            assertTrue(stored.hideLockedChats());
            assertTrue(stored.secretCode().isPresent());
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan dimension is n/a")
    class OrphanDimension {
        @Test
        @DisplayName("chat lock is a global setting; no per-entity orphan path")
        void noOrphan() {
            var result = new ChatLockSettingsHandler().applyMutation(client, mutation(true, null, SyncdOperation.SET, Instant.now()));
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
            var mutation = new DecryptedMutation.Trusted("[\"setting_chatLock\"]", value, SyncdOperation.SET, ts, 7);
            var result = new ChatLockSettingsHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("a secret code with the wrong transformer returns MALFORMED")
        void wrongTransformerIsMalformed() {
            var saltArg = new UserPasswordTransformerArgBuilder()
                    .key("salt")
                    .value(new TransformerArgUserPasswordValueBuilder().asBlob(new byte[]{1, 2, 3, 4}).build())
                    .build();
            var iterArg = new UserPasswordTransformerArgBuilder()
                    .key("iterations")
                    .value(new TransformerArgUserPasswordValueBuilder().asUnsignedInteger(100_000).build())
                    .build();
            var secret = new UserPasswordBuilder()
                    .encoding(UserPassword.Encoding.UTF8)
                    .transformer(UserPassword.Transformer.NONE) // not PBKDF2
                    .transformerArg(List.of(saltArg, iterArg))
                    .transformedData(new byte[]{9, 9, 9})
                    .build();
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = new ChatLockSettingsHandler().applyMutation(client, mutation(true, secret, SyncdOperation.SET, ts));
            assertEquals(SyncActionState.MALFORMED, result.actionState(),
                    "WAWebChatLockSettingsSync rejects secret codes whose transformer is not PBKDF2_HMAC_SHA512");
        }

        @Test
        @DisplayName("a secret code missing the iterations arg returns MALFORMED")
        void missingIterationsIsMalformed() {
            var saltArg = new UserPasswordTransformerArgBuilder()
                    .key("salt")
                    .value(new TransformerArgUserPasswordValueBuilder().asBlob(new byte[]{1, 2, 3, 4}).build())
                    .build();
            var secret = new UserPasswordBuilder()
                    .encoding(UserPassword.Encoding.UTF8)
                    .transformer(UserPassword.Transformer.PBKDF2_HMAC_SHA512)
                    .transformerArg(List.of(saltArg))
                    .transformedData(new byte[]{9, 9, 9})
                    .build();
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = new ChatLockSettingsHandler().applyMutation(client, mutation(true, secret, SyncdOperation.SET, ts));
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("a secret code missing the salt arg returns MALFORMED")
        void missingSaltIsMalformed() {
            var iterArg = new UserPasswordTransformerArgBuilder()
                    .key("iterations")
                    .value(new TransformerArgUserPasswordValueBuilder().asUnsignedInteger(100_000).build())
                    .build();
            var secret = new UserPasswordBuilder()
                    .encoding(UserPassword.Encoding.UTF8)
                    .transformer(UserPassword.Transformer.PBKDF2_HMAC_SHA512)
                    .transformerArg(List.of(iterArg))
                    .transformedData(new byte[]{9, 9, 9})
                    .build();
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = new ChatLockSettingsHandler().applyMutation(client, mutation(true, secret, SyncdOperation.SET, ts));
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed action index")
    class MalformedActionIndex {
        @Test
        @DisplayName("the handler ignores the index shape (global setting)")
        void indexShapeIgnored() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .chatLockSettings(new ChatLockSettingsBuilder().hideLockedChats(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted("", value, SyncdOperation.SET, ts, 7);
            var result = new ChatLockSettingsHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.SUCCESS, result.actionState(),
                    "the handler does not parse the index; the setting is keyed off the action only");
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE returns UNSUPPORTED")
    class RemoveOperation {
        @Test
        @DisplayName("REMOVE is unsupported per the WA Web fall-through")
        void removeIsUnsupported() {
            var result = new ChatLockSettingsHandler().applyMutation(client,
                    mutation(true, null, SyncdOperation.REMOVE, Instant.now()));
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - inherits default timestamp comparison")
    class ResolveConflicts {
        @Test
        @DisplayName("newer remote -> APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var local = mutation(false, null, SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            var remote = mutation(true, null, SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    new ChatLockSettingsHandler().resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("older remote -> SKIP_REMOTE")
        void olderRemoteSkipped() {
            var local = mutation(false, null, SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            var remote = mutation(true, null, SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    new ChatLockSettingsHandler().resolveConflicts(local, remote).state());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - accumulates last pending and writes once")
    class ApplyBatchOverride {
        @Test
        @DisplayName("an empty batch produces an empty result list and does not touch the store")
        void emptyBatch() {
            assertTrue(new ChatLockSettingsHandler().applyMutationBatch(client, List.of()).isEmpty());
            assertTrue(client.store().settingsStore().chatLockSettings().isEmpty());
        }

        @Test
        @DisplayName("a malformed-secret mutation reports MALFORMED but commits the sanitised value")
        void malformedSecretCommitsSanitised() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var iterArg = new UserPasswordTransformerArgBuilder()
                    .key("iterations")
                    .value(new TransformerArgUserPasswordValueBuilder().asUnsignedInteger(100_000).build())
                    .build();
            // missing salt -> secret invalid
            var badSecret = new UserPasswordBuilder()
                    .encoding(UserPassword.Encoding.UTF8)
                    .transformer(UserPassword.Transformer.PBKDF2_HMAC_SHA512)
                    .transformerArg(List.of(iterArg))
                    .transformedData(new byte[]{9, 9, 9})
                    .build();
            var results = new ChatLockSettingsHandler().applyMutationBatch(client, List.of(
                    mutation(true, badSecret, SyncdOperation.SET, ts)
            ));
            assertEquals(1, results.size());
            assertEquals(SyncActionState.MALFORMED, results.get(0).actionState());
            // sanitised value still landed: hide=true, secret=null
            var stored = client.store().settingsStore().chatLockSettings().orElseThrow();
            assertTrue(stored.hideLockedChats());
            assertTrue(stored.secretCode().isEmpty(),
                    "WAWebChatLockSettingsSync clears the secret on a malformed-secret mutation but still persists hideLockedChats");
        }

        @Test
        @DisplayName("a REMOVE mutation in the batch is reported as UNSUPPORTED")
        void removeInBatch() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var results = new ChatLockSettingsHandler().applyMutationBatch(client, List.of(
                    mutation(true, null, SyncdOperation.REMOVE, ts)
            ));
            assertEquals(1, results.size());
            assertEquals(SyncActionState.UNSUPPORTED, results.get(0).actionState());
            assertTrue(client.store().settingsStore().chatLockSettings().isEmpty(),
                    "no UNSUPPORTED mutation contributes to the pending save target");
        }

        @Test
        @DisplayName("multiple valid SETs collapse to the last value")
        void multipleSetsCollapseToLast() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var results = new ChatLockSettingsHandler().applyMutationBatch(client, List.of(
                    mutation(false, null, SyncdOperation.SET, ts),
                    mutation(true, null, SyncdOperation.SET, ts.plusSeconds(1))
            ));
            assertEquals(2, results.size());
            for (var r : results) {
                assertEquals(SyncActionState.SUCCESS, r.actionState());
            }
            assertTrue(client.store().settingsStore().chatLockSettings().orElseThrow().hideLockedChats(),
                    "the last SET's value lands in the store");
        }

        @Test
        @DisplayName("a batch of nothing but malformed-action-value mutations leaves the store untouched")
        void allMalformedActionValue() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .archiveChatAction(new ArchiveChatActionBuilder().archived(true).build())
                    .build();
            var bad = new DecryptedMutation.Trusted("[\"setting_chatLock\"]", value, SyncdOperation.SET, ts, 7);
            var results = new ChatLockSettingsHandler().applyMutationBatch(client, List.of(bad));
            assertEquals(1, results.size());
            assertEquals(SyncActionState.MALFORMED, results.get(0).actionState());
            assertFalse(client.store().settingsStore().chatLockSettings().isPresent(),
                    "WAWebChatLockSettingsSync skips the save when no chatLockSettings was ever parsed");
        }
    }

}
