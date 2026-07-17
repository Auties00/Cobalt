package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.media.RemoveRecentStickerAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSyncStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;

/**
 * Removes a sticker from the recent-stickers picker shelf when a paired
 * companion device retires it.
 *
 * <p>The sync dispatcher routes incoming {@code removeRecentSticker} mutations
 * here whenever the paired phone curates its recent-stickers list. The handler
 * deletes the matching entry from the local
 * {@link LinkedWhatsAppStore} recent-stickers map so
 * the sticker picker on this device stops surfacing the retired sticker.
 */
@WhatsAppWebModule(moduleName = "WAWebStickersRemoveRecentSyncAction")
public final class RemoveRecentStickerHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link RemoveRecentStickerHandler}.
     */
    private static final System.Logger LOGGER = Log.get(RemoveRecentStickerHandler.class);

    /**
     * The primary-feature gate name that enables recent-sticker sync.
     *
     * <p>The handler consults the paired phone's reported feature set before
     * applying any mutation; phones that have not announced this feature do
     * not maintain a recent-stickers shelf and incoming mutations are reported
     * as unsupported rather than applied.
     *
     * @implNote
     * This implementation reads the literal {@code "recent_sticker"} directly
     * from
     * {@link LinkedWhatsAppSyncStore#primaryFeatures()}
     * rather than going through {@code WAWebPrimaryFeatures.primaryFeatureEnabled};
     * the predicate is identical because that helper is just a containment
     * check against the same set.
     */
    @WhatsAppWebExport(moduleName = "WAWebMiscGatingUtils", exports = "isRecentStickersMDEnabled", adaptation = WhatsAppAdaptation.ADAPTED)
    private static final String RECENT_STICKER_FEATURE = "recent_sticker";

    /**
     * Constructs the handler.
     *
     * <p>The handler is stateless; Cobalt's sync registry holds a single
     * instance per client.
     */
    @WhatsAppWebExport(moduleName = "WAWebStickersRemoveRecentSyncAction", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public RemoveRecentStickerHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebStickersRemoveRecentSyncAction", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return RemoveRecentStickerAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebStickersRemoveRecentSyncAction", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return RemoveRecentStickerAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebStickersRemoveRecentSyncAction", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return RemoveRecentStickerAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The mutation is reported as {@link MutationApplicationResult#unsupported()}
     * when the {@code recent_sticker} primary feature is absent or the operation
     * is not a {@link SyncdOperation#SET}. The sticker hash is read from index
     * slot {@code [1]} and a missing slot is reported as malformed. The matching
     * recent sticker is looked up by hash; an absent sticker is an
     * {@link MutationApplicationResult#orphan()}. The sticker is removed only when
     * the action carries no {@code lastStickerSentTs} or the local sticker
     * timestamp is less than or equal to that value.
     *
     * @implNote
     * This implementation drops WA Web's {@code WALongInt.maybeNumberOrThrowIfTooLarge} /
     * {@code numberOrThrowIfTooLarge} safe-integer guards because Cobalt's
     * timestamps are already plain {@code long}, and omits the trailing
     * {@code WALogger.WARN} aggregation of the unsupported and malformed counters
     * as telemetry.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebStickersRemoveRecentSyncAction", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (!client.store().syncStore().primaryFeatures().contains(RECENT_STICKER_FEATURE)) {
            if (Log.DEBUG)
                LOGGER.log(Level.DEBUG, "remove recent sticker: unsupported, {0} feature not advertised", RECENT_STICKER_FEATURE);
            return MutationApplicationResult.unsupported();
        }

        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        var indexArray = JSON.parseArray(mutation.index());
        if (indexArray.size() <= 1) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "remove recent sticker mutation malformed: missing hash");
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }
        var stickerHash = indexArray.getString(1);
        if (stickerHash == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "remove recent sticker mutation malformed: null hash");
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        var action = mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof RemoveRecentStickerAction entry ? entry : null;
        var lastStickerSentTs = action == null ? null : action.lastStickerSentTs().orElse(null);

        var sticker = client.store().settingsStore().findRecentSticker(stickerHash);
        if (sticker.isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "remove recent sticker: orphan hash={0}", stickerHash);
            return MutationApplicationResult.orphan();
        }

        var stickerTimestamp = sticker.get().timestamp().orElse(0L);
        if (lastStickerSentTs == null || stickerTimestamp <= toEpochComparable(lastStickerSentTs)) {
            client.store().settingsStore().removeRecentSticker(stickerHash);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "remove recent sticker: removed hash={0}", stickerHash);
        } else {
            if (Log.DEBUG)
                LOGGER.log(Level.DEBUG, "remove recent sticker: kept hash={0}, sent after last-sent watermark", stickerHash);
        }

        return MutationApplicationResult.success();
    }

    /**
     * Converts the action's {@code lastStickerSentTs} {@link Instant} back to an
     * epoch-second {@code long} comparable against the local sticker timestamp.
     *
     * <p>Unwraps the {@link Instant} to its epoch-second value so the
     * {@code localTs <= lastSent} comparison performed by
     * {@link #applyMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)} stays
     * purely numeric.
     *
     * @implNote
     * The unit mismatch between
     * {@link RemoveRecentStickerAction#lastStickerSentTs()} (decoded as
     * epoch-seconds in Cobalt via {@code InstantSecondsMixin}) and the local
     * sticker's millis-since-epoch field is a defect in the
     * {@link RemoveRecentStickerAction} protobuf model; the field should be
     * exposed as a raw {@code OptionalLong} so no unit assumption is encoded
     * here.
     *
     * @param instant the decoded {@link Instant} from the protobuf
     * @return the epoch-second value of {@code instant}
     */
    @WhatsAppWebExport(moduleName = "WAWebStickersRemoveRecentSyncAction", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    private static long toEpochComparable(Instant instant) {
        return instant.getEpochSecond();
    }

}
