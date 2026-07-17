package com.github.auties00.cobalt.message.send;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.exception.linked.WhatsAppMessageException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.ack.AckParser;
import com.github.auties00.cobalt.ack.AckResult;
import com.github.auties00.cobalt.ack.MessageAck;
import com.github.auties00.cobalt.ack.NackReason;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryptedPayload;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.message.send.senderkey.SenderKeyDistribution;
import com.github.auties00.cobalt.message.send.stanza.*;
import com.github.auties00.cobalt.message.send.token.ContentBindingToken;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageContextInfoBuilder;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMetadata;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadata;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupParticipant;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupParticipantBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerSpec;
import com.github.auties00.cobalt.wire.linked.message.event.EncEventResponseMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollUpdateMessage;
import com.github.auties00.cobalt.wire.linked.message.security.EncCommentMessage;
import com.github.auties00.cobalt.wire.linked.message.security.EncReactionMessage;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.AddressingModeMismatchEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MdDeviceSyncAckEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MdGroupParticipantMissAckEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PrekeysDepletionEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.AddressingMode;
import com.github.auties00.cobalt.wire.wam.type.ClientGroupSizeBucket;
import com.github.auties00.cobalt.wire.wam.type.E2eDestination;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.MismatchOriginType;
import com.github.auties00.cobalt.wire.wam.type.PrekeysFetchContext;
import com.github.auties00.cobalt.wire.wam.type.SizeBucket;
import com.github.auties00.cobalt.wire.wam.type.TypeOfGroupEnum;

import java.lang.System.Logger.Level;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Sends messages to group and community chats.
 *
 * <p>The default path uses sender-key (SKMSG) encryption: the payload is
 * encrypted once with the group sender key and a separate sender-key
 * distribution payload is encrypted per device for participants that do not yet
 * hold the key. When the server reports a phash mismatch the delta devices
 * receive the same message through the per-device group-direct fanout. When the
 * server reports a stale addressing mode the group is migrated (LID to PN or
 * vice versa) and the send is retried from scratch.
 */
@WhatsAppWebModule(moduleName = "WAWebSendGroupMsgJob")
@WhatsAppWebModule(moduleName = "WAWebSendGroupSkmsgJob")
@WhatsAppWebModule(moduleName = "WAWebSendGroupDirectJob")
@WhatsAppWebModule(moduleName = "WAWebSendGroupKeyDistributionMsgJob")
final class GroupMessageSender extends MessageSender<ChatMessageInfo> {
    /**
     * The logger for {@link GroupMessageSender}.
     */
    private static final System.Logger LOGGER = Log.get(GroupMessageSender.class);

    /**
     * Performs both the SKMSG and the per-device fanout encryption paths.
     */
    private final MessageEncryption encryption;

    /**
     * Resolves the group fanout and manages Signal sessions.
     */
    private final DeviceService deviceService;

    /**
     * Encrypts the per-device sender-key distribution payloads.
     */
    private final SenderKeyDistribution senderKeyDistribution;

    /**
     * Builds the {@code <bot>} child.
     */
    private final BotStanza botStanza;

    /**
     * Builds the payment-native-flow business child.
     */
    private final BizStanza bizStanza;

    /**
     * Builds the {@code <meta>} child.
     */
    private final MetaStanza metaStanza;

    /**
     * Builds the {@code <reporting>} child.
     */
    private final ReportingStanza reportingStanza;

