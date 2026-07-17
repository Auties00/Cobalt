package com.github.auties00.cobalt.stream.receipt;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.ack.AckClass;
import com.github.auties00.cobalt.ack.AckSender;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.listener.MessageStatusListener;
import com.github.auties00.cobalt.message.MessageService;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.device.DeviceConstants;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.linked.message.MessageReceipt;
import com.github.auties00.cobalt.wire.linked.message.MessageReceiptBuilder;
import com.github.auties00.cobalt.wire.core.message.MessageStatus;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo;
import com.github.auties00.cobalt.stream.NodeStreamService;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.E2eRetryRejectEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MdRetryFromUnknownDeviceEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ReceiptStanzaReceiveEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.DeviceType;
import com.github.auties00.cobalt.wire.wam.type.E2eDeviceType;
import com.github.auties00.cobalt.wire.wam.type.EncryptionTypeCode;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.ReceiptStanzaStage;
import com.github.auties00.cobalt.wire.wam.type.RetryRejectReason;
import com.github.auties00.cobalt.wire.wam.type.SessionScopeType;
import com.github.auties00.libsignal.SignalSessionCipher;
import com.github.auties00.libsignal.key.SignalIdentityPublicKey;
import com.github.auties00.libsignal.state.SignalPreKeyBundleBuilder;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Consumes the {@code <receipt>} stanzas that carry delivery, read, played
 * and retry acknowledgements, then mirrors them into the local store.
 *
 * <p>This handler drives the per-message read/delivery status fanned out via
 * {@link LinkedWhatsAppClientListener#onMessageStatus}
 * and, for retry receipts, transparently re-encrypts and re-ships the
 * original outbound message. {@link ReceiptStreamHandler} forwards every
 * non-call receipt here regardless of whether it is a retry or a regular
 * acknowledgement; the secondary split is performed inline by
 * {@link #isRetryReceipt(Stanza)} in {@link #handle(Stanza)}.
 *
 * @implNote
 * This implementation merges three WA Web pipelines into a single Java
 * class.
 * <ul>
 * <li>{@code WAWebHandleMsgReceipt} and {@code WAWebHandleMsgReceiptParser}
 * for the parse + per-stanza-class dispatch of regular delivery / read /
 * played acknowledgements.</li>
 * <li>{@code WAWebHandleMessageRetryRequest},
 * {@code WAWebHandleRetryRequest} and {@code WAWebRetryRequestParser} for
 * the retry-receipt flow that drives Signal session rebuild and message
 * re-send.</li>
 * <li>The {@code createReceiptStanzaReceiveMetric} closure from
 * {@code WAWebCreateReceiptStanzaReceiveMetric} for the
 * {@link com.github.auties00.cobalt.wire.wam.event.ReceiptStanzaReceiveEvent}
 * telemetry that wraps the regular-receipt path.</li>
 * </ul>
 * WA Web routes simple receipts through a per-surface fan-out
 * ({@code handleChatSimpleReceipt}, {@code handleGroupSimpleReceipt},
 * {@code handleStatusSimpleReceipt}, {@code handleNewsletterSimpleReceipt},
 * {@code handleBotOneToOneInvokeSimpleReadReceipt}, ...) keyed on the
 * sender JID class; Cobalt collapses all surfaces into the
 * {@link #updateMessage} path because the store is uniform across chat
 * surfaces and the per-surface side effects (read-receipts-disabled
 * downgrade in {@code WAWebHandleDirectChatReceipt}, view-once unlock in
 * {@code handleViewOnceOpenedIfNecessary}, peer-message deletion in
 * {@code handleAckPeerSimpleReceipt}) are either not modelled or live in
 * a different package.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleMsgReceipt")
@WhatsAppWebModule(moduleName = "WAWebHandleMsgReceiptParser")
@WhatsAppWebModule(moduleName = "WAWebHandleMessageRetryRequest")
@WhatsAppWebModule(moduleName = "WAWebHandleRetryRequest")
@WhatsAppWebModule(moduleName = "WAWebRetryRequestParser")
public final class MessageReceiptStreamHandler extends SocketStreamHandler.Concurrent {
    /**
     * The logger for {@link MessageReceiptStreamHandler}.
     */
    private static final System.Logger LOGGER = Log.get(MessageReceiptStreamHandler.class);

    /**
     * The hard ceiling on the {@code count} attribute of a retry request,
     * above which the request is refused outright.
     *
     * @implNote
     * This implementation matches WA Web's
     * {@code WAWebPostMessageHighRetryCountMetric.MAX_RETRY}, which is the
     * same {@code 5} value used by the server to cap retry escalation
     * before it stops sending further retry stanzas.
     */
    private static final int MAX_RETRY = 5;

    /**
     * The {@link LinkedWhatsAppClient} that owns the store this handler mutates
     * and that ships outgoing {@code <ack>} stanzas via
     * {@link LinkedWhatsAppClient#sendNodeWithNoResponse(Stanza)}.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * The {@link MessageService} used to re-send a message in response to
     * a retry receipt.
     */
    private final MessageService messageService;

    /**
     * The {@link WamService} used to commit the
     * {@link com.github.auties00.cobalt.wire.wam.event.ReceiptStanzaReceiveEvent}
     * and {@link com.github.auties00.cobalt.wire.wam.event.MdRetryFromUnknownDeviceEvent}
     * telemetry events.
     */
    private final WamService wamService;

    /**
     * The {@link AckSender} used to ship the {@code <ack class="receipt">}
     * stanza for inbound receipts and the {@code <ack class="receipt"
     * type="retry">} stanza that closes the retry-receipt handshake.
     */
    private final AckSender ackSender;

    /**
     * Constructs a new {@link MessageReceiptStreamHandler} bound to the
     * given collaborators.
     *
     * @param whatsapp       the non-{@code null} client used to read the
     *                       store
     * @param messageService the non-{@code null} service used to re-send a
     *                       message in response to a retry receipt
     * @param wamService     the non-{@code null} telemetry service that
     *                       commits receipt-related WAM events
     * @param ackSender      the non-{@code null} ack sender used for the
     *                       outbound {@code <ack class="receipt">} stanza
     */
    public MessageReceiptStreamHandler(LinkedWhatsAppClient whatsapp, MessageService messageService, WamService wamService, AckSender ackSender) {
        this.whatsapp = whatsapp;
        this.messageService = messageService;
        this.wamService = wamService;
        this.ackSender = ackSender;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Performs the secondary retry-vs-regular split that
     * {@link ReceiptStreamHandler} defers to this class. Retry receipts
     * flow into {@link #handleRetryReceipt(Stanza)}; regular receipts are
     * parsed into a {@link ParsedReceipt} and routed by class to
     * {@link #handleSimple(SimpleReceipt)},
     * {@link #handleAggregatedByType(AggregatedByTypeReceipt)} or
     * {@link #handleAggregatedByMessage(AggregatedByMessageReceipt)}. The
     * {@link ReceiptAck#CONTENT_GONE} short-circuit (server-error response
     * to a re-upload request) only sends the {@code <ack>} and commits the
     * metric without invoking the per-class handler.
     *
     * @implNote
     * This implementation mirrors WA Web's
     * {@code WAWebHandleMsgReceipt} default export, which builds the
     * {@code ReceiptStanzaReceiveMetric} closure first, parses via
     * {@code msgReceiptParser.parse}, then dispatches by parsed
     * {@code type}. The telemetry commit is gated on the {@code offline}
     * attribute being absent, matching the WA Web guard
     * {@code u==null && t(a)} inside the {@code C(e)} arrow function.
     *
     * @param stanza {@inheritDoc}
     */
    @Override
    public void handle(Stanza stanza) {
        if (isRetryReceipt(stanza)) {
            handleRetryReceipt(stanza);
            return;
        }

        var receiptMetric = new ReceiptStanzaReceiveEventBuilder()
                .receiptStanzaStage(ReceiptStanzaStage.OVERALL)
                .receiptStanzaTotalCount(1)
                .startReceiptStanzaDuration();

        var parsed = parseReceipt(stanza);
        if (parsed == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "unparsable receipt stanza id={0}", stanza.getAttributeAsString("id", "<unknown>"));
            sendAck(stanza, null);
            return;
        }

        if (parsed instanceof SimpleReceipt simple && simple.ack() == ReceiptAck.CONTENT_GONE) {
            sendAck(stanza, parsed);
            commitReceiptMetric(receiptMetric, parsed);
            return;
        }

        try {
            switch (parsed) {
                case SimpleReceipt simpleReceipt -> handleSimple(simpleReceipt);
                case AggregatedByTypeReceipt aggregatedByTypeReceipt -> handleAggregatedByType(aggregatedByTypeReceipt);
                case AggregatedByMessageReceipt aggregatedByMessageReceipt -> handleAggregatedByMessage(aggregatedByMessageReceipt);
            }
        } catch (RuntimeException exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "failed to process incoming receipt id=" + stanza.getAttributeAsString("id", "<unknown>"),
                        exception);
            }
        }

        sendAck(stanza, parsed);
        commitReceiptMetric(receiptMetric, parsed);
    }

    /**
     * Finalizes and commits the
     * {@link com.github.auties00.cobalt.wire.wam.event.ReceiptStanzaReceiveEvent}
     * carried by {@code builder} using the fields surfaced by the parsed
     * receipt.
     *
     * <p>Runs once per non-offline receipt after {@link #handle(Stanza)} has
     * finished its per-class dispatch. Offline receipts skip the commit
     * entirely so the metric does not double-count replays from the
     * offline queue.
     *
     * @implNote
     * This implementation mirrors the closure returned by
     * {@code WAWebCreateReceiptStanzaReceiveMetric.createReceiptStanzaReceiveMetric}.
     * The {@code receiptStanzaType} field is set to the literal
     * {@code "delivery"} when the stanza had no {@code type} attribute, or
     * to the raw {@code type} string when it matches a known
     * {@code RECEIPT_TYPES_TO_ACK} entry; unknown {@code type} strings
     * leave the field unset, matching WA Web's
     * {@code n==null ? type=DELIVERY : RECEIPT_TYPES_TO_ACK[n]!=null && (type=n)}
     * gate. The default {@code receiptStanzaTotalCount} of {@code 1} is
     * preserved for {@link SimpleReceipt} because WA Web's parser never
     * attaches a {@code receipts} array to simple receipts; only the
     * aggregated variants override the count with the actual
     * {@code <user>} child count.
     *
     * @param builder the pre-populated event builder from
     *                {@link #handle(Stanza)}
     * @param parsed  the parsed receipt carrying {@code from},
     *                {@code ackString}, {@code offline} and, for
     *                aggregated receipts, the per-user count
     */
    private void commitReceiptMetric(ReceiptStanzaReceiveEventBuilder builder, ParsedReceipt parsed) {
        if (parsed.offline()) {
            return;
        }

        builder.messageType(resolveMessageType(parsed.from()));

        var ackString = parsed.ackString();
        if (ackString == null) {
            builder.receiptStanzaType("delivery");
        } else if (isReceiptTypeToAck(ackString)) {
            builder.receiptStanzaType(ackString);
        }

        switch (parsed) {
            case AggregatedByTypeReceipt aggregatedByType ->
                    builder.receiptStanzaTotalCount(aggregatedByType.receipts().size());
            case AggregatedByMessageReceipt aggregatedByMessage ->
                    builder.receiptStanzaTotalCount(aggregatedByMessage.receipts().size());
            case SimpleReceipt ignored -> {
            }
        }

        builder.stopReceiptStanzaDuration();
        wamService.commit(builder.build());
    }

    /**
     * Classifies the receipt's {@code from} JID into a {@link MessageType}
     * WAM enum value.
     *
     * <p>The classification populates {@code messageType} on the
     * {@link com.github.auties00.cobalt.wire.wam.event.ReceiptStanzaReceiveEvent}
     * built by
     * {@link #commitReceiptMetric(ReceiptStanzaReceiveEventBuilder, ParsedReceipt)};
     * WA aggregates receipt telemetry by surface kind to detect regressions
     * in specific chat surfaces.
     *
     * @implNote
     * This implementation matches the {@code s(e)} helper inside
     * {@code WAWebCreateReceiptStanzaReceiveMetric} verbatim. The
     * status-broadcast check runs before the broadcast check because the
     * status account uses a broadcast-shaped JID and must be classified as
     * {@link MessageType#STATUS}; group-or-community comes next because
     * communities reuse the group server suffix; the remaining branches
     * are mutually exclusive on the server suffix alone.
     *
     * @param from the {@code from} attribute of the receipt stanza, or
     *             {@code null}
     * @return the resolved {@link MessageType}, defaulting to
     *         {@link MessageType#INDIVIDUAL} when no other classifier
     *         matches or {@code from} is {@code null}
     */
    private static MessageType resolveMessageType(Jid from) {
        if (from == null) {
            return MessageType.INDIVIDUAL;
        }
        if (from.isStatusBroadcastAccount()) {
            return MessageType.STATUS;
        }
        if (from.hasGroupOrCommunityServer()) {
            return MessageType.GROUP;
        }
        if (from.hasBroadcastServer()) {
            return MessageType.BROADCAST;
        }
        if (from.hasNewsletterServer()) {
            return MessageType.CHANNEL;
        }
        return MessageType.INDIVIDUAL;
    }

    /**
     * Applies one simple (non-aggregated) receipt to every message it
     * references.
     *
     * <p>The {@link ReceiptAck#PEER} branch is dropped because Cobalt has no
     * peer-message store; the self-broadcast guard suppresses the double
     * notification WA delivers when a broadcast message originated from the
     * local user.
     *
     * @implNote
     * This implementation diverges from WA Web's {@code v(t)} inside
     * {@code WAWebHandleMsgReceipt}. WA Web routes the receipt to one of
     * {@code handleAckPeerSimpleReceipt} (peer),
     * {@code handleNewsletterSimpleReceipt} (newsletter),
     * {@code handleBotOneToOneInvokeSimpleReadReceipt} (bot 1:1 read),
     * {@code handleStatusSimpleReceipt} (status),
     * {@code handleChatSimpleReceipt} (user) and
     * {@code handleGroupSimpleReceipt} (group); Cobalt invokes
     * {@link #updateMessage} for every receipt and lets the store-side
     * lookup decide what to do. The peer-message-deletion side effect
     * implemented by {@code handleAckPeerSimpleReceipt} is not modelled
     * here; the {@link ReceiptAck#PEER} early-return matches that gap.
     *
     * @param receipt the parsed simple receipt
     */
    private void handleSimple(SimpleReceipt receipt) {
        if (receipt.ack() == ReceiptAck.PEER) {
            return;
        }

        if (receipt.from().hasBroadcastServer() && receipt.participant() != null) {
            var self = whatsapp.store().accountStore().jid().orElse(null);
            if (self != null && sameUser(self, receipt.participant())) {
                return;
            }
        }

        for (var externalId : receipt.externalIds()) {
            var info = findMessage(receipt.from(), receipt.participant(), externalId);
            if (info == null) {
                if (Log.TRACE) LOGGER.log(Level.TRACE, "no stored message for simple receipt id={0}", externalId);
                continue;
            }

            updateMessage(receipt, info, externalId, receipt.participant(), receipt.ack(), receipt.timestamp());
        }
    }

    /**
     * Deaggregates a by-type receipt into one per-participant update,
     * applying the parent ack to every {@code <user>} child.
     *
     * <p>By-type aggregation is the wire-format optimisation WA uses when a
     * group or broadcast message reaches its delivery / read milestone for
     * many participants simultaneously: instead of one stanza per
     * participant, the server sends one stanza per ack with a
     * {@code <user>} list inside. This method expands the list back into
     * the per-participant view the store expects.
     *
     * @implNote
     * This implementation mirrors WA Web's {@code S(e)} inside
     * {@code WAWebHandleMsgReceipt}, which calls
     * {@code WAWebHandleMsgReceiptUtils.deaggregateGroupedByTypeReceipt}
     * and then forwards each entry through the same {@code v} simple-receipt
     * handler. Cobalt applies the parent ack inline rather than
     * materializing intermediate {@link SimpleReceipt} instances. The
     * {@link ReceiptAck#CONTENT_GONE} reject and the group/broadcast-only
     * guard match WA Web's validation exactly.
     *
     * @param receipt the parsed aggregated-by-type receipt
     */
    private void handleAggregatedByType(AggregatedByTypeReceipt receipt) {
        if (receipt.ack() == ReceiptAck.CONTENT_GONE) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "reupload receipts cannot be aggregated");
            return;
        }

        if (!receipt.from().hasGroupOrCommunityServer() && !receipt.from().hasBroadcastServer()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "aggregated receipts only supported for group/broadcast");
            return;
        }

        for (var participantReceipt : receipt.receipts()) {
            var info = findMessage(receipt.from(), participantReceipt.participant(), receipt.externalId());
            if (info == null) {
                if (Log.TRACE) LOGGER.log(Level.TRACE, "no stored message for aggregated-by-type receipt id={0}", receipt.externalId());
                continue;
            }

            updateMessage(
                    receipt,
                    info,
                    receipt.externalId(),
                    participantReceipt.participant(),
                    receipt.ack(),
                    participantReceipt.timestamp()
            );
        }
    }

    /**
     * Deaggregates a by-message receipt into one per-participant update,
     * using each participant's own ack type.
     *
     * <p>By-message aggregation is the wire-format optimisation WA uses when
     * multiple participants in a single group or broadcast message hit
     * different milestones at the same instant: each {@code <user>} child
     * carries its own {@code type} attribute.
     *
     * @implNote
     * This implementation mirrors WA Web's {@code R(e)} inside
     * {@code WAWebHandleMsgReceipt}, which calls
     * {@code WAWebHandleMsgReceiptUtils.deaggregateGroupedByMessageReceipt}
     * and forwards each entry through the same {@code v} simple-receipt
     * handler. Cobalt applies the per-participant ack inline rather than
     * materializing intermediate {@link SimpleReceipt} instances.
     *
     * @param receipt the parsed aggregated-by-message receipt
     */
    private void handleAggregatedByMessage(AggregatedByMessageReceipt receipt) {
        for (var participantReceipt : receipt.receipts()) {
            var info = findMessage(receipt.from(), participantReceipt.participant(), receipt.externalId());
            if (info == null) {
                if (Log.TRACE) LOGGER.log(Level.TRACE, "no stored message for aggregated-by-message receipt id={0}", receipt.externalId());
                continue;
            }

            updateMessage(
                    receipt,
                    info,
                    receipt.externalId(),
                    participantReceipt.participant(),
                    participantReceipt.ack(),
                    participantReceipt.timestamp()
            );
        }
    }

    /**
     * Processes one retry receipt: rebuilds the Signal session from any
     * carried key bundle and re-ships the original message.
     *
     * <p>Retry receipts are the recovery path WA uses when the peer device
     * cannot decrypt a delivered message; the peer asks the sender for a
     * fresh ciphertext encrypted against an updated key bundle. The
     * acknowledgement of the retry itself is always sent, regardless of
     * whether the re-send succeeds, so the server stops re-delivering the
     * retry request.
     *
     * @implNote
     * This implementation mirrors WA Web's
     * {@code WAWebHandleMessageRetryRequest.handleMessageRetryRequest},
     * which wraps {@code WAWebHandleRetryRequest.handleRetryRequest} in
     * the same try / send-ack pattern. Cobalt does not run the WA Web
     * {@code WAWebMessageQueue.onMessageQueue} serialization layer because
     * the dispatcher already runs each stanza on its own virtual thread
     * and the store mutations downstream are short-lived.
     *
     * @param stanza the retry receipt stanza
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMessageRetryRequest",
            exports = "handleMessageRetryRequest",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void handleRetryReceipt(Stanza stanza) {
        try {
            processRetryRequest(stanza);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG,
                        "processed retry receipt request type={0} id={1}",
                        stanza.getAttributeAsString("type", null),
                        stanza.getAttributeAsString("id", null));
            }
        } catch (RuntimeException exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "failed to inspect retry receipt id=" + stanza.getAttributeAsString("id", "<unknown>"),
                        exception);
            }
        } finally {
            sendRetryAck(stanza);
        }
    }

    /**
     * Performs the inner retry-receipt work: validates the retry count,
     * processes the key bundle, looks up the original message and re-ships
     * it through {@link MessageService}.
     *
     * <p>The {@code enc_rekey_retry} and {@code voip_1x1_retry} type variants
     * are skipped because Cobalt does not implement the VoIP rekey path
     * driven by {@code WAWebVoipStackInterface.resendEncRekeyRetry} in
     * WA Web.
     *
     * <p>Every branch that refuses the resend commits one
     * {@link com.github.auties00.cobalt.wire.wam.event.E2eRetryRejectEvent} through
     * {@link #emitRetryReject(Jid, Jid, int, LinkedMessageInfo, RetryRejectReason)}:
     * the retry-count ceiling maps to
     * {@link RetryRejectReason#HIGH_RETRY_COUNT}, a missing original message to
     * {@link RetryRejectReason#MESSAGE_NOT_EXIST} and a message that is not
     * outbound to {@link RetryRejectReason#OTHER}. The unknown-device branch
     * keeps its own {@link com.github.auties00.cobalt.wire.wam.event.MdRetryFromUnknownDeviceEvent}
     * emission and the self-broadcast suppression path stays silent because it
     * is a duplicate-avoidance guard rather than a rejection.
     *
     * @implNote
     * This implementation mirrors WA Web's
     * {@code WAWebHandleRetryRequest.handleRetryRequest} and its inner
     * {@code E} (key-bundle eligibility) helper, collapsed onto a single
     * Java method because Cobalt does not have the
     * {@code WAWebSendMsgQueueMap.enqueue} serialization or the
     * pre-migration LID dual-key lookup of {@code getActualChatInfo}. The
     * {@code requester = participant ?? from} selection matches WA Web's
     * {@code e.isUser() ? a : c} branch (peer 1:1 retries carry the
     * device id on the top-level {@code from}; group and broadcast
     * retries carry it on {@code participant}). The peer-message branch
     * (a {@link ProtocolMessage} with a participant present) routes
     * through {@link MessageService#sendPeer(Jid, ChatMessageInfo)} so
     * the message is re-shipped as a peer broadcast rather than a
     * group/chat fanout. The self-broadcast guard avoids resending a
     * broadcast originating from the local user, which would produce a
     * duplicate.
     *
     * @param stanza the retry receipt stanza
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleRetryRequest",
            exports = "handleRetryRequest",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void processRetryRequest(Stanza stanza) {
        var type = stanza.getAttributeAsString("type", null);
        if ("enc_rekey_retry".equals(type) || "voip_1x1_retry".equals(type)) {
            return;
        }

        var from = stanza.getAttributeAsJid("from").orElse(null);
        var participant = stanza.getAttributeAsJid("participant").orElse(null);
        var retryNode = stanza.getChild("retry").orElse(null);
        var originalId = retryNode != null ? retryNode.getAttributeAsString("id", null) : null;

        var retryCount = retryNode != null
                ? retryNode.getAttributeAsInt("count").orElse(0)
                : 0;
        if (retryCount >= MAX_RETRY) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG,
                        "refusing retry attempt #{0}, exceeds max retry count {1}",
                        retryCount, MAX_RETRY);
            }
            var highRetryRequester = participant != null ? participant : from;
            if (highRetryRequester != null) {
                emitRetryReject(highRetryRequester, from, retryCount, null, RetryRejectReason.HIGH_RETRY_COUNT);
            }
            return;
        }

        var requester = participant != null ? participant : from;
        if (requester == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no requester found for incoming retry request");
            return;
        }

        var deviceId = Math.max(requester.device(), 0);
        if (!isDeviceKnown(requester, deviceId)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "device {0} not found for {1}", deviceId, requester);
            var offline = stanza.hasAttribute("offline");
            emitRetryFromUnknownDevice(deviceId, offline);
            return;
        }

        processRetryKeyBundle(stanza, requester);
        if (originalId == null) {
            return;
        }

        var message = findRetryMessage(from, participant, originalId);
        if (message == null) {
            emitRetryReject(requester, from, retryCount, null, RetryRejectReason.MESSAGE_NOT_EXIST);
            return;
        }
        if (!message.key().fromMe()) {
            emitRetryReject(requester, from, retryCount, message, RetryRejectReason.OTHER);
            return;
        }

        if (message instanceof ChatMessageInfo chatMessageInfo
                && chatMessageInfo.message().content() instanceof ProtocolMessage
                && participant != null) {
            messageService.sendPeer(participant, chatMessageInfo);
            return;
        }

        var parentJid = message.key().parentJid().orElse(null);
        if (parentJid == null || isBroadcastSelfRetry(message, from, participant)) {
            return;
        }

        messageService.send(message);
    }

    /**
     * Looks up the original outbound {@link LinkedMessageInfo} addressed by a
     * retry receipt.
     *
     * <p>The broadcast fallback uses the participant's user JID as the chat
     * key because broadcast messages are stored per-participant in the chat
     * thread.
     *
     * @implNote
     * This implementation mirrors WA Web's {@code D(t)} helper inside
     * {@code WAWebHandleRetryRequest}, exported as {@code getTargetChat}.
     * WA Web also runs the {@code I(e)} {@code getActualChatInfo} helper
     * to disambiguate pre-migration LID dual-keyed messages; Cobalt does
     * not maintain that dual key and relies on the store's canonical PN
     * keying instead.
     *
     * @param provider    the {@code from} JID of the retry stanza
     * @param participant the {@code participant} JID of the retry stanza,
     *                    or {@code null}
     * @param id          the original message id read from the
     *                    {@code <retry>} child
     * @return the resolved {@link LinkedMessageInfo}, or {@code null} when the
     *         message cannot be located in the store
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleRetryRequest",
            exports = "getTargetChat",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private LinkedMessageInfo findRetryMessage(Jid provider, Jid participant, String id) {
        var direct = whatsapp.store().chatStore().findMessageById(provider, id)
                .map(LinkedMessageInfo.class::cast)
                .orElse(null);
        if (direct != null) {
            return direct;
        }

        if (participant == null || provider == null || !provider.hasBroadcastServer() || provider.isStatusBroadcastAccount()) {
            return null;
        }

        return whatsapp.store().chatStore().findMessageById(participant.toUserJid(), id)
                .map(LinkedMessageInfo.class::cast)
                .orElse(null);
    }

    /**
     * Returns {@code true} when a broadcast retry would re-ship a message
     * the local user originally broadcast, in which case the re-send is
     * suppressed.
     *
     * <p>The guard prevents the duplicate-delivery race that occurs when the
     * server pushes a retry for a broadcast addressed by the local user
     * itself (status messages are the canonical case): re-shipping would
     * send the message back to the local user.
     *
     * @implNote
     * This implementation has no direct WA Web counterpart because WA Web
     * routes self-broadcast retries through a different pipeline
     * ({@code WAWebHandleAckPeerSimpleReceipt} and the
     * {@code receiptBatcher.acceptPeerReceipt} fast path) that never
     * reaches {@code handleRetryRequest}. The Cobalt guard catches the
     * same case at the unified retry entry point.
     *
     * @param message     the {@link LinkedMessageInfo} resolved by
     *                    {@link #findRetryMessage(Jid, Jid, String)}
     * @param from        the {@code from} JID of the retry stanza
     * @param participant the {@code participant} JID of the retry stanza,
     *                    or {@code null}
     * @return {@code true} when the resolved message is a self-broadcast
     *         and must not be re-shipped; {@code false} otherwise
     */
    private boolean isBroadcastSelfRetry(LinkedMessageInfo message, Jid from, Jid participant) {
        var parentJid = message.key().parentJid().orElse(null);
        if (parentJid == null || !parentJid.hasBroadcastServer() || parentJid.isStatusBroadcastAccount()) {
            return false;
        }

        var sender = participant != null ? participant.toUserJid() : from != null ? from.toUserJid() : null;
        return sender != null
                && whatsapp.store().accountStore().jid().map(self -> Objects.equals(self.toUserJid(), sender)).orElse(false);
    }

    /**
     * Rebuilds the Signal session against the remote device using the key
     * bundle carried inside a retry receipt.
     *
     * <p>The remote device installs a fresh signed pre-key (and optionally a
     * one-time pre-key) that the next encryption pass binds the new
     * ciphertext to. The canonical wire shape is:
     * {@snippet :
     *     <receipt type="retry" ...>
     *         <retry id="..." count="..."/>
     *         <registration>4-byte BE unsigned regId</registration>
     *         <keys>
     *             <identity>32-byte Curve25519 public key</identity>
     *             <skey>
     *                 <id>3-byte BE unsigned signed-prekey id</id>
     *                 <value>32-byte Curve25519 public key</value>
     *                 <signature>64-byte Ed25519 signature</signature>
     *             </skey>
     *             <key>
     *                 <id>3-byte BE unsigned one-time-prekey id</id>
     *                 <value>32-byte Curve25519 public key</value>
     *             </key>
     *         </keys>
     *     </receipt>
     * }
     *
     * @implNote
     * This implementation mirrors WA Web's
     * {@code WAWebRetryRequestParser} default export plus the downstream
     * {@code WAWebProcessRetryKeyBundle.processKeyBundle} that feeds the
     * Signal session create call. WA Web's parser splits
     * {@code regular_retry} from {@code bot_retry} (the latter expects a
     * top-level {@code <key>} sibling rather than nested inside
     * {@code <keys>}) and asserts the presence of the one-time pre-key
     * differently per kind; Cobalt unifies the two by treating the
     * one-time pre-key as optional, which works for both shapes because
     * the Signal session rebuild succeeds with the signed pre-key alone.
     * The catch-all on {@link Throwable} mirrors WA Web's outer
     * try/catch in {@code processKeyBundle}, which swallows session-rebuild
     * failures so the retry-ack still flows.
     *
     * @param stanza         the retry receipt stanza
     * @param remoteDevice the remote device JID extracted from
     *                     {@code participant ?? from}
     */
    @WhatsAppWebExport(moduleName = "WAWebRetryRequestParser",
            exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    private void processRetryKeyBundle(Stanza stanza, Jid remoteDevice) {
        if (remoteDevice == null) {
            return;
        }

        var keysNode = stanza.getChild("keys").orElse(null);
        var registrationId = stanza.getChild("registration")
                .flatMap(Stanza::toContentBytes)
                .map(bytes -> convertBytesToUint(bytes, 4))
                .orElse(null);
        if (keysNode == null || registrationId == null) {
            return;
        }

        try {
            var identityKey = keysNode.getChild("identity")
                    .flatMap(Stanza::toContentBytes)
                    .map(SignalIdentityPublicKey::ofDirect)
                    .orElse(null);
            var signedPreKey = keysNode.getChild("skey").orElse(null);
            if (identityKey == null || signedPreKey == null) {
                return;
            }

            var builder = new SignalPreKeyBundleBuilder()
                    .registrationId(registrationId)
                    .deviceId(Math.max(remoteDevice.device(), 0))
                    .signedPreKeyId(signedPreKey.getChild("id")
                            .flatMap(Stanza::toContentBytes)
                            .map(bytes -> convertBytesToUint(bytes, 3))
                            .orElseThrow())
                    .signedPreKeyPublic(signedPreKey.getChild("value")
                            .flatMap(Stanza::toContentBytes)
                            .map(SignalIdentityPublicKey::ofDirect)
                            .orElseThrow())
                    .signedPreKeySignature(signedPreKey.getChild("signature")
                            .flatMap(Stanza::toContentBytes)
                            .orElseThrow())
                    .identityKey(identityKey);

            var preKey = keysNode.getChild("key").orElse(null);
            if (preKey != null) {
                var preKeyId = preKey.getChild("id")
                        .flatMap(Stanza::toContentBytes)
                        .map(bytes -> convertBytesToUint(bytes, 3))
                        .orElse(null);
                var preKeyValue = preKey.getChild("value")
                        .flatMap(Stanza::toContentBytes)
                        .map(SignalIdentityPublicKey::ofDirect)
                        .orElse(null);
                if (preKeyId != null && preKeyValue != null) {
                    builder.preKeyId(preKeyId);
                    builder.preKeyPublic(preKeyValue);
                }
            }

            var sessionCipher = new SignalSessionCipher(whatsapp.store().signalStore());
            sessionCipher.process(remoteDevice.toSignalAddress(), builder.build());
            whatsapp.store().save();
        } catch (Throwable throwable) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "failed to process retry key bundle for " + new LogRedactable.User(String.valueOf(remoteDevice)), throwable);
            }
        }
    }

    /**
     * Returns the value of the first {@code byteCount} bytes of
     * {@code bytes} interpreted as a big-endian unsigned integer.
     *
     * <p>The helper decodes the fixed-width identifier fields carried inside
     * retry-receipt sub-nodes: the {@code <registration>} content is 4
     * bytes; the {@code <id>} sub-stanza inside {@code <skey>} and
     * {@code <key>} is 3 bytes.
     *
     * @implNote
     * This implementation mirrors WA Web's
     * {@code WAParsableXmlNode.convertBytesToUint} byte for byte:
     * {@code n = n * 256 + bytes[i]} for the first {@code byteCount}
     * positions. The same helper is consumed by every smax
     * {@code KeyIDMixin} and {@code RegistrationIDMixin} parser inside
     * WA Web's {@code WAWebFetchPrekeysJob} and
     * {@code WAWebFetchResendMissingKeyJob}.
     *
     * @param bytes     the raw content bytes read from a wire-format stanza
     * @param byteCount the number of leading bytes to interpret
     * @return the decoded big-endian unsigned integer
     * @throws IllegalArgumentException if {@code bytes} is {@code null} or
     *                                  shorter than {@code byteCount}
     */
    @WhatsAppWebExport(moduleName = "WAParsableXmlNode",
            exports = "convertBytesToUint",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static int convertBytesToUint(byte[] bytes, int byteCount) {
        if (bytes == null || bytes.length < byteCount) {
            throw new IllegalArgumentException("Expected " + byteCount + " bytes, got " + (bytes == null ? 0 : bytes.length));
        }
        var n = 0;
        for (var i = 0; i < byteCount; i++) {
            n = (n << 8) | (bytes[i] & 0xFF);
        }
        return n;
    }

    /**
     * Returns {@code true} when {@code deviceId} corresponds to a device
     * recorded for {@code requester} in the local device-list cache.
     *
     * <p>The check gates key-bundle processing on the requester device being
     * one Cobalt knows about. An unknown device id signals that the local
     * device-list cache is stale; the retry must be refused and a
     * {@code MdRetryFromUnknownDevice} WAM event emitted so the server can
     * detect the gap.
     *
     * @implNote
     * This implementation mirrors {@code WAWebApiDeviceList.hasDevice}
     * verbatim: the primary device id
     * ({@link DeviceConstants#PRIMARY_DEVICE_ID}, value {@code 0}) is
     * always treated as known without consulting the cache, and any other
     * id is verified against the cached device list for the user.
     *
     * @param requester the device-level JID extracted by
     *                  {@link #processRetryRequest(Stanza)}
     * @param deviceId  the device id to verify, where {@code 0} means
     *                  primary
     * @return {@code true} when the device is known; {@code false}
     *         otherwise
     */
    private boolean isDeviceKnown(Jid requester, int deviceId) {
        if (deviceId == DeviceConstants.PRIMARY_DEVICE_ID) {
            return true;
        }
        var deviceList = whatsapp.store().contactStore().findDeviceList(requester.toUserJid()).orElse(null);
        if (deviceList == null || deviceList.deleted()) {
            return false;
        }
        return deviceList.devices().stream().anyMatch(device -> device.id() == deviceId);
    }

    /**
     * Commits one
     * {@link com.github.auties00.cobalt.wire.wam.event.MdRetryFromUnknownDeviceEvent}
     * to the {@link WamService}.
     *
     * <p>The event is emitted when the retry requester is unknown to the
     * local device-list cache. The {@code offline} flag distinguishes
     * retries delivered live from retries replayed off the offline queue so
     * the server can correlate cache-staleness with reconnect timing.
     *
     * @implNote
     * This implementation matches the WA Web emission in
     * {@code WAWebHandleRetryRequest.E}, which constructs the event with
     * {@code senderType = DEVICE_TYPE.PRIMARY} when the device id equals
     * {@code WAJids.DEFAULT_DEVICE_ID} and {@code DEVICE_TYPE.COMPANION}
     * otherwise.
     *
     * @param deviceId the device id extracted from the retry stanza
     * @param offline  {@code true} when the retry stanza carried the
     *                 {@code offline} attribute
     */
    private void emitRetryFromUnknownDevice(int deviceId, boolean offline) {
        var senderType = deviceId == DeviceConstants.PRIMARY_DEVICE_ID
                ? DeviceType.PRIMARY
                : DeviceType.COMPANION;
        wamService.commit(new MdRetryFromUnknownDeviceEventBuilder()
                .offline(offline)
                .senderType(senderType)
                .build());
    }

    /**
     * Commits one
     * {@link com.github.auties00.cobalt.wire.wam.event.E2eRetryRejectEvent} for a
     * retry request the local client refuses to satisfy as the message
     * sender.
     *
     * <p>The event is emitted from every reject branch of
     * {@link #processRetryRequest(Stanza)}: the retry-count ceiling, a missing
     * original message and a message that is not outbound. The
     * {@code senderDeviceType} and {@code e2eSenderType} classify the
     * requesting device against the local account; {@code sessionScope}
     * distinguishes the status-broadcast encryption session from the default
     * one; {@code encryptionType} is set to {@link EncryptionTypeCode#COEX}
     * only when the requester lives on a hosted server; {@code messageType}
     * buckets the conversation surface and {@code retryRevoke} flags a
     * rejected revoke.
     *
     * @implNote
     * This implementation mirrors the {@code E2eRetryRejectWamEvent}
     * construction inside {@code WAWebProcessRetryKeyBundle.getMsgIfAuthorized},
     * which commits the event with {@code senderDeviceType} derived from
     * {@code l.isCompanion()}, {@code messageType} from
     * {@code getWamMessageType}, {@code msgRetryCount} from the retry count,
     * {@code retryRevoke} from the revoke flag, {@code retryRejectReason} from
     * the {@code RetryEligibilityResult}, {@code sessionScope} from
     * {@code sessionScopeToWamType}, the optional {@code e2eSenderType} from
     * {@code getWamE2eSenderType} and the {@code encryptionType} COEX override
     * for hosted requesters. WA Web classifies {@code messageType} from the
     * stored message body; Cobalt derives the same surface bucket from the
     * conversation JID through {@link #resolveMessageType(Jid)} because the
     * rejected message is not always resolvable on the count-ceiling branch.
     *
     * @param requester  the device-level JID that issued the retry request
     * @param chat       the conversation JID the retry targets, or
     *                   {@code null}
     * @param retryCount the retry attempt count read from the {@code <retry>}
     *                   child
     * @param message    the resolved original message, or {@code null} when
     *                   it could not be located
     * @param reason     the {@link RetryRejectReason} for this refusal
     */
    @WhatsAppWebExport(moduleName = "WAWebProcessRetryKeyBundle",
            exports = "getMsgIfAuthorized",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitRetryReject(Jid requester, Jid chat, int retryCount, LinkedMessageInfo message, RetryRejectReason reason) {
        var deviceId = Math.max(requester.device(), 0);
        var builder = new E2eRetryRejectEventBuilder()
                .senderDeviceType(deviceId == DeviceConstants.PRIMARY_DEVICE_ID ? DeviceType.PRIMARY : DeviceType.COMPANION)
                .e2eSenderType(resolveE2eSenderType(requester, deviceId))
                .msgRetryCount(retryCount)
                .retryRevoke(isRevokeMessage(message))
                .retryRejectReason(reason)
                .messageType(resolveMessageType(chat))
                .sessionScope(chat != null && chat.isStatusBroadcastAccount() ? SessionScopeType.STATUS : SessionScopeType.DEFAULT);
        if (requester.hasHostedServer() || requester.hasHostedLidServer()) {
            builder.encryptionType(EncryptionTypeCode.COEX);
        }
        wamService.commit(builder.build());
    }

    /**
     * Classifies the retry requester's device into the {@link E2eDeviceType}
     * WAM enum value carried by
     * {@link com.github.auties00.cobalt.wire.wam.event.E2eRetryRejectEvent}.
     *
     * <p>The classification splits on whether the requesting device belongs to
     * the local account and on whether it is a primary, companion or hosted
     * device.
     *
     * @implNote
     * This implementation reconstructs WA Web's
     * {@code WAWebWamMsgUtils.getWamE2eSenderType}: hosted requesters resolve
     * to the {@code HOSTED_COMPANION} pair, the primary device id
     * ({@link DeviceConstants#PRIMARY_DEVICE_ID}) to the {@code PRIMARY} pair
     * and any other device id to the {@code COMPANION} pair, with the
     * {@code MY_}/{@code OTHER_} prefix selected by comparing the requester
     * against the local account JID through {@link #sameUser(Jid, Jid)}.
     *
     * @param requester the device-level JID that issued the retry request
     * @param deviceId  the requester device id, where
     *                  {@link DeviceConstants#PRIMARY_DEVICE_ID} means primary
     * @return the resolved {@link E2eDeviceType}
     */
    private E2eDeviceType resolveE2eSenderType(Jid requester, int deviceId) {
        var mine = whatsapp.store().accountStore().jid()
                .map(self -> sameUser(self, requester))
                .orElse(false);
        if (requester.hasHostedServer() || requester.hasHostedLidServer()) {
            return mine ? E2eDeviceType.MY_HOSTED_COMPANION : E2eDeviceType.OTHER_HOSTED_COMPANION;
        }
        if (deviceId == DeviceConstants.PRIMARY_DEVICE_ID) {
            return mine ? E2eDeviceType.MY_PRIMARY : E2eDeviceType.OTHER_PRIMARY;
        }
        return mine ? E2eDeviceType.MY_COMPANION : E2eDeviceType.OTHER_COMPANION;
    }

    /**
     * Returns {@code true} when the rejected message is a revoke.
     *
     * <p>The result drives the {@code retryRevoke} flag on
     * {@link com.github.auties00.cobalt.wire.wam.event.E2eRetryRejectEvent}.
     * Newsletter messages and a {@code null} message resolve to {@code false}
     * because only chat messages carry a revoke {@link ProtocolMessage}.
     *
     * @implNote
     * This implementation matches WA Web's
     * {@code f.data.type === MSG_TYPE.REVOKED} check inside
     * {@code getMsgIfAuthorized}: a Cobalt revoke is a {@link ChatMessageInfo}
     * whose content is a {@link ProtocolMessage} of type
     * {@link ProtocolMessage.Type#REVOKE}.
     *
     * @param message the resolved original message, or {@code null}
     * @return {@code true} when {@code message} is a revoke; {@code false}
     *         otherwise
     */
    private static boolean isRevokeMessage(LinkedMessageInfo message) {
        return message instanceof ChatMessageInfo chatMessageInfo
                && chatMessageInfo.message().content() instanceof ProtocolMessage protocolMessage
                && protocolMessage.type().orElse(null) == ProtocolMessage.Type.REVOKE;
    }

    /**
     * Looks up an inbound-receipt-targeted message in the store, with a
     * broadcast-only fallback to the participant's user JID.
     *
     * <p>The broadcast fallback covers the case where the message was stored
     * under the participant's user JID rather than under the broadcast
     * chat JID.
     *
     * @implNote
     * This implementation collapses what WA Web spreads across
     * {@code handleChatSimpleReceipt}, {@code handleGroupSimpleReceipt}
     * and {@code handleStatusSimpleReceipt}, which each look the message
     * up through {@code WAWebSchemaMessage.getMessageTable().get(...)}
     * with a per-surface key shape. Cobalt's store keys uniformly on
     * {@code (chatJid, messageId)} so the lookup is a single call.
     *
     * @param provider    the {@code from} JID of the receipt
     * @param participant the {@code participant} JID, used as a broadcast
     *                    fallback; may be {@code null}
     * @param id          the external message id
     * @return the matching {@link LinkedMessageInfo}, or {@code null} when the
     *         message is not in the store
     */
    private LinkedMessageInfo findMessage(Jid provider, Jid participant, String id) {
        var direct = whatsapp.store().chatStore().findMessageById(provider, id)
                .map(LinkedMessageInfo.class::cast)
                .orElse(null);
        if (direct != null) {
            return direct;
        }

        if (participant == null || !provider.hasBroadcastServer() || provider.isStatusBroadcastAccount()) {
            return null;
        }

        return whatsapp.store().chatStore().findMessageById(participant.toUserJid(), id)
                .map(LinkedMessageInfo.class::cast)
                .orElse(null);
    }

    /**
     * Folds one per-participant receipt event into the existing
     * {@link LinkedMessageInfo}, then fans out
     * {@link LinkedWhatsAppClientListener#onMessageStatus}.
     *
     * <p>Both {@link ChatMessageInfo} and {@link NewsletterMessageInfo} are
     * mutated in place because the store hands out the same instance across
     * lookups.
     *
     * @implNote
     * This implementation collapses two WA Web layers. The per-surface
     * "fan-out into receipt batcher" step performed by
     * {@code WAWebMessageReceiptBatcher.receiptBatcher.acceptOtherReceipt}
     * is skipped because Cobalt updates the receipt in place rather than
     * accumulating a batch. The per-status timestamp split (delivery /
     * read / played stored separately) mirrors WA Web's
     * {@code WAWebApiMessageInfoStore} record. The
     * {@link #mergeStatus(MessageStatus, MessageStatus)} reduction
     * preserves the highest ack already seen so a late-arriving delivery
     * receipt cannot down-rank a previous read receipt.
     *
     * @param receipt           the upstream {@link ReceiptLike} carrying
     *                          {@code from} and {@code recipient}
     * @param info              the message being updated
     * @param messageId         the external id of the message
     * @param participantDevice the device JID that produced the receipt
     * @param ack               the {@link ReceiptAck} for this update
     * @param timestamp         the timestamp of the receipt event
     */
    private void updateMessage(
            ReceiptLike receipt,
            LinkedMessageInfo info,
            String messageId,
            Jid participantDevice,
            ReceiptAck ack,
            Instant timestamp
    ) {
        var userJid = resolveReceiptUser(receipt.from(), participantDevice, receipt.recipient());
        var deviceUpdate = userJid != null
                ? resolveDeviceUpdate(messageId, userJid, participantDevice)
                : new ReceiptUpdate(List.of(), List.of());

        var nextStatus = mapStatus(ack);
        var receiptTimestamp = isDeliveryLike(ack) ? timestamp : null;
        var readTimestamp = ack == ReceiptAck.READ ? timestamp : null;
        var playedTimestamp = ack == ReceiptAck.PLAYED ? timestamp : null;
        var nextReceipts = userJid != null
                ? mergeReceipt(info, userJid, receiptTimestamp, readTimestamp, playedTimestamp, deviceUpdate.pendingDevices(), deviceUpdate.deliveredDevices())
                : info.receipts();

        switch (info) {
            case ChatMessageInfo chatMessageInfo -> {
                chatMessageInfo.setStatus(mergeStatus(chatMessageInfo.status().orElse(null), nextStatus));
                if (!nextReceipts.isEmpty()) {
                    chatMessageInfo.setReceipts(nextReceipts);
                }
            }
            case NewsletterMessageInfo newsletterMessageInfo -> {
                newsletterMessageInfo.setStatus(mergeStatus(newsletterMessageInfo.status().orElse(null), nextStatus));
                if (!nextReceipts.isEmpty()) {
                    newsletterMessageInfo.setReceipts(nextReceipts);
                }
            }
        }

        notifyMessageStatus(info);
    }

    /**
     * Ships an {@code <ack>} stanza in response to a regular (non-retry)
     * receipt.
     *
     * <p>The ack drops the {@code participant} attribute when it would equal
     * the {@code to} attribute, matching WA Web's wire shape.
     *
     * @implNote
     * This implementation matches the {@code <ack>} builder inside
     * {@code WAWebHandleMsgReceipt}, exported via
     * {@code WAWebReceiptAck.buildReceiptAck(from, id, type, participant)}.
     * The {@code class="receipt"} attribute is always present; the
     * {@code type} attribute is the raw {@code ackString} from the parser
     * or the inbound stanza's {@code type} attribute when parsing failed.
     *
     * @param stanza   the inbound receipt stanza
     * @param parsed the {@link ParsedReceipt} returned by
     *               {@link #parseReceipt(Stanza)}, or {@code null} when
     *               parsing failed
     */
    private void sendAck(Stanza stanza, ParsedReceipt parsed) {
        var participant = parsed instanceof SimpleReceipt simple ? simple.participant() : null;
        var ackString = parsed != null ? parsed.ackString() : stanza.getAttributeAsString("type", null);
        ackSender.ack(AckClass.RECEIPT, stanza)
                .type(ackString)
                .participantIfDifferent(participant)
                .send();
    }

    /**
     * Ships the {@code <ack type="retry">} stanza that completes the
     * retry-receipt handshake.
     *
     * <p>The ack is sent unconditionally so the server stops re-delivering
     * the retry request whether or not Cobalt successfully re-shipped the
     * original message.
     *
     * @implNote
     * This implementation matches the {@code <ack>} builder inside
     * {@code WAWebHandleMessageRetryRequest}, which always sets
     * {@code type="retry"} and preserves the original
     * {@code participant} attribute when present.
     *
     * @param stanza the retry receipt stanza
     */
    private void sendRetryAck(Stanza stanza) {
        ackSender.ack(AckClass.RECEIPT, stanza)
                .type("retry")
                .participant(stanza.getAttributeAsJid("participant").orElse(null))
                .send();
    }

    /**
     * Parses an inbound {@code <receipt>} stanza into the
     * {@link ParsedReceipt} variant that matches its on-wire shape.
     *
     * <p>Returns {@code null} when the stanza is missing either {@code id}
     * or {@code from}, in which case the caller sends a bare {@code <ack>}
     * and drops further processing.
     *
     * @implNote
     * This implementation mirrors WA Web's
     * {@code WAWebHandleMsgReceiptParser.msgReceiptParser}: the absence
     * of a {@code <participants>} child means a simple receipt, the
     * presence of a {@code message_id} attribute on that child means an
     * aggregated-by-message receipt and otherwise it is aggregated by
     * type. The {@code <error reason="lid" type="feature-incapable">}
     * override sets the ack to {@link ReceiptAck#RECEIVED} instead of the
     * default {@code RECEIVED} mapped from a missing type; WA Web sets it
     * to {@code ACK.SENT}, which Cobalt does not model as a receipt-level
     * concept and folds back into {@code RECEIVED}.
     *
     * @param stanza the inbound receipt stanza
     * @return the parsed receipt, or {@code null} when {@code id} or
     *         {@code from} is missing
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgReceiptParser",
            exports = "msgReceiptParser", adaptation = WhatsAppAdaptation.ADAPTED)
    private ParsedReceipt parseReceipt(Stanza stanza) {
        var id = stanza.getAttributeAsString("id", null);
        var from = stanza.getAttributeAsJid("from").orElse(null);
        if (id == null || from == null) {
            return null;
        }

        var offline = stanza.hasAttribute("offline");
        var ackString = stanza.getAttributeAsString("type", null);
        var ack = ReceiptAck.fromType(ackString);

        var errorNode = stanza.getChild("error").orElse(null);
        if (errorNode != null
                && "lid".equals(errorNode.getAttributeAsString("reason", null))
                && "feature-incapable".equals(errorNode.getAttributeAsString("type", null))) {
            ack = ReceiptAck.RECEIVED;
        }

        var participantsNode = stanza.getChild("participants").orElse(null);
        if (participantsNode == null) {
            return parseSimple(stanza, id, from, ack, ackString, offline);
        }

        if (participantsNode.hasAttribute("message_id")) {
            return parseAggregatedByMessage(id, from, stanza.getAttributeAsJid("recipient").orElse(null), ackString, participantsNode, offline);
        }

        return parseAggregatedByType(id, from, stanza.getAttributeAsJid("recipient").orElse(null), ack, ackString, participantsNode, offline);
    }

    /**
     * Builds a {@link SimpleReceipt} from a non-aggregated receipt stanza.
     *
     * <p>Simple receipts cover both 1:1 and the per-participant flavour of
     * group / broadcast receipts (a group receipt with a single
     * {@code <user>} ack lands here, not in the aggregated path). The
     * optional {@code <list>} child enumerates additional message ids the
     * receipt covers; the optional {@code <biz>} child carries business
     * actor/storage/privacy metadata.
     *
     * @implNote
     * This implementation mirrors the {@code p(e, t)} helper inside
     * {@code WAWebHandleMsgReceiptParser}. For {@code type="view"}
     * receipts the {@code <item>} children carry the message id under
     * {@code server_id} rather than {@code id}; for any other type the
     * stanza id is appended to the external-id list so the simple-receipt
     * handler picks it up even when no {@code <list>} child is present.
     * The bot {@code is_lid} flag is honoured only when the
     * {@code participant} JID resolves to a bot, matching WA Web's
     * {@code n!=null && n.isBot() && t.hasAttr("is_lid")} guard.
     *
     * @param stanza      the inbound receipt stanza
     * @param id        the {@code id} attribute already extracted by
     *                  {@link #parseReceipt(Stanza)}
     * @param from      the {@code from} attribute already extracted by
     *                  {@link #parseReceipt(Stanza)}
     * @param ack       the {@link ReceiptAck} already resolved by
     *                  {@link #parseReceipt(Stanza)}
     * @param ackString the raw {@code type} attribute, or {@code null}
     * @param offline   {@code true} when the stanza carried the
     *                  {@code offline} attribute
     * @return the parsed {@link SimpleReceipt}
     */
    private ParsedReceipt parseSimple(
            Stanza stanza,
            String id,
            Jid from,
            ReceiptAck ack,
            String ackString,
            boolean offline
    ) {
        var participant = stanza.getAttributeAsJid("participant").orElse(null);

        var participantPn = stanza.getAttributeAsJid("participant_pn").orElse(null);

        var participantUsername = stanza.getAttributeAsString("participant_username", null);

        var recipient = stanza.getAttributeAsJid("recipient").orElse(null);

        var isLidBot = false;
        if (participant != null && participant.isBot() && stanza.hasAttribute("is_lid")) {
            isLidBot = "true".equals(stanza.getAttributeAsString("is_lid", null));
        }

        var externalIds = new ArrayList<String>();
        var listNode = stanza.getChild("list").orElse(null);
        var viewReceipt = "view".equals(ackString);
        if (listNode != null) {
            for (var item : listNode.getChildren("item")) {
                var childId = item.getAttributeAsString(viewReceipt ? "server_id" : "id", null);
                if (childId != null) {
                    externalIds.add(childId);
                }
            }
        }

        if (!viewReceipt) {
            externalIds.add(id);
        }

        BizInfo bizInfo = null;
        var bizNode = stanza.getChild("biz").orElse(null);
        if (bizNode != null) {
            var actualActors = bizNode.getAttributeAsInt("actual_actors").orElse(-1);
            var hostStorage = bizNode.getAttributeAsInt("host_storage").orElse(-1);
            var privacyModeTs = bizNode.getAttributeAsInt("privacy_mode_ts").orElse(-1);
            if (actualActors >= 0 && hostStorage >= 0 && privacyModeTs >= 0) {
                bizInfo = new BizInfo(actualActors, hostStorage, privacyModeTs);
            }
        }

        return new SimpleReceipt(
                id,
                from,
                participant,
                participantPn,
                participantUsername,
                recipient,
                attributeInstant(stanza, "t"),
                ack,
                ackString,
                List.copyOf(externalIds),
                offline,
                isLidBot,
                bizInfo
        );
    }

    /**
     * Builds an {@link AggregatedByTypeReceipt} from a receipt stanza
     * whose {@code <participants>} child has no {@code message_id}
     * attribute.
     *
     * <p>By-type aggregation means every {@code <user>} child shares the
     * parent stanza's ack type and the external id read from the
     * {@code key} attribute of the {@code <participants>} child.
     *
     * @implNote
     * This implementation mirrors WA Web's {@code d(e, t)} helper inside
     * {@code WAWebHandleMsgReceiptParser}. WA Web swallows individual
     * malformed {@code <user>} entries through a {@code try / return null}
     * pattern and filters them out; Cobalt drops them implicitly by
     * skipping the iteration when the {@code jid} attribute is missing.
     *
     * @param id               the {@code id} attribute already extracted
     *                         by {@link #parseReceipt(Stanza)}
     * @param from             the {@code from} attribute already extracted
     *                         by {@link #parseReceipt(Stanza)}
     * @param recipient        the {@code recipient} attribute, or
     *                         {@code null}
     * @param ack              the parent {@link ReceiptAck}
     * @param ackString        the raw parent {@code type} attribute
     * @param participantsStanza the {@code <participants>} child
     * @param offline          {@code true} when the stanza carried the
     *                         {@code offline} attribute
     * @return the parsed {@link AggregatedByTypeReceipt}, or {@code null}
     *         when the {@code <participants>} child has no {@code key}
     *         attribute
     */
    private ParsedReceipt parseAggregatedByType(
            String id,
            Jid from,
            Jid recipient,
            ReceiptAck ack,
            String ackString,
            Stanza participantsStanza,
            boolean offline
    ) {
        var externalId = participantsStanza.getAttributeAsString("key", null);
        if (externalId == null) {
            return null;
        }

        var receipts = new ArrayList<ParticipantReceipt>();
        for (var userNode : participantsStanza.getChildren("user")) {
            var participantJid = userNode.getAttributeAsJid("jid").orElse(null);
            if (participantJid == null) {
                continue;
            }
            var participantPn = userNode.getAttributeAsJid("participant_pn").orElse(null);
            var participantUsername = userNode.getAttributeAsString("participant_username", null);
            receipts.add(new ParticipantReceipt(
                    participantJid,
                    participantPn,
                    participantUsername,
                    attributeInstant(userNode, "t"),
                    ack,
                    ackString
            ));
        }

        return new AggregatedByTypeReceipt(id, from, recipient, null, null, ack, ackString, externalId, List.copyOf(receipts), offline);
    }

    /**
     * Builds an {@link AggregatedByMessageReceipt} from a receipt stanza
     * whose {@code <participants>} child carries a {@code message_id}
     * attribute.
     *
     * <p>By-message aggregation means each {@code <user>} child has its own
     * ack type; the external message id is read from the
     * {@code message_id} attribute of the {@code <participants>} child.
     *
     * @implNote
     * This implementation mirrors WA Web's {@code m(t, n)} helper inside
     * {@code WAWebHandleMsgReceiptParser}. The parent-level ack is
     * forced to {@link ReceiptAck#RECEIVED} because the per-user ack
     * supersedes it; WA Web does not bother carrying a parent ack on
     * this variant.
     *
     * @param id               the {@code id} attribute already extracted
     *                         by {@link #parseReceipt(Stanza)}
     * @param from             the {@code from} attribute already extracted
     *                         by {@link #parseReceipt(Stanza)}
     * @param recipient        the {@code recipient} attribute, or
     *                         {@code null}
     * @param ackString        the raw parent {@code type} attribute
     * @param participantsStanza the {@code <participants>} child
     * @param offline          {@code true} when the stanza carried the
     *                         {@code offline} attribute
     * @return the parsed {@link AggregatedByMessageReceipt}, or
     *         {@code null} when the {@code <participants>} child has no
     *         {@code message_id} attribute
     */
    private ParsedReceipt parseAggregatedByMessage(
            String id,
            Jid from,
            Jid recipient,
            String ackString,
            Stanza participantsStanza,
            boolean offline
    ) {
        var externalId = participantsStanza.getAttributeAsString("message_id", null);
        if (externalId == null) {
            return null;
        }

        var receipts = new ArrayList<ParticipantReceipt>();
        for (var userNode : participantsStanza.getChildren("user")) {
            var participantJid = userNode.getAttributeAsJid("jid").orElse(null);
            if (participantJid == null) {
                continue;
            }
            var childAckString = userNode.getAttributeAsString("type", null);
            var participantPn = userNode.getAttributeAsJid("participant_pn").orElse(null);
            var participantUsername = userNode.getAttributeAsString("participant_username", null);
            receipts.add(new ParticipantReceipt(
                    participantJid,
                    participantPn,
                    participantUsername,
                    attributeInstant(userNode, "t"),
                    ReceiptAck.fromType(childAckString),
                    childAckString
            ));
        }

        return new AggregatedByMessageReceipt(
                id,
                from,
                recipient,
                null,
                null,
                ReceiptAck.RECEIVED,
                ackString,
                externalId,
                List.copyOf(receipts),
                offline
        );
    }

    /**
     * Returns the {@link Instant} parsed from the {@code key}-named
     * attribute of {@code stanza}, or {@code null} when the attribute is
     * missing or non-numeric.
     *
     * <p>The per-class parsers use it to decode the {@code t} attribute on
     * receipt stanzas and {@code <user>} children, which carries the
     * receipt event time as a unix epoch second.
     *
     * @implNote
     * This implementation mirrors WA Web's {@code attrTime} helper used
     * inside {@code WAWebHandleMsgReceiptParser}, which reads the
     * attribute as a number and converts it through
     * {@code WATimeUtils.castToUnixTime}.
     *
     * @param stanza the stanza to read from
     * @param key  the attribute name
     * @return the parsed {@link Instant}, or {@code null} when the
     *         attribute is missing or non-numeric
     */
    private static Instant attributeInstant(Stanza stanza, String key) {
        return stanza.getAttributeAsLong(key)
                .stream()
                .mapToObj(Instant::ofEpochSecond)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the user-level JID to associate with a receipt event,
     * choosing between {@code participant}, {@code recipient} and
     * {@code from} in that priority order.
     *
     * <p>The resolved JID selects which {@link MessageReceipt#userJid()} slot
     * the receipt event folds into. The fallback to {@code from} is gated on
     * the JID resolving to a user, LID, bot or hosted server because group
     * and broadcast JIDs are not valid receipt-user values.
     *
     * @implNote
     * This implementation matches the priority order WA Web uses in
     * {@code handleChatSimpleReceipt}: participant takes precedence
     * (it identifies the actual device that delivered), then recipient
     * (set when the sender wants to disambiguate against the receiver's
     * device), then the {@code from} JID itself for direct receipts.
     *
     * @param from        the {@code from} attribute of the receipt
     * @param participant the participant device JID, or {@code null}
     * @param recipient   the {@code recipient} attribute, or {@code null}
     * @return the resolved user-level JID, or {@code null} when no
     *         candidate matches the user/LID/bot/hosted check
     */
    private static Jid resolveReceiptUser(Jid from, Jid participant, Jid recipient) {
        if (participant != null) {
            return participant.toUserJid();
        }
        if (recipient != null) {
            return recipient.toUserJid();
        }
        if (from.hasUserServer() || from.hasLidServer() || from.hasBotServer() || from.hasHostedServer() || from.hasHostedLidServer()) {
            return from.toUserJid();
        }
        return null;
    }

    /**
     * Maps a {@link ReceiptAck} to the corresponding {@link MessageStatus}
     * milestone stored on {@link LinkedMessageInfo}.
     *
     * <p>The {@link ReceiptAck#CONTENT_GONE} and {@link ReceiptAck#INACTIVE}
     * acks map to {@link MessageStatus#ERROR} because both indicate a
     * server-side failure to deliver: {@code content_gone} means the
     * server-side blob expired before re-upload and {@code inactive}
     * means the recipient device is inactive.
     *
     * @implNote
     * This implementation mirrors the keyset of WA Web's
     * {@code WAWebAck.ACK} enum and the message-status promotion
     * performed inside {@code WAWebApiMessageInfoStore}: every non-read,
     * non-played, non-error ack maps to {@link MessageStatus#DELIVERED}.
     *
     * @param ack the {@link ReceiptAck}
     * @return the corresponding {@link MessageStatus}
     */
    private static MessageStatus mapStatus(ReceiptAck ack) {
        return switch (ack) {
            case READ -> MessageStatus.READ;
            case PLAYED -> MessageStatus.PLAYED;
            case CONTENT_GONE -> MessageStatus.ERROR;
            case INACTIVE -> MessageStatus.ERROR;
            default -> MessageStatus.DELIVERED;
        };
    }

    /**
     * Returns {@code true} when {@code ack} should contribute to the
     * {@link MessageReceipt#receiptTimestamp()} slot rather than the
     * read/played-specific slots.
     *
     * <p>Read and played receipts land on their own dedicated timestamp
     * slots so the delivery timestamp is preserved separately.
     *
     * @implNote
     * This implementation matches the WA Web split inside
     * {@code WAWebApiMessageInfoStore}, where delivery / sender / peer /
     * content-gone events all carry a generic receipt timestamp and only
     * {@code READ} and {@code PLAYED} get their own timestamp fields.
     *
     * @param ack the {@link ReceiptAck} to classify
     * @return {@code true} for every ack except {@link ReceiptAck#READ}
     *         and {@link ReceiptAck#PLAYED}
     */
    private static boolean isDeliveryLike(ReceiptAck ack) {
        return ack != ReceiptAck.READ && ack != ReceiptAck.PLAYED;
    }

    /**
     * Reconciles the cached per-message pending-receipt set with the
     * device that just confirmed delivery.
     *
     * <p>The returned {@link ReceiptUpdate} drives the
     * {@link MessageReceipt#pendingDeviceJid()} and
     * {@link MessageReceipt#deliveredDeviceJid()} slots so the embedder
     * can render per-device delivery status (the "delivered to N of M
     * devices" indicator on multi-device chats).
     *
     * @implNote
     * This implementation has no exact WA Web counterpart because WA Web
     * tracks pending-receipt-by-device inside the
     * {@code WAWebMessageReceiptBatcher} batch loop rather than as a
     * separate store accessor. The Cobalt store keeps the pending-device
     * set externally so the lookup here is a single
     * {@code findReceiptRecords} call. When the lookup yields zero
     * delivered devices, the store remains untouched; when it yields
     * one or more, the entire record is rewritten so the remaining
     * devices replace the previous set.
     *
     * @param messageId         the external message id
     * @param userJid           the user JID extracted by
     *                          {@link #resolveReceiptUser(Jid, Jid, Jid)}
     * @param participantDevice the device JID that produced the receipt,
     *                          or {@code null} when only the user-level
     *                          JID is known
     * @return the {@link ReceiptUpdate} carrying the delivered and the
     *         still-pending device lists
     */
    private ReceiptUpdate resolveDeviceUpdate(String messageId, Jid userJid, Jid participantDevice) {
        if (messageId == null || userJid == null) {
            return new ReceiptUpdate(List.of(), List.of());
        }

        var current = new LinkedHashSet<>(whatsapp.store().chatStore().findReceiptRecords(messageId));
        if (current.isEmpty()) {
            return new ReceiptUpdate(List.of(), List.of());
        }

        var delivered = new LinkedHashSet<Jid>();
        var remaining = new LinkedHashSet<Jid>();
        for (var device : current) {
            if (sameUser(device, userJid) && (participantDevice == null || Objects.equals(device, participantDevice))) {
                delivered.add(device);
            } else {
                remaining.add(device);
            }
        }

        if (!delivered.isEmpty()) {
            whatsapp.store().chatStore().removeReceiptRecords(messageId);
            if (!remaining.isEmpty()) {
                whatsapp.store().chatStore().createOrMergeReceiptRecords(messageId, remaining);
            }
        }

        return new ReceiptUpdate(
                List.copyOf(delivered),
                remaining.stream().filter(device -> sameUser(device, userJid)).toList()
        );
    }

    /**
     * Folds one per-user receipt event into the message's existing
     * {@link MessageReceipt} list, creating a fresh entry when none
     * exists for {@code userJid}.
     *
     * <p>Timestamps move forward only; a stale receipt arriving out of order
     * cannot rewind an existing read or played timestamp.
     *
     * @implNote
     * This implementation has no single WA Web counterpart because WA
     * Web manages per-user receipt state through the
     * {@code WAWebMessageReceiptBatcher} batch operations rather than a
     * direct list mutation. The pending-devices slot is overwritten
     * outright (it is the latest known snapshot) while the delivered
     * devices set is unioned across receipts so a per-device delivery
     * confirmation never erases prior confirmations.
     *
     * @param info             the message holding the existing receipts
     * @param userJid          the user-level JID whose receipt is being
     *                         updated
     * @param receiptTimestamp the delivery timestamp, or {@code null}
     * @param readTimestamp    the read timestamp, or {@code null}
     * @param playedTimestamp  the played timestamp, or {@code null}
     * @param pendingDevices   the devices still pending delivery
     * @param deliveredDevices the devices that have just confirmed
     *                         delivery
     * @return the updated immutable {@link MessageReceipt} list
     */
    private List<MessageReceipt> mergeReceipt(
            LinkedMessageInfo info,
            Jid userJid,
            Instant receiptTimestamp,
            Instant readTimestamp,
            Instant playedTimestamp,
            List<Jid> pendingDevices,
            List<Jid> deliveredDevices
    ) {
        if (userJid == null) {
            return info.receipts();
        }

        var receipts = new ArrayList<>(info.receipts());
        MessageReceipt target = null;
        for (var receipt : receipts) {
            if (sameUser(receipt.userJid(), userJid)) {
                target = receipt;
                break;
            }
        }

        if (target == null) {
            target = new MessageReceiptBuilder()
                    .userJid(userJid)
                    .receiptTimestamp(null)
                    .readTimestamp(null)
                    .playedTimestamp(null)
                    .pendingDeviceJid(List.of())
                    .deliveredDeviceJid(List.of())
                    .build();
            receipts.add(target);
        }

        if (receiptTimestamp != null && target.receiptTimestamp().map(receiptTimestamp::isAfter).orElse(true)) {
            target.setReceiptTimestamp(receiptTimestamp);
        }
        if (readTimestamp != null && target.readTimestamp().map(readTimestamp::isAfter).orElse(true)) {
            target.setReadTimestamp(readTimestamp);
        }
        if (playedTimestamp != null && target.playedTimestamp().map(playedTimestamp::isAfter).orElse(true)) {
            target.setPlayedTimestamp(playedTimestamp);
        }
        target.setPendingDeviceJid(List.copyOf(pendingDevices));
        if (!deliveredDevices.isEmpty()) {
            var mergedDelivered = new LinkedHashSet<>(target.deliveredDeviceJid());
            mergedDelivered.addAll(deliveredDevices);
            target.setDeliveredDeviceJid(List.copyOf(mergedDelivered));
        }

        return List.copyOf(receipts);
    }

    /**
     * Reduces a current and an incoming {@link MessageStatus} to the
     * single value the message should now carry.
     *
     * <p>The per-message status is promoted monotonically: an existing
     * {@link MessageStatus#READ} is never down-ranked by a late-arriving
     * {@link MessageStatus#DELIVERED}, and a transient
     * {@link MessageStatus#ERROR} is replaced as soon as any non-error ack
     * arrives.
     *
     * @implNote
     * This implementation diverges from WA Web's
     * {@code WAWebApiMessageInfoStore} by using
     * {@link Enum#ordinal()} to pick the higher-priority value. The
     * {@link MessageStatus} enum is therefore declared in the
     * ascending-priority order callers depend on; reordering its
     * constants would silently break this reduction.
     *
     * @param current  the status currently stored on the message, or
     *                 {@code null}
     * @param incoming the status just derived from a receipt, or
     *                 {@code null}
     * @return the reduced {@link MessageStatus}
     */
    private MessageStatus mergeStatus(MessageStatus current, MessageStatus incoming) {
        if (current == null) {
            return incoming;
        }
        if (incoming == null) {
            return current;
        }
        if (current == MessageStatus.ERROR) {
            return incoming;
        }
        if (incoming == MessageStatus.ERROR) {
            return current;
        }
        return incoming.ordinal() > current.ordinal() ? incoming : current;
    }

    /**
     * Fans out an {@code onMessageStatus} notification to every registered
     * {@link LinkedWhatsAppClientListener}.
     *
     * @implNote
     * This implementation starts one virtual thread per listener so that
     * a blocking listener cannot stall the {@link NodeStreamService} dispatch
     * loop.
     *
     * @param info the message whose status has changed
     */
    private void notifyMessageStatus(LinkedMessageInfo info) {
        for (var listener : whatsapp.store().listeners()) {
            if (listener instanceof MessageStatusListener typed) {
                Thread.startVirtualThread(() -> typed.onMessageStatus(whatsapp, info));
            }
        }
    }

    /**
     * Returns {@code true} when {@code left} and {@code right} refer to
     * the same user, ignoring any device suffix.
     *
     * @implNote
     * This implementation compares the canonical user form returned by
     * {@link Jid#toUserJid()}, which collapses the device segment for
     * both PN and LID server JIDs.
     *
     * @param left  the first {@link Jid}, or {@code null}
     * @param right the second {@link Jid}, or {@code null}
     * @return {@code true} when both JIDs resolve to the same user;
     *         {@code false} when either is {@code null} or the user
     *         segments differ
     */
    private boolean sameUser(Jid left, Jid right) {
        return left != null
                && right != null
                && Objects.equals(left.toUserJid(), right.toUserJid());
    }

    /**
     * Returns {@code true} when the receipt stanza is a retry request.
     *
     * @implNote
     * This implementation accepts both {@code "retry"} and
     * {@code "enc_rekey_retry"} as WA Web's
     * {@code WAWebCommsHandleLoggedInStanza} does. The
     * {@code "voip_1x1_retry"} variant is not split off here because
     * {@link #processRetryRequest(Stanza)} drops it explicitly further
     * down the pipeline.
     *
     * @param stanza the receipt stanza
     * @return {@code true} when the {@code type} attribute is
     *         {@code "retry"} or {@code "enc_rekey_retry"}
     */
    private static boolean isRetryReceipt(Stanza stanza) {
        var type = stanza.getAttributeAsString("type", null);
        return "retry".equals(type) || "enc_rekey_retry".equals(type);
    }

    /**
     * Returns {@code true} when the raw {@code type} attribute string is a
     * recognized {@code RECEIPT_TYPES_TO_ACK} key in WA Web's
     * {@code WAWebHandleMsgReceiptParser}.
     *
     * <p>The result decides whether the raw ack string is safe to forward to
     * the
     * {@link com.github.auties00.cobalt.wire.wam.event.ReceiptStanzaReceiveEvent}
     * {@code receiptStanzaType} field. Unrecognized strings would skew the
     * WAM enum bucket counts on the server and are therefore filtered out.
     *
     * @implNote
     * This implementation enumerates the keyset of WA Web's module-local
     * {@code u} object inside {@code WAWebHandleMsgReceiptParser}:
     * {@code delivery}, {@code read}, {@code played}, {@code inactive},
     * {@code server-error}, {@code sender}, {@code read-self},
     * {@code played-self}, {@code peer_msg}.
     *
     * @param type the raw {@code type} attribute, or {@code null}
     * @return {@code true} when the string is a recognized
     *         {@code RECEIPT_TYPES_TO_ACK} key; {@code false} otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgReceiptParser",
            exports = "RECEIPT_TYPES_TO_ACK", adaptation = WhatsAppAdaptation.ADAPTED)
    private static boolean isReceiptTypeToAck(String type) {
        if (type == null) {
            return false;
        }
        return switch (type) {
            case "delivery", "read", "played", "inactive", "server-error",
                 "sender", "read-self", "played-self", "peer_msg" -> true;
            default -> false;
        };
    }

    /**
     * Carries the {@code (delivered, pending)} device split returned by
     * {@link #resolveDeviceUpdate(String, Jid, Jid)}.
     *
     * <p>The two lists feed the per-user
     * {@link MessageReceipt#deliveredDeviceJid()} and
     * {@link MessageReceipt#pendingDeviceJid()} slots.
     *
     * @param deliveredDevices the device JIDs that just confirmed
     *                         delivery
     * @param pendingDevices   the device JIDs still pending delivery
     */
    private record ReceiptUpdate(List<Jid> deliveredDevices, List<Jid> pendingDevices) {
    }

    /**
     * Sealed parent of the three parsed receipt shapes
     * {@link #parseReceipt(Stanza)} can return.
     *
     * <p>The variants are consumed by {@link #handle(Stanza)} for per-class
     * dispatch and by
     * {@link #commitReceiptMetric(ReceiptStanzaReceiveEventBuilder, ParsedReceipt)}
     * for the per-shape telemetry overrides.
     *
     * @implNote
     * This implementation seals the type so {@link #handle(Stanza)} can
     * exhaustively switch over the three variants; adding a new
     * receipt shape would require touching both the parser and the
     * dispatcher in lockstep.
     */
    private sealed interface ParsedReceipt extends ReceiptLike
            permits SimpleReceipt, AggregatedByTypeReceipt, AggregatedByMessageReceipt {
    }

    /**
     * Common accessors shared by every receipt-like payload, regardless
     * of whether it is parsed or upstream.
     *
     * <p>The shared view lets {@link #updateMessage} and
     * {@link #commitReceiptMetric(ReceiptStanzaReceiveEventBuilder, ParsedReceipt)}
     * read the five common fields without unpacking each per-class record
     * through a pattern match.
     *
     * @implNote
     * This implementation pulls only the five fields the downstream
     * helpers actually touch. The full per-class record fields stay
     * accessible through pattern matching when more detail is needed
     * (the parser-private {@code stanzaId}, {@code participantPn} and
     * {@code participantUsername} fields are intentionally excluded
     * because no caller consumes them).
     */
    private interface ReceiptLike {
        /**
         * Returns the {@code from} JID of the receipt, which identifies the
         * conversation the receipt applies to.
         *
         * @return the {@code from} JID
         */
        Jid from();

        /**
         * Returns the optional {@code recipient} attribute of the receipt.
         *
         * <p>This attribute is set on direct receipts when the sender wants
         * to disambiguate against a specific recipient device; it is absent
         * in the common case.
         *
         * @return the recipient JID, or {@code null}
         */
        Jid recipient();

        /**
         * Returns the {@code t} attribute of the receipt as an
         * {@link Instant}.
         *
         * <p>The value is the wall-clock time at which the receipt event was
         * produced; it is consumed as the timestamp for each per-user
         * {@link MessageReceipt} field.
         *
         * @return the event timestamp, or {@code null} when the attribute
         *         was absent
         */
        Instant timestamp();

        /**
         * Returns the raw {@code type} attribute string of the receipt.
         *
         * <p>The string is carried through to the WAM telemetry event by
         * {@link #commitReceiptMetric(ReceiptStanzaReceiveEventBuilder, ParsedReceipt)}.
         *
         * @return the raw ack string, or {@code null} when the attribute
         *         was absent
         */
        String ackString();

        /**
         * Returns {@code true} when the receipt was delivered via the
         * offline queue.
         *
         * <p>The flag lets
         * {@link #commitReceiptMetric(ReceiptStanzaReceiveEventBuilder, ParsedReceipt)}
         * skip the telemetry commit on offline replays so the metric does
         * not double-count.
         *
         * @return {@code true} when the {@code offline} attribute was
         *         present on the receipt stanza
         */
        boolean offline();
    }

    /**
     * A receipt with no {@code <participants>} child: a single
     * acknowledgement that may cover several external message ids.
     *
     * <p>Both 1:1 and per-participant group / broadcast receipts land in
     * this record. The {@link #bizInfo} field is set only when the
     * receipt carried a {@code <biz>} child.
     *
     * @param stanzaId            the {@code id} attribute of the receipt
     * @param from                the {@code from} JID of the receipt
     * @param participant         the participant device JID, or
     *                            {@code null}
     * @param participantPn       the participant's phone-server JID, or
     *                            {@code null}
     * @param participantUsername the participant's WhatsApp username, or
     *                            {@code null}
     * @param recipient           the {@code recipient} JID, or
     *                            {@code null}
     * @param timestamp           the event time parsed from {@code t}
     * @param ack                 the {@link ReceiptAck} resolved from
     *                            {@code type}
     * @param ackString           the raw {@code type} attribute
     * @param externalIds         every message id this receipt covers
     * @param offline             {@code true} when the stanza carried the
     *                            {@code offline} attribute
     * @param isLidBot            {@code true} when the participant is a
     *                            LID-server bot and the stanza carried
     *                            {@code is_lid="true"}
     * @param bizInfo             the {@link BizInfo} parsed from the
     *                            {@code <biz>} child, or {@code null}
     */
    private record SimpleReceipt(
            String stanzaId,
            Jid from,
            Jid participant,
            Jid participantPn,
            String participantUsername,
            Jid recipient,
            Instant timestamp,
            ReceiptAck ack,
            String ackString,
            List<String> externalIds,
            boolean offline,
            boolean isLidBot,
            BizInfo bizInfo
    ) implements ParsedReceipt {
    }

    /**
     * A receipt whose {@code <participants>} child has no
     * {@code message_id} attribute: every {@code <user>} child shares
     * the parent ack type.
     *
     * <p>The {@link #participant} and {@link #timestamp} fields are
     * structurally unused for this shape; they are kept so
     * {@link ReceiptLike} can be implemented uniformly across the three
     * parsed variants.
     *
     * @param stanzaId    the {@code id} attribute of the receipt
     * @param from        the {@code from} JID of the receipt
     * @param recipient   the {@code recipient} JID, or {@code null}
     * @param participant always {@code null} for this shape
     * @param timestamp   always {@code null} for this shape
     * @param ack         the parent {@link ReceiptAck} applied to every
     *                    {@code <user>} child
     * @param ackString   the raw parent {@code type} attribute
     * @param externalId  the external message id read from the
     *                    {@code key} attribute of the
     *                    {@code <participants>} child
     * @param receipts    the per-participant receipts
     * @param offline     {@code true} when the stanza carried the
     *                    {@code offline} attribute
     */
    private record AggregatedByTypeReceipt(
            String stanzaId,
            Jid from,
            Jid recipient,
            Jid participant,
            Instant timestamp,
            ReceiptAck ack,
            String ackString,
            String externalId,
            List<ParticipantReceipt> receipts,
            boolean offline
    ) implements ParsedReceipt {
    }

    /**
     * A receipt whose {@code <participants>} child carries a
     * {@code message_id} attribute: each {@code <user>} child has its
     * own ack type.
     *
     * <p>The {@link #participant} and {@link #timestamp} fields are
     * structurally unused for this shape; they are kept so
     * {@link ReceiptLike} can be implemented uniformly. The
     * {@link #ack} slot always carries {@link ReceiptAck#RECEIVED}
     * because the per-user ack supersedes any parent value.
     *
     * @param stanzaId    the {@code id} attribute of the receipt
     * @param from        the {@code from} JID of the receipt
     * @param recipient   the {@code recipient} JID, or {@code null}
     * @param participant always {@code null} for this shape
     * @param timestamp   always {@code null} for this shape
     * @param ack         always {@link ReceiptAck#RECEIVED} for this
     *                    shape
     * @param ackString   the raw parent {@code type} attribute
     * @param externalId  the external message id read from the
     *                    {@code message_id} attribute of the
     *                    {@code <participants>} child
     * @param receipts    the per-participant receipts, each with its own
     *                    ack
     * @param offline     {@code true} when the stanza carried the
     *                    {@code offline} attribute
     */
    private record AggregatedByMessageReceipt(
            String stanzaId,
            Jid from,
            Jid recipient,
            Jid participant,
            Instant timestamp,
            ReceiptAck ack,
            String ackString,
            String externalId,
            List<ParticipantReceipt> receipts,
            boolean offline
    ) implements ParsedReceipt {
    }

    /**
     * One {@code <user>} child of an aggregated receipt.
     *
     * <p>Each entry drives one per-participant {@link #updateMessage} call
     * during deaggregation.
     *
     * @param participant         the device JID of the participant
     * @param participantPn       the participant's phone-server JID, or
     *                            {@code null}
     * @param participantUsername the participant's WhatsApp username, or
     *                            {@code null}
     * @param timestamp           the {@code t} attribute of the
     *                            {@code <user>} child
     * @param ack                 the {@link ReceiptAck} for this
     *                            participant (parent ack for the by-type
     *                            shape, per-user ack for the by-message
     *                            shape)
     * @param ackString           the raw {@code type} attribute, when
     *                            applicable
     */
    private record ParticipantReceipt(
            Jid participant,
            Jid participantPn,
            String participantUsername,
            Instant timestamp,
            ReceiptAck ack,
            String ackString
    ) {
    }

    /**
     * The business-metadata payload optionally carried inside the
     * {@code <biz>} child of a {@link SimpleReceipt}.
     *
     * <p>The three fields tag the receipt for downstream
     * {@code WAWebApiContact} privacy-mode reconciliation on WhatsApp
     * Business surfaces. Cobalt parses the payload but does not yet surface
     * it through a public listener.
     *
     * @param actualActors  the {@code ActualActors} enum value
     * @param hostStorage   the {@code HostStorage} enum value
     * @param privacyModeTs the privacy-mode change timestamp in seconds
     *                      since the epoch
     */
    private record BizInfo(
            int actualActors,
            int hostStorage,
            int privacyModeTs
    ) {
    }

    /**
     * The receipt-acknowledgement classifier used internally to drive
     * {@link #mapStatus(ReceiptAck)},
     * {@link #isDeliveryLike(ReceiptAck)} and the per-class dispatch.
     *
     * <p>Each constant corresponds to a value of WA Web's
     * {@code WAWebAck.ACK} enum.
     *
     * @implNote
     * This implementation diverges from WA Web's {@code ACK} enum in two
     * places. WA Web includes an {@code ACK.SENT = 1} value used for the
     * outbound-server-side feature-incapable error override; Cobalt
     * folds {@code SENT} into {@link #RECEIVED} because there is no
     * receipt-level distinction between the two in the local store. WA
     * Web does not carry {@code ACK.PEER} as a positive ack; Cobalt
     * uses it as a sentinel that diverts the simple-receipt path to
     * the peer-message branch before any store mutation runs.
     */
    private enum ReceiptAck {
        /**
         * Delivery-level acknowledgement, mapped to WA Web
         * {@code ACK.RECEIVED = 2}.
         */
        RECEIVED,

        /**
         * Read acknowledgement, mapped to WA Web {@code ACK.READ = 3}.
         */
        READ,

        /**
         * Played acknowledgement for audio or video media, mapped to WA
         * Web {@code ACK.PLAYED = 4}.
         */
        PLAYED,

        /**
         * Content-gone (server-error) acknowledgement, mapped to WA Web
         * {@code ACK.CONTENT_GONE = -3}.
         */
        CONTENT_GONE,

        /**
         * Peer-message acknowledgement, mapped to WA Web
         * {@code ACK.PEER = 5}.
         */
        PEER,

        /**
         * Inactive-device acknowledgement, mapped to WA Web
         * {@code ACK.INACTIVE = -6}.
         */
        INACTIVE;

        /**
         * Resolves a raw {@code type} attribute string to the
         * corresponding {@link ReceiptAck}.
         *
         * <p>Both {@code null} and unknown strings fall back to
         * {@link #RECEIVED}, matching WA Web's
         * {@code t!=null ? t : ACK.RECEIVED} default after the
         * {@code RECEIPT_TYPES_TO_ACK} lookup.
         *
         * @implNote
         * This implementation enumerates WA Web's
         * {@code RECEIPT_TYPES_TO_ACK} map verbatim:
         * {@code delivery, sender -> RECEIVED};
         * {@code read, read-self -> READ};
         * {@code played, played-self -> PLAYED};
         * {@code server-error -> CONTENT_GONE};
         * {@code peer_msg -> PEER};
         * {@code inactive -> INACTIVE}.
         *
         * @param type the raw {@code type} attribute, or {@code null}
         * @return the resolved {@link ReceiptAck}
         */
        @WhatsAppWebExport(moduleName = "WAWebHandleMsgReceiptParser",
                exports = "RECEIPT_TYPES_TO_ACK", adaptation = WhatsAppAdaptation.ADAPTED)
        private static ReceiptAck fromType(String type) {
            if (type == null || "delivery".equals(type) || "sender".equals(type)) {
                return RECEIVED;
            }
            return switch (type) {
                case "read", "read-self" -> READ;
                case "played", "played-self" -> PLAYED;
                case "server-error" -> CONTENT_GONE;
                case "peer_msg" -> PEER;
                case "inactive" -> INACTIVE;
                default -> RECEIVED;
            };
        }
    }
}
