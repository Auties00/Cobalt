package it.auties.whatsapp.util;

import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.whatsapp.crypto.GroupCipher;
import it.auties.whatsapp.crypto.GroupSessionBuilder;
import it.auties.whatsapp.crypto.SessionCipher;
import it.auties.whatsapp.exchange.Node;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.protobuf.info.MessageInfo;
import it.auties.whatsapp.protobuf.message.model.MessageContainer;
import it.auties.whatsapp.protobuf.message.model.MessageKey;
import it.auties.whatsapp.protobuf.message.server.ProtocolMessage;
import it.auties.whatsapp.protobuf.message.server.SenderKeyDistributionMessage;
import it.auties.whatsapp.protobuf.signal.message.SignalDistributionMessage;
import it.auties.whatsapp.protobuf.signal.message.SignalMessage;
import it.auties.whatsapp.protobuf.signal.message.SignalPreKeyMessage;
import it.auties.whatsapp.protobuf.signal.sender.SenderKeyName;
import it.auties.whatsapp.protobuf.sync.HistorySync;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import static java.util.Arrays.copyOfRange;

@UtilityClass
@Log
public class Messages {
    public List<MessageInfo> decodeMessages(Node node, WhatsappStore store, WhatsappKeys keys) {
        var timestamp = node.attributes().getLong("t");
        var id = node.attributes().getRequiredString("id");
        var from = node.attributes().getJid("from")
                .orElseThrow(() -> new NoSuchElementException("Missing from"));
        var recipient = node.attributes().getJid("recipient")
                .orElse(from);
        var participant = node.attributes().getJid("participant")
                .orElse(null);
        var messageBuilder = MessageInfo.newMessageInfo();
        var keyBuilder = MessageKey.newMessageKey();
        switch (from.type()){
            case USER -> {
                keyBuilder.chatJid(recipient);
                messageBuilder.senderJid(from);
            }

            case GROUP, BROADCAST -> {
                var sender = Objects.requireNonNull(participant, "Missing participant in group message");
                keyBuilder.chatJid(from);
                messageBuilder.senderJid(sender);
            }

            default -> throw new IllegalArgumentException("Unsupported message type: %s".formatted(from.type().name()));
        }

        var key = keyBuilder.id(id)
                .storeUuid(store.uuid())
                .create();
        var info = messageBuilder.key(key)
                .timestamp(timestamp)
                .create();

        log.info("Starting to deserialize %s messages".formatted(node.findNodes("enc").size()));
        return node.findNodes("enc")
                .stream()
                .map(messageNode -> decodeMessage(info, messageNode, from, store, keys))
                .toList();
    }

    private MessageInfo decodeMessage(MessageInfo info, Node node, ContactJid from, WhatsappStore store, WhatsappKeys keys) {
        try {
            var encodedMessage = node.bytes();
            var messageType = node.attributes().getString("type");
            var buffer = decodeCipheredMessage(info, encodedMessage, messageType, keys);
            info.message(decodeMessageContainer(buffer));
            handleSenderKeyMessage(store, keys, from, info);
            return info;
        } catch (Throwable throwable) {
            log.warning("An exception occurred while processing a message: %s".formatted(throwable.getMessage()));
            log.warning("This message will not be decoded and the application should continue running");
            return null;
        }
    }

    private byte[] decodeCipheredMessage(MessageInfo info, byte[] message, String type, WhatsappKeys keys) {
        return switch (type) {
            case "skmsg" -> {
                var senderName = new SenderKeyName(info.chatJid().toString(), info.senderJid().toSignalAddress());
                var signalGroup = new GroupCipher(senderName, keys);
                yield signalGroup.decrypt(message);
            }

            case "pkmsg" -> {
                var session = new SessionCipher(info.chatJid().toSignalAddress(), keys);
                var preKey = SignalPreKeyMessage.ofSerialized(message);
                yield session.decrypt(preKey);
            }

            case "msg" -> {
                var session = new SessionCipher(info.chatJid().toSignalAddress(), keys);
                var signalMessage = SignalMessage.ofSerialized(message);
                yield session.decrypt(signalMessage);
            }

            default -> throw new IllegalArgumentException("Unsupported encoded message type: %s".formatted(type));
        };
    }

    private MessageContainer decodeMessageContainer(byte[] buffer) throws IOException {
        var bufferWithNoPadding = copyOfRange(buffer, 0, buffer.length - buffer[buffer.length - 1]);
        return ProtobufDecoder.forType(MessageContainer.class)
                .decode(bufferWithNoPadding);
    }

    @SneakyThrows
    private void handleSenderKeyMessage(WhatsappStore store, WhatsappKeys keys, ContactJid from, MessageInfo info) {
        switch (info.message().content()){
            case SenderKeyDistributionMessage distributionMessage -> handleDistributionMessage(keys, from, distributionMessage);
            case ProtocolMessage protocolMessage -> handleProtocolMessage(store, keys, info, protocolMessage);
            default -> {}
        }
    }

    private void handleDistributionMessage(WhatsappKeys keys, ContactJid from, SenderKeyDistributionMessage distributionMessage) {
        var groupName = new SenderKeyName(distributionMessage.groupId(), from.toSignalAddress());
        var builder = new GroupSessionBuilder(keys);
        var message = SignalDistributionMessage.ofSerialized(distributionMessage.data());
        builder.process(groupName, message);
    }

    private void handleProtocolMessage(WhatsappStore store, WhatsappKeys keys, MessageInfo info, ProtocolMessage protocolMessage) throws IOException {
        switch(protocolMessage.type()) {
            case HISTORY_SYNC_NOTIFICATION -> {
                    var historyBytes = Downloader.download(protocolMessage.historySyncNotification(), store);
                    var history = ProtobufDecoder.forType(HistorySync.class)
                            .decode(historyBytes);
                    switch(history.syncType()) {
                        case INITIAL_BOOTSTRAP -> history.conversations().forEach(store::addChat);
                        case RECENT -> history.conversations()
                                .forEach(recent -> store.findChatByJid(recent.jid().toString())
                                        .ifPresent(oldChat -> oldChat.messages().addAll(recent.messages())));
                        case PUSH_NAME -> history.pushNames()
                                .forEach(pushName -> store.findContactByJid(pushName.id())
                                        .ifPresent(contact -> contact.chosenName(pushName.pushname())));
                        case INITIAL_STATUS_V3 -> log.info("Got %s status".formatted(history.statusV3Messages()));
                    }

                // Send receipt
            }

            case APP_STATE_SYNC_KEY_SHARE -> {
                keys.appStateKeys().addAll(protocolMessage.appStateSyncKeyShare().keys());
                // Re-sync app state
            }

            case REVOKE -> {
                var chat = protocolMessage.key().chat()
                        .orElseThrow(() -> new NoSuchElementException("Missing chat"));
                var message = store.findMessageById(chat, protocolMessage.key().id())
                        .orElseThrow(() -> new NoSuchElementException("Missing message"));
                store.callListeners(listener -> listener.onMessageDeleted(message, true));
            }

            case EPHEMERAL_SETTING -> protocolMessage.key().chat()
                      .orElseThrow(() -> new NoSuchElementException("Missing chat"))
                      .ephemeralMessagesToggleTime(info.timestamp())
                      .ephemeralMessageDuration(protocolMessage.ephemeralExpiration());
        }
    }
}