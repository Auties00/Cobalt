package it.auties.whatsapp.model.message.device;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.message.model.DeviceMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.BYTES;

/**
 * A model class that represents a WhatsappMessage that refers to a message sent by the device paired with the active WhatsappWeb session to dataSync.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor(staticName = "newDeviceSyncMessage")
@NoArgsConstructor
@Data
@Jacksonized
@Builder(builderMethodName = "newDeviceSyncMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class DeviceSyncMessage implements DeviceMessage {
  /**
   * The data that this synchronization wraps encoded as xml and stored in an array of bytes
   */
  @ProtobufProperty(index = 1, type = BYTES)
  private byte[] serializedXmlBytes;
}
