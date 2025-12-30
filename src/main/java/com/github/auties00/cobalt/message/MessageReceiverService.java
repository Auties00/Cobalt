package com.github.auties00.cobalt.message;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.message.signal.SignalMessageDecoder;
import com.github.auties00.cobalt.model.business.BusinessVerifiedNameCertificateSpec;
import com.github.auties00.cobalt.model.chat.Chat;
import com.github.auties00.cobalt.model.info.*;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.model.message.model.*;
import com.github.auties00.cobalt.model.message.server.SenderKeyDistributionMessage;
import com.github.auties00.cobalt.model.message.standard.PollCreationMessage;
import com.github.auties00.cobalt.model.message.standard.PollUpdateMessage;
import com.github.auties00.cobalt.model.message.standard.ReactionMessage;
import com.github.auties00.cobalt.model.newsletter.NewsletterReaction;
import com.github.auties00.cobalt.model.poll.PollUpdateBuilder;
import com.github.auties00.cobalt.model.poll.PollUpdateEncryptedOptionsSpec;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.util.Clock;
import com.github.auties00.libsignal.SignalProtocolAddress;
import com.github.auties00.libsignal.SignalSessionCipher;
import com.github.auties00.libsignal.groups.SignalGroupCipher;
import com.github.auties00.libsignal.groups.SignalSenderKeyName;
import com.github.auties00.libsignal.protocol.SignalSenderKeyDistributionMessage;

import javax.crypto.Cipher;
import javax.crypto.KDF;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.auties00.cobalt.client.WhatsAppClientErrorHandler.Location.MESSAGE;

public final class MessageReceiverService {
    private final WhatsAppClient whatsapp;
    private final SignalMessageDecoder signalMessageDecoder;
    private final DeviceService deviceService;

    public MessageReceiverService(WhatsAppClient whatsapp, DeviceService deviceService, SignalSessionCipher sessionCipher, SignalGroupCipher groupCipher) {
        this.whatsapp = whatsapp;
        this.signalMessageDecoder = new SignalMessageDecoder(sessionCipher, groupCipher);
        this.deviceService = deviceService;
    }
    
    public SequencedCollection<? extends MessageInfo> readMessages(Node node) {
        var businessName = getBusinessNameFromAttribute(node);
        if (node.hasChild("unavailable")) {
            return decodeChatMessage(node, null, businessName)
                    .toList();
        }

        var encrypted = node.getChildren("enc");
        if (!encrypted.isEmpty()) {
            return encrypted.stream()
                    .flatMap(message -> decodeChatMessage(node, message, businessName))
                    .toList();
        }


        var plainText = node.getChild("plaintext");
        if (plainText.isPresent()) {
            return decodeNewsletterMessage(node, plainText.get())
                    .toList();
        }

        var reaction = node.getChild("reaction");
        if (reaction.isPresent()) {
            return decodeNewsletterReaction(node, reaction.get())
                    .toList();
        }

        return decodeChatMessage(node, null, businessName)
                .toList();
    }

    public void validateMessages(Chat chat) {
        for(var message : chat.messages()) {
            attributeChatMessage(message);
        }
    }

    private String getBusinessNameFromAttribute(Node node) {
        return node.getAttributeAsString("verified_name")
                .or(() -> getBusinessNameFromContent(node))
                .orElse(null);
    }

    private static Optional<String> getBusinessNameFromContent(Node node) {
        return node.getChild("verified_name")
                .flatMap(Node::toContentBytes)
                .map(BusinessVerifiedNameCertificateSpec::decode)
                .flatMap(certificate -> certificate.details().name());
    }

    private Stream<NewsletterMessageInfo> decodeNewsletterMessage(Node messageInfoNode, Node messageNode) {
        try {
            var newsletterJid = messageInfoNode
                    .getAttributeAsJid("from")
                    .orElseThrow(() -> new NoSuchElementException("Missing from"));
            var messageId = messageInfoNode
                    .getRequiredAttributeAsString("id");
            whatsapp.sendAck(messageInfoNode);
            whatsapp.sendReceipt(messageId, newsletterJid, null, false);

            var newsletter = whatsapp.store()
                    .findNewsletterByJid(newsletterJid);
            if (newsletter.isEmpty()) {
                return Stream.empty();
            }

            var serverId = Math.toIntExact(messageInfoNode.getRequiredAttributeAsLong("server_id"));
            var timestamp = messageInfoNode.getAttributeAsLong("t", 0);
            var views = messageInfoNode.getChild("views_count")
                    .map(value -> value.getRequiredAttributeAsLong("count"))
                    .orElse(null);
            var reactions = messageInfoNode.streamChild("reactions")
                    .flatMap(node -> node.streamChildren("reaction"))
                    .collect(Collectors.toConcurrentMap(
                            entry -> entry.getRequiredAttributeAsString("code"),
                            entry -> new NewsletterReaction(
                                    entry.getRequiredAttributeAsString("code"),
                                    entry.getRequiredAttributeAsLong("count"),
                                    entry.getRequiredAttributeAsBool("is_sender")
                            )
                    ));
            var result = messageNode.toContentBytes()
                    .map(MessageContainerSpec::decode)
                    .map(messageContainer -> {
                        var readStatus = MessageStatus.DELIVERED;
                        var message = new NewsletterMessageInfoBuilder()
                                .id(messageId)
                                .serverId(serverId)
                                .timestampSeconds(timestamp)
                                .views(views)
                                .reactions(reactions)
                                .message(messageContainer)
                                .status(readStatus)
                                .build();
                        message.setNewsletter(newsletter.get());
                        return message;
                    });
            if (result.isEmpty()) {
                return Stream.empty();
            }

            newsletter.get()
                    .addMessage(result.get());

            return Stream.of(result.get());
        } catch (Throwable throwable) {
            whatsapp.handleFailure(MESSAGE, throwable);
            return Stream.empty();
        }
    }

    private Stream<NewsletterMessageInfo> decodeNewsletterReaction(Node reactionInfoNode, Node reactionNode) {
        try {
            var messageId = reactionInfoNode
                    .getRequiredAttributeAsString("id");
            var newsletterJid = reactionInfoNode
                    .getAttributeAsJid("from")
                    .orElseThrow(() -> new NoSuchElementException("Missing from"));
            var isSender = reactionInfoNode
                    .getAttributeAsBool("is_sender", false);
            whatsapp.sendAck(reactionInfoNode);
            whatsapp.sendReceipt(messageId, newsletterJid, null, false);

            var newsletter = whatsapp.store()
                    .findNewsletterByJid(newsletterJid);
            if (newsletter.isEmpty()) {
                return Stream.empty();
            }

            var message = whatsapp.store()
                    .findMessageById(newsletter.get(), messageId);
            if (message.isEmpty()) {
                return Stream.empty();
            }

            var myReaction = isSender ? message.get()
                    .reactions()
                    .stream()
                    .filter(NewsletterReaction::fromMe)
                    .findFirst()
                    .orElse(null) : null;
            if (myReaction != null) {
                message.get().decrementReaction(myReaction.content());
            }

            var code = reactionNode
                    .getAttributeAsString("code");
            if (code.isEmpty()) {
                return Stream.empty();
            }

            message.get()
                    .incrementReaction(code.get(), isSender);
            return Stream.of(message.get());
        } catch (Throwable throwable) {
            whatsapp.handleFailure(MESSAGE, throwable);
            return Stream.empty();
        }
    }

    private Stream<ChatMessageInfo> decodeChatMessage(Node infoNode, Node messageNode, String businessName) {
        var localJid = whatsapp.store()
                .jid()
                .orElseThrow(() -> new IllegalStateException("Local jid is not available"));

        ChatMessageKey chatMessageKey = null;
        try {
            var pushName = infoNode.getAttributeAsString("notify", null);
            var timestamp = infoNode.getAttributeAsLong("t", 0);
            var id = infoNode.getRequiredAttributeAsString("id");
            var from = infoNode.getRequiredAttributeAsJid("from");
            var messageBuilder = new ChatMessageInfoBuilder()
                    .status(MessageStatus.PENDING);
            var keyBuilder = new ChatMessageKeyBuilder()
                    .id(id);
            if (from.hasServer(JidServer.user()) || from.hasServer(JidServer.legacyUser()) || from.hasServer(JidServer.lid())) {
                var recipient = infoNode
                        .getAttributeAsJid("recipient_pn")
                        .or(() -> infoNode.getAttributeAsJid("recipient"))
                        .orElse(from);
                keyBuilder.chatJid(recipient);
                keyBuilder.senderJid(from);
                var fromMe = Objects.equals(from.withoutData(), localJid.withoutData());
                keyBuilder.fromMe(fromMe);
                messageBuilder.senderJid(from);
            }else if(from.hasServer(JidServer.bot())) {
                var meta = infoNode.getChild("meta")
                        .orElseThrow();
                var chatJid = meta
                        .getRequiredAttributeAsJid("target_chat_jid");
                var senderJid = meta
                        .getAttributeAsJid("target_sender_jid")
                        .orElse(chatJid);
                keyBuilder.chatJid(chatJid);
                keyBuilder.senderJid(senderJid);
                keyBuilder.fromMe(Objects.equals(senderJid.withoutData(), localJid.withoutData()));
            } else if(from.hasServer(JidServer.groupOrCommunity()) || from.hasServer(JidServer.broadcast()) || from.hasServer(JidServer.newsletter())) {
                var participant = infoNode
                        .getAttributeAsJid("participant_pn")
                        .or(() -> infoNode.getAttributeAsJid("participant"))
                        .orElseThrow(() -> new NoSuchElementException("Missing sender"));
                keyBuilder.chatJid(from);
                keyBuilder.senderJid(Objects.requireNonNull(participant, "Missing participant in group message"));
                var fromMe = Objects.equals(participant.withoutData(), localJid.withoutData());
                keyBuilder.fromMe(fromMe);
                messageBuilder.senderJid(Objects.requireNonNull(participant, "Missing participant in group message"));
            }else {
                throw new RuntimeException("Unknown value server: " + from.server());
            }
            chatMessageKey = keyBuilder.build();

            var senderJid = chatMessageKey.senderJid()
                    .orElseThrow(() -> new InternalError("Missing sender"));
            if (localJid.equals(senderJid)) {
                return Stream.empty();
            }

            var container = decodeChatMessageContainer(chatMessageKey, messageNode);
            var info = messageBuilder.key(chatMessageKey)
                    .broadcast(chatMessageKey.chatJid().hasServer(JidServer.broadcast()))
                    .pushName(pushName)
                    .status(MessageStatus.DELIVERED)
                    .businessVerifiedName(businessName)
                    .timestampSeconds(timestamp)
                    .message(container)
                    .build();
            info.message()
                    .senderKeyDistributionMessage()
                    .ifPresent(keyDistributionMessage -> handleSenderKeyDistributionMessage(keyDistributionMessage, info.senderJid().toSignalAddress()));
            attributeChatMessage(info);
            return Stream.of(info);
        } catch (Throwable throwable) {
            whatsapp.handleFailure(MESSAGE, throwable);
            return Stream.empty();
        }finally {
            whatsapp.sendAck(infoNode);
            if(chatMessageKey != null) {
                whatsapp.sendReceipt(
                        chatMessageKey.id(),
                        chatMessageKey.chatJid(),
                        chatMessageKey.senderJid().orElse(null),
                        infoNode.hasAttribute("category", "peer")
                );
            }
        }
    }

