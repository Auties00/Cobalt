package com.github.auties00.cobalt.socket.message;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.model.info.ChatMessageInfo;
import com.github.auties00.cobalt.model.info.MessageInfo;
import com.github.auties00.cobalt.model.info.NewsletterMessageInfo;
import com.github.auties00.cobalt.model.info.QuotedMessageInfo;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.model.message.model.MessageStatus;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.socket.SocketStream;
import com.github.auties00.cobalt.util.Clock;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.github.auties00.cobalt.client.WhatsAppClientErrorHandler.Location.MESSAGE;

public final class MessageReceiptStreamNodeHandler extends SocketStream.Handler {
    private final DeviceService deviceService;
    private final Set<String> retries;

    public MessageReceiptStreamNodeHandler(WhatsAppClient whatsapp, DeviceService deviceService) {
        super(whatsapp, "receipt");
        this.deviceService = deviceService;
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
            acceptMessageRetry(node, message);
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
        // Newsletter messages don't support retry (they're plaintext)
    }

    private void acceptMessageRetry(Node node, MessageInfo message) {
        // Only retry messages we sent
        var meJid = whatsapp.store()
                .jid()
                .orElse(null);
        if (meJid == null) {
            return;
        }

        // Check if this message is from us (comparing sender with our JID)
        var senderJid = message.senderJid();
        if (senderJid == null || !senderJid.toUserJid().equals(meJid.toUserJid())) {
            return;
        }

        // Prevent duplicate retry attempts for the same message
        if (!retries.add(message.id())) {
            return;
        }

        // Get the device that needs the retry from the receipt
        var retryDeviceJid = node.getAttributeAsJid("from").orElse(null);
        if (retryDeviceJid == null) {
            return;
        }

        switch (message) {
            // Chat messages support messages retries
            case ChatMessageInfo chatMessage -> Thread.startVirtualThread(() -> {
                try {
                    // Query fresh session for the retry device
                    var deviceJids = deviceService.queryDevices(List.of(retryDeviceJid.toUserJid()));
                    if (deviceJids.isEmpty()) {
                        return;
                    }

                    // Resend the message to the specific device
                    whatsapp.resendMessage(chatMessage, retryDeviceJid);
                } catch (Throwable throwable) {
                    whatsapp.handleFailure(MESSAGE, throwable);
                }
            });

            // Newsletter messages don't support retry (they're plaintext)
            case NewsletterMessageInfo _ -> {}

            case QuotedMessageInfo _ -> throw new IllegalArgumentException("QuotedMessages cannot be retried");
        }
    }

    @Override
    public void reset() {
        retries.clear();
    }
}
