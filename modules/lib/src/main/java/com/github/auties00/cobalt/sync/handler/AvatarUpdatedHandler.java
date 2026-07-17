package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.media.AvatarUpdatedAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Updates the local user's avatar-presence state in response to {@code avatar_updated_action} sync mutations.
 *
 * <p>When another device creates, updates, or deletes the user's Meta-AI
 * avatar, the resulting state is mirrored into
 * {@link LinkedWhatsAppStore#hasAvatar()}, and the
 * recent-avatar-sticker cache is cleared on every mutation.
 *
 * @implNote
 * This implementation drops the per-batch {@code notSupported},
 * {@code malformed}, and {@code skipped} counter logging that WA Web
 * emits via {@code WALogger.WARN}; the per-mutation outcome surfaces in
 * the returned {@link MutationApplicationResult} instead.
 */
@WhatsAppWebModule(moduleName = "WAWebStickersAvatarUpdatedSyncAction")
public final class AvatarUpdatedHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link AvatarUpdatedHandler}.
     */
    private static final System.Logger LOGGER = Log.get(AvatarUpdatedHandler.class);

    /**
     * The {@link ABPropsService} consulted before applying any mutation.
     *
     * <p>Reads the {@link ABProp#ENABLE_AVATARS_ON_WEB_COMPANION} gate; when
     * the gate is off, every mutation resolves to
     * {@link MutationApplicationResult#unsupported()} unchanged.
     */
    private final ABPropsService abPropsService;

    /**
     * Constructs the avatar-updated handler with its dependency on the AB-props subsystem.
     *
     * <p>The sync handler registry instantiates this type with a shared
     * {@link ABPropsService}.
     *
     * @param abPropsService the {@link ABPropsService} consulted on every mutation
     */
    @WhatsAppWebExport(moduleName = "WAWebStickersAvatarUpdatedSyncAction", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public AvatarUpdatedHandler(ABPropsService abPropsService) {
        this.abPropsService = abPropsService;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebStickersAvatarUpdatedSyncAction", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return AvatarUpdatedAction.ACTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebStickersAvatarUpdatedSyncAction", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return AvatarUpdatedAction.COLLECTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebStickersAvatarUpdatedSyncAction", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return AvatarUpdatedAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads the {@link AvatarUpdatedAction#eventType()} field from the
     * mutation value and either marks the user as having an avatar
     * ({@code CREATED} / {@code UPDATED}) or as not having one ({@code DELETED}),
     * then drops the recent-avatar-sticker cache via
     * {@link LinkedWhatsAppSettingsStore#removeAllRecentAvatarStickers()}.
     *
     * @implNote
     * This implementation gates on the
     * {@link ABProp#ENABLE_AVATARS_ON_WEB_COMPANION} prop first;
     * non-{@link SyncdOperation#SET} operations and missing event types are
     * reported directly through {@link MutationApplicationResult#unsupported()}
     * and {@link SyncdIndexUtils#malformedActionValue(String)}.
     * Mutations whose timestamp is at or before the local pairing
     * timestamp are reported as
     * {@link MutationApplicationResult#skipped()}, mirroring WA Web's
     * "events before pairing" filter.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebStickersAvatarUpdatedSyncAction", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (!abPropsService.getBool(ABProp.ENABLE_AVATARS_ON_WEB_COMPANION)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "avatar updated mutation unsupported: avatars ab-prop disabled");
            return MutationApplicationResult.unsupported();
        }

        if (mutation.operation() != SyncdOperation.SET) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "avatar updated mutation unsupported: operation={0}", mutation.operation());
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof AvatarUpdatedAction action)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "avatar updated mutation malformed: missing action value");
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }
        var eventType = action.eventType().orElse(null);
        if (eventType == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "avatar updated mutation malformed: missing event type");
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        var pairingTimestamp = client.store().accountStore().pairingTimestamp().orElse(null);
        if (pairingTimestamp != null && !mutation.timestamp().isAfter(pairingTimestamp)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "avatar updated mutation skipped: predates pairing timestamp");
            return MutationApplicationResult.skipped();
        }

        switch (eventType) {
            case CREATED, UPDATED -> client.store().accountStore().setHasAvatar(true);
            case DELETED -> client.store().accountStore().setHasAvatar(false);
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "avatar updated: hasAvatar updated via event={0}", eventType);

        client.store().settingsStore().removeAllRecentAvatarStickers();
        return MutationApplicationResult.success();
    }
}
