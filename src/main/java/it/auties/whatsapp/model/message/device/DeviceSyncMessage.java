package it.auties.whatsapp.model.message.device;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.message.model.DeviceMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.BYTES;

/**
 * A model class that represents a message that refers to a message sent by the device paired with the active WhatsappWeb session to dataSync.
 */
@AllArgsConstructor(staticName = "newDeviceSyncMessage")
@NoArgsConstructor
@Data
@Jacksonized
@Builder(builderMethodName = "newDeviceSyncMessage")
@Accessors(fluent = true)
public final class DeviceSyncMessage implements DeviceMessage {
    /**
     * The data that this synchronization wraps encoded as xml and stored in an array of bytes
     */
    @ProtobufProperty(index = 1, type = BYTES)
    private byte[] serializedXmlBytes;
}
