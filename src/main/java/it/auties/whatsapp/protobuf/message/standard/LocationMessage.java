package it.auties.whatsapp.protobuf.message.standard;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.message.model.ContextualMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a location inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newLocationMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class LocationMessage extends ContextualMessage {
  /**
   * The latitude of the location that this message wraps
   */
  @JsonProperty("1")
  private double latitude;

  /**
   * The longitude of the location that this message wraps
   */
  @JsonProperty("2")
  private double longitude;

  /**
   * The name of the location that this message wraps
   */
  @JsonProperty("3")
  private String name;

  /**
   * The address of the location that this message wraps
   */
  @JsonProperty("4")
  private String address;

  /**
   * A URL to visit the location that this message wraps in Google Maps
   */
  @JsonProperty("5")
  private String url;

  /**
   * Determines whether this message is a {@link LiveLocationMessage}
   */
  @JsonProperty("6")
  private boolean live;

  /**
   * The accuracy in meters of the location that this message wraps
   */
  @JsonProperty("7")
  private int accuracy;

  /**
   * The speed in meters per second of the device that sent this live location message
   */
  @JsonProperty("8")
  private float speed;

  /**
   * Degrees Clockwise from Magnetic North
   */
  @JsonProperty("9")
  private int magneticNorthOffset;

  /**
   * The caption of this message
   */
  @JsonProperty("11")
  private String caption;

  /**
   * The thumbnail for this image message encoded as jpeg in an array of bytes
   */
  @JsonProperty("16")
  private byte[] thumbnail;
}
