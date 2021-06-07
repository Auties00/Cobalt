package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a live location inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class LiveLocationMessage implements ContextualMessage {
  @JsonProperty(value = "17")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "8")
  private int timeOffset;

  @JsonProperty(value = "7")
  private long sequenceNumber;

  @JsonProperty(value = "6")
  private String caption;

  @JsonProperty(value = "5")
  private int degreesClockwiseFromMagneticNorth;

  @JsonProperty(value = "4")
  private float speedInMps;

  @JsonProperty(value = "3")
  private int accuracyInMeters;

  @JsonProperty(value = "2")
  private double degreesLongitude;

  @JsonProperty(value = "1")
  private double degreesLatitude;
}
