package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.setting.ChatLockSettings;
import com.github.auties00.cobalt.wire.linked.setting.ChatLockSettingsBuilder;
import com.github.auties00.cobalt.wire.linked.setting.UserPassword;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

/**
 * Maintains the global chat-lock settings (hide-locked-chats flag and PBKDF2 secret-code material) from {@code setting_chatLock} sync mutations.
 *
 * <p>This handler drives the Chat Lock surface that hides locked chats from
 * the primary chat list and gates them behind a user secret code. When the
 * user toggles either field on another device, the server replays the
 * resulting {@link ChatLockSettings} here, and the result becomes readable
 * through
 * {@link LinkedWhatsAppSettingsStore#chatLockSettings()}.
 *
 * @implNote
 * This implementation validates the secret-code payload exactly the
 * way WA Web does: the {@link UserPassword#transformer()} must be
 * {@link UserPassword.Transformer#PBKDF2_HMAC_SHA512} and the
 * {@code transformerArg} list must contain both an
 * {@code iterations} entry (as unsigned integer) and a {@code salt}
 * entry (as blob). The {@code hideLockedChats} null-vs-false
 * distinction WA Web flags as malformed is collapsed to {@code false}
 * because Cobalt's nullable-Boolean accessors coalesce {@code null} on
 * read. The eight WA Web counter-logged warning lines are dropped in
 * favour of the per-mutation result; the only retained log is the
 * "mutations parse failed" warning when no batch produced a writable
 * record.
 */
