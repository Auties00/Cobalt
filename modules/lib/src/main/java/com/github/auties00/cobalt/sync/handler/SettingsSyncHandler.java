package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.SettingsSyncAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppAccountStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Applies the cross-device user-preference mirror carried by
 * {@code settings_sync} mutations.
 *
 * <p>The sync dispatcher routes incoming {@code settings_sync} mutations here
 * whenever a paired desktop client (typically the Windows hybrid shell) changes
 * a setting the user expects mirrored across companions: locale, link-preview
 * suppression, notification tone selection, font size, theme, wallpaper, media
 * auto-download policy. Cobalt persists the subset for which the store has a
 * backing field and silently ignores the rest because the remaining settings
 * are pure UI shell concerns.
 *
 * @implNote
 * This implementation flattens WA Web's batch entry point plus the per-mutation
 * {@code $SettingsSync$p_1} closure into a single
 * {@link #applyMutationBatch(LinkedWhatsAppClient, List)} that reproduces the
 * latest-mutation-per-index dedup map. Mutations whose index is unique within
 * the batch reach the validation pipeline; mutations displaced by a later
 * same-index entry are reported as {@link MutationApplicationResult#skipped()}.
 */
@WhatsAppWebModule(moduleName = "WAWebSettingsSync")
@WhatsAppWebModule(moduleName = "WAWebSettingsSyncHelpers")
public final class SettingsSyncHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link SettingsSyncHandler}.
     */
    private static final System.Logger LOGGER = Log.get(SettingsSyncHandler.class);

    /**
     * The {@code "app"} scope literal used as the wildcard mutation scope for
     * global (non-per-chat) settings.
     */
    private static final String APP_SCOPE = "app";

    /**
     * The AB-props service consulted before applying any mutation.
     */
    private final ABPropsService abPropsService;

    /**
     * The required arity of the parsed {@code indexParts} array for a settings
     * sync mutation.
     *
     * <p>The four index slots are {@code [userIndex, platform, settingKey, scope]}
     * as produced by WA Web's {@code getMutation} builder.
     */
    private static final int INDEX_PARTS_LENGTH = 4;

    /**
     * Constructs the handler.
     *
     * <p>The handler is stateful only through the injected {@link ABPropsService};
     * Cobalt's sync registry holds a single instance per client.
     *
     * @param abPropsService the AB-props service consulted on every mutation
     */
    @WhatsAppWebExport(moduleName = "WAWebSettingsSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public SettingsSyncHandler(ABPropsService abPropsService) {
        this.abPropsService = abPropsService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebSettingsSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return SettingsSyncAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebSettingsSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return SettingsSyncAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebSettingsSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return SettingsSyncAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>When the {@code settings_sync_enabled} primary feature or
     * {@link ABProp#SETTINGS_SYNC_ENABLED} is disabled, every mutation resolves
     * to {@link MutationApplicationResult#unsupported()}. Otherwise the handler
     * builds an index-to-latest-{@code SET} map and dispatches each input
     * mutation to one of three outcomes:
     * {@link MutationApplicationResult#malformed()} when no {@code SET} exists for
     * that index, {@link MutationApplicationResult#skipped()} when a later
     * {@code SET} supersedes this one, or the
     * {@link #applyOne(LinkedWhatsAppClient, DecryptedMutation.Trusted)} result for the
     * per-index winner.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebSettingsSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public List<MutationApplicationResult> applyMutationBatch(LinkedWhatsAppClient client, List<DecryptedMutation.Trusted> mutations) {
        if (!isSettingsSyncEnabled(client)) {
            if (Log.DEBUG)
                LOGGER.log(Level.DEBUG, "settings sync: unsupported batch of {0} mutations, gate closed", mutations.size());
            var unsupported = new ArrayList<MutationApplicationResult>(mutations.size());
            for (var ignored : mutations) {
                unsupported.add(MutationApplicationResult.unsupported());
            }
            return unsupported;
        }

        var latestByIndex = new HashMap<String, DecryptedMutation.Trusted>();
        for (var mutation : mutations) {
            if (mutation.operation() != SyncdOperation.SET) {
                continue;
            }
            var key = mutation.index();
            var existing = latestByIndex.get(key);
            if (existing == null || mutation.timestamp().compareTo(existing.timestamp()) > 0) {
                latestByIndex.put(key, mutation);
            }
        }

        var results = new ArrayList<MutationApplicationResult>(mutations.size());
        for (var mutation : mutations) {
            var latest = latestByIndex.get(mutation.index());
            if (latest == null) {
                results.add(MutationApplicationResult.malformed());
            } else if (latest != mutation) {
                if (Log.TRACE) LOGGER.log(Level.TRACE, "settings sync: skipped superseded mutation for index");
                results.add(MutationApplicationResult.skipped());
            } else {
                results.add(applyOne(client, mutation));
            }
        }
        if (Log.DEBUG)
            LOGGER.log(Level.DEBUG, "settings sync: applied batch of {0} mutations, {1} distinct indices", mutations.size(), latestByIndex.size());
        return results;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is the per-mutation adapter for callers that bypass the batch entry
     * point. It performs the same {@code settings_sync_enabled} gate and
     * delegates to {@link #applyOne(LinkedWhatsAppClient, DecryptedMutation.Trusted)}.
     * Non-{@link SyncdOperation#SET} operations are reported as
     * {@link MutationApplicationResult#unsupported()}; in the batch entry point
     * such mutations never reach the per-mutation path because the dedup map only
     * keeps {@code SET} entries.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebSettingsSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (!isSettingsSyncEnabled(client)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "settings sync: unsupported, gate closed");
            return MutationApplicationResult.unsupported();
        }
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }
        return applyOne(client, mutation);
    }

    /**
     * Validates a single {@code SET} mutation and writes the resolved setting
     * into the store.
     *
     * <p>Serves as the per-mutation worker for both
     * {@link #applyMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)} and
     * {@link #applyMutationBatch(LinkedWhatsAppClient, List)}; the caller is responsible
     * for the {@code settings_sync_enabled} gate and the {@code SET} operation
     * filter. The JSON index is parsed and required to hold exactly
     * {@value #INDEX_PARTS_LENGTH} elements. The mutation is gated on the
     * {@link SettingsSyncAction.SettingPlatform} (only
     * {@link SettingsSyncAction.SettingPlatform#WEB} or, on the Windows hybrid
     * client, {@link SettingsSyncAction.SettingPlatform#HYBRID}). The
     * {@link SettingsSyncAction.SettingKey} is decoded (rejecting
     * {@link SettingsSyncAction.SettingKey#SETTING_KEY_UNKNOWN}), the value is
     * verified to carry a {@link SettingsSyncAction} with the field that backs the
     * key, and the resolved value is written through
     * {@link #applySettingUpdate(LinkedWhatsAppClient, SettingsSyncAction, SettingsSyncAction.SettingKey, String)}.
     *
     * @implNote
     * This implementation omits WA Web's trailing {@code WALogger.WARN}/{@code ERROR}
     * messages as telemetry.
     *
     * @param client   the {@link LinkedWhatsAppClient} whose store receives the update
     * @param mutation the mutation to apply
     * @return the detailed application result
     */
    @WhatsAppWebExport(moduleName = "WAWebSettingsSync", exports = "$SettingsSync$p_1", adaptation = WhatsAppAdaptation.ADAPTED)
    private MutationApplicationResult applyOne(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        JSONArray indexArray;
        try {
            indexArray = JSON.parseArray(mutation.index());
        } catch (Exception exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "settings sync mutation malformed: unparseable index");
            return MutationApplicationResult.malformed();
        }
        if (indexArray == null || indexArray.size() != INDEX_PARTS_LENGTH) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "settings sync mutation malformed: wrong index arity");
            return MutationApplicationResult.malformed();
        }

        var platformValue = indexArray.getString(1);
        var settingKeyValue = indexArray.getString(2);
        var scope = indexArray.getString(3);

        var platform = parsePlatform(platformValue);
        if (!appliesToCurrentPlatform(client, platform)) {
            if (Log.DEBUG)
                LOGGER.log(Level.DEBUG, "settings sync: skipped, platform {0} does not apply to this client", platformValue);
            return MutationApplicationResult.skipped();
        }

        var settingKey = parseSettingKey(settingKeyValue);
        if (settingKey == null) {
            if (Log.WARNING)
                LOGGER.log(Level.WARNING, "settings sync mutation malformed: unknown setting key {0}", settingKeyValue);
            return MutationApplicationResult.malformed();
        }

        if (settingKey == SettingsSyncAction.SettingKey.SETTING_KEY_UNKNOWN) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "settings sync mutation malformed: setting key unknown");
            return MutationApplicationResult.malformed();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof SettingsSyncAction action)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "settings sync mutation malformed: missing action value");
            return MutationApplicationResult.malformed();
        }

        if (!hasFieldFor(action, settingKey)) {
            if (Log.WARNING)
                LOGGER.log(Level.WARNING, "settings sync mutation malformed: missing field for key {0}", settingKey);
            return MutationApplicationResult.malformed();
        }

        try {
            applySettingUpdate(client, action, settingKey, scope);
        } catch (RuntimeException exception) {
            if (Log.ERROR)
                LOGGER.log(Level.ERROR, "settings sync: apply failed for key " + settingKey, exception);
            return MutationApplicationResult.failed();
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "settings sync: applied key={0}", settingKey);
        return MutationApplicationResult.success();
    }

    /**
     * Reports whether settings sync is currently enabled for the given client.
     *
     * <p>Both the {@code settings_sync_enabled} primary feature (reported by the
     * paired phone) and {@link ABProp#SETTINGS_SYNC_ENABLED} must be {@code true};
     * either gate disables the entire batch.
     *
     * @param client the {@link LinkedWhatsAppClient} whose feature flags are inspected
     * @return {@code true} if both gates are open
     */
    private boolean isSettingsSyncEnabled(LinkedWhatsAppClient client) {
        return client.store().syncStore().primaryFeatures().contains("settings_sync_enabled")
                && abPropsService.getBool(ABProp.SETTINGS_SYNC_ENABLED);
    }

    /**
     * Parses the raw platform string from {@code indexParts[1]} into a
     * {@link SettingsSyncAction.SettingPlatform}.
     *
     * <p>Returns {@code null} for any value that is not a numeric string mapping
     * to a known enum index; callers take the
     * {@link MutationApplicationResult#skipped()} branch when the platform is
     * unknown rather than the malformed branch.
     *
     * @param value the raw platform string, may be {@code null}
     * @return the parsed platform, or {@code null} if the value does not map
     */
    private SettingsSyncAction.SettingPlatform parsePlatform(String value) {
        if (value == null) {
            return null;
        }
        try {
            var index = Integer.parseInt(value);
            for (var platform : SettingsSyncAction.SettingPlatform.values()) {
                if (platform.index() == index) {
                    return platform;
                }
            }
            return null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * Parses the raw setting-key string from {@code indexParts[2]} into a
     * {@link SettingsSyncAction.SettingKey}.
     *
     * <p>Returns {@code null} for any value that is not a numeric string mapping
     * to a known enum index; callers take the
     * {@link MutationApplicationResult#malformed()} branch when the key is
     * unknown.
     *
     * @param value the raw setting-key string, may be {@code null}
     * @return the parsed key, or {@code null} if the value does not map
     */
    private SettingsSyncAction.SettingKey parseSettingKey(String value) {
        if (value == null) {
            return null;
        }
        try {
            var index = Integer.parseInt(value);
            for (var settingKey : SettingsSyncAction.SettingKey.values()) {
                if (settingKey.index() == index) {
                    return settingKey;
                }
            }
            return null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * Reports whether a mutation targeting the given platform should be applied
     * on the current client.
     *
     * <p>Mutations targeting {@link SettingsSyncAction.SettingPlatform#WEB} apply
     * unconditionally; mutations targeting
     * {@link SettingsSyncAction.SettingPlatform#HYBRID} apply only when the paired
     * device's platform is {@link ClientPlatformType#WINDOWS}. A {@code null}
     * platform is never applied.
     *
     * @implNote
     * This implementation reads the paired-device platform from
     * {@link LinkedWhatsAppAccountStore#device()} rather than
     * from a runtime environment probe; Cobalt has no per-client environment
     * object equivalent to WA Web's {@code WAWebEnvironment.isWindows} check.
     *
     * @param client   the {@link LinkedWhatsAppClient} whose paired-device platform is consulted
     * @param platform the parsed platform from the mutation index, may be {@code null}
     * @return {@code true} if the mutation should be applied
     */
    private boolean appliesToCurrentPlatform(LinkedWhatsAppClient client, SettingsSyncAction.SettingPlatform platform) {
        if (platform == null) {
            return false;
        }
        return switch (platform) {
            case WEB -> true;
            case HYBRID -> client.store().accountStore().device() != null && client.store().accountStore().device().platform() == ClientPlatformType.WINDOWS;
            default -> false;
        };
    }

    /**
     * Reports whether the protobuf field that backs the given setting key is
     * present on the action.
     *
     * <p>Mutations whose declared {@link SettingsSyncAction.SettingKey} does not
     * have the matching value populated are reported as
     * {@link MutationApplicationResult#malformed()} by the caller.
     *
     * @implNote
     * This implementation cannot strictly inspect "field present" for
     * {@code Boolean}-typed fields because the boolean accessor coalesces a
     * missing value to {@code false} per Cobalt's nullable-boolean convention;
     * for those keys the method returns {@code true} so a default-{@code false}
     * mutation is still applied, which matches the protobuf deserialization model
     * where a {@code BOOL} field defaults to its zero value when absent on the
     * wire. The {@link SettingsSyncAction.SettingKey#SETTING_KEY_UNKNOWN} branch
     * is filtered out by the caller and exists here only for exhaustiveness.
     *
     * @param action the decoded settings sync action
     * @param key    the setting key whose backing field is being checked
     * @return {@code true} if the field is present
     */
    private boolean hasFieldFor(SettingsSyncAction action, SettingsSyncAction.SettingKey key) {
        return switch (key) {
            case START_AT_LOGIN -> true;
            case MINIMIZE_TO_TRAY -> true;
            case LANGUAGE -> action.language().isPresent();
            case REPLACE_TEXT_WITH_EMOJI -> true;
            case BANNER_NOTIFICATION_DISPLAY_MODE -> action.bannerNotificationDisplayMode().isPresent();
            case UNREAD_COUNTER_BADGE_DISPLAY_MODE -> action.unreadCounterBadgeDisplayMode().isPresent();
            case IS_MESSAGES_NOTIFICATION_ENABLED -> true;
            case IS_CALLS_NOTIFICATION_ENABLED -> true;
            case IS_REACTIONS_NOTIFICATION_ENABLED -> true;
            case IS_STATUS_REACTIONS_NOTIFICATION_ENABLED -> true;
            case IS_TEXT_PREVIEW_FOR_NOTIFICATION_ENABLED -> true;
            case DEFAULT_NOTIFICATION_TONE_ID -> action.defaultNotificationToneId().isPresent();
            case GROUP_DEFAULT_NOTIFICATION_TONE_ID -> action.groupDefaultNotificationToneId().isPresent();
            case APP_THEME -> action.appTheme().isPresent();
            case WALLPAPER_ID -> action.wallpaperId().isPresent();
            case IS_DOODLE_WALLPAPER_ENABLED -> true;
            case FONT_SIZE -> action.fontSize().isPresent();
            case IS_PHOTOS_AUTODOWNLOAD_ENABLED -> true;
            case IS_AUDIOS_AUTODOWNLOAD_ENABLED -> true;
            case IS_VIDEOS_AUTODOWNLOAD_ENABLED -> true;
            case IS_DOCUMENTS_AUTODOWNLOAD_ENABLED -> true;
            case DISABLE_LINK_PREVIEWS -> true;
            case NOTIFICATION_TONE_ID -> action.notificationToneId().isPresent();
            case MEDIA_UPLOAD_QUALITY -> action.mediaUploadQuality().isPresent();
            case IS_SPELL_CHECK_ENABLED -> true;
            case IS_ENTER_TO_SEND_ENABLED -> true;
            case IS_GROUP_MESSAGE_NOTIFICATION_ENABLED -> true;
            case IS_GROUP_REACTIONS_NOTIFICATION_ENABLED -> true;
            case IS_STATUS_NOTIFICATION_ENABLED -> true;
            case STATUS_NOTIFICATION_TONE_ID -> action.statusNotificationToneId().isPresent();
            case SHOULD_PLAY_SOUND_FOR_CALL_NOTIFICATION -> true;
            case SETTING_KEY_UNKNOWN -> false;
        };
    }

    /**
     * Writes the resolved setting value into the local store.
     *
     * <p>Only keys with a backing {@link LinkedWhatsAppStore}
     * field produce an observable side-effect; every other key resolves to a
     * successful no-op so the WA-side action state stays consistent. Per-chat
     * overrides (any scope other than {@link #APP_SCOPE}) are likewise a no-op.
     *
     * @implNote
     * WA Web forwards the {@code (settingKey, value, scope)} triple to the Windows
     * hybrid shell via {@code WAWebSettingsSyncHelpers.applySettingUpdate} which
     * calls {@code WAWebBackendApi.frontendSendAndReceive}; Cobalt has no shell, so
     * the only persisted keys are {@code LANGUAGE} and {@code DISABLE_LINK_PREVIEWS},
     * which are the two keys with a backing store field.
     *
     * @param client     the {@link LinkedWhatsAppClient} whose store receives the update
     * @param action     the decoded settings sync action carrying the new value
     * @param settingKey the setting key whose value should be persisted
     * @param scope      either {@link #APP_SCOPE} for global settings or a chat JID string
     */
    @WhatsAppWebExport(moduleName = "WAWebSettingsSyncHelpers", exports = "applySettingUpdate", adaptation = WhatsAppAdaptation.ADAPTED)
    private void applySettingUpdate(LinkedWhatsAppClient client,
                                    SettingsSyncAction action,
                                    SettingsSyncAction.SettingKey settingKey,
                                    String scope) {
        if (!APP_SCOPE.equals(scope)) {
            return;
        }
        var store = client.store();
        switch (settingKey) {
            case START_AT_LOGIN -> store.settingsStore().setStartAtLogin(action.startAtLogin());
            case MINIMIZE_TO_TRAY -> store.settingsStore().setMinimizeToTray(action.minimizeToTray());
            case LANGUAGE -> action.language().ifPresent(store.accountStore()::setLocale);
            case REPLACE_TEXT_WITH_EMOJI -> store.settingsStore().setReplaceTextWithEmoji(action.replaceTextWithEmoji());
            case BANNER_NOTIFICATION_DISPLAY_MODE ->
                    action.bannerNotificationDisplayMode().ifPresent(store.settingsStore()::setBannerNotificationDisplayMode);
            case UNREAD_COUNTER_BADGE_DISPLAY_MODE ->
                    action.unreadCounterBadgeDisplayMode().ifPresent(store.settingsStore()::setUnreadCounterBadgeDisplayMode);
            case IS_MESSAGES_NOTIFICATION_ENABLED ->
                    store.settingsStore().setMessagesNotificationEnabled(action.isMessagesNotificationEnabled());
            case IS_CALLS_NOTIFICATION_ENABLED ->
                    store.settingsStore().setCallsNotificationEnabled(action.isCallsNotificationEnabled());
            case IS_REACTIONS_NOTIFICATION_ENABLED ->
                    store.settingsStore().setReactionsNotificationEnabled(action.isReactionsNotificationEnabled());
            case IS_STATUS_REACTIONS_NOTIFICATION_ENABLED ->
                    store.settingsStore().setStatusReactionsNotificationEnabled(action.isStatusReactionsNotificationEnabled());
            case IS_TEXT_PREVIEW_FOR_NOTIFICATION_ENABLED ->
                    store.settingsStore().setTextPreviewForNotificationEnabled(action.isTextPreviewForNotificationEnabled());
            case DEFAULT_NOTIFICATION_TONE_ID ->
                    action.defaultNotificationToneId().ifPresent(value -> store.settingsStore().setDefaultNotificationToneId(value));
            case GROUP_DEFAULT_NOTIFICATION_TONE_ID ->
                    action.groupDefaultNotificationToneId().ifPresent(value -> store.settingsStore().setGroupDefaultNotificationToneId(value));
            case APP_THEME -> action.appTheme().ifPresent(store.settingsStore()::setAppTheme);
            case WALLPAPER_ID -> action.wallpaperId().ifPresent(value -> store.settingsStore().setWallpaperId(value));
            case IS_DOODLE_WALLPAPER_ENABLED ->
                    store.settingsStore().setDoodleWallpaperEnabled(action.isDoodleWallpaperEnabled());
            case FONT_SIZE -> action.fontSize().ifPresent(value -> store.settingsStore().setFontSize(value));
            case IS_PHOTOS_AUTODOWNLOAD_ENABLED ->
                    store.settingsStore().setPhotosAutodownloadEnabled(action.isPhotosAutodownloadEnabled());
            case IS_AUDIOS_AUTODOWNLOAD_ENABLED ->
                    store.settingsStore().setAudiosAutodownloadEnabled(action.isAudiosAutodownloadEnabled());
            case IS_VIDEOS_AUTODOWNLOAD_ENABLED ->
                    store.settingsStore().setVideosAutodownloadEnabled(action.isVideosAutodownloadEnabled());
            case IS_DOCUMENTS_AUTODOWNLOAD_ENABLED ->
                    store.settingsStore().setDocumentsAutodownloadEnabled(action.isDocumentsAutodownloadEnabled());
            case DISABLE_LINK_PREVIEWS -> store.settingsStore().setDisableLinkPreviews(action.disableLinkPreviews());
            case NOTIFICATION_TONE_ID ->
                    action.notificationToneId().ifPresent(value -> store.settingsStore().setNotificationToneId(value));
            case MEDIA_UPLOAD_QUALITY ->
                    action.mediaUploadQuality().ifPresent(store.settingsStore()::setMediaUploadQuality);
            case IS_SPELL_CHECK_ENABLED -> store.settingsStore().setSpellCheckEnabled(action.isSpellCheckEnabled());
            case IS_ENTER_TO_SEND_ENABLED -> store.settingsStore().setEnterToSendEnabled(action.isEnterToSendEnabled());
            case IS_GROUP_MESSAGE_NOTIFICATION_ENABLED ->
                    store.settingsStore().setGroupMessageNotificationEnabled(action.isGroupMessageNotificationEnabled());
            case IS_GROUP_REACTIONS_NOTIFICATION_ENABLED ->
                    store.settingsStore().setGroupReactionsNotificationEnabled(action.isGroupReactionsNotificationEnabled());
            case IS_STATUS_NOTIFICATION_ENABLED ->
                    store.settingsStore().setStatusNotificationEnabled(action.isStatusNotificationEnabled());
            case STATUS_NOTIFICATION_TONE_ID ->
                    action.statusNotificationToneId().ifPresent(value -> store.settingsStore().setStatusNotificationToneId(value));
            case SHOULD_PLAY_SOUND_FOR_CALL_NOTIFICATION ->
                    store.settingsStore().setPlaySoundForCallNotification(action.shouldPlaySoundForCallNotification());
            case SETTING_KEY_UNKNOWN -> {
            }
        }
    }
}
