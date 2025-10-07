package com.github.auties00.cobalt.socket.receipt;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.io.node.Node;
import com.github.auties00.cobalt.io.node.NodeAttribute;
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

public final class ReceiptStreamNodeHandler extends SocketStream.Handler {
    private final Set<String> retries;

    public ReceiptStreamNodeHandler(Whatsapp whatsapp) {
        super(whatsapp, "receipt");
        this.retries = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void handle(Node node) {
        var senderJid = node.getRequiredAttribute("from")
                .toJid();
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
        var outerId = node.getAttribute("id")
                .map(NodeAttribute::toString)
                .stream();
        var innerIds = node.findChild("list")
                .stream()
                .flatMap(list -> list.streamChildren("item"))
                .flatMap(item -> item.getAttribute("id").stream())
                .map(NodeAttribute::toString);
        return Stream.concat(outerId, innerIds);
    }


    private void updateChatReceipt(Node node, ChatMessageInfo message) {
        var status = node.getAttribute("type")
                .map(NodeAttribute::toString)
                .flatMap(MessageStatus::of)
                .orElse(MessageStatus.DELIVERED);
        message.chat().ifPresent(chat -> {
            var newCount = chat.unreadMessagesCount() - 1;
            chat.setUnreadMessagesCount(newCount);
            var participant = node.getAttribute("participant")
                    .map(NodeAttribute::toJid)
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
                Thread.startVirtualThread(() -> listener.onMessageStatus(message));
                Thread.startVirtualThread(() -> listener.onMessageStatus(whatsapp, message));
            }
        });
        var type = node.getAttribute("type")
                .map(NodeAttribute::toString)
                .orElse("");
        if(type.equals("retry")) {
            acceptMessageRetry(message);
        }
        message.setStatus(status);
    }

    private void updateNewsletterReceipt(Node node, NewsletterMessageInfo message) {
        var status = node.getAttribute("type")
                .map(NodeAttribute::toString)
                .flatMap(MessageStatus::of)
                .orElse(MessageStatus.DELIVERED);
        message.setStatus(status);
        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onMessageStatus(message));
            Thread.startVirtualThread(() -> listener.onMessageStatus(whatsapp, message));
        }
        var type = node.getAttribute("type")
                .map(NodeAttribute::toString)
                .orElse("");
        if(type.equals("retry")) {
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
    public void dispose() {
        retries.clear();
    }
}
