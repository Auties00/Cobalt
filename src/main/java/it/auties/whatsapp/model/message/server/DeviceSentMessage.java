package it.auties.whatsapp.model.message.server;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.ServerMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents a message that refers to a message sent by the device paired with the active WhatsappWeb session.
 */
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public final class DeviceSentMessage
        implements ServerMessage {
    /**
     * The unique identifier that this message update regards.
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String destinationJid;

    /**
     * The message container that this object wraps.
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = MessageContainer.class)
    private MessageContainer message;

    /**
     * The hash of the destination chat
     */
    @ProtobufProperty(index = 3, type = STRING)
    private String phash;

    @Override
    public MessageType type() {
        return MessageType.DEVICE_SENT;
    }
}