@WhatsAppWebModule(moduleName = "WAWebChatLockSettingsSync")
public final class ChatLockSettingsHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link ChatLockSettingsHandler}.
     */
    private static final System.Logger LOGGER = Log.get(ChatLockSettingsHandler.class);

    /**
     * Constructs the singleton chat-lock-settings handler.
     *
     * <p>The sync handler registry instantiates this once during client
     * bootstrap.
     */
    @WhatsAppWebExport(moduleName = "WAWebChatLockSettingsSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public ChatLockSettingsHandler() {
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebChatLockSettingsSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return ChatLockSettings.ACTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebChatLockSettingsSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return ChatLockSettings.COLLECTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebChatLockSettingsSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return ChatLockSettings.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Validates the {@link ChatLockSettings} value (including the full
     * secret-code payload) and persists it. Returns
     * {@link MutationApplicationResult#unsupported()} for non-{@code SET}
     * operations and {@link SyncdIndexUtils#malformedActionValue(String)} when
     * the value is missing or the secret code fails validation.
     *
     * @implNote
     * This implementation persists only on full success: a
     * malformed-secret-code mutation is rejected without writing the
     * sanitized hide-locked-chats flag, where WA Web instead writes a
     * partial record carrying the flag and a {@code null} secret code.
     * Use {@link #applyMutationBatch(LinkedWhatsAppClient, List)} to obtain
     * the WA Web partial-write semantic across a batch.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebChatLockSettingsSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "chat lock settings mutation unsupported: operation={0}", mutation.operation());
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof ChatLockSettings settings)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "chat lock settings mutation malformed: missing action value");
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        if (!isSecretCodeValid(settings)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "chat lock settings mutation malformed: invalid secret code payload");
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        client.store().settingsStore().setChatLockSettings(settings);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "chat lock settings updated: hideLockedChats={0}", settings.hideLockedChats());
        return MutationApplicationResult.success();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Iterates the batch, building up a single pending
     * {@link ChatLockSettings} record across all SET mutations, and persists it
     * once via
     * {@link LinkedWhatsAppSettingsStore#setChatLockSettings(ChatLockSettings)}.
     * If no SET mutation ever populated the pending record, emits the
     * mutations-parse-failed warning.
     *
     * @implNote
     * This implementation mirrors WA Web's partial-write behaviour:
     * each SET mutation unconditionally re-initialises the pending
     * record with the new {@code hideLockedChats} flag and a
     * {@code null} secret code, and only sets the secret code when
     * its payload validates. A malformed secret code therefore still
     * leaves the new hide-locked-chats flag in the pending record but
     * with the secret code cleared.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebChatLockSettingsSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.DIRECT)
    public List<MutationApplicationResult> applyMutationBatch(LinkedWhatsAppClient client, List<DecryptedMutation.Trusted> mutations) {
        ChatLockSettings pending = null;
        var results = new ArrayList<MutationApplicationResult>(mutations.size());
        for (var mutation : mutations) {
            if (mutation.operation() != SyncdOperation.SET) {
                results.add(MutationApplicationResult.unsupported());
                continue;
            }

            if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof ChatLockSettings settings)) {
                results.add(SyncdIndexUtils.malformedActionValue(collectionName().name()));
                continue;
            }

            pending = new ChatLockSettingsBuilder()
                    .hideLockedChats(settings.hideLockedChats())
                    .secretCode(null)
                    .build();

            if (!isSecretCodeValid(settings)) {
                results.add(SyncdIndexUtils.malformedActionValue(collectionName().name()));
                continue;
            }

            pending.setSecretCode(settings.secretCode().orElse(null));
            results.add(MutationApplicationResult.success());
        }

        if (pending != null) {
            client.store().settingsStore().setChatLockSettings(pending);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "chat lock settings batch applied: mutations={0}", mutations.size());
        } else {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "chat lock settings sync: mutations parse failed, batch size={0}", mutations.size());
        }

        return results;
    }

    /**
     * Returns whether the {@link ChatLockSettings#secretCode()} payload (when present) is well-formed.
     *
     * <p>Gates the secret-code update admitted by both
     * {@link #applyMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)} and
     * {@link #applyMutationBatch(LinkedWhatsAppClient, List)}. An absent secret code
     * is always valid (the user toggled chat-lock off).
     *
     * @implNote
     * This implementation requires
     * {@link UserPassword.Transformer#PBKDF2_HMAC_SHA512} as the
     * transformer; rejects any payload missing
     * {@link UserPassword#encoding()},
     * {@link UserPassword#transformedData()},
     * {@link UserPassword#transformer()}, or
     * {@link UserPassword#transformerArg()}; and walks the
     * transformer-arg list to require both an {@code iterations}
     * entry decoded as
     * {@link UserPassword.TransformerArg.ValueSpec.AsUnsignedInteger}
     * and a {@code salt} entry decoded as
     * {@link UserPassword.TransformerArg.ValueSpec.AsBlob}, mirroring
     * WA Web's {@code reduce} loop.
     *
     * @param settings the {@link ChatLockSettings} whose secret-code payload is validated
     * @return {@code true} when the secret code is absent or fully well-formed; {@code false} otherwise
     */
    private boolean isSecretCodeValid(ChatLockSettings settings) {
        var secretCode = settings.secretCode();
        if (secretCode.isEmpty()) {
            return true;
        }

        var password = secretCode.get();

        var encoding = password.encoding();
        var transformedData = password.transformedData();
        var transformer = password.transformer();
        var transformerArgs = password.transformerArg();

        if (transformerArgs.isEmpty() || transformer.isEmpty() || transformedData.isEmpty() || encoding.isEmpty()) {
            return false;
        }

        if (transformer.get() != UserPassword.Transformer.PBKDF2_HMAC_SHA512) {
            return false;
        }

        var hasIterations = false;
        var hasSalt = false;
        for (var arg : transformerArgs) {
            var value = arg.value().orElse(null);
            if (value == null) {
                continue;
            }
            var key = arg.key().orElse(null);
            if ("iterations".equals(key)) {
                if (value.value().orElse(null) instanceof UserPassword.TransformerArg.ValueSpec.AsUnsignedInteger ui
                        && ui.asUnsignedInteger() != null) {
                    hasIterations = true;
                }
            } else if ("salt".equals(key)) {
                if (value.value().orElse(null) instanceof UserPassword.TransformerArg.ValueSpec.AsBlob blob
                        && blob.asBlob() != null) {
                    hasSalt = true;
                }
            }
        }

        return hasIterations && hasSalt;
    }
}
