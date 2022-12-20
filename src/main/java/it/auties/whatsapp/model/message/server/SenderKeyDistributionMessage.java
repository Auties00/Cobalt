package it.auties.whatsapp.model.message.server;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.ServerMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.BYTES;
import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents a message sent by WhatsappWeb for security purposes.
 * Whatsapp follows the Signal Standard, for more information about this message visit <a href="https://archive.kaidan.im/libsignal-protocol-c-docs/html/struct___textsecure_____sender_key_distribution_message.html">their documentation</a>
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class SenderKeyDistributionMessage
        implements ServerMessage {
    /**
     * The jid of the sender
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String groupId;

    /**
     * The sender key
     */
    @ProtobufProperty(index = 2, type = BYTES)
    private byte[] data;

    @Override
    public MessageType type() {
        return MessageType.SENDER_KEY_DISTRIBUTION;
    }
}
