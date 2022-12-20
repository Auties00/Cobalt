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

/**
 * A model class that represents a message that refers to a message sent by the device paired with the active WhatsappWeb session to dataSync.
 */
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public final class DeviceSyncMessage
        implements ServerMessage {
    /**
     * The data that this synchronization wraps encoded as xml and stored in an array of bytes
     */
    @ProtobufProperty(index = 1, type = BYTES)
    private byte[] serializedXmlBytes;

    @Override
    public MessageType type() {
        return MessageType.DEVICE_SYNC;
    }
}
