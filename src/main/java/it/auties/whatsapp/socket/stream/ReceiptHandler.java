package it.auties.whatsapp.socket.stream;

import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.info.NewsletterMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.model.message.model.MessageStatus;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.request.MessageRequest;
import it.auties.whatsapp.socket.SocketConnection;
import it.auties.whatsapp.util.Clock;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

final class ReceiptHandler extends NodeHandler.Dispatcher {
    private static final int MAX_MESSAGE_RETRIES = 5;
    private final ConcurrentMap<String, Integer> retries;

    ReceiptHandler(SocketConnection socketConnection) {
        super(socketConnection, "receipt");
        this.retries = new ConcurrentHashMap<>();
    }

    @Override
    void execute(Node node) {
        var senderJid = node.attributes()
                .getRequiredJid("from");
        updateReceipt(node, senderJid);
        socketConnection.sendMessageAck(senderJid, node);
    }

    private void updateReceipt(Node node, Jid senderJid) {
        getReceiptsMessageIds(node).forEachOrdered(messageId -> {
            var message = socketConnection.store().findMessageById(senderJid, messageId);
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

    private void updateChatReceipt(Node node, Jid chatJid, ChatMessageInfo message) {
        var status = node.attributes()
                .getOptionalString("type")
                .flatMap(MessageStatus::of)
                .orElse(MessageStatus.DELIVERED);
        socketConnection.store().findChatByJid(chatJid).ifPresent(chat -> {
            var newCount = chat.unreadMessagesCount() - 1;
            chat.setUnreadMessagesCount(newCount);
            var participant = node.attributes()
                    .getOptionalJid("participant")
                    .flatMap(socketConnection.store()::findContactByJid)
                    .orElse(null);
            var target = participant != null ? participant.jid() : message.senderJid();
            if(status == MessageStatus.READ) {
                message.receipt().addReadJid(target);
            }else {
                message.receipt().addDeliveredJid(target);
            }
            if(chat.jid().hasServer(JidServer.groupOrCommunity())) {
                var metadata = socketConnection.queryGroupOrCommunityMetadata(chat.jid());
                var jids = status == MessageStatus.READ ? message.receipt().readJids() : message.receipt().deliveredJids();
                if (participant == null || metadata.participants().size() == jids.size()) {
                    switch (status) {
                        case READ -> message.receipt().setReadTimestampSeconds(Clock.nowSeconds());
                        case PLAYED -> message.receipt().setPlayedTimestampSeconds(Clock.nowSeconds());
                    }
                }
            }
            socketConnection.onMessageStatus(message);
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
            socketConnection.onMessageStatus(message);
        }
        if(node.attributes().hasValue("type", "retry")) {
            // TODO: Support newsletter messages retries
        }
    }

    private void acceptMessageRetry(ChatMessageInfo message) {
        if (!message.fromMe()) {
            return;
        }

        var attempts = retries.getOrDefault(message.id(), 0);
        if (attempts > MAX_MESSAGE_RETRIES) {
            return;
        }

        try {
            socketConnection.querySessionsForcefully(message.senderJid());
            var all = message.senderJid().device() == 0;
            var recipients = all ? null : Set.of(message.senderJid());
            var request = new MessageRequest.Chat(message, recipients, !all, false, null);
            socketConnection.sendMessage(request);
            retries.put(message.id(), attempts + 1);
        } catch (Exception ignored) {

        }
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

    @Override
    void dispose() {
        super.dispose();
        retries.clear();
    }
}