    /**
     * Constructs a {@link GroupMessageSender} bound to the supplied
     * dependencies.
     *
     * <p>Constructed once by {@link MessageSendingService}; embedders should not
     * instantiate directly.
     *
     * @param client                the {@link LinkedWhatsAppClient} used to dispatch
     *                              stanzas
     * @param encryption            the {@link MessageEncryption} service
     * @param deviceService         the {@link DeviceService}
     * @param abPropsService        the {@link ABPropsService}
     * @param senderKeyDistribution the {@link SenderKeyDistribution} service
     * @param botStanza             the {@link BotStanza} builder
     * @param bizStanza             the {@link BizStanza} builder
     * @param metaStanza            the {@link MetaStanza} builder
     * @param reportingStanza       the {@link ReportingStanza} builder
     * @param wamService            the {@link WamService} shared with the base
     *                              sender
     * @throws NullPointerException if any non-base argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendGroupMsgJob", exports = "encryptAndSendGroupMsg",
            adaptation = WhatsAppAdaptation.ADAPTED)
    GroupMessageSender(
            LinkedWhatsAppClient client,
            MessageEncryption encryption,
            DeviceService deviceService,
            ABPropsService abPropsService,
            SenderKeyDistribution senderKeyDistribution,
            BotStanza botStanza,
            BizStanza bizStanza,
            MetaStanza metaStanza,
            ReportingStanza reportingStanza,
            WamService wamService
    ) {
        super(client, abPropsService, wamService);
        this.encryption = Objects.requireNonNull(encryption, "encryption");
        this.deviceService = Objects.requireNonNull(deviceService, "deviceService");
        this.senderKeyDistribution = Objects.requireNonNull(senderKeyDistribution, "senderKeyDistribution");
        this.botStanza = Objects.requireNonNull(botStanza, "botStanza");
        this.bizStanza = Objects.requireNonNull(bizStanza, "bizStanza");
        this.metaStanza = Objects.requireNonNull(metaStanza, "metaStanza");
        this.reportingStanza = Objects.requireNonNull(reportingStanza, "reportingStanza");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the addressing mode, encrypts the payload with the group
     * sender key, distributes the sender key to devices that do not yet hold it,
     * dispatches the stanza, and reacts to the server ack by migrating the
     * addressing mode and/or resending to the delta devices. The dispatch is
     * serialised per group through {@link #enqueue}.
     */
    @WhatsAppWebExport(moduleName = "WAWebSendGroupMsgJob", exports = "encryptAndSendGroupMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebSendGroupSkmsgJob", exports = "encryptAndSendSenderKeyMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    AckResult doSend(Jid groupJid, ChatMessageInfo messageInfo) {
        var rawContainer = messageInfo.message();

        // getGroupFanout refreshes the group metadata; the addressing mode must be read
        // from the store afterwards so it reflects the server's current addressing_mode.
        var allDevices = deviceService.getGroupFanout(groupJid);

        var chatMetadata = store.chatStore().findChatMetadata(groupJid).orElse(null);
        var isCag = chatMetadata instanceof GroupMetadata gm
                && gm.isDefaultSubgroup();
        var isCagAddon = isCag && isCagAddonMessage(rawContainer);
        var isLidAddressingMode = (chatMetadata != null && chatMetadata.isLidAddressingMode())
                || isCagAddon;
        var addressingMode = isLidAddressingMode ? "lid" : "pn";

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "group send: {0} fanout device(s) for {1}, addressing={2}",
                    allDevices.size(), groupJid, addressingMode);
        }

        var senderJid = isLidAddressingMode ? selfLidOrPn() : requireSelfJid();

        var skDistribDevices = new ArrayList<Jid>();
        var skExistingDevices = new ArrayList<Jid>();
        for (var device : allDevices) {
            if (store.signalStore().hasSenderKeyDistributed(groupJid, device)) {
                skExistingDevices.add(device);
            } else {
                skDistribDevices.add(device);
            }
        }

        if (isLidAddressingMode) {
            skDistribDevices.removeIf(d -> !d.hasLidServer());
            skExistingDevices.removeIf(d -> !d.hasLidServer());
        } else {
            skDistribDevices.removeIf(Jid::hasLidServer);
            skExistingDevices.removeIf(Jid::hasLidServer);
        }

        var phashOpenBotGate = chatMetadata != null && chatMetadata.isOpenBotGroup();
        var phashTeeBotGate = chatMetadata instanceof GroupMetadata gmTee && gmTee.isTeeBotGroup();
        var phash = deviceService.computeGroupPhash(
                Stream.concat(skExistingDevices.stream(), skDistribDevices.stream()).toList(),
                senderJid, phashOpenBotGate, phashTeeBotGate);

        var isCapiGroup = chatMetadata instanceof GroupMetadata gm2
                && gm2.hasCapi();
        var container = isCapiGroup
                ? applyCapiFlag(rawContainer)
                : rawContainer;

        var rotateKey = store.signalStore().clearKeyRotation(groupJid);
        if (rotateKey) {
            encryption.rotateSenderKey(groupJid, senderJid);
            skDistribDevices.addAll(skExistingDevices);
            skExistingDevices.clear();
        }

        var participantUserJids = Stream.concat(skDistribDevices.stream(), skExistingDevices.stream())
                .map(Jid::toUserJid)
                .distinct()
                .toList();
        var contentBindings = generateContentBindings(messageInfo, participantUserJids);

        var allSkDevices = Stream.concat(skDistribDevices.stream(), skExistingDevices.stream())
                .toList();
        store.chatStore().createOrMergeReceiptRecords(messageInfo.key().id().orElseThrow(), allSkDevices);

        var senderKeyBytes = encryption.getSenderKeyBytes(groupJid, senderJid);
        List<MessageEncryptedPayload> skDistPayloads;
        if (skDistribDevices.isEmpty()) {
            skDistPayloads = List.of();
        } else {
            var depletedPrekeyCount = deviceService.ensureSessions(skDistribDevices);
            emitPrekeysDepletionEvents(depletedPrekeyCount, MessageType.GROUP, allSkDevices.size());
            skDistPayloads = senderKeyDistribution.encrypt(groupJid, senderKeyBytes, skDistribDevices);
        }

        var isBotFeedback = container.content() instanceof ProtocolMessage pm
                && pm.type().orElse(null) == ProtocolMessage.Type.BOT_FEEDBACK_MESSAGE;

        byte[] skmsgCiphertext;
        if (isBotFeedback) {
            skmsgCiphertext = null;
        } else {
            var plaintext = LinkedMessageContainerSpec.encode(container);
            try {
                skmsgCiphertext = encryption.encryptForGroup(groupJid, senderJid, plaintext)
                        .ciphertext();
                emitE2eMessageSendSenderKeyEvent(
                        groupJid, container,
                        E2eDestination.GROUP,
                        isLidAddressingMode, true);
            } catch (RuntimeException skmsgError) {
                emitE2eMessageSendSenderKeyEvent(
                        groupJid, container,
                        E2eDestination.GROUP,
                        isLidAddressingMode, false);
                throw skmsgError;
            }
        }

        var decryptFail = resolveDecryptFail(container);
        Stanza participantsStanza;
        if (!isBotFeedback && !skDistPayloads.isEmpty()) {
            participantsStanza = ParticipantsStanza.buildSenderKeyDistribution(
                    skDistPayloads, contentBindings, decryptFail);
        } else if (contentBindings != null) {
            participantsStanza = ParticipantsStanza.buildContentBindingOnly(
                    skExistingDevices, contentBindings);
        } else {
            participantsStanza = null;
        }

        var isOpenBotGroup = chatMetadata != null && chatMetadata.isOpenBotGroup()
                && abPropsService.getBool(ABProp.WEB_AI_GROUP_OPEN_SUPPORT)
                && abPropsService.getBool(ABProp.AI_GROUP_PARTICIPATION_ENABLED);
        Stanza openBotStanza = null;
        if (isOpenBotGroup) {
            deviceService.ensureSessions(List.of(Jid.metaAiBotAccount()));
            store.chatStore().createOrMergeReceiptRecords(
                    messageInfo.key().id().orElseThrow(), List.of(Jid.metaAiBotAccount()));
            openBotStanza = botStanza.buildForGroup(messageInfo, true);
        }

        var needsIdentity = ParticipantsStanza.requiresIdentityNode(skDistPayloads);
        if (!needsIdentity && openBotStanza != null) {
            needsIdentity = openBotStanza.streamChild("to")
                    .flatMap(to -> to.streamChild("enc"))
                    .anyMatch(enc -> "pkmsg".equals(enc.getAttributeAsString("type", null)));
        }
        var identityNode = needsIdentity ? buildIdentityNode() : null;

        var mediaType = resolveMediaType(container);
        var botNode = openBotStanza != null
                ? openBotStanza
                : botStanza.build(messageInfo, groupJid);
        var stanzaPhash = isBotFeedback ? null : phash;
        var stanza = GroupSkmsgFanoutStanza.build(
                messageInfo.key().id().orElseThrow(),
                groupJid,
                resolveStanzaType(container),
                stanzaPhash,
                skmsgCiphertext,
                mediaType,
                decryptFail,
                resolveEditAttribute(container),
                addressingMode,
                participantsStanza,
                identityNode,
                metaStanza.buildChat(groupJid, container, null),
                bizStanza.buildGroup(container),
                botNode,
                reportingStanza.build(messageInfo, requireSelfJid(), groupJid),
                SenderContentBindingStanza.build(senderJid, contentBindings)
        );

        store.signalStore().updateIdentityRange(allSkDevices);

        flushStore();
        var ackNode = client.sendNode(stanza);
        var ack = AckParser.parse(ackNode);

        if (!ack.isSuccess()) {
            var errorCode = ack.error().orElse(-1);
            if (errorCode == NackReason.STALE_GROUP_ADDRESSING_MODE.code()) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING,
                            "encryptAndSendSenderKeyMsg: ack with error code 421 for {0}, refreshing metadata",
                            groupJid);
                }
                migrateAddressingMode(groupJid, !isLidAddressingMode);
                throw new WhatsAppMessageException.Send.Unknown(
                        "Stale group addressing mode for " + groupJid, null);
            }
            throw new WhatsAppMessageException.Send.Unknown(
                    "Invalid ack from server for group " + groupJid
                    + ", error: " + errorCode, null);
        }

        for (var device : skDistribDevices) {
            store.signalStore().markSenderKeyDistributed(groupJid, device);
        }

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "group send: ack success for {0} id={1}",
                    groupJid, messageInfo.key().id().orElse(null));
        }

        if (ack instanceof MessageAck messageAck) {
            var serverPhash = messageAck.phash().orElse(null);
            if (serverPhash != null && !serverPhash.equals(phash)) {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG,
                            "encryptAndSendSenderKeyMsg: phash mismatch for {0}, server: {1}",
                            messageInfo.key().id().orElse(null), serverPhash);
                }
                var serverAddressingMode = messageAck.addressingMode().orElse(null);
                resendAsGroupDirect(groupJid, messageInfo, allSkDevices,
                        addressingMode, serverAddressingMode, chatMetadata, senderJid);
            }

            messageAck.addressingMode().ifPresent(serverMode -> {
                if (!serverMode.equals(addressingMode)) {
                    if (Log.INFO) {
                        LOGGER.log(Level.INFO,
                                "Addressing mode mismatch for {0}: local={1}, server={2}, migrating",
                                groupJid, addressingMode, serverMode);
                    }
                    wamService.commit(new AddressingModeMismatchEventBuilder()
                            .localAddressingMode(wamAddressingMode(addressingMode))
                            .serverAddressingMode(wamAddressingMode(serverMode))
                            .mismatchOrigin(MismatchOriginType.ACK_OUTGOING_MESSAGE)
                            .build());
                    migrateAddressingMode(groupJid, "lid".equals(serverMode));
                }
            });
        }

        return ack;
    }

    /**
     * Dispatches a standalone sender-key distribution to a group with no message
     * content.
     *
     * <p>Pre-distributes the sender key to participants that do not yet hold it.
     * The stanza carries the per-device PKMSG envelopes, {@code type="text"},
     * and {@code device_fanout="false"}. Returns silently when every audience
     * device already has the key.
     *
     * @param groupJid the group {@link Jid}
     * @param msgId    the wire id stamped on the distribution stanza
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendGroupKeyDistributionMsgJob",
            exports = "encryptAndSendGroupKeyDistributionMsg", adaptation = WhatsAppAdaptation.DIRECT)
    void sendKeyDistribution(Jid groupJid, String msgId) {
        Objects.requireNonNull(groupJid, "groupJid");
        Objects.requireNonNull(msgId, "msgId");

        waitForOfflineDelivery();

        try {
            enqueue(groupJid.toString(), () -> {
                var allDevices = deviceService.getGroupFanout(groupJid);

                var skDistribDevices = new ArrayList<Jid>();
                var skExistingDevices = new ArrayList<Jid>();
                for (var device : allDevices) {
                    if (store.signalStore().hasSenderKeyDistributed(groupJid, device)) {
                        skExistingDevices.add(device);
                    } else {
                        skDistribDevices.add(device);
                    }
                }

                if (skDistribDevices.isEmpty()) {
                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG,
                                "encryptAndSendGroupKeyDistributionMsg: skip sending {0}: " +
                                        "sender key distribution list is empty", groupJid);
                    }
                    return null;
                }

                var allSkDevices = Stream.concat(skDistribDevices.stream(), skExistingDevices.stream())
                        .toList();
                store.chatStore().createOrMergeReceiptRecords(msgId, allSkDevices);

                var allLid = skDistribDevices.stream().allMatch(Jid::hasLidServer);
                var senderJid = allLid ? selfLidOrPn() : requireSelfJid();

                var rotateKey = store.signalStore().clearKeyRotation(groupJid);
                if (rotateKey) {
                    encryption.rotateSenderKey(groupJid, senderJid);
                }

                var senderKeyBytes = encryption.getSenderKeyBytes(groupJid, senderJid);
                deviceService.ensureSessions(skDistribDevices);
                var skDistPayloads = senderKeyDistribution.encrypt(
                        groupJid, senderKeyBytes, skDistribDevices);

                var phash = deviceService.computeGroupPhash(allSkDevices, requireSelfJid(), false, false);

                Stanza participantsStanza = null;
                if (!skDistPayloads.isEmpty()) {
                    participantsStanza = ParticipantsStanza.buildSenderKeyDistribution(
                            skDistPayloads, null, "hide");
                }

                var needsIdentity = ParticipantsStanza.requiresIdentityNode(skDistPayloads);
                var identityNode = needsIdentity ? buildIdentityNode() : null;

                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "sending key distribution to {0} device(s) for {1}",
                            skDistribDevices.size(), groupJid);
                }

                var metaNode = new StanzaBuilder()
                        .description("meta")
                        .attribute("appdata", "default")
                        .build();
                var encNode = new StanzaBuilder()
                        .description("enc")
                        .attribute("v", String.valueOf(MessageEncryption.CIPHERTEXT_VERSION))
                        .attribute("type", MessageEncryptionType.SKMSG.protocolValue())
                        .attribute("decrypt-fail", "hide")
                        .build();
                var stanza = new StanzaBuilder()
                        .description("message")
                        .attribute("id", msgId)
                        .attribute("to", groupJid)
                        .attribute("phash", phash)
                        .attribute("type", "text")
                        .attribute("device_fanout", "false")
                        .content(metaNode, encNode, participantsStanza, identityNode);

                flushStore();
                var ackNode = client.sendNode(stanza);
                var ack = AckParser.parse(ackNode);

                if (ack.error().isPresent()) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "key distribution ack failed for {0} error={1}",
                                groupJid, ack.error().orElse(-1));
                    }
                    throw new WhatsAppMessageException.Send.Unknown(
                            "encryptAndSendSenderKeyMsg: Invalid ack from server for " + groupJid, null);
                }

                for (var device : skDistribDevices) {
                    store.signalStore().markSenderKeyDistributed(groupJid, device);
                }

                return ack;
            });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "key distribution failed for " + new LogRedactable.User(String.valueOf(groupJid)), e);
            }
            throw new RuntimeException("Failed to send key distribution to " + groupJid, e);
        }
    }

    /**
     * Generates the per-recipient RCAT content-binding tags for the outgoing
     * message.
     *
     * <p>RCAT tags are emitted only for URL-bearing extended-text payloads in
     * groups smaller than the {@link ABProp#MAXIMUM_GROUP_SIZE_FOR_RCAT} limit;
     * they let the receiver attribute the link impression to the sender without
     * exposing the participant set. Returns {@code null} when the conditions are
     * not met (no message secret, non-URL payload, or group too big).
     *
     * @param messageInfo     the outgoing {@link ChatMessageInfo}
     * @param participantJids the list of participant user {@link Jid}s
     * @return the per-recipient RCAT tags, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgRcatUtils", exports = "genContentBindingForMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    private Map<Jid, byte[]> generateContentBindings(
            ChatMessageInfo messageInfo,
            List<Jid> participantJids
    ) {
        var messageSecret = messageInfo.messageSecret().orElse(null);
        if (messageSecret == null) {
            return null;
        }

        var message = messageInfo.message().content();
        if (!(message instanceof ExtendedTextMessage text) || text.matchedText().isEmpty()) {
            return null;
        }

        var maxGroupSize = abPropsService.getInt(ABProp.MAXIMUM_GROUP_SIZE_FOR_RCAT);
        if (participantJids.size() > maxGroupSize) {
            return null;
        }

        var contentId = ContentBindingToken.resolveContentId(text.matchedText().get());
        var selfJid = requireSelfJid().toUserJid();

        try {
            return ContentBindingToken.generate(
                    messageInfo.key().id().orElseThrow(), messageSecret,
                    selfJid, participantJids, contentId);
        } catch (GeneralSecurityException e) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "content binding generation failed for message id="
                        + messageInfo.key().id().orElse(null), e);
            }
            return null;
        }
    }

    /**
     * Returns the given container with {@code capiCreatedGroup=true} stamped onto
     * its message-context info.
     *
     * <p>Called for CAPI-created groups so the receiver can render the "group
     * created via Cloud API" badge; mutates the existing context info in place
     * when present and otherwise allocates a new one.
     *
     * @param container the original {@link LinkedMessageContainer}
     * @return the container with the CAPI flag applied
     */
    @WhatsAppWebExport(moduleName = "WAWebE2EProtoGenerator", exports = "updateGroupMsgProtoWithCapiFlag",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static LinkedMessageContainer applyCapiFlag(LinkedMessageContainer container) {
        var existingCtxInfo = container.messageContextInfo().orElse(null);
        if (existingCtxInfo != null) {
            existingCtxInfo.setCapiCreatedGroup(true);
            return container;
        }

        return container.withMessageContextInfo(
                new ChatMessageContextInfoBuilder()
                        .capiCreatedGroup(true)
                        .build());
    }

    /**
     * Returns whether the given container holds a CAG addon payload.
     *
     * <p>CAG addons (reactions, comments, event responses, poll votes) must
     * reach LID-addressed participants regardless of the group's nominal
     * addressing mode; the caller forces the addressing-mode-LID branch on
     * detection.
     *
     * @param container the {@link LinkedMessageContainer}
     * @return {@code true} when the payload is a CAG addon
     */
    @WhatsAppWebExport(moduleName = "WAWebSendGroupMsgJob", exports = "isCagAddon",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static boolean isCagAddonMessage(LinkedMessageContainer container) {
        return switch (container.content()) {
            case EncReactionMessage _, EncCommentMessage _, EncEventResponseMessage _, PollUpdateMessage _ -> true;
            default -> false;
        };
    }

    /**
     * Resends the message to the delta devices via the per-device group-direct
     * path after a server phash mismatch.
     *
     * <p>Emits the {@code MdDeviceSyncAck} WAM event, re-queries the group
     * fanout, computes the delta against the original device list, and
     * dispatches the per-device fanout stanza with an empty SKMSG sibling to
     * match WA Web's wire shape.
     *
     * @param groupJid               the group {@link Jid}
     * @param messageInfo            the message being resent
     * @param originalDevices        the device list used for the original send
     * @param addressingMode         the local addressing mode used on the wire
     *                               ({@code "lid"} or {@code "pn"})
     * @param serverAddressingMode   the addressing mode reported on the ack,
     *                               possibly {@code null}
     * @param groupMetadataCandidate the group metadata used to populate the
     *                               {@code localAddressingMode} slot on the
     *                               emitted event
     * @param senderJid              the sender device JID used for the original
     *                               send; drives the {@code isLid} slot on the
     *                               emitted event
     */
    @WhatsAppWebExport(moduleName = "WAWebResendGroupMsg", exports = "resendGroupMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebSendGroupDirectJob", exports = "encryptAndSendGroupDirectMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebPostMdDeviceSyncAckMetric",
            exports = "postMdDeviceSyncAckMetric", adaptation = WhatsAppAdaptation.DIRECT)
    private void resendAsGroupDirect(
            Jid groupJid,
            ChatMessageInfo messageInfo,
            Collection<Jid> originalDevices,
            String addressingMode,
            String serverAddressingMode,
            ChatMetadata groupMetadataCandidate,
            Jid senderJid
    ) {
        var senderIsLid = senderJid != null && senderJid.hasLidServer();
        AddressingMode localWamMode = null;
        if (groupMetadataCandidate instanceof GroupMetadata gm) {
            localWamMode = gm.isLidAddressingMode() ? AddressingMode.LID : AddressingMode.PN;
        }
        wamService.commit(new MdDeviceSyncAckEventBuilder()
                .revoke(UserMessageSender.isRevokeMessage(messageInfo))
                .chatType(UserMessageSender.chatTypeFromJid(groupJid))
                .isLid(senderIsLid)
                .localAddressingMode(localWamMode)
                .serverAddressingMode(wamAddressingMode(serverAddressingMode))
                .build());

        var refreshedDevices = deviceService.getGroupFanout(groupJid);

        var refreshedMetadata = store.chatStore().findChatMetadata(groupJid).orElse(null);
        emitMdGroupParticipantMissAck(messageInfo, originalDevices, refreshedMetadata);

        var originalJids = originalDevices.stream()
                .map(Jid::toString)
                .collect(Collectors.toUnmodifiableSet());
        var deltaDevices = refreshedDevices.stream()
                .filter(device -> !originalJids.contains(device.toString()))
                .toList();

        if (deltaDevices.isEmpty()) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "No new devices after group phash resync for {0}", groupJid);
            }
            return;
        }

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "Resending as group-direct to {0} new devices for {1}",
                    deltaDevices.size(), groupJid);
        }

        var container = messageInfo.message();
        var depletedPrekeyCount = deviceService.ensureSessions(deltaDevices);
        emitPrekeysDepletionEvents(depletedPrekeyCount, MessageType.GROUP, deltaDevices.size());
        var senderIcdc = deviceService.computeIcdc(requireSelfJid())
                .orElse(null);
        var payloads = encryptForDevices(encryption, deltaDevices, container, groupJid, senderIcdc, null);
        if (payloads.isEmpty()) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "Group direct: encryption failed for all delta devices for {0}",
                        groupJid);
            }
            return;
        }

        var identityNode = ParticipantsStanza.requiresIdentityNode(payloads)
                ? buildIdentityNode() : null;

        var emptySkmsgNode = new StanzaBuilder()
                .description("enc")
                .attribute("v", String.valueOf(MessageEncryption.CIPHERTEXT_VERSION))
                .attribute("type", MessageEncryptionType.SKMSG.protocolValue())
                .attribute("mediatype", resolveMediaType(container))
                .build();

        var stanza = ChatFanoutStanza.build(
                messageInfo.key().id().orElseThrow(),
                groupJid,
                resolveStanzaType(container),
                payloads,
                resolveEditAttribute(container),
                addressingMode,
                null,
                resolveMediaType(container),
                resolveDecryptFail(container),
                resolveNativeFlowName(container),
                null,
                false,
                null,
                null,
                null,
                null,
                identityNode,
                metaStanza.buildChat(groupJid, container, null),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                emptySkmsgNode
        );

        flushStore();
        client.sendNode(stanza);
    }

    /**
     * Commits the {@code MdGroupParticipantMissAck} WAM event when the group's
     * participant set changed between the original SKMSG fanout and the
     * post-phash-mismatch re-query.
     *
     * <p>Suppressed when no participants were added or removed. The emitted event
     * records the size bucket, the group type, and the participant add/remove
     * counts so the server can compare client-side participant lag against its
     * own view.
     *
     * @param messageInfo       the message being resent
     * @param originalDevices   the device list used for the original SKMSG send
     * @param refreshedMetadata the refreshed {@link ChatMetadata}, possibly
     *                          {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebMaybePostMdGroupSyncMetrics",
            exports = "maybePostGroupSyncMetrics", adaptation = WhatsAppAdaptation.DIRECT)
    private void emitMdGroupParticipantMissAck(
            ChatMessageInfo messageInfo,
            Collection<Jid> originalDevices,
            ChatMetadata refreshedMetadata
    ) {
        var originalUserJids = originalDevices.stream()
                .map(Jid::toUserJid)
                .map(Jid::toString)
                .collect(Collectors.toUnmodifiableSet());

        Set<String> currentUserJids;
        if (refreshedMetadata instanceof GroupMetadata gm) {
            currentUserJids = gm.participants().stream()
                    .map(p -> p.userJid().toString())
                    .collect(Collectors.toUnmodifiableSet());
        } else {
            currentUserJids = Set.of();
        }

        var added = 0;
        for (var jid : currentUserJids) {
            if (!originalUserJids.contains(jid)) {
                added++;
            }
        }
        var removed = 0;
        for (var jid : originalUserJids) {
            if (!currentUserJids.contains(jid)) {
                removed++;
            }
        }

        if (added == 0 && removed == 0) {
            return;
        }

        var isLid = originalDevices.stream().anyMatch(Jid::hasLidServer);

        var participantCount = 0;
        if (refreshedMetadata instanceof GroupMetadata gm) {
            participantCount = gm.participants().size();
        }
        var groupSizeBucket = toGroupSizeBucket(Math.max(participantCount, 32));

        var typeOfGroup = refreshedMetadata instanceof GroupMetadata gm
                ? typeOfGroupFromMetadata(gm)
                : TypeOfGroupEnum.GROUP;

        wamService.commit(new MdGroupParticipantMissAckEventBuilder()
                .messageIsRevoke(UserMessageSender.isRevokeMessage(messageInfo))
                .groupSizeBucket(groupSizeBucket)
                .typeOfGroup(typeOfGroup)
                .isLid(isLid)
                .participantAddCount(added)
                .participantRemoveCount(removed)
                .build());
    }

    /**
     * Maps the given participant count to its {@link ClientGroupSizeBucket}.
     *
     * <p>Buckets group-size metrics on the {@code MdGroupParticipantMissAck}
     * event; the cascade mirrors WA Web's
     * {@code WAWebWamNumberToClientGroupSizeBucket} default export.
     *
     * @param count the participant count, already capped to a minimum of
     *              {@code 32} by the caller
     * @return the matching {@link ClientGroupSizeBucket}; never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebWamNumberToClientGroupSizeBucket",
            exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    private static ClientGroupSizeBucket toGroupSizeBucket(int count) {
        if (count <= 33) return ClientGroupSizeBucket.SMALL;
        if (count <= 65) return ClientGroupSizeBucket.MEDIUM;
        if (count <= 129) return ClientGroupSizeBucket.LARGE;
        if (count <= 257) return ClientGroupSizeBucket.EXTRA_LARGE;
        if (count <= 513) return ClientGroupSizeBucket.XX_LARGE;
        if (count <= 1025) return ClientGroupSizeBucket.LT1024;
        if (count <= 1501) return ClientGroupSizeBucket.LT1500;
        if (count <= 2001) return ClientGroupSizeBucket.LT2000;
        if (count <= 2501) return ClientGroupSizeBucket.LT2500;
        if (count <= 3001) return ClientGroupSizeBucket.LT3000;
        if (count <= 3501) return ClientGroupSizeBucket.LT3500;
        if (count <= 4001) return ClientGroupSizeBucket.LT4000;
        if (count <= 4501) return ClientGroupSizeBucket.LT4500;
        if (count <= 5001) return ClientGroupSizeBucket.LT5000;
        return ClientGroupSizeBucket.LARGEST_BUCKET;
    }

    /**
     * Maps the given {@link GroupMetadata} to the WAM {@link TypeOfGroupEnum}
     * used on metrics events.
     *
     * <p>Routes default subgroups to {@link TypeOfGroupEnum#DEFAULT_SUBGROUP},
     * non-default community subgroups to {@link TypeOfGroupEnum#SUBGROUP}, and
     * everything else (standalone groups, general subgroups) to
     * {@link TypeOfGroupEnum#GROUP}.
     *
     * @param metadata the {@link GroupMetadata}
     * @return the matching {@link TypeOfGroupEnum}; never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebGroupType",
            exports = {"getGroupTypeFromGroupMetadata", "groupTypeToWamEnum"},
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static TypeOfGroupEnum typeOfGroupFromMetadata(GroupMetadata metadata) {
        if (metadata.isDefaultSubgroup()) {
            return TypeOfGroupEnum.DEFAULT_SUBGROUP;
        }
        if (metadata.isGeneralSubgroup()) {
            return TypeOfGroupEnum.GROUP;
        }
        if (metadata.parentCommunityJid().isPresent()) {
            return TypeOfGroupEnum.SUBGROUP;
        }
        return TypeOfGroupEnum.GROUP;
    }

    /**
     * Migrates the group's addressing mode by converting every participant JID
     * to the target server and clearing the sender-key distribution state.
     *
     * <p>Triggered by a {@link NackReason#STALE_GROUP_ADDRESSING_MODE} nack or by
     * an ack whose {@code addressing_mode} attribute differs from the mode used
     * on the wire; clearing the sender-key distribution forces the next send to
     * redistribute the keys to the migrated participant set.
     *
     * @param groupJid the group {@link Jid}
     * @param toLid    {@code true} to migrate to LID, {@code false} to PN
     */
    @WhatsAppWebExport(moduleName = "WAWebGroupHandleAddressingModeMismatch", exports = "handleAddressingModeMismatch",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebDBGroupParticipant", exports = "migrateParticipantInfoAddressingMode",
            adaptation = WhatsAppAdaptation.DIRECT)
    private void migrateAddressingMode(Jid groupJid, boolean toLid) {
        var metadata = store.chatStore().findChatMetadata(groupJid).orElse(null);
        if (metadata == null) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "Cannot migrate addressing mode for {0}: no metadata", groupJid);
            }
            return;
        }

        var migratedParticipants = new ArrayList<GroupParticipant>();
        for (var participant : metadata.participants()) {
            var convertedJid = convertJid(participant.userJid(), toLid);
            if (convertedJid != null) {
                migratedParticipants.add(new GroupParticipantBuilder()
                        .userJid(convertedJid)
                        .rank(participant.rank().orElse(null))
                        .build());
            } else {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "No {0} mapping for {1}, keeping original",
                            toLid ? "LID" : "PN", participant.userJid());
                }
                migratedParticipants.add(participant);
            }
        }

        metadata.clearParticipants();
        metadata.addAllParticipants(migratedParticipants);
        metadata.setLidAddressingMode(toLid);

        store.signalStore().clearSenderKeyDistribution(groupJid);

        if (Log.INFO) {
            LOGGER.log(Level.INFO, "Migrated addressing mode for {0} to {1} ({2} participants)",
                    groupJid, toLid ? "lid" : "pn", migratedParticipants.size());
        }
    }

    /**
     * Converts the given JID to the target addressing mode through the store's
     * LID/PN mapping.
     *
     * <p>Returns the original JID unchanged when it already matches the target
     * mode, the mapped JID when a mapping exists, or {@code null} when the store
     * has no mapping for the requested conversion.
     *
     * @param jid   the {@link Jid} to convert
     * @param toLid {@code true} for PN to LID, {@code false} for LID to PN
     * @return the converted {@link Jid}, or {@code null} when no mapping exists
     */
    @WhatsAppWebExport(moduleName = "WAWebLidMigrationUtils", exports = "toAddressingModeFactory",
            adaptation = WhatsAppAdaptation.DIRECT)
    private Jid convertJid(Jid jid, boolean toLid) {
        if (toLid) {
            return jid.hasLidServer() ? jid : store.contactStore().findLidByPhone(jid).orElse(null);
        } else {
            return jid.hasUserServer() ? jid : store.contactStore().findPhoneByLid(jid).orElse(null);
        }
    }

    /**
     * Maps the wire addressing-mode string to the WAM {@link AddressingMode}
     * enum.
     *
     * <p>Populates the {@code localAddressingMode} and
     * {@code serverAddressingMode} slots on the
     * {@link AddressingModeMismatchEventBuilder} and the
     * {@link MdDeviceSyncAckEventBuilder} events; {@code null} input propagates
     * as {@code null}.
     *
     * @param mode the wire addressing-mode string, possibly {@code null}
     * @return {@link AddressingMode#LID} for {@code "lid"},
     *         {@link AddressingMode#PN} for any other non-null value, or
     *         {@code null} when {@code mode} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebWamAddressingModeUtils",
            exports = "getWamAddressingModeFromString",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static AddressingMode wamAddressingMode(String mode) {
        if (mode == null) {
            return null;
        }
        return "lid".equals(mode) ? AddressingMode.LID : AddressingMode.PN;
    }

    /**
     * Commits one
     * {@link com.github.auties00.cobalt.wire.wam.event.PrekeysDepletionEvent} per
     * depleted one-time pre-key reported by the last
     * {@link DeviceService#ensureSessions(Collection)} call.
     *
     * <p>No-op when {@code depletedPrekeyCount} is not positive. Mirrors WA Web's
     * {@code maybePostPrekeysDepletionMetric}.
     *
     * @param depletedPrekeyCount the number of depleted one-time pre-keys
     * @param messageType         the WAM {@link MessageType} for this send
     * @param deviceCount         the device count used for the
     *                            {@code deviceSizeBucket} classification, or
     *                            {@code null} to omit the bucket
     */
    @WhatsAppWebExport(moduleName = "WAWebPostPrekeysDepletionMetric",
            exports = "maybePostPrekeysDepletionMetric",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitPrekeysDepletionEvents(int depletedPrekeyCount, MessageType messageType, Integer deviceCount) {
        if (depletedPrekeyCount <= 0) {
            return;
        }
        var bucket = deviceCount == null ? null : numberToSizeBucket(deviceCount);
        for (var i = 0; i < depletedPrekeyCount; i++) {
            wamService.commit(new PrekeysDepletionEventBuilder()
                    .prekeysFetchReason(PrekeysFetchContext.SEND_MESSAGE)
                    .messageType(messageType)
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
