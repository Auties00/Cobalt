package com.github.auties00.cobalt.calls.telemetry;

import com.github.auties00.cobalt.calls.engine.context.CallContext;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.call.CallEndReason;
import com.github.auties00.cobalt.wire.linked.call.CallLog;
import com.github.auties00.cobalt.wire.linked.call.CallLogBuilder;
import com.github.auties00.cobalt.wire.linked.call.CallLogParticipantInfoBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.call.CallLogAction;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppAccountStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppChatStore;
import com.github.auties00.cobalt.sync.factory.CallLogMutationFactory;
import com.github.auties00.cobalt.sync.WebAppStateService;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Produces the durable call history record at the end of a call.
 *
 * <p>This is the calls outbound half of the call log seam; the inbound half is
 * {@link com.github.auties00.cobalt.sync.handler.CallLogHandler} and
 * {@link com.github.auties00.cobalt.sync.handler.DeleteIndividualCallLogHandler}. When the engine reaches
 * its ending transition, the lifecycle controller hands the finished {@link CallContext} and the terminal
 * {@link CallEndReason} to {@link #recordEndOfCall(CallContext, CallEndReason)}, which builds one
 * {@link CallLog} from the context, mirrors it into the runtime call history table through
 * {@link LinkedWhatsAppChatStore#addCallLog(CallLog)}, and queues an outbound {@code call_log} mutation
 * through {@link CallLogMutationFactory} so every linked device's call tab updates.
 *
 * <p>Seeding the call tab on a fresh link is not this collaborator's job: the primary ships the existing
 * call history inside the history sync blob, and
 * {@link com.github.auties00.cobalt.sync.LiveWebHistorySyncService} ingests those call records into the
 * same runtime table as part of its chunk dispatch, so this collaborator owns only the end of call output
 * and never double ingests the history sync records.
 *
 * <p>The call log output is kept distinct from the WAM telemetry the call service drains from the call's
 * stats at the same moment: the two are independent end of call outputs and are never conflated. The live
 * call, session, transport, and key state is never touched here; only the terminal {@link CallLog} crosses
 * into the store.
 *
 * @apiNote This is an internal engine collaborator the call service ({@code LiveCallsService}) drives;
 * embedders never call it directly. Embedders observe call history through
 * {@link LinkedWhatsAppChatStore#callLogStates()} and call end through the
 * {@code LinkedCallEndedListener} the event bus fans out.
 * @implNote The runtime call history table {@link LinkedWhatsAppChatStore#callLogStates()} is runtime only
 * (it has no persisted protobuf field) and is rebuilt deterministically on every startup from the
 * history sync call records {@link com.github.auties00.cobalt.sync.LiveWebHistorySyncService} ingests plus
 * the {@code call_log} app state collection, so it stays consistent across restarts without a live on disk
 * call snapshot.
 */
// TODO: resume an interrupted in progress call across a client restart. WhatsApp snapshots the live call
//  to disk while it is running and reloads it after a crash; Cobalt persists nothing live, so an
//  interrupted call is derived again as ended/missed and its CallLog is written on the next clean teardown.
//  Implementing resume needs a product decision plus a new runtime snapshot of the live
//  signal/session/transport/key state that none of the calls store currently retains; this is a
//  data loss feature gap.
public final class CallLogSync {
    /**
     * The logger for {@link CallLogSync}.
     */
    private static final System.Logger LOGGER = Log.get(CallLogSync.class);

    /**
     * The number of milliseconds in one second, used to convert the engine's millisecond duration
     * accumulators into the call log's whole second {@link CallLog#duration()} field.
     */
    private static final long MILLIS_PER_SECOND = 1_000L;

    /**
     * The client whose store the call history record is mirrored into and whose own JID resolves the
     * caller fallback.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * The factory that builds the outbound {@code call_log} app state mutation.
     */
    private final CallLogMutationFactory mutationFactory;

    /**
     * The app state service the outbound {@code call_log} mutation is pushed through.
     */
    private final WebAppStateService webAppStateService;

    /**
     * Builds a call log sync bound to one client.
     *
     * <p>Constructed once by the call service during client startup; all collaborators are required and
     * validated up front so that a misconfigured service fails fast.
     *
     * @param whatsapp           the {@link LinkedWhatsAppClient} that owns this collaborator
     * @param mutationFactory    the factory that builds the outbound {@code call_log} mutation
     * @param webAppStateService the app state service the mutation is pushed through
     * @throws NullPointerException if any argument is {@code null}
     */
    public CallLogSync(LinkedWhatsAppClient whatsapp, CallLogMutationFactory mutationFactory,
                             WebAppStateService webAppStateService) {
        this.whatsapp = Objects.requireNonNull(whatsapp, "whatsapp cannot be null");
        this.mutationFactory = Objects.requireNonNull(mutationFactory, "mutationFactory cannot be null");
        this.webAppStateService = Objects.requireNonNull(webAppStateService, "webAppStateService cannot be null");
    }

    /**
     * Records the call history entry for a call that has ended and replicates it to every linked device.
     *
     * <p>Builds one {@link CallLog} from the finished context and the terminal reason through
     * {@link #buildCallLog(CallContext, CallEndReason)}, mirrors it into the runtime call history
     * table through {@link LinkedWhatsAppChatStore#addCallLog(CallLog)}, resolves the
     * caller JID that keys the cross device index through
     * {@link #resolveCallerJid(CallContext)}, and pushes a {@code call_log} SET mutation built by
     * {@link CallLogMutationFactory} on the {@link CallLogAction#COLLECTION_NAME} collection. The outbound
     * push is best effort: a failure is logged and swallowed so it never breaks the call teardown, exactly
     * as the WAM commit on the same path does. Call this once, at the engine's ending transition, after
     * the durations have been closed out on the context.
     *
     * @implNote This implementation runs the build and the local mirror unconditionally, then guards only
     * the outbound {@link WebAppStateService#pushPatches(com.github.auties00.cobalt.wire.linked.sync.SyncPatchType, java.util.SequencedCollection)}
     * call, because the local table must reflect the just ended call even when the device is momentarily
     * unable to push (the {@code call_log} collection reruns the round once the socket recovers). The
     * caller JID resolution follows the native index key order: the call creator first, then the local
     * device's own JID when the call was outgoing, then the peer.
     *
     * @param context the finished call context, with its durations already accumulated
     * @param reason  the terminal end reason that produced the teardown
     * @throws NullPointerException if {@code context} or {@code reason} is {@code null}
     */
    public void recordEndOfCall(CallContext context, CallEndReason reason) {
        Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");
        var log = buildCallLog(context, reason);
        whatsapp.store().chatStore().addCallLog(log);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "recorded call log for call {0}, result {1}, duration {2}s",
                    context.callId(), log.callResult().orElse(null), log.duration().orElse(0L));
        }
        try {
            var callerJid = resolveCallerJid(context);
            var fromMe = context.outgoing();
            var callId = context.callId();
            var mutation = mutationFactory.getCallLogMutation(Instant.now(), callerJid, callId, fromMe, log);
            webAppStateService.pushPatches(CallLogAction.COLLECTION_NAME, List.of(mutation));
        } catch (RuntimeException exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "failed to push call-log mutation for call " + context.callId(),
                        exception);
            }
        }
    }

    /**
     * Builds the call history record for a finished call.
     *
     * <p>Projects the context's identity and topology onto a {@link CallLog}: the
     * {@linkplain CallContext#callId() call id}, the LID based
     * {@linkplain CallContext#creator() creator} JID, the group JID for a group call, the
     * incoming flag (the negation of {@linkplain CallContext#outgoing() outgoing}), the video flag,
     * the connected duration in whole seconds, and the per participant outcomes for a group call. The
     * {@link CallLog.Result} is projected from the terminal {@link CallEndReason} and whether the call
     * connected through {@link #resolveResult(CallContext, CallEndReason)}; the start time falls back
     * to the teardown instant because the finished context carries no absolute offer or placement time.
     *
     * @implNote This implementation reads the connected duration as the sum of the context's active and
     * connected lonely accumulators converted to whole seconds, matching the native call log duration that
     * counts both the peer connected and the connected but alone segments. The call type is left at the
     * model default and the call link and scheduled call fields are left unset because the end of call path
     * produces a regular call record; the link and scheduled variants are written by their own control
     * flows.
     *
     * @param context the finished call context
     * @param reason  the terminal end reason
     * @return the built call history record, never {@code null}
     */
    private CallLog buildCallLog(CallContext context, CallEndReason reason) {
        var result = resolveResult(context, reason);
        var durationSeconds = (context.activeDurationMillis() + context.lonelyDurationMillis()) / MILLIS_PER_SECOND;
        // WhatsApp records the call start time, not the end time; use the placed/accepted instant the
        // lifecycle controller stamped on the context, falling back to now (the teardown instant) only for a
        // call that never reached the placing or accepted state.
        var builder = new CallLogBuilder()
                .callId(context.callId())
                .callResult(result)
                .isIncoming(!context.outgoing())
                .isVideo(context.video())
                .duration(durationSeconds)
                .startTime(context.startedAt().orElse(Instant.now()))
                .callCreatorJid(context.creator());
        if (context.group()) {
            builder.groupJid(context.chatJid());
            builder.participants(buildParticipants(context, result));
        }
        return builder.build();
    }

    /**
     * Builds the per participant outcome list for a group call.
     *
     * <p>Maps every current membership identity to a {@link CallLog.ParticipantInfo} carrying the
     * member's {@linkplain com.github.auties00.cobalt.calls.engine.participant.CallParticipantUserNode#jid()
     * user JID} and the call level {@code result}. The live membership set is the surviving roster at
     * teardown; the call level result is applied to each entry because the native per participant
     * result derivation cannot be reconstructed faithfully from the state Cobalt retains at teardown.
     *
     * @param context the finished call context
     * @param result  the call level result applied to each participant
     * @return the per participant outcome list, possibly empty, never {@code null}
     */
    // TODO: write a divergent per participant CallLog.Result per member instead of stamping every member
    //  with the call level result. The model already supports it (CallLog.ParticipantInfo.callResult is
    //  per participant). The blocker is the per participant state -> Result mapping: WhatsApp derives each
    //  member's Result from its runtime participant state, but that state is engine only and is not
    //  retained past teardown (only the final ParticipantInfo is kept). The one per participant signal
    //  Cobalt does retain is the server projected CallParticipantUserNode.state() literal (e.g.
    //  "connected"/"rejected" from the <user state=...> attribute), and "connected" maps firmly to
    //  CallLog.Result.CONNECTED, but the exact Result for the other states
    //  (invited/timedout/terminated/...) is not recoverable, so projecting only "connected" would still
    //  wrongly stamp non connected members with the call level result. Blocked on recovering the full
    //  per state -> Result table; until then every member carries the call level result.
    private static List<CallLog.ParticipantInfo> buildParticipants(CallContext context, CallLog.Result result) {
        var identities = context.membership().identities();
        var participants = new ArrayList<CallLog.ParticipantInfo>(identities.size());
        for (var identity : identities) {
            participants.add(new CallLogParticipantInfoBuilder()
                    .userJid(identity.jid().toUserJid())
                    .callResult(result)
                    .build());
        }
        return participants;
    }

    /**
     * Resolves the call history result from the terminal reason and whether the call connected.
     *
     * <p>A call that brought up media ({@linkplain CallContext#mediaStarted() media started}) is
     * recorded as {@link CallLog.Result#CONNECTED} unless it was explicitly handed off elsewhere
     * ({@link CallEndReason#ACCEPTED_ELSEWHERE} to {@link CallLog.Result#ACCEPTEDELSEWHERE}). A call that
     * never connected is classified by reason: a rejection (whether local, remote, or on another device)
     * is {@link CallLog.Result#REJECTED}, a remote accept on another device is
     * {@link CallLog.Result#ACCEPTEDELSEWHERE}, a setup, media, or relay failure is
     * {@link CallLog.Result#FAILED}, a timeout is {@link CallLog.Result#MISSED} for an incoming call and
     * {@link CallLog.Result#CANCELLED} for an outgoing one, and anything else (a plain hangup before
     * connecting) is {@link CallLog.Result#CANCELLED} for an outgoing call and {@link CallLog.Result#MISSED}
     * for an incoming one.
     *
     * @implNote This implementation projects the public {@link CallEndReason} onto {@link CallLog.Result}
     * at the API boundary rather than carrying the engine's richer {@link CallResult} into the store,
     * keeping the store model on its stable public axis. The connected but lonely case (the caller
     * connected but no peer ever joined) is folded into {@link CallLog.Result#CONNECTED} here, since the
     * model has no dedicated lonely connected result and the call did establish a media session locally.
     *
     * @param context the finished call context
     * @param reason  the terminal end reason
     * @return the resolved call history result, never {@code null}
     */
    private static CallLog.Result resolveResult(CallContext context, CallEndReason reason) {
        if (reason == CallEndReason.ACCEPTED_ELSEWHERE) {
            return CallLog.Result.ACCEPTEDELSEWHERE;
        }
        if (context.mediaStarted()) {
            return CallLog.Result.CONNECTED;
        }
        return switch (reason) {
            case REJECTED, REJECT_DO_NOT_DISTURB, REJECT_BLOCKED, REJECTED_ELSEWHERE -> CallLog.Result.REJECTED;
            case SETUP_FAILED, MEDIA_TX_TIMEOUT, MEDIA_RX_TIMEOUT, RELAY_BIND_FAILED, MIC_PERMISSION_DENIED,
                 CAMERA_PERMISSION_DENIED -> CallLog.Result.FAILED;
            case TIMEOUT -> context.outgoing() ? CallLog.Result.CANCELLED : CallLog.Result.MISSED;
            default -> context.outgoing() ? CallLog.Result.CANCELLED : CallLog.Result.MISSED;
        };
    }

    /**
     * Resolves the caller JID that keys the cross device {@code call_log} index.
     *
     * <p>Follows the native index key order: the {@linkplain CallContext#creator() call creator}
     * first; failing that the local device's own JID from
     * {@link LinkedWhatsAppAccountStore#jid()} when the call was
     * {@linkplain CallContext#outgoing() outgoing}; failing that the
     * {@linkplain CallContext#peer() peer}. Every candidate is normalized to its user JID so the
     * index segment is device stripped and stable across devices.
     *
     * @implNote This implementation moves the Me user lookup store side (the
     * {@link LinkedWhatsAppAccountStore}) here rather than into
     * {@link CallLogMutationFactory}, matching the factory's contract that the caller resolves in advance the
     * index JID. The creator is normally already present on the context, so the self and peer fallbacks
     * only fire for an outgoing call whose creator was not stamped.
     *
     * @param context the finished call context
     * @return the resolved caller JID, never {@code null}
     */
    private Jid resolveCallerJid(CallContext context) {
        var creator = context.creator();
        if (creator != null) {
            return creator.toUserJid();
        }
        if (context.outgoing()) {
            var self = whatsapp.store().accountStore().jid().orElse(null);
            if (self != null) {
                return self.toUserJid();
            }
        }
        return context.peer().toUserJid();
    }
}
