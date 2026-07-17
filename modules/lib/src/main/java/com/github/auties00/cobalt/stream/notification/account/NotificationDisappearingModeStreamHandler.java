package com.github.auties00.cobalt.stream.notification.account;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.ack.AckClass;
import com.github.auties00.cobalt.ack.AckSender;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.ChatEphemeralTimer;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.EphemeralOutOfSyncInfoEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.EphemeralSyncResponseReceiveEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.EphemeralSyncResponseSendEventBuilder;
import com.github.auties00.cobalt.wam.threadlogging.LiveThreadLoggingService;
import com.github.auties00.cobalt.wire.wam.type.DisappearingChatInitiatorType;
import com.github.auties00.cobalt.wire.wam.type.EphemeralityInitiatorType;
import com.github.auties00.cobalt.wire.wam.type.EphemeralityTriggerActionType;
import com.github.auties00.cobalt.wire.wam.type.EsrFailureReasonType;
import com.github.auties00.cobalt.wire.wam.type.EsrSendResultType;

import java.lang.System.Logger.Level;
import java.time.Instant;

/**
 * Handles {@code type="disappearing_mode"} notifications carrying a peer-initiated change to a
 * one-to-one chat's ephemeral-message timer.
 *
 * <p>Each notification targets the chat identified by its {@code from} attribute and updates that chat's
 * ephemeral expiration plus its per-chat ephemeral setting timestamp. Stanzas whose target chat is not
 * in the local store are ignored; absent chats are not auto-created here.</p>
 *
 * @implNote This implementation operates on chat-level ephemeral fields, whereas WA Web persists the
 * same value on a contact record because it models disappearing mode at the contact level. The
 * semantics converge: each chat has at most one timer at a time and the timestamp guard prevents a late
 * stanza from clobbering a fresher value.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleDisappearingModeNotification")
final class NotificationDisappearingModeStreamHandler extends SocketStreamHandler.Concurrent {
    /**
     * Logs diagnostics for this handler.
     */
    private static final System.Logger LOGGER = Log.get(NotificationDisappearingModeStreamHandler.class);

    /**
     * Holds the client used for store reads and chat mutations.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Holds the telemetry service used to commit the ephemeral-sync-response and out-of-sync WAM events
     * emitted while resolving a peer-initiated disappearing-mode change.
     */
    private final WamService wamService;

    /**
     * Holds the ack sender used to ship the post-processing
     * {@code <ack class="notification" type="disappearing_mode"/>} stanza.
     */
    private final AckSender ackSender;

    /**
     * Constructs the handler with the shared client, telemetry service, and ack sender.
     *
     * @param whatsapp   the non-{@code null} client
     * @param wamService the non-{@code null} telemetry service used to commit ephemeral-sync WAM events
     * @param ackSender  the non-{@code null} ack sender
     */
    NotificationDisappearingModeStreamHandler(LinkedWhatsAppClient whatsapp, WamService wamService, AckSender ackSender) {
        this.whatsapp = whatsapp;
        this.wamService = wamService;
        this.ackSender = ackSender;
    }

    /**
     * Applies the new ephemeral timer to the addressed chat (when known) and sends the protocol-level
     * ACK.
     *
     * <p>Delegates to {@link #handleDisappearingModeNotification(Stanza)} for the mutation, then always
     * sends the ACK regardless of whether the chat existed.</p>
     *
     * @implNote This implementation always sends the ACK regardless of whether the chat existed, matching
     * WA Web which returns the ack from the parser's promise resolution path.
     *
     * @param stanza the non-{@code null} {@code <notification>} stanza
     */
    @Override
    public void handle(Stanza stanza) {
        handleDisappearingModeNotification(stanza);
        sendNotificationAck(stanza);
    }

    /**
     * Parses the duration and setting timestamp from the {@code <disappearing_mode>} child, looks up the
     * addressed chat, and writes the new timer only when the incoming timestamp is strictly fresher than
     * the stored one.
     *
     * <p>The timestamp guard makes the mutation idempotent against stanza replays: the write happens only
     * when the stored timestamp is absent and the incoming timestamp is non-zero, or the stored timestamp
     * strictly precedes the incoming timestamp. A stanza older than or equal to the stored value is
     * ignored.</p>
     *
     * <p>The notification is the wire-level equivalent of an inbound ephemeral-sync-response (ESR) peer
     * message, so resolving it drives the same telemetry WA Web emits from its ESR pipeline. When the
     * incoming setting wins the timestamp comparison it is applied and an
     * {@link EphemeralSyncResponseReceiveEventBuilder ephemeral-sync-response-receive} success beacon is
     * committed; an {@link EphemeralOutOfSyncInfoEventBuilder out-of-sync-info} beacon is additionally
     * committed when the incoming duration disagrees with the chat's current timer. When the local record
     * is fresher, the incoming setting is rejected as stale, a receive beacon carrying
     * {@link EsrFailureReasonType#OLDER_EPHEMERAL_SETTING_TIMESTAMP} is committed, and a reconciling
     * {@link EphemeralSyncResponseSendEventBuilder ephemeral-sync-response-send} beacon records the newer
     * local setting that a full ESR exchange would push back to the peer.</p>
     *
     * @implNote This implementation applies the same timestamp guard WA Web applies, but at the
     * chat-record level rather than the contact-record level. WA Web gates the ESR beacons behind the
     * {@code dm_reliability_logging} AB prop; this implementation commits them unconditionally because the
     * wiring exists to surface the telemetry rather than to replicate the server-pushed logging throttle.
     * The disappearing-mode notification carries only a duration and a timestamp, so the per-party
     * initiator and trigger-action enums are stamped with the fixed values that a one-to-one, chat-scoped,
     * peer-initiated change implies.
     *
     * @param stanza the {@code <notification>} stanza
     */
    private void handleDisappearingModeNotification(Stanza stanza) {
        var from = stanza.getAttributeAsJid("from")
                .map(jid -> jid.toUserJid())
                .orElse(null);
        var disappearingMode = stanza.getChild("disappearing_mode").orElse(null);
        if (from == null || disappearingMode == null) {
            return;
        }

        var duration = disappearingMode.getAttributeAsInt("duration", 0);
        var rawTimestamp = disappearingMode.getAttributeAsLong("t", (Long) null);
        var settingTimestamp = rawTimestamp != null ? Instant.ofEpochSecond(rawTimestamp) : null;

        var chat = whatsapp.store().chatStore().findChatByJid(from)
                .orElse(null);
        if (chat == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "dropping disappearing_mode notification for unknown chat {0}", from);
            return;
        }

        var existingTimestamp = chat.ephemeralSettingTimestamp().orElse(null);
        int localDurationSeconds = chat.ephemeralExpiration()
                .map(ChatEphemeralTimer::periodSeconds)
                .orElse(0);
        var newTimestampSeconds = rawTimestamp != null ? rawTimestamp : 0L;
        var apply = existingTimestamp == null && newTimestampSeconds != 0
                || existingTimestamp != null && settingTimestamp != null && existingTimestamp.isBefore(settingTimestamp);
        if (apply) {
            if (localDurationSeconds != duration) {
                emitEphemeralOutOfSyncInfo(from, duration, localDurationSeconds, existingTimestamp == null);
            }
            chat.setEphemeralExpiration(ChatEphemeralTimer.of(duration));
            chat.setEphemeralSettingTimestamp(settingTimestamp);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                    "applied disappearing_mode duration {0}s -> {1}s for {2}", localDurationSeconds, duration, from);
            emitEphemeralSyncResponseReceive(from, duration, newTimestampSeconds,
                    existingTimestamp != null ? localDurationSeconds : null, existingTimestamp, null);
        } else if (existingTimestamp != null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                    "rejected stale disappearing_mode notification for {0}, local setting is newer", from);
            emitEphemeralSyncResponseReceive(from, duration, newTimestampSeconds,
                    localDurationSeconds, existingTimestamp, EsrFailureReasonType.OLDER_EPHEMERAL_SETTING_TIMESTAMP);
            emitEphemeralSyncResponseSend(from, duration, newTimestampSeconds, localDurationSeconds, existingTimestamp);
        }
    }

    /**
     * Sends the {@code <ack class="notification" type="disappearing_mode"/>} stanza for the processed
     * notification.
     *
     * <p>The ack is fire-and-forget.</p>
     *
     * @param stanza the original {@code <notification>} stanza
     */
    private void sendNotificationAck(Stanza stanza) {
        ackSender.ack(AckClass.NOTIFICATION, stanza).type("disappearing_mode").send();
    }

    /**
     * Commits the {@link EphemeralSyncResponseReceiveEventBuilder ephemeral-sync-response-receive} beacon
     * for a resolved inbound disappearing-mode change.
     *
     * <p>The incoming notification supplies the {@code esr}-prefixed fields (the peer's setting), while the
     * chat's stored timer supplies the {@code client}-prefixed fields; the latter are set only when a local
     * setting existed at resolution time. A non-{@code null} {@code failureReason} marks the resolution as
     * an {@link EsrSendResultType#ERROR}, otherwise the resolution is recorded as
     * {@link EsrSendResultType#SUCCESS}.</p>
     *
     * @implNote This implementation stamps the per-party initiator and trigger-action enums with the fixed
     * values a one-to-one, chat-scoped change implies: the peer's setting is
     * {@link DisappearingChatInitiatorType#INITIATED_BY_OTHER} and the local setting is
     * {@link DisappearingChatInitiatorType#CHAT}, both triggered by
     * {@link EphemeralityTriggerActionType#CHAT_SETTINGS}. The notification carries no such metadata, so
     * these cannot be recovered from the wire.
     *
     * @param chatJid                       the resolved one-to-one chat JID
     * @param incomingDurationSeconds       the peer's ephemeral timer in seconds
     * @param incomingSettingTimestampSeconds the peer's setting timestamp in epoch seconds
     * @param localDurationSeconds          the stored local timer in seconds, or {@code null} when no local setting existed
     * @param localSettingTimestamp         the stored local setting timestamp, or {@code null} when absent
     * @param failureReason                 the failure reason when the incoming setting was rejected as stale, or {@code null} on success
     */
    @WhatsAppWebExport(moduleName = "WAWebEphemeralSyncResponseWAM", exports = "sendEphemeralSyncResponseReceiveWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitEphemeralSyncResponseReceive(Jid chatJid, int incomingDurationSeconds, long incomingSettingTimestampSeconds,
                                                  Integer localDurationSeconds, Instant localSettingTimestamp, EsrFailureReasonType failureReason) {
        var builder = new EphemeralSyncResponseReceiveEventBuilder()
                .threadId(threadId(chatJid))
                .isAGroup(false)
                .esrDisappearingModeInitiator(DisappearingChatInitiatorType.INITIATED_BY_OTHER)
                .esrEphemeralityInitiator(EphemeralityInitiatorType.INITIATED_BY_OTHER)
                .esrEphemeralityTriggerAction(EphemeralityTriggerActionType.CHAT_SETTINGS)
                .esrEphemeralityDuration(incomingDurationSeconds)
                .esrEphemeralitySettingTimestamp(incomingSettingTimestampSeconds);
        if (localDurationSeconds != null) {
            builder.clientDisappearingModeInitiator(DisappearingChatInitiatorType.CHAT)
                    .clientEphemeralityInitiator(EphemeralityInitiatorType.INITIATED_BY_ME)
                    .clientEphemeralityTriggerAction(EphemeralityTriggerActionType.CHAT_SETTINGS)
                    .clientEphemeralityDuration(localDurationSeconds)
                    .clientEphemeralitySettingTimestamp(localSettingTimestamp == null ? 0L : localSettingTimestamp.getEpochSecond());
        }
        if (failureReason != null) {
            builder.esrResolveResult(EsrSendResultType.ERROR)
                    .esrFailureReason(failureReason);
        } else {
            builder.esrResolveResult(EsrSendResultType.SUCCESS);
        }
        wamService.commit(builder.build());
    }

    /**
     * Commits the {@link EphemeralSyncResponseSendEventBuilder ephemeral-sync-response-send} beacon for the
     * reconciling response a full ESR exchange would push to the peer when the inbound setting is stale.
     *
     * <p>The {@code esr}-prefixed and {@code client}-prefixed fields carry the newer local timer being
     * pushed back, while the {@code message}-prefixed fields carry the stale incoming setting that
     * triggered the reconcile; the result is recorded as {@link EsrSendResultType#SUCCESS} on the first
     * attempt.</p>
     *
     * @implNote This implementation records a single send attempt because the notification handler resolves
     * one stanza at a time and does not retry with back-off the way WA Web's ESR sender does.
     *
     * @param chatJid                         the resolved one-to-one chat JID
     * @param incomingDurationSeconds         the stale peer timer in seconds
     * @param incomingSettingTimestampSeconds the stale peer setting timestamp in epoch seconds
     * @param localDurationSeconds            the newer local timer in seconds being reconciled outward
     * @param localSettingTimestamp           the newer local setting timestamp, or {@code null} when absent
     */
    @WhatsAppWebExport(moduleName = "WAWebEphemeralSyncResponseWAM", exports = "sendEphemeralSyncResponseSendWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitEphemeralSyncResponseSend(Jid chatJid, int incomingDurationSeconds, long incomingSettingTimestampSeconds,
                                               int localDurationSeconds, Instant localSettingTimestamp) {
        var localTimestampSeconds = localSettingTimestamp == null ? 0L : localSettingTimestamp.getEpochSecond();
        wamService.commit(new EphemeralSyncResponseSendEventBuilder()
                .threadId(threadId(chatJid))
                .isAGroup(false)
                .esrSendResult(EsrSendResultType.SUCCESS)
                .esrSendAttempt(1)
                .esrDisappearingModeInitiator(DisappearingChatInitiatorType.CHAT)
                .esrEphemeralityInitiator(EphemeralityInitiatorType.INITIATED_BY_ME)
                .esrEphemeralityTriggerAction(EphemeralityTriggerActionType.CHAT_SETTINGS)
                .esrEphemeralityDuration(localDurationSeconds)
                .esrEphemeralitySettingTimestamp(localTimestampSeconds)
                .clientDisappearingModeInitiator(DisappearingChatInitiatorType.CHAT)
                .clientEphemeralityInitiator(EphemeralityInitiatorType.INITIATED_BY_ME)
                .clientEphemeralityTriggerAction(EphemeralityTriggerActionType.CHAT_SETTINGS)
                .clientEphemeralityDuration(localDurationSeconds)
                .clientEphemeralitySettingTimestamp(localTimestampSeconds)
                .messageDisappearingModeInitiator(DisappearingChatInitiatorType.INITIATED_BY_OTHER)
                .messageEphemeralityInitiator(EphemeralityInitiatorType.INITIATED_BY_OTHER)
                .messageEphemeralityTriggerAction(EphemeralityTriggerActionType.CHAT_SETTINGS)
                .messageEphemeralityDuration(incomingDurationSeconds)
                .messageEphemeralitySettingTimestamp(incomingSettingTimestampSeconds)
                .build());
    }

    /**
     * Commits the {@link EphemeralOutOfSyncInfoEventBuilder ephemeral-out-of-sync-info} beacon when the
     * incoming timer disagrees with the chat's current timer.
     *
     * <p>The incoming duration, the thread's current duration, and the account-wide default disappearing
     * timer are recorded so the collector can distinguish a genuine mismatch from a first-time setting. The
     * peer's account default is not carried on the notification and is recorded as {@code 0} (off), the
     * common real-world default; the group-size bucket is omitted because the addressed chat is always a
     * one-to-one thread.</p>
     *
     * @implNote This implementation reads the account default from
     * {@code store().settingsStore().disappearingMode()} rather than a per-thread setting, matching the
     * {@code userDefaultModeDuration} the WA Web out-of-sync collector reports.
     *
     * @param chatJid                 the resolved one-to-one chat JID
     * @param incomingDurationSeconds the incoming ephemeral timer in seconds
     * @param threadDurationSeconds   the chat's current ephemeral timer in seconds
     * @param newThread               whether the chat had no prior ephemeral setting at resolution time
     */
    private void emitEphemeralOutOfSyncInfo(Jid chatJid, int incomingDurationSeconds, int threadDurationSeconds, boolean newThread) {
        var userDefaultDuration = whatsapp.store().settingsStore().disappearingMode()
                .map(mode -> mode.duration().toSeconds())
                .orElse(0L);
        wamService.commit(new EphemeralOutOfSyncInfoEventBuilder()
                .threadId(threadId(chatJid))
                .isAGroup(false)
                .isNewThreadForUser(newThread)
                .incomingMessageEphemeralityDuration(incomingDurationSeconds)
                .threadEphemeralityDuration(threadDurationSeconds)
                .userDefaultModeDuration(userDefaultDuration)
                .otherDefaultModeDuration(0)
                .build());
    }

    /**
     * Derives the WAM thread id for a chat JID, matching WA Web's {@code getChatThreadID} HMAC.
     *
     * <p>Returns {@code null} rather than an empty string when no chat-thread-logging secret has been
     * provisioned, so callers leave the {@code threadId} field unset instead of stamping a meaningless
     * value.</p>
     *
     * @param chatJid the chat JID to derive the thread id for
     * @return the Base64-encoded HMAC thread id, or {@code null} when no secret has been provisioned
     */
    private String threadId(Jid chatJid) {
        var id = LiveThreadLoggingService.chatThreadIdHmac(whatsapp, chatJid.toString());
        return id.isEmpty() ? null : id;
    }
}
