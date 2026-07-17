package com.github.auties00.cobalt.message.send;

import com.github.auties00.cobalt.ack.AckParser;
import com.github.auties00.cobalt.ack.AckResult;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.exception.linked.WhatsAppMessageException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryptedPayload;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.message.send.senderkey.SenderKeyDistribution;
import com.github.auties00.cobalt.message.send.stanza.MetaStanza;
import com.github.auties00.cobalt.message.send.stanza.ParticipantsStanza;
import com.github.auties00.cobalt.message.send.stanza.ReportingStanza;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.BroadcastListParticipant;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerSpec;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.PrekeysDepletionEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.PrekeysFetchContext;
import com.github.auties00.cobalt.wire.wam.type.SizeBucket;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Publishes a message to a business broadcast list addressed via
 * {@code <id>@broadcast}.
 *
 * <p>A broadcast list is a client-only saved audience: the roster lives on
 * {@link com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastList} and
 * never round-trips through server-side group metadata. The wire shape is
 * otherwise identical to a status broadcast: a sender-key (SKMSG) ciphertext
 * targeted at the synthetic broadcast JID, accompanied by per-recipient
 * {@code <to>} children that either carry a fresh sender-key distribution (for
 * devices that do not yet hold the key) or just reference the device (for
 * devices that already hold a previously distributed key).
 *
 * <p>Embedders reach this sender through
 * {@link LinkedWhatsAppClient#sendBroadcast(com.github.auties00.cobalt.wire.core.jid.JidProvider, LinkedMessageContainer)};
 * the routing on
 * {@link MessageSendingService#send(com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo)}
 * keys on the broadcast-server JID.
 *
 * @implNote
 * This implementation mirrors {@link StatusMessageSender}'s SKMSG flow verbatim
 * with two divergences: the recipient list is resolved from the local
 * {@link com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastList} roster
 * (rather than from the user's status privacy preferences), and the
 * {@code <meta>} child is built without a {@code status_setting} attribute
 * (which is meaningful only for status broadcasts). Cobalt does not create
 * per-recipient {@link ChatMessageInfo} clones the way WA Web's
 * {@code buildBroadcastMsgModelsFromMsgData} does: per-recipient delivery state
 * is carried on the existing
 * {@link com.github.auties00.cobalt.wire.linked.message.MessageReceipt} records created
 * via
 * {@link LinkedWhatsAppStore#createOrMergeReceiptRecords(String, java.util.Collection)},
 * which collapses WA Web's per-clone {@code ackLevel} into the Cobalt-uniform
 * receipt model.
 */
@WhatsAppWebModule(moduleName = "WAWebSendBroadcastMsgAction")
@WhatsAppWebModule(moduleName = "WAWebEncryptAndSendBroadcastMsg")
@WhatsAppWebModule(moduleName = "WAWebBuildBroadcastMsgModels")
@WhatsAppWebModule(moduleName = "WAWebBatchUpdateBroadcastAck")
final class BroadcastMessageSender extends MessageSender<ChatMessageInfo> {
    /**
     * The logger for {@link BroadcastMessageSender}.
     */
    private static final System.Logger LOGGER = Log.get(BroadcastMessageSender.class);

    /**
     * Performs the SKMSG group encryption and per-device sender-key
     * distribution.
     */
    private final MessageEncryption encryption;

    /**
     * Resolves the per-device fanout for the broadcast-list recipients via
     * {@link DeviceService#getBroadcastFanout(Jid, java.util.Collection)}.
     */
    private final DeviceService deviceService;

    /**
     * Encrypts the per-device sender-key distribution payloads.
     */
    private final SenderKeyDistribution senderKeyDistribution;

    /**
     * Builds the {@code <meta>} child carried alongside the SKMSG payload.
     */
    private final MetaStanza metaStanza;

    /**
     * Builds the {@code <reporting>} child carried alongside the SKMSG payload.
     */
    private final ReportingStanza reportingStanza;

    /**
     * Constructs a {@link BroadcastMessageSender} bound to the supplied
     * dependencies.
     *
     * <p>Constructed once by {@link MessageSendingService}; embedders never
     * instantiate directly.
     *
     * @param client                the {@link LinkedWhatsAppClient} used to dispatch
     *                              stanzas
     * @param encryption            the {@link MessageEncryption} service
     * @param deviceService         the {@link DeviceService} used to resolve the
     *                              per-device fanout
     * @param abPropsService        the {@link ABPropsService} consulted by the
     *                              base sender
     * @param senderKeyDistribution the {@link SenderKeyDistribution} service
     * @param metaStanza            the {@link MetaStanza} builder
     * @param reportingStanza       the {@link ReportingStanza} builder
     * @param wamService            the {@link WamService} forwarded to the base
     *                              sender
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendBroadcastMsgAction",
            exports = "sendBroadcastMsgAction", adaptation = WhatsAppAdaptation.ADAPTED)
    BroadcastMessageSender(
            LinkedWhatsAppClient client,
            MessageEncryption encryption,
            DeviceService deviceService,
            ABPropsService abPropsService,
            SenderKeyDistribution senderKeyDistribution,
            MetaStanza metaStanza,
            ReportingStanza reportingStanza,
            WamService wamService
    ) {
        super(client, abPropsService, wamService);
        this.encryption = Objects.requireNonNull(encryption, "encryption");
        this.deviceService = Objects.requireNonNull(deviceService, "deviceService");
        this.senderKeyDistribution = Objects.requireNonNull(senderKeyDistribution, "senderKeyDistribution");
        this.metaStanza = Objects.requireNonNull(metaStanza, "metaStanza");
        this.reportingStanza = Objects.requireNonNull(reportingStanza, "reportingStanza");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Encrypts the payload with the broadcast-list sender key, distributes
     * the sender key to recipient devices that do not yet hold it, and
     * dispatches the SKMSG stanza to {@code broadcastJid}. After a successful
     * server ack, the per-device receipt records created at the start of the
     * send carry the SENT level for every recipient device, which is the Cobalt
     * counterpart of WA Web's {@code batchUpdateAckForBroadcastMessages}
     * clone-level fanout.
     *
     * @implNote
     * This implementation looks up the recipient roster from
     * {@link LinkedWhatsAppBusinessStore#findBusinessBroadcastList(String)};
     * a missing list surfaces as
     * {@link WhatsAppMessageException.Send.InvalidRecipient}, mirroring WA Web's
     * {@code NO_FANOUT_KEYS} sentinel that
     * {@code batchUpdateAckForBroadcastMessages} would otherwise surface after
     * the send completes. The recipient's
     * {@link BroadcastListParticipant#lidJid()} is preferred over
     * {@link BroadcastListParticipant#pnJid()} because the broadcast list roster
     * is already LID-indexed by {@code BusinessBroadcastAssociationHandler}.
     */
    @WhatsAppWebExport(moduleName = "WAWebSendBroadcastMsgAction",
            exports = "sendBroadcastMsgAction", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebEncryptAndSendBroadcastMsg",
            exports = "encryptAndSendBroadcastMsg", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public AckResult doSend(Jid broadcastJid, ChatMessageInfo messageInfo) {
        var container = messageInfo.message();
        var selfJid = requireSelfJid();
        var messageId = messageInfo.key().id().orElseThrow();

        var recipients = resolveRecipients(broadcastJid);
        var allDevices = deviceService.getBroadcastFanout(broadcastJid, recipients);
        var phash = deviceService.computeGroupPhash(allDevices, selfLidOrPn(), false, false);

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "broadcast send: {0} recipients, {1} fanout device(s) for {2}",
                    recipients.size(), allDevices.size(), broadcastJid);
        }

        store.chatStore().createOrMergeReceiptRecords(messageId, allDevices);

        var skDistribDevices = new ArrayList<Jid>();
        var skExistingDevices = new ArrayList<Jid>();
        for (var device : allDevices) {
            if (store.signalStore().hasSenderKeyDistributed(broadcastJid, device)) {
                skExistingDevices.add(device);
            } else {
                skDistribDevices.add(device);
            }
        }

        var rotateKey = store.signalStore().clearKeyRotation(broadcastJid);
        if (rotateKey) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "broadcast send: rotating sender key for {0}", broadcastJid);
            }
            encryption.rotateSenderKey(broadcastJid, selfJid);
            skDistribDevices.addAll(skExistingDevices);
            skExistingDevices.clear();
        }

        var plaintext = LinkedMessageContainerSpec.encode(container);
        var skmsgPayload = encryption.encryptForGroup(broadcastJid, selfJid, plaintext);
        var senderKeyBytes = encryption.getSenderKeyBytes(broadcastJid, selfJid);

        List<MessageEncryptedPayload> skDistPayloads;
        if (skDistribDevices.isEmpty()) {
            skDistPayloads = List.of();
        } else {
            var depletedPrekeyCount = deviceService.ensureSessions(skDistribDevices);
            emitPrekeysDepletionEvents(depletedPrekeyCount, allDevices.size());
            skDistPayloads = senderKeyDistribution.encrypt(broadcastJid, senderKeyBytes, skDistribDevices);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "broadcast send: distributing sender key to {0} device(s) for {1}",
                        skDistribDevices.size(), broadcastJid);
            }
        }

        var participantsNode = buildParticipantsNode(skDistPayloads, skExistingDevices);
        store.signalStore().updateIdentityRange(allDevices);

        var skmsgEncNode = new StanzaBuilder()
                .description("enc")
                .attribute("v", String.valueOf(MessageEncryption.CIPHERTEXT_VERSION))
                .attribute("type", MessageEncryptionType.SKMSG.protocolValue())
                .attribute("mediatype", resolveMediaType(container))
                .content(skmsgPayload.ciphertext())
                .build();

        var identityNode = ParticipantsStanza.requiresIdentityNode(skDistPayloads)
                ? buildIdentityNode() : null;
        var metaNode = metaStanza.buildChat(broadcastJid, container, null);
        var reportingNode = reportingStanza.build(messageInfo, selfJid, broadcastJid);

        var stanza = new StanzaBuilder()
                .description("message")
                .attribute("id", messageId)
                .attribute("to", broadcastJid)
                .attribute("type", resolveStanzaType(container))
                .attribute("edit", resolveEditAttribute(container))
                .attribute("phash", phash)
                .content(
                        participantsNode,
                        skmsgEncNode,
                        identityNode,
                        metaNode,
                        reportingNode
                );

        flushStore();
        var ackNode = client.sendNode(stanza);
        var ack = AckParser.parse(ackNode);

        if (ack.isSuccess()) {
            for (var device : skDistribDevices) {
                store.signalStore().markSenderKeyDistributed(broadcastJid, device);
            }
            if (allDevices.isEmpty()) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "broadcast send: no fanout devices for {0}", broadcastJid);
                }
            }
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "broadcast send: ack success for {0} id={1}", broadcastJid, messageId);
            }
        } else if (Log.WARNING) {
            LOGGER.log(Level.WARNING, "broadcast send: ack failed for {0} id={1} error={2}",
                    broadcastJid, messageId, ack.error().orElse(-1));
        }

        return ack;
    }

    /**
     * Resolves the recipient user JIDs for the given broadcast list JID by
     * looking up the local
     * {@link com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastList}
     * roster.
     *
     * <p>The broadcast list is the source of truth for who receives the
     * message, matching WA Web's {@code buildBroadcastMsgModelsFromMsgData}
     * recipient input. The returned JIDs are LID-form when the participant
     * carries an LID and PN-form otherwise, matching the addressing mode the
     * rest of the sender chain expects.
     *
     * @implNote
     * This implementation throws
     * {@link WhatsAppMessageException.Send.InvalidRecipient} when the list is
     * unknown locally; WA Web's {@code batchUpdateAckForBroadcastMessages}
     * reports the same condition as {@code NO_FANOUT_KEYS} after the send, but
     * Cobalt surfaces it as a precondition failure before the wire write so the
     * caller receives a typed error instead of a silent log entry.
     *
     * @param broadcastJid the broadcast list JID ({@code <id>@broadcast})
     * @return the recipient user {@link Jid}s
     * @throws WhatsAppMessageException.Send.InvalidRecipient if the broadcast
     *                                                        list is not in the
     *                                                        local store
     */
    @WhatsAppWebExport(moduleName = "WAWebBuildBroadcastMsgModels",
            exports = "buildBroadcastMsgModelsFromMsgData",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebBatchUpdateBroadcastAck",
            exports = "batchUpdateAckForBroadcastMessages",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private List<Jid> resolveRecipients(Jid broadcastJid) {
        var listId = broadcastJid.user();
        var list = store.businessStore().findBusinessBroadcastList(listId)
                .orElseThrow(() -> {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "broadcast send: list {0} not found in local store", listId);
                    }
                    return new WhatsAppMessageException.Send.InvalidRecipient(
                            broadcastJid,
                            "Broadcast list " + listId + " is not in the local store");
                });
        var participants = list.participants();
        if (participants.isEmpty()) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "broadcast send: list {0} has no participants", listId);
            }
            throw new WhatsAppMessageException.Send.InvalidRecipient(
                    broadcastJid,
                    "Broadcast list " + listId + " has no participants");
        }
        var recipients = new ArrayList<Jid>(participants.size());
        for (var participant : participants) {
            var recipient = participant.lidJid();
            if (recipient == null) {
                recipient = participant.pnJid().orElse(null);
            }
            if (recipient != null) {
                recipients.add(recipient);
            }
        }
        return recipients;
    }

    /**
     * Assembles the {@code <participants>} child carrying per-device sender-key
     * distribution entries and reference-only entries for devices that already
     * hold the key.
     *
     * <p>Returns {@code null} when both lists are empty so the caller drops the
     * child entirely, matching WA Web's {@code encryptAndSendBroadcastMsg} which
     * only emits the {@code <participants>} wrap when at least one device entry
     * exists.
     *
     * @param skDistPayloads    the per-device sender-key distribution payloads
     *                          for devices that did not hold the key
     * @param skExistingDevices the device JIDs that already hold the sender key
     * @return the assembled {@code <participants>} {@link Stanza}, or {@code null}
     *         when no entries apply
     */
    @WhatsAppWebExport(moduleName = "WAWebEncryptAndSendBroadcastMsg",
            exports = "genBroadcastMessageBody", adaptation = WhatsAppAdaptation.ADAPTED)
    private Stanza buildParticipantsNode(List<MessageEncryptedPayload> skDistPayloads,
                                         List<Jid> skExistingDevices) {
        var children = new ArrayList<Stanza>(skDistPayloads.size() + skExistingDevices.size());
        for (var payload : skDistPayloads) {
            if (payload.recipientJid() == null) {
                continue;
            }
            var encNode = new StanzaBuilder()
                    .description("enc")
                    .attribute("v", String.valueOf(MessageEncryption.CIPHERTEXT_VERSION))
                    .attribute("type", payload.type().protocolValue())
                    .content(payload.ciphertext())
                    .build();
            children.add(new StanzaBuilder()
                    .description("to")
                    .attribute("jid", payload.recipientJid())
                    .content(encNode)
                    .build());
        }
        for (var device : skExistingDevices) {
            children.add(new StanzaBuilder()
                    .description("to")
                    .attribute("jid", device)
                    .build());
        }
        if (children.isEmpty()) {
            return null;
        }
        return new StanzaBuilder()
                .description("participants")
                .content(children)
                .build();
    }

    /**
     * Commits one
     * {@link com.github.auties00.cobalt.wire.wam.event.PrekeysDepletionEvent} per
     * depleted one-time pre-key reported by the last
     * {@link DeviceService#ensureSessions(java.util.Collection)} call.
     *
     * <p>No-op when {@code depletedPrekeyCount} is not positive. The recipient
     * device count drives the {@code deviceSizeBucket} slot on the emitted
     * event; {@code messageType} is fixed to {@link MessageType#BROADCAST} for
     * broadcast-list sends.
     *
     * @param depletedPrekeyCount the number of depleted one-time pre-keys
     * @param deviceCount         the device count used for the
     *                            {@code deviceSizeBucket} classification, or
     *                            {@code null} to omit the bucket
     */
    @WhatsAppWebExport(moduleName = "WAWebPostPrekeysDepletionMetric",
            exports = "maybePostPrekeysDepletionMetric",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitPrekeysDepletionEvents(int depletedPrekeyCount, Integer deviceCount) {
        if (depletedPrekeyCount <= 0) {
            return;
        }
        var bucket = deviceCount == null ? null : numberToSizeBucket(deviceCount);
        for (var i = 0; i < depletedPrekeyCount; i++) {
            wamService.commit(new PrekeysDepletionEventBuilder()
                    .prekeysFetchReason(PrekeysFetchContext.SEND_MESSAGE)
                    .messageType(MessageType.BROADCAST)
                    .deviceSizeBucket(bucket)
                    .build());
        }
    }

    /**
     * Maps a fanout device count to the matching {@link SizeBucket} carried by
     * the {@code deviceSizeBucket} WAM property.
     *
     * <p>Buckets are exclusive upper bounds: {@code count=31} returns
     * {@link SizeBucket#LT32}, {@code count=1024} returns
     * {@link SizeBucket#LT1500}, and any {@code count >= 5000} returns
     * {@link SizeBucket#LARGEST_BUCKET}.
     *
     * @param count the device count to classify
     * @return the matching {@link SizeBucket}; never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebWamNumberToSizeBucket",
            exports = "default",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static SizeBucket numberToSizeBucket(int count) {
        if (count < 32) return SizeBucket.LT32;
        if (count < 64) return SizeBucket.LT64;
        if (count < 128) return SizeBucket.LT128;
        if (count < 256) return SizeBucket.LT256;
        if (count < 512) return SizeBucket.LT512;
        if (count < 1024) return SizeBucket.LT1024;
        if (count < 1500) return SizeBucket.LT1500;
        if (count < 2000) return SizeBucket.LT2000;
        if (count < 2500) return SizeBucket.LT2500;
        if (count < 3000) return SizeBucket.LT3000;
        if (count < 3500) return SizeBucket.LT3500;
        if (count < 4000) return SizeBucket.LT4000;
        if (count < 4500) return SizeBucket.LT4500;
        if (count < 5000) return SizeBucket.LT5000;
        return SizeBucket.LARGEST_BUCKET;
    }
}
