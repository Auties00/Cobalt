package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * A model class that represents a WhatsappMessage that refers to a message sent by the device paired with the active WhatsappWeb session to sync.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class DeviceSyncMessage implements Message {
  /**
   * The data that this synchronization wraps encoded as xml and stored in an array of bytes
   */
  @JsonProperty(value = "1")
  private byte[] serializedXmlBytes;
}
