package it.auties.whatsapp.protobuf.message.device;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.message.model.DeviceMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * A model class that represents a WhatsappMessage that refers to a message sent by the device paired with the active WhatsappWeb session to dataSync.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor(staticName = "newDeviceSyncMessage")
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class DeviceSyncMessage implements DeviceMessage {
  /**
   * The data that this synchronization wraps encoded as xml and stored in an array of bytes
   */
  @JsonProperty("1")
  @JsonPropertyDescription("bytes")
  private byte[] serializedXmlBytes;
}
