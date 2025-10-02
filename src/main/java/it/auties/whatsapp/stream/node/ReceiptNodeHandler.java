package it.auties.whatsapp.stream.node;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.stream.message.MessageRequest;
import it.auties.whatsapp.io.node.Node;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.info.NewsletterMessageInfo;
import it.auties.whatsapp.model.info.QuotedMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.model.message.model.MessageStatus;
import it.auties.whatsapp.util.Clock;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class ReceiptNodeHandler extends AbstractNodeHandler {
    private final Set<String> retries;

    public ReceiptNodeHandler(Whatsapp whatsapp) {
        super(whatsapp, "receipt");
        this.retries = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void handle(Node node) {
        var senderJid = node.attributes()
                .getRequiredJid("from");
        getReceiptsMessageIds(node).forEachOrdered(messageId -> {
            whatsapp.sendAck(messageId, node);
            var message = whatsapp.store().findMessageById(senderJid, messageId);
            if (message.isEmpty()) {
                return;
            }

            switch (message.get()) {
                case ChatMessageInfo chatMessageInfo -> updateChatReceipt(node, senderJid, chatMessageInfo);
                case NewsletterMessageInfo newsletterMessageInfo -> updateNewsletterReceipt(node, newsletterMessageInfo);
                default -> throw new IllegalStateException("Unexpected value: " + message.get());
            }
        });
    }

    private Stream<String> getReceiptsMessageIds(Node node) {
        var outerId = node.attributes()
                .getOptionalString("id")
                .stream();
        var innerIds = node.findChild("list")
                .stream()
                .map(list -> list.listChildren("item"))
                .flatMap(Collection::stream)
                .map(item -> item.attributes().getOptionalString("id"))
                .flatMap(Optional::stream);
        return Stream.concat(outerId, innerIds);
    }


    private void updateChatReceipt(Node node, Jid chatJid, ChatMessageInfo message) {
        var status = node.attributes()
                .getOptionalString("type")
                .flatMap(MessageStatus::of)
                .orElse(MessageStatus.DELIVERED);
        whatsapp.store().findChatByJid(chatJid).ifPresent(chat -> {
            var newCount = chat.unreadMessagesCount() - 1;
            chat.setUnreadMessagesCount(newCount);
            var participant = node.attributes()
                    .getOptionalJid("participant")
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
        if(node.attributes().hasValue("type", "retry")) {
            acceptMessageRetry(message);
        }
        message.setStatus(status);
    }

    private void updateNewsletterReceipt(Node node, NewsletterMessageInfo message) {
        var messageStatus = node.attributes()
                .getOptionalString("type")
                .flatMap(MessageStatus::of);
        if (messageStatus.isPresent()) {
            message.setStatus(messageStatus.get());
            for (var listener : whatsapp.store().listeners()) {
                Thread.startVirtualThread(() -> listener.onMessageStatus(message));
                Thread.startVirtualThread(() -> listener.onMessageStatus(whatsapp, message));
            }
        }
        if(node.attributes().hasValue("type", "retry")) {
            acceptMessageRetry(message);
        }
    }

    private void acceptMessageRetry(MessageInfo message) {
        if (!message.fromMe()) {
            return;
        }

        if (!retries.add(message.id())) {
            return;
        }

        try {
            whatsapp.querySessionsForcefully(message.senderJid());
            var all = message.senderJid().device() == 0;
            var recipients = all ? null : Set.of(message.senderJid());
            var request = switch (message) {
                case ChatMessageInfo chatMessageInfo -> new MessageRequest.Chat(chatMessageInfo, recipients, !all, false, null);
                case NewsletterMessageInfo newsletterMessageInfo -> new MessageRequest.Newsletter(newsletterMessageInfo, recipients, !all, false, null);
                case QuotedMessageInfo _ -> throw new IllegalStateException("Unexpected value");
            };
            whatsapp.sendMessage(request);
        } catch (Exception ignored) {

        }
    }

    @Override
    public void dispose() {
        retries.clear();
    }
}
