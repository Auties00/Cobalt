package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.device.TimeFormatAction;
import com.github.auties00.cobalt.wire.linked.sync.action.device.TimeFormatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Builds outgoing time-format sync mutations.
 *
 * <p>This factory backs the 12h/24h time-format toggle; one call produces a single
 * {@link SyncPendingMutation} that propagates the chosen format to every linked device and is
 * consumed on receiving devices by
 * {@link com.github.auties00.cobalt.sync.handler.TimeFormatHandler}.
 *
 * @implNote
 * This factory has no direct WA Web counterpart on the time-format module, which exposes only the
 * inbound apply half; outgoing time-format changes there are wrapped via the generic
 * {@code WAWebSyncdActionUtils.buildPendingMutation} pathway shared by every account-scoped syncd
 * action. Cobalt surfaces the typed helper directly so the public time-format setter can build a
 * single mutation without hand-rolling the protobuf wrapping.
 */
public final class TimeFormatMutationFactory {
    /**
     * The logger for {@link TimeFormatMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(TimeFormatMutationFactory.class);

    /**
     * Constructs a time-format mutation factory.
     *
     * <p>The factory keeps no state, so a single instance is sufficient per client.
     */
    public TimeFormatMutationFactory() {

    }

    /**
     * Builds a pending {@code time_format} mutation that broadcasts the given 12h/24h preference to
     * every linked device.
     *
     * <p>The index carries only the action name because the preference is a singleton per account.
     *
     * @implNote
     * This implementation models the {@code SyncActionValue.timeFormatAction} protobuf shape as used
     * by WA Web's generic pending-mutation builder; the mutation is routed through the
     * {@code RegularLow} collection alongside the other account-scoped device settings.
     *
     * @param timestamp            the mutation timestamp recorded on both the outer mutation and the
     *                             inner {@code SyncActionValue}
     * @param twentyFourHourFormat {@code true} to enable 24-hour display, {@code false} for 12-hour
     *                             display
     * @return a pending mutation carrying the {@code time_format} action
     * @throws NullPointerException if {@code timestamp} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdActionUtils", exports = "buildPendingMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getTimeFormatMutation(Instant timestamp, boolean twentyFourHourFormat) {
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building time format mutation, twentyFourHour={0}", twentyFourHourFormat);
        var action = new TimeFormatActionBuilder()
                .isTwentyFourHourFormatEnabled(twentyFourHourFormat)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .timeFormatAction(action)
                .build();
        var index = JSON.toJSONString(List.of(TimeFormatAction.ACTION_NAME));
        var pending = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                TimeFormatAction.ACTION_VERSION
        );
        return new SyncPendingMutation(pending, 0);
    }
}
