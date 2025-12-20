package com.github.auties00.cobalt.socket.message;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.model.info.ChatMessageInfo;
import com.github.auties00.cobalt.model.info.MessageInfo;
import com.github.auties00.cobalt.model.info.NewsletterMessageInfo;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.model.message.model.MessageStatus;
import com.github.auties00.cobalt.socket.SocketStream;
import com.github.auties00.cobalt.util.Clock;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class MessageReceiptStreamNodeHandler extends SocketStream.Handler {
    private final Set<String> retries;

    public MessageReceiptStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "receipt");
        this.retries = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void handle(Node node) {
        var senderJid = node.getRequiredAttributeAsJid("from");
        getReceiptsMessageIds(node).forEachOrdered(messageId -> {
            whatsapp.sendAck(messageId, node);
            var message = whatsapp.store().findMessageById(senderJid, messageId);
            if (message.isEmpty()) {
                return;
            }

            switch (message.get()) {
                case ChatMessageInfo chatMessageInfo -> updateChatReceipt(node, chatMessageInfo);
                case NewsletterMessageInfo newsletterMessageInfo -> updateNewsletterReceipt(node, newsletterMessageInfo);
                default -> throw new IllegalStateException("Unexpected value: " + message.get());
            }
        });
    }

    private Stream<String> getReceiptsMessageIds(Node node) {
        var outerId = node.streamAttributeAsString("id");
        var innerIds = node.streamChildren("list")
                .flatMap(list -> list.streamChildren("item"))
                .flatMap(item -> item.streamAttributeAsString("id"));
        return Stream.concat(outerId, innerIds);
    }


    private void updateChatReceipt(Node node, ChatMessageInfo message) {
        var status = node.getAttributeAsString("type")
                .flatMap(MessageStatus::of)
                .orElse(MessageStatus.DELIVERED);
        message.chat().ifPresent(chat -> {
            var newCount = chat.unreadMessagesCount() - 1;
            chat.setUnreadMessagesCount(newCount);
            var participant = node.getAttributeAsJid("participant")
                    .flatMap(whatsapp.store()::findContactByJid)
                    .orElse(null);
            var target = participant != null ? participant.jid() : message.senderJid();
            if(status == MessageStatus.READ) {
                message.receipt().addReadJid(target);
            }else {
                message.receipt().addDeliveredJid(target);
            }
            if(chat.jid().hasServer(JidServer.groupOrCommunity())) {
                var metadata = whatsapp.queryGroupOrCommunityMetadata(chat.jid());
                var jids = status == MessageStatus.READ ? message.receipt().readJids() : message.receipt().deliveredJids();
                if (participant == null || metadata.participants().size() == jids.size()) {
                    switch (status) {
                        case READ -> message.receipt().setReadTimestampSeconds(Clock.nowSeconds());
                        case PLAYED -> message.receipt().setPlayedTimestampSeconds(Clock.nowSeconds());
                    }
                }
            }
            for(var listener : whatsapp.store().listeners()) {
                Thread.startVirtualThread(() -> listener.onMessageStatus(whatsapp, message));
            }
        });
        if(node.hasAttribute("type", "retry")) {
            acceptMessageRetry(message);
        }
        message.setStatus(status);
    }

    private void updateNewsletterReceipt(Node node, NewsletterMessageInfo message) {
        var status = node.getAttributeAsString("type")
                .flatMap(MessageStatus::of)
                .orElse(MessageStatus.DELIVERED);
        message.setStatus(status);
        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onMessageStatus(whatsapp, message));
        }
        if(node.hasAttribute("type", "retry")) {
            acceptMessageRetry(message);
        }
    }

    private void acceptMessageRetry(MessageInfo message) {
        var meJid = whatsapp.store()
                .jid()
                .orElse(null);
        var senderJid = message.senderJid();
        if (meJid != null && meJid.equals(senderJid)) {
            return;
        }

        if (!retries.add(message.id())) {
            return;
        }

        // TODO: Rewrite retry logic
    }

    @Override
    public void reset() {
        retries.clear();
    }
}
