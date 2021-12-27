package it.auties.whatsapp.util;

import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.whatsapp.crypto.SignalGroup;
import it.auties.whatsapp.crypto.SignalSession;
import it.auties.whatsapp.exchange.Node;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.protobuf.info.MessageInfo;
import it.auties.whatsapp.protobuf.message.model.MessageKey;
import it.auties.whatsapp.protobuf.message.server.SenderKeyDistributionMessage;
import it.auties.whatsapp.protobuf.signal.message.SignalDistributionMessage;
import it.auties.whatsapp.protobuf.signal.message.SignalMessage;
import it.auties.whatsapp.protobuf.signal.message.SignalPreKeyMessage;
import it.auties.whatsapp.protobuf.signal.sender.SenderKeyName;
import it.auties.whatsapp.protobuf.signal.sender.SenderKeyRecord;
import it.auties.whatsapp.protobuf.signal.sender.SenderKeyStructure;
import it.auties.whatsapp.protobuf.signal.session.ProtocolAddress;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

@UtilityClass
@Log
public class Messages {
    public List<MessageInfo> decodeMessages(Node node, WhatsappStore store, WhatsappKeys keys){
        var from = node.attributes().getJid("from")
                .orElseThrow(() -> new NoSuchElementException("Missing from"));
        var recipient = node.attributes().getJid("recipient");
        var participant = node.attributes().getJid("participant");
        var messageBuilder = MessageInfo.newMessageInfo();
        var keyBuilder = MessageKey.newMessageKey();
        switch (from.type()){
            case USER -> {
                keyBuilder.chatJid(recipient.orElse(from));
                messageBuilder.senderJid(from);
            }

            case GROUP, BROADCAST -> {
                var sender = participant.orElseThrow(() -> new NoSuchElementException("Missing participant in group message"));
                keyBuilder.chatJid(from);
                messageBuilder.senderJid(sender);
            }

            default -> throw new IllegalArgumentException("Unsupported message type: %s".formatted(from.type().name()));
        }

        keyBuilder.storeUuid(store.uuid());
        messageBuilder.key(keyBuilder.create());
        var modelMessage = messageBuilder.create();
        var encodedMessages = node.findNodes("enc");
        var results = new ArrayList<MessageInfo>();
        encodedMessages.forEach(messageNode -> {
            try {
                var encodedMessage = (byte[]) messageNode.content();
                var sender = modelMessage.senderJid().toString().split("@")[0];
                var senderAddress = new ProtocolAddress(sender, 0);
                var messageType = messageNode.attributes().getString("type");
                var buffer = switch (messageType) {
                    case "skmsg" -> {
                        var senderName = new SenderKeyName(from.toString(), senderAddress);
                        var signalGroup = new SignalGroup(senderName, keys);
                        yield signalGroup.decipher(encodedMessage);
                    }

                    case "pkmsg" -> {
                        var preKey = new SignalPreKeyMessage(encodedMessage);
                        var session = new SignalSession(senderAddress, keys);
                        yield session.decipher(preKey);
                    }

                    case "msg" -> {
                        var session = new SignalSession(senderAddress, keys);
                        yield session.decipher(new SignalMessage(encodedMessage));
                    }


                    default -> throw new IllegalArgumentException("Unsupported message type: %s".formatted(messageType));
                };

                var bufferWithNoPadding = Arrays.copyOfRange(buffer, 0, buffer.length - buffer[buffer.length - 1]);
                var info = ProtobufDecoder.forType(MessageInfo.class).decode(bufferWithNoPadding);
                if(info.message().content() instanceof SenderKeyDistributionMessage distributionMessage) {
                    var group = new SignalGroup(keys);
                    var groupJid = from.toString().split("@")[0];
                    var groupAddress = new ProtocolAddress(groupJid, 0);
                    var groupName = new SenderKeyName(distributionMessage.groupId(), groupAddress);
                    var signalDistributionMessage = new SignalDistributionMessage(0, 0, null, distributionMessage.axolotlSenderKeyDistributionMessage());
                    var senderKey = keys.findSenderKeyByName(groupName);
                    if(senderKey.isEmpty()){
                        var structure = new SenderKeyStructure(groupName, new SenderKeyRecord());
                        keys.senderKeyStructures().add(structure);
                    }

                    group.process(groupName, signalDistributionMessage);
                }

                info.key(modelMessage.key());
                info.senderJid(modelMessage.senderJid());
                results.add(info);
            } catch (Throwable throwable) {
                log.warning("An exception occurred while processing a message");
                log.warning("This message will not be decoded and the application should continue running");
                log.warning("Please report this issue on GitHub with the following stacktrace:");
                throwable.printStackTrace();
            }
        });

        return results;
    }
}