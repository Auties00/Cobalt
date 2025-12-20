package com.github.auties00.cobalt.message;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.message.processor.MessageAttributer;
import com.github.auties00.cobalt.message.processor.MessageDecoder;
import com.github.auties00.cobalt.model.business.BusinessVerifiedNameCertificateSpec;
import com.github.auties00.cobalt.model.chat.Chat;
import com.github.auties00.cobalt.model.info.*;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.model.message.model.*;
import com.github.auties00.cobalt.model.message.server.SenderKeyDistributionMessage;
import com.github.auties00.cobalt.model.newsletter.NewsletterReaction;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.libsignal.SignalProtocolAddress;
import com.github.auties00.libsignal.groups.SignalSenderKeyName;
import com.github.auties00.libsignal.protocol.SignalSenderKeyDistributionMessage;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.auties00.cobalt.client.WhatsAppClientErrorHandler.Location.MESSAGE;

public final class MessageReceiverService {
    private final WhatsAppClient whatsapp;
    private final MessageDecoder messageDecoder;
    private final MessageAttributer messageAttributer;

    public MessageReceiverService(WhatsAppClient whatsapp) {
        this.whatsapp = whatsapp;
        this.messageDecoder = new MessageDecoder(whatsapp.store());
        this.messageAttributer = new MessageAttributer(whatsapp.store());
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
            messageAttributer.attributeChatMessage(message);
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
        ChatMessageKey chatMessageKey = null;
        try {
            var selfJid = whatsapp.store()
                    .jid()
                    .orElse(null);
            if (selfJid == null) {
                return Stream.empty();
            }

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
                var fromMe = isFromMe(from, selfJid);
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
                keyBuilder.fromMe(Objects.equals(senderJid.withoutData(), selfJid.withoutData()));
            } else if(from.hasServer(JidServer.groupOrCommunity()) || from.hasServer(JidServer.broadcast()) || from.hasServer(JidServer.newsletter())) {
                var participant = infoNode
                        .getAttributeAsJid("participant_pn")
                        .or(() -> infoNode.getAttributeAsJid("participant"))
                        .orElseThrow(() -> new NoSuchElementException("Missing sender"));
                keyBuilder.chatJid(from);
                keyBuilder.senderJid(Objects.requireNonNull(participant, "Missing participant in group message"));
                var fromMe = isFromMe(participant, selfJid);
                keyBuilder.fromMe(fromMe);
                messageBuilder.senderJid(Objects.requireNonNull(participant, "Missing participant in group message"));
            }else {
                throw new RuntimeException("Unknown value server: " + from.server());
            }
            chatMessageKey = keyBuilder.build();

            var senderJid = chatMessageKey.senderJid()
                    .orElseThrow(() -> new InternalError("Missing sender"));
            if (selfJid.equals(senderJid)) {
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
            messageAttributer.attributeChatMessage(info);
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

    private boolean isFromMe(Jid participant, Jid selfJid) {
        if(Objects.equals(participant.withoutData(), selfJid.withoutData())) {
            return true;
        } else {
            return whatsapp.store().getAlternateJid(participant.withoutData())
                    .map(alt -> Objects.equals(alt, selfJid.withoutData()))
                    .orElse(false);
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
            return messageDecoder.decode(messageKey, type, encodedMessage.get());
        }catch (Throwable throwable) {
            whatsapp.handleFailure(MESSAGE, throwable);
            return MessageContainer.empty();
        }
    }

    private void handleSenderKeyDistributionMessage(SenderKeyDistributionMessage keyDistributionMessage, SignalProtocolAddress address) {
        var groupName = new SignalSenderKeyName(keyDistributionMessage.groupJid().toString(), address);
        var signalDistributionMessage = SignalSenderKeyDistributionMessage.ofSerialized(keyDistributionMessage.data());
        messageDecoder.process(groupName, signalDistributionMessage);
    }
}
