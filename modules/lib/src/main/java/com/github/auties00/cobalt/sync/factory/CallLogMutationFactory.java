package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.call.CallLog;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.call.CallLogAction;
import com.github.auties00.cobalt.wire.linked.sync.action.call.CallLogActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing app-state mutations that record a call-log entry.
 *
 * <p>When a VoIP call ends, the resulting mutation is pushed so the call
 * appears in every linked device's call tab. This factory builds the outgoing
 * mutation; the inbound counterpart is
 * {@link com.github.auties00.cobalt.sync.handler.CallLogHandler}.
 *
 * @implNote
 * This implementation takes the resolved caller JID as a parameter. WA Web
 * derives it inline by reading {@code callCreatorJid} first, then falling back
 * to the local-device PN when {@code fromMe} or to the {@code peerJid}
 * otherwise; Cobalt callers resolve that upstream because the Me-user lookup is
 * store-side.
 */
public final class CallLogMutationFactory {
    /**
     * The logger for {@link CallLogMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(CallLogMutationFactory.class);

    /**
     * Creates a stateless factory with no collaborators.
     *
     * <p>A single instance may be shared across the lifetime of the client.
     */
    public CallLogMutationFactory() {

    }

    /**
     * Returns a SET mutation that records the given call in the cross-device call log.
     *
     * <p>Emit one mutation per terminated VoIP call. The mutation index follows
     * {@snippet :
     *     ["call_log", callerJid.toString(), callId, fromMe ? "1" : "0"]
     * }
     * and the {@link CallLogAction} sub-message carries the {@link CallLog}
     * record (result, duration, start time, video flag, participants,
     * scheduled-call metadata).
     *
     * @implNote
     * This implementation passes {@code log} through verbatim into the
     * {@link CallLogAction} because {@link CallLog} already carries the same
     * shape WA Web builds field-by-field, so its per-participant projection
     * collapses into a single builder field.
     *
     * @param timestamp the mutation timestamp
     * @param callerJid the JID used as the first index segment (pre-resolved
     *                  caller, falling back to the peer or the local device PN)
     * @param callId    the call identifier as exposed by the VoIP stack
     * @param fromMe    {@code true} when the local user initiated the call,
     *                  encoded as {@code "1"}/{@code "0"} in the index
     * @param log       the call record to ship
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebCallLogSync", exports = "getCallLogMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getCallLogMutation(
            Instant timestamp,
            Jid callerJid,
            String callId,
            boolean fromMe,
            CallLog log
    ) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building call log mutation caller={0} call={1} fromMe={2}", callerJid, callId, fromMe);
        var action = new CallLogActionBuilder()
                .log(log)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .callLogAction(action)
                .build();
        var fromMeStr = fromMe ? "1" : "0";
        var index = JSON.toJSONString(List.of(CallLogAction.ACTION_NAME, callerJid.toString(), callId, fromMeStr));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                CallLogAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