    private MessageContainer decodeChatMessageContainer(ChatMessageKey messageKey, Node messageNode) {
        if (messageNode == null) {
            return MessageContainer.empty();
        }

        var type = messageNode.getRequiredAttributeAsString("type");
        var encodedMessage = messageNode.toContentBytes();
        if (encodedMessage.isEmpty()) {
            return MessageContainer.empty();
        }

        try {
            return signalMessageDecoder.decode(messageKey, type, encodedMessage.get());
        }catch (Throwable throwable) {
            whatsapp.handleFailure(MESSAGE, throwable);
            return MessageContainer.empty();
        }
    }

    private void handleSenderKeyDistributionMessage(SenderKeyDistributionMessage keyDistributionMessage, SignalProtocolAddress address) {
        var groupName = new SignalSenderKeyName(keyDistributionMessage.groupJid().toString(), address);
        var signalDistributionMessage = SignalSenderKeyDistributionMessage.ofSerialized(keyDistributionMessage.data());
        deviceService.processDistributionMessage(groupName, signalDistributionMessage);
    }

    public void attributeChatMessage(ChatMessageInfo info) {
        attributeMessageReceipt(info);
        var chat = whatsapp.store()
                .findChatByJid(info.chatJid())
                .orElseGet(() -> whatsapp.store().addNewChat(info.chatJid()));
        info.setChat(chat);
        var me = whatsapp.store().jid().orElse(null);
        if (info.fromMe() && me != null) {
            info.key().setSenderJid(me.withoutData());
        }

        attributeSender(info, info.senderJid());
        info.message()
                .contentWithContext()
                .ifPresent(message -> {
                    message.contextInfo()
                            .ifPresent(this::attributeContext);
                    processMessageWithSecret(info, message);
                });
    }

    private void processMessageWithSecret(ChatMessageInfo info, Message message) {
        switch (message) {
            case PollCreationMessage pollCreationMessage -> handlePollCreation(info, pollCreationMessage);
            case PollUpdateMessage pollUpdateMessage -> handlePollUpdate(info, pollUpdateMessage);
            case ReactionMessage reactionMessage -> handleReactionMessage(info, reactionMessage);
            default -> {}
        }
    }

    private void handlePollCreation(ChatMessageInfo info, PollCreationMessage pollCreationMessage) {
        if (pollCreationMessage.encryptionKey().isPresent()) {
            return;
        }

        info.message()
                .deviceInfo()
                .flatMap(DeviceContextInfo::messageSecret)
                .or(info::messageSecret)
                .ifPresent(pollCreationMessage::setEncryptionKey);
    }

