package it.auties.whatsapp.util;

import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.whatsapp.crypto.GroupCipher;
import it.auties.whatsapp.crypto.SessionCipher;
import it.auties.whatsapp.exchange.Node;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.protobuf.info.MessageInfo;
import it.auties.whatsapp.protobuf.message.model.MessageKey;
import it.auties.whatsapp.protobuf.message.server.SenderKeyDistributionMessage;
import it.auties.whatsapp.protobuf.signal.message.SignalDistributionMessage;
import it.auties.whatsapp.protobuf.signal.message.SignalMessage;
import it.auties.whatsapp.protobuf.signal.message.SignalPreKeyMessage;
import it.auties.whatsapp.protobuf.signal.sender.SenderKeyName;
import it.auties.whatsapp.protobuf.signal.sender.SenderKeyRecord;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

import java.io.IOException;
import java.util.*;

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
        var modelMessage = messageBuilder.key(key)
                .timestamp(timestamp)
                .create();
        return node.findNodes("enc")
                .stream()
                .map(messageNode -> decodeMessage(modelMessage, messageNode, from, keys))
                .toList();
    }

    private MessageInfo decodeMessage(MessageInfo model, Node node, ContactJid from, WhatsappKeys keys) {
        try {
            var encodedMessage = node.bytes();
            var messageType = node.attributes().getString("type");
            var buffer = decodeCipheredMessage(encodedMessage, messageType, from, keys);
            var message = decodeMessageInfo(buffer);
            handleSenderKeyMessage(keys, from, message);
            return message.key(model.key())
                    .senderJid(model.senderJid());
        } catch (Throwable throwable) {
            log.warning("An exception occurred while processing a message: %s".formatted(throwable.getMessage()));
            log.warning("This message will not be decoded and the application should continue running");
            throwable.printStackTrace();
            return null;
        }
    }

    private byte[] decodeCipheredMessage(byte[] message, String type, ContactJid from, WhatsappKeys keys) {
        return switch (type) {
            case "skmsg" -> {
                var senderName = new SenderKeyName(from.toString(), from.toSignalAddress());
                var signalGroup = new GroupCipher(senderName, keys);
                yield signalGroup.decrypt(message);
            }

            case "pkmsg" -> {
                var session = new SessionCipher(from.toSignalAddress(), keys);
                var preKey = SignalPreKeyMessage.ofSerialized(message);
                yield session.decrypt(preKey);
            }

            case "msg" -> {
                var session = new SessionCipher(from.toSignalAddress(), keys);
                var signalMessage = SignalMessage.ofSerialized(message);
                yield session.decrypt(signalMessage);
            }

            default -> throw new IllegalArgumentException("Unsupported message type: %s".formatted(type));
        };
    }

    private MessageInfo decodeMessageInfo(byte[] buffer) throws IOException {
        var bufferWithNoPadding = copyOfRange(buffer, 0, buffer.length - buffer[buffer.length - 1]);
        return ProtobufDecoder.forType(MessageInfo.class)
                .decode(bufferWithNoPadding);
    }

    private void handleSenderKeyMessage(WhatsappKeys keys, ContactJid from, MessageInfo info) {
        if (!(info.message().content() instanceof SenderKeyDistributionMessage distributionMessage)) {
            return;
        }

        var groupName = new SenderKeyName(distributionMessage.groupId(), from.toSignalAddress());
        var group = new GroupCipher(groupName, keys);
        var senderKey = keys.findSenderKeyByName(groupName);
        if(senderKey.isEmpty()){
            keys.addSenderKey(groupName, new SenderKeyRecord());
        }

        group.decrypt(distributionMessage.data());
    }
}