package com.github.auties00.cobalt.io.message;

import com.github.auties00.cobalt.model.proto.message.model.ChatMessageKey;
import com.github.auties00.cobalt.model.proto.message.model.MessageContainer;
import com.github.auties00.cobalt.model.proto.message.model.MessageContainerSpec;
import com.github.auties00.libsignal.SignalProtocolStore;
import com.github.auties00.libsignal.SignalSessionCipher;
import com.github.auties00.libsignal.groups.SignalGroupCipher;
import com.github.auties00.libsignal.groups.SignalSenderKeyName;
import com.github.auties00.libsignal.protocol.SignalMessage;
import com.github.auties00.libsignal.protocol.SignalPreKeyMessage;
import com.github.auties00.libsignal.protocol.SignalSenderKeyDistributionMessage;
import it.auties.protobuf.stream.ProtobufInputStream;

import static com.github.auties00.cobalt.io.message.MessageConstants.*;

public final class MessageDecoder {
    private final SignalSessionCipher sessionCipher;
    private final SignalGroupCipher groupCipher;

    public MessageDecoder(SignalProtocolStore store) {
        this.groupCipher = new SignalGroupCipher(store);
        this.sessionCipher = new SignalSessionCipher(store);
    }

    // TODO: Can everything here be streamed? I think it's possible but very hard
    public MessageContainer decode(ChatMessageKey messageKey, String type, byte[] encodedMessage) {
        if(MSMG.equals(type)) {
            return MessageContainer.empty();
        }else {
            var result = switch (type) {
                case MSG -> {
                    var signalAddress = messageKey.senderJid()
                            .orElse(messageKey.chatJid())
                            .toSignalAddress();
                    var signalMessage = SignalMessage.ofSerialized(encodedMessage);
                    yield sessionCipher.decrypt(signalAddress, signalMessage);
                }
                case PKMSG -> {
                    var signalAddress = messageKey.senderJid()
                            .orElse(messageKey.chatJid())
                            .toSignalAddress();
                    var preKey = SignalPreKeyMessage.ofSerialized(encodedMessage);
                    yield sessionCipher.decrypt(signalAddress, preKey);
                }
                case SKMSG -> {
                    var groupJid = messageKey.chatJid();
                    var signalAddress = messageKey.senderJid()
                            .orElseThrow(() -> new IllegalArgumentException("Missing sender value"))
                            .toSignalAddress();
                    var senderName = new SignalSenderKeyName(groupJid.toString(), signalAddress);
                    yield groupCipher.decrypt(senderName, encodedMessage);
                }
                default -> throw new IllegalArgumentException("Unsupported encodedPoint message type: %s".formatted(type));
            };
            var messageLength = result.length - result[result.length - 1];
            return MessageContainerSpec.decode(ProtobufInputStream.fromBytes(result, 0, messageLength))
                    .unbox();
        }
    }

    public void process(SignalSenderKeyName groupName, SignalSenderKeyDistributionMessage signalDistributionMessage) {
        groupCipher.process(groupName, signalDistributionMessage);
    }
}
