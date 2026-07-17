package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientType;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.pairing.LinkedPrimaryPlatform;
import com.github.auties00.cobalt.wire.linked.preference.Label;
import com.github.auties00.cobalt.wire.linked.preference.LabelBuilder;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.LabelEditAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.MdLabelSyncTrackingEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.LabelSyncDeviceRoleType;
import com.github.auties00.cobalt.wire.wam.type.LabelSyncDirectionType;
import com.github.auties00.cobalt.wire.wam.type.LabelSyncResultType;
import com.github.auties00.cobalt.wire.wam.type.LabelSyncTypeEnum;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Applies the {@code label_edit} app-state sync action that creates, edits or
 * deletes a chat label.
 *
 * <p>The action fans out across the {@link SyncPatchType#REGULAR} collection so
 * companion devices show the same set of labels. The mutation index keys each
 * entry by the server-assigned label id, formatted as
 * {@snippet :
 *     ["label_edit", labelId]
 * }
 *
 * @implNote
 * This implementation merges incoming edits into the existing {@link Label} in
 * place so the assignments populated by {@link LabelAssociationHandler} survive
 * the edit. Server-assigned labels (type
 * {@link LabelEditAction.ListType#SERVER_ASSIGNED}) are not added to the main
 * collection because Cobalt has no server-assigned id map yet, so the
 * {@code predefinedId} mapping is currently dropped.
 */
@WhatsAppWebModule(moduleName = "WAWebLabelSync")
public final class LabelEditHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link LabelEditHandler}.
     */
    private static final System.Logger LOGGER = Log.get(LabelEditHandler.class);

    /**
     * Holds the fixed HMAC key WA Web salts every label-sync hash with.
     *
     * <p>Matches the {@code WAWebWamLabelSyncTrackingReporter} module
     * constant {@code "whatsapp_label_sync_tracking_v1"}; the value is the
     * raw UTF-8 key material for the {@code HmacSHA256} computed in
     * {@link #labelSyncHash(String)}.
     */
    private static final String LABEL_SYNC_HASH_KEY = "whatsapp_label_sync_tracking_v1";

    /**
     * Holds the WAM telemetry service used to commit the multi-device
     * {@code MdLabelSyncTracking} event, or {@code null} when the handler is
     * constructed without a telemetry sink.
     *
     * <p>The registry-wired instance is built with a live {@link WamService};
     * the no-argument constructor leaves this {@code null} so unit tests can
     * exercise the mutation logic without a telemetry backend, in which case
     * {@link #emitLabelSyncTracking(LinkedWhatsAppClient, String, LabelSyncResultType, boolean, Integer)}
     * is a no-op.
     */
    private final WamService wamService;

    /**
     * Constructs a new {@link LabelEditHandler} without a telemetry sink.
     *
     * <p>Equivalent to {@link #LabelEditHandler(WamService)} with a
     * {@code null} argument; the {@code MdLabelSyncTracking} WAM event is not
     * emitted for handlers built this way.
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public LabelEditHandler() {
        this(null);
    }

    /**
     * Constructs a new {@link LabelEditHandler} bound to the given WAM
     * telemetry service.
     *
     * @implNote
     * This implementation injects {@link WamService} so the
     * {@code MdLabelSyncTracking} emission that WA Web performs through the
     * global {@code WAWebWamLabelSyncTrackingReporter} singleton is testable
     * and can be suppressed by passing {@code null}.
     *
     * @param wamService the WAM telemetry service used to commit the
     *                   label-sync tracking event, or {@code null} to
     *                   suppress the emission
     */
    public LabelEditHandler(WamService wamService) {
        this.wamService = wamService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebLabelSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return LabelEditAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebLabelSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return LabelEditAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebLabelSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return LabelEditAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Rejects non-{@link SyncdOperation#SET} operations as
     * {@link MutationApplicationResult#unsupported()} and a missing label id or
     * action payload as malformed. A {@link LabelEditAction#deleted()} action
     * removes the label via
     * {@link LinkedWhatsAppSettingsStore#removeLabel(String)};
     * otherwise the row is upserted, merging into an existing {@link Label} in
     * place when one is found or building a new one via {@link LabelBuilder}.
     *
     * <p>Every terminal outcome that reaches a valid label id commits an
     * {@code MdLabelSyncTracking} telemetry event through
     * {@link #emitLabelSyncTracking(LinkedWhatsAppClient, String, LabelSyncResultType, boolean, Integer)}:
     * the missing-action-payload arm reports
     * {@link LabelSyncResultType#FAILED_MISSING_ACTION}, and the delete,
     * server-assigned and upsert arms report
     * {@link LabelSyncResultType#SUCCESS}.
     *
     * @implNote
     * This implementation classifies a missing label-id slot as
     * {@link MutationApplicationResult#malformed()} before parsing to avoid an
     * out-of-bounds exception on {@code JSON.parseArray}. On an upsert the
     * existing {@link Label#assignments()} set survives because the merge path
     * mutates the existing row. Because the {@code isActive} and
     * {@code isImmutable} flags coalesce {@code null} to {@code false}, a
     * {@code true} reading is persisted but a {@code false} reading does not
     * clobber a previously-set {@code true}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebLabelSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        var indexArray = JSON.parseArray(mutation.index());
        if (indexArray.size() <= 1) {
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }
        var labelId = indexArray.getString(1);
        if (labelId == null || labelId.isEmpty()) {
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof LabelEditAction action)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "label edit mutation has malformed action value, label={0}", labelId);
            emitLabelSyncTracking(client, labelId, LabelSyncResultType.FAILED_MISSING_ACTION, false, null);
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        if (action.deleted()) {
            client.store().settingsStore().removeLabel(labelId);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "label deleted, label={0}", labelId);
            emitLabelSyncTracking(client, labelId, LabelSyncResultType.SUCCESS, false, predefinedId(action));
            return MutationApplicationResult.success();
        }

        var name = action.name().orElse("");
        var color = action.color().orElse(0);

        var type = action.type().orElse(null);
        if (type == LabelEditAction.ListType.SERVER_ASSIGNED) {
            // TODO: persist the server-assigned label id to predefined id mapping; Cobalt has
            //       no equivalent store field yet, so the predefinedId association is dropped.
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "label server-assigned mapping not persisted, label={0}", labelId);
            emitLabelSyncTracking(client, labelId, LabelSyncResultType.SUCCESS, true, predefinedId(action));
            return MutationApplicationResult.success();
        }

        var existing = client.store().settingsStore().findLabel(labelId).orElse(null);
        if (existing != null) {
            existing.setName(name);
            existing.setColor(color);
            existing.setPredefinedId(action.predefinedId().isPresent() ? action.predefinedId().getAsInt() : null);
            if (action.orderIndex().isPresent()) {
                existing.setOrderIndex(action.orderIndex().getAsInt());
            }
            if (type != null) {
                existing.setType(type);
            }
            if (action.isActive()) {
                existing.setActive(Boolean.TRUE);
            }
            if (action.isImmutable()) {
                existing.setImmutable(Boolean.TRUE);
            }
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "label updated, label={0}", labelId);
        } else {
            var label = new LabelBuilder()
                    .id(labelId)
                    .name(name)
                    .color(color)
                    .predefinedId(action.predefinedId().isPresent() ? action.predefinedId().getAsInt() : null)
                    .orderIndex(action.orderIndex().isPresent() ? action.orderIndex().getAsInt() : null)
                    .type(type)
                    .isActive(action.isActive() ? Boolean.TRUE : null)
                    .isImmutable(action.isImmutable() ? Boolean.TRUE : null)
                    .build();
            client.store().settingsStore().addLabel(label);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "label created, label={0}", labelId);
        }

        emitLabelSyncTracking(client, labelId, LabelSyncResultType.SUCCESS, true, predefinedId(action));
        return MutationApplicationResult.success();
    }

    /**
     * Extracts the optional predefined-label id carried by the action as a
     * boxed {@link Integer}.
     *
     * @param action the label-edit action
     * @return the predefined id, or {@code null} when the action carries none
     */
    private static Integer predefinedId(LabelEditAction action) {
        return action.predefinedId().isPresent() ? action.predefinedId().getAsInt() : null;
    }

    /**
     * Commits the {@code MdLabelSyncTracking} telemetry event for one applied
     * {@code label_edit} mutation.
     *
     * <p>The emission mirrors the receiver arm of WA Web's
     * {@code WAWebWamLabelSyncTrackingReporter.logLabelSyncEvent}: it is gated
     * on the linked primary being a WhatsApp Business (SMB) client, tags the
     * event as a {@link LabelSyncTypeEnum#LABEL_EDIT} sync in the
     * {@link LabelSyncDirectionType#RECEIVER} direction, records the
     * device-derived {@link #deviceRole(LinkedWhatsAppClient) device role},
     * the current wall-clock millisecond timestamp, and the salted
     * {@link #labelSyncHash(String) label hash}. The predefined id is written
     * only when the action carries one. It is a no-op when no
     * {@link WamService} is bound.
     *
     * @param client       the client whose store supplies the SMB and
     *                     device-role signals
     * @param labelId      the server-assigned label id being synced
     * @param result       the classified outcome of the mutation
     * @param isLabeled    whether the mutation left the label present
     *                     ({@code true}) or absent ({@code false})
     * @param predefinedId the predefined-label id, or {@code null} when absent
     */
    @WhatsAppWebExport(moduleName = "WAWebWamLabelSyncTrackingReporter", exports = "logLabelSyncEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitLabelSyncTracking(LinkedWhatsAppClient client, String labelId, LabelSyncResultType result, boolean isLabeled, Integer predefinedId) {
        if (wamService == null || !isSmb(client)) {
            return;
        }

        var builder = new MdLabelSyncTrackingEventBuilder()
                .labelSyncHash(labelSyncHash(labelId))
                .labelSyncType(LabelSyncTypeEnum.LABEL_EDIT)
                .labelSyncDirection(LabelSyncDirectionType.RECEIVER)
                .labelSyncResult(result)
                .labelSyncIsLabeled(isLabeled)
                .labelSyncTimestamp(Instant.now().toEpochMilli())
                .labelSyncDeviceRole(deviceRole(client));
        if (predefinedId != null) {
            builder.labelSyncPredefinedId(predefinedId);
        }
        wamService.commit(builder.build());
    }

    /**
     * Computes the salted label hash WA Web reports for the given label id.
     *
     * <p>The digest is the lowercase hex {@code HmacSHA256} of the JSON array
     * {@snippet :
     *     ["label_edit", labelId]
     * } keyed by the fixed secret {@value #LABEL_SYNC_HASH_KEY}, mirroring WA
     * Web's {@code generateLabelEditHash}. A failure to compute the MAC yields
     * the {@code "hash_generation_failed"} sentinel WA Web falls back to.
     *
     * @param labelId the server-assigned label id
     * @return the lowercase hex-encoded HMAC-SHA256 digest, or
     *         {@code "hash_generation_failed"} on error
     */
    @WhatsAppWebExport(moduleName = "WAWebWamLabelSyncTrackingReporter", exports = "generateLabelEditHash", adaptation = WhatsAppAdaptation.DIRECT)
    private static String labelSyncHash(String labelId) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(LABEL_SYNC_HASH_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            var payload = "[\"" + LabelEditAction.ACTION_NAME + "\",\"" + labelId + "\"]";
            var digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            var hex = new StringBuilder(digest.length * 2);
            for (var b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "label sync hash generation failed", e);
            return "hash_generation_failed";
        }
    }

    /**
     * Derives the reported label-sync device role from the bound client type.
     *
     * <p>A {@link LinkedWhatsAppClientType#WEB} companion reports
     * {@link LabelSyncDeviceRoleType#COMPANION} (the value WA Web hardcodes),
     * while a mobile primary reports {@link LabelSyncDeviceRoleType#PRIMARY}.
     *
     * @implNote
     * This implementation derives the role from
     * {@link LinkedWhatsAppClientType} rather than hardcoding
     * {@link LabelSyncDeviceRoleType#COMPANION} as WA Web's reporter does,
     * because Cobalt can drive the {@code label_edit} receiver arm as either a
     * companion (web) or the primary (mobile) reimplementation.
     *
     * @param client the client whose type selects the role
     * @return the label-sync device role
     */
    private static LabelSyncDeviceRoleType deviceRole(LinkedWhatsAppClient client) {
        return client.store().accountStore().clientType() == LinkedWhatsAppClientType.WEB
                ? LabelSyncDeviceRoleType.COMPANION
                : LabelSyncDeviceRoleType.PRIMARY;
    }

    /**
     * Returns whether the linked primary device runs a WhatsApp Business
     * (SMB) client.
     *
     * <p>The {@code MdLabelSyncTracking} event is reported only for SMB
     * clients, mirroring the {@code WAWebMobilePlatforms.isSMB} gate WA Web's
     * reporter applies before committing.
     *
     * @param client the client whose account store carries the primary
     *               platform
     * @return {@code true} when the primary platform is a Business variant,
     *         {@code false} otherwise or when no primary platform is recorded
     */
    @WhatsAppWebExport(moduleName = "WAWebMobilePlatforms", exports = "isSMB", adaptation = WhatsAppAdaptation.ADAPTED)
    private static boolean isSmb(LinkedWhatsAppClient client) {
        return client.store().accountStore().primaryPlatform()
                .map(LinkedPrimaryPlatform::isBusiness)
                .orElse(false);
    }

}
