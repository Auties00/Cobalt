package it.auties.whatsapp4j.protobuf.message.device;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.protobuf.message.model.DeviceMessage;
import it.auties.whatsapp4j.protobuf.message.model.MessageContainer;
import it.auties.whatsapp4j.whatsapp.WhatsappAPI;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * A model class that represents a WhatsappMessage that refers to a message sent by the device paired with the active WhatsappWeb session.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor(staticName = "newDeviceSentMessage")
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class DeviceSentMessage implements DeviceMessage {
  /**
   * The message container that this object wraps.
   */
  @JsonProperty(value = "2")
  private MessageContainer message;

  /**
   * The unique identifier that this message update regards.
   */
  @JsonProperty(value = "1")
  private String destinationJid;
}
