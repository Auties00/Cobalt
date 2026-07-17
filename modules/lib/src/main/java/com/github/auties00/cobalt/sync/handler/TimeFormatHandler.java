package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.device.TimeFormatAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Mirrors the user's 12/24-hour clock preference across linked devices.
 *
 * <p>The sync dispatcher routes incoming {@code time_format} mutations here
 * whenever the user toggles 12/24-hour display on another linked device. The
 * handler writes the boolean preference into
 * {@link LinkedWhatsAppSettingsStore#setTwentyFourHourFormat(boolean)}
 * so any UI built on top of Cobalt can render timestamps using the user-chosen
 * format.
 */
@WhatsAppWebModule(moduleName = "WAWebTimeFormatSync")
public final class TimeFormatHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link TimeFormatHandler}.
     */
    private static final System.Logger LOGGER = Log.get(TimeFormatHandler.class);

    /**
     * Constructs the handler.
     *
     * <p>The handler is stateless; Cobalt's sync registry holds a single
     * instance per client.
     */
    @WhatsAppWebExport(moduleName = "WAWebTimeFormatSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public TimeFormatHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebTimeFormatSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return TimeFormatAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebTimeFormatSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return TimeFormatAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebTimeFormatSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return TimeFormatAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A non-{@link SyncdOperation#SET} operation is reported as
     * {@link MutationApplicationResult#unsupported()}; a value that does not decode
     * to a {@link TimeFormatAction} is reported as malformed; otherwise the boolean
     * is written to
     * {@link LinkedWhatsAppSettingsStore#setTwentyFourHourFormat(boolean)}.
     *
     * @implNote
     * This implementation writes directly to the store in place of WA Web's
     * {@code WAWebBackendApi.frontendFireAndForget("setIs24Hour", ...)} shell hop,
     * never consults the {@code md_syncd_24_hour_time_format_sync_enabled} AB-prop
     * because WA Web itself never reads it, and omits the trailing
     * {@code WALogger.WARN} unsupported counter as telemetry.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebTimeFormatSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof TimeFormatAction action)) {
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        client.store().settingsStore().setTwentyFourHourFormat(action.isTwentyFourHourFormatEnabled());
        if (Log.DEBUG)
            LOGGER.log(Level.DEBUG, "time format: set twentyFourHour={0}", action.isTwentyFourHourFormatEnabled());

        return MutationApplicationResult.success();
    }

}
