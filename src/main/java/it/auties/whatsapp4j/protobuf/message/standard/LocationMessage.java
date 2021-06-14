package it.auties.whatsapp4j.protobuf.message.standard;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.whatsapp.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.message.model.ContextualMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a location inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newLocationMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class LocationMessage extends ContextualMessage {
  /**
   * The thumbnail for this image message encoded as jpeg in an array of bytes
   */
  @JsonProperty(value = "16")
  private byte[] jpegThumbnail;

  /**
   * The caption of this message
   */
  @JsonProperty(value = "11")
  private String caption;

  /**
   * Degrees Clockwise from Magnetic North
   */
  @JsonProperty(value = "9")
  private int degreesClockwiseFromMagneticNorth;

  /**
   * The speed in meters per second of the device that sent this live location message
   */
  @JsonProperty(value = "8")
  private float speedInMps;

  /**
   * The accuracy in meters of the location that this message wraps
   */
  @JsonProperty(value = "7")
  private int accuracyInMeters;

  /**
   * Determines whether this message is a {@link LiveLocationMessage}
   */
  @JsonProperty(value = "6")
  private boolean isLive;

  /**
   * A URL to visit the location that this message wraps in Google Maps
   */
  @JsonProperty(value = "5")
  private String url;

  /**
   * The address of the location that this message wraps
   */
  @JsonProperty(value = "4")
  private String address;

  /**
   * The name of the location that this message wraps
   */
  @JsonProperty(value = "3")
  private String name;

  /**
   * The longitude of the location that this message wraps
   */
  @JsonProperty(value = "2")
  private double degreesLongitude;

  /**
   * The latitude of the location that this message wraps
   */
  @JsonProperty(value = "1")
  private double degreesLatitude;
}
