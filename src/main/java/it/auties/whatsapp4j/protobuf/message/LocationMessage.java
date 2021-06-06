package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a location inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class LocationMessage implements ContextualMessage {
  @JsonProperty(value = "17")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "11")
  private String comment;

  @JsonProperty(value = "9")
  private int degreesClockwiseFromMagneticNorth;

  @JsonProperty(value = "8")
  private float speedInMps;

  @JsonProperty(value = "7")
  private int accuracyInMeters;

  @JsonProperty(value = "6")
  private boolean isLive;

  @JsonProperty(value = "5")
  private String url;

  @JsonProperty(value = "4")
  private String address;

  @JsonProperty(value = "3")
  private String name;

  @JsonProperty(value = "2")
  private double degreesLongitude;

  @JsonProperty(value = "1")
  private double degreesLatitude;
}
