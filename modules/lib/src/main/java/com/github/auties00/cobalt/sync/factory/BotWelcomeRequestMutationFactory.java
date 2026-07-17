package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.BotWelcomeRequestAction;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.BotWelcomeRequestActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.HatchUserJourneyEvent;
import com.github.auties00.cobalt.wire.wam.event.HatchUserJourneyEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.BotEntryPointType;
import com.github.auties00.cobalt.wire.wam.type.HatchActionType;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Builds outgoing app-state mutations that record whether a bot welcome message has been requested for a chat.
 *
 * <p>The flag is set to {@code true} when the user opens a bot chat for the
 * first time and cleared back to {@code false} after the bot replies, keeping
 * the per-bot-chat welcome affordance consistent across linked devices. This
 * factory builds the outgoing mutation; the inbound counterpart is
 * {@link com.github.auties00.cobalt.sync.handler.BotWelcomeRequestHandler}.
 *
 * <p>When the welcome request is sent (not when the flag is cleared) the factory
 * also emits the {@link HatchUserJourneyEvent} telemetry beacon, mirroring
 * WhatsApp Web's {@code WAWebHatchLogging.logHatchRequestWelcomeMsgSent} which
 * fires from the same {@code bot_request_welcome} send path.
 */
public final class BotWelcomeRequestMutationFactory {
    /**
     * The logger for {@link BotWelcomeRequestMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(BotWelcomeRequestMutationFactory.class);

    /**
     * Telemetry sink used to emit the Hatch (Meta AI bot) user-journey beacon on a welcome-request send.
     */
    private final WamService wamService;

    /**
     * Creates a factory that emits Hatch user-journey telemetry through the given {@link WamService}.
     *
     * <p>A single instance may be shared across the lifetime of the client.
     *
     * @param wamService the telemetry service that receives the {@link HatchUserJourneyEvent} beacon
     */
    public BotWelcomeRequestMutationFactory(WamService wamService) {
        this.wamService = wamService;
    }

    /**
     * Returns a {@link SyncPendingMutation} that sets the welcome-requested flag for the given bot chat.
     *
     * <p>Emit this mutation right after sending the bot a welcome request (with
     * {@code isSent == true}) so other linked devices stop showing the welcome
     * affordance, and again after the bot replies (with {@code isSent == false})
     * so the affordance can be reused later. The mutation index follows
     * {@snippet :
     *     ["botWelcomeRequest", chatJid.toString()]
     * }
     * and the {@link BotWelcomeRequestAction} sub-message carries the
     * {@code isSent} flag.
     *
     * <p>As a side effect, when {@code isSent} is {@code true} this also emits the
     * {@link HatchUserJourneyEvent} telemetry beacon through {@link #emitHatchWelcomeRequestSent()};
     * the {@code isSent == false} clearing call emits nothing.
     *
     * @implNote
     * This implementation captures the timestamp via {@link Instant#now()} and
     * pins the version to {@link BotWelcomeRequestAction#ACTION_VERSION}.
     *
     * @param chatJid the bot chat {@link Jid}
     * @param isSent  {@code true} once the welcome has been requested, {@code false} after the bot replies
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebBotWelcomeRequestSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getBotWelcomeRequestSetMutation(Jid chatJid, boolean isSent) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building bot welcome request mutation chat={0} sent={1}", chatJid, isSent);
        var action = new BotWelcomeRequestActionBuilder()
                .isSent(isSent)
                .build();
        var timestamp = Instant.now();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .botWelcomeRequestAction(action)
                .build();
        var index = JSON.toJSONString(List.of(BotWelcomeRequestAction.ACTION_NAME, chatJid.toString()));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                BotWelcomeRequestAction.ACTION_VERSION
        );
        if (isSent) {
            emitHatchWelcomeRequestSent();
        }
        return new SyncPendingMutation(mutation, 0);
    }

    /**
     * Emits the {@link HatchUserJourneyEvent} beacon that records a welcome-request send to a Meta AI (Hatch) bot.
     *
     * <p>Fires with {@link HatchActionType#REQUEST_WELCOME_MSG_SENT} to mirror
     * WhatsApp Web's {@code logHatchRequestWelcomeMsgSent}, which commits the
     * same event immediately after a {@code bot_request_welcome} protocol message
     * is delivered. The event is only produced on the send path; the flag-clearing
     * mutation that follows the bot reply has no WhatsApp Web counterpart and emits
     * nothing.
     *
     * @implNote
     * WhatsApp Web sources {@code aiSessionId} and {@code unifiedSessionId} from an
     * active Meta AI journey context that Cobalt does not model, so this
     * implementation mints a fresh {@link UUID} for each to carry a well-formed,
     * unique session correlator, and reports {@link BotEntryPointType#WA_CHAT} as
     * the entry point because the welcome request originates from an open bot chat.
     */
    @WhatsAppWebExport(moduleName = "WAWebHatchLogging", exports = "logHatchRequestWelcomeMsgSent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitHatchWelcomeRequestSent() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "committing hatch welcome request sent journey event");
        var event = new HatchUserJourneyEventBuilder()
                .hatchActionType(HatchActionType.REQUEST_WELCOME_MSG_SENT)
                .botEntryPoint(BotEntryPointType.WA_CHAT)
                .aiSessionId(UUID.randomUUID().toString())
                .unifiedSessionId(UUID.randomUUID().toString())
                .build();
        wamService.commit(event);
    }
}
