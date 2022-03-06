package it.auties.whatsapp.model.message.device;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.message.model.DeviceMessage;
import it.auties.whatsapp.model.message.model.MessageContainer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * A model class that represents a WhatsappMessage that refers to a message sent by the device paired with the active WhatsappWeb session.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor(staticName = "newDeviceSentMessage")
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public final class DeviceSentMessage implements DeviceMessage {
  /**
   * The unique identifier that this message update regards.
   */
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String destinationJid;

  /**
   * The message container that this object wraps.
   */
  @JsonProperty("2")
  @JsonPropertyDescription("message")
  private MessageContainer message;
}