    private void handlePollUpdate(ChatMessageInfo info, PollUpdateMessage pollUpdateMessage) {
        try {
            var originalPollInfo = whatsapp.store()
                    .findChatMessageByKey(pollUpdateMessage.pollCreationMessageKey());
            if (originalPollInfo.isEmpty()) {
                return;
            }

            if(!(originalPollInfo.get().message().content() instanceof PollCreationMessage originalPollMessage)) {
                return;
            }

            pollUpdateMessage.setPollCreationMessage(originalPollMessage);
            var originalPollSenderJid = originalPollInfo.get()
                    .senderJid()
                    .withoutData();
            var modificationSenderJid = info.senderJid().withoutData();
            pollUpdateMessage.setVoter(modificationSenderJid);
            var originalPollId = originalPollInfo.get().id();
            var useSecretPayload = originalPollId + originalPollSenderJid + modificationSenderJid + pollUpdateMessage.secretName();
            var encryptionKey = originalPollMessage.encryptionKey()
                    .orElseThrow(() -> new NoSuchElementException("Missing encryption key"));
            var hkdf = KDF.getInstance("HKDF-SHA256");
            var params = HKDFParameterSpec.ofExtract()
                    .addIKM(new SecretKeySpec(encryptionKey, "AES"))
                    .thenExpand(useSecretPayload.getBytes(), 32);
            var useCaseSecret = hkdf.deriveData(params);
            var additionalData = "%s\0%s".formatted(
                    originalPollId,
                    modificationSenderJid
            );
            var metadata = pollUpdateMessage.encryptedMetadata()
                    .orElseThrow(() -> new NoSuchElementException("Missing encrypted metadata"));
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(useCaseSecret, "AES"),
                    new GCMParameterSpec(128, metadata.iv())
            );
            cipher.updateAAD(additionalData.getBytes(StandardCharsets.UTF_8));
            var decrypted = cipher.doFinal(metadata.payload());
            var pollVoteMessage = PollUpdateEncryptedOptionsSpec.decode(decrypted);
            var selectedOptions = pollVoteMessage.selectedOptions()
                    .stream()
                    .map(sha256 -> originalPollMessage.getSelectableOption(HexFormat.of().formatHex(sha256)))
                    .flatMap(Optional::stream)
                    .toList();
            originalPollMessage.addSelectedOptions(modificationSenderJid, selectedOptions);
            pollUpdateMessage.setVotes(selectedOptions);
            var update = new PollUpdateBuilder()
                    .pollUpdateMessageKey(info.key())
                    .vote(pollVoteMessage)
                    .senderTimestampMilliseconds(Clock.nowMilliseconds())
                    .build();
            info.pollUpdates()
                    .add(update);
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot decrypt poll update", exception);
        }
    }

    private void handleReactionMessage(ChatMessageInfo info, ReactionMessage reactionMessage) {
        info.setIgnore(true);
        whatsapp.store()
                .findChatMessageByKey(reactionMessage.key())
                .ifPresent(message -> message.reactions().add(reactionMessage));
    }

    private void attributeSender(ChatMessageInfo info, Jid senderJid) {
        if (!senderJid.hasUserServer() && !senderJid.hasLidServer()) {
            return;
        }

        whatsapp.store().findContactByJid(senderJid)
                .ifPresent(info::setSender);
    }

    private void attributeContext(ContextInfo contextInfo) {
        contextInfo.quotedMessageSenderJid().ifPresent(senderJid -> attributeContextSender(contextInfo, senderJid));
        contextInfo.quotedMessageParentJid().ifPresent(parentJid -> attributeContextParent(contextInfo, parentJid));
    }

    private void attributeContextParent(ContextInfo contextInfo, Jid parentJid) {
        if(parentJid.hasServer(JidServer.newsletter())) {
            var newsletter = whatsapp.store()
                    .findNewsletterByJid(parentJid)
                    .orElseGet(() -> whatsapp.store().addNewNewsletter(parentJid));
            contextInfo.setQuotedMessageParent(newsletter);
        }else {
            var chat = whatsapp.store()
                    .findChatByJid(parentJid)
                    .orElseGet(() -> whatsapp.store().addNewChat(parentJid));
            contextInfo.setQuotedMessageParent(chat);
        }
    }

    private void attributeContextSender(ContextInfo contextInfo, Jid senderJid) {
        whatsapp.store().findContactByJid(senderJid)
                .ifPresent(contextInfo::setQuotedMessageSender);
    }

    private void attributeMessageReceipt(ChatMessageInfo info) {
        var self = whatsapp.store().jid()
                .map(Jid::withoutData)
                .orElse(null);
        if (!info.fromMe() || (self != null && !info.chatJid().equals(self))) {
            return;
        }
        info.receipt().setReadTimestampSeconds(info.timestampSeconds().orElse(0L));
        info.receipt().addDeliveredJid(self);
        info.receipt().addReadJid(self);
        info.setStatus(MessageStatus.READ);
    }
}
