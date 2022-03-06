package it.auties.whatsapp.model.message.standard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.message.model.ContextualMessage;
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
  @JsonPropertyDescription("double")
  private double latitude;

  /**
   * The longitude of the location that this message wraps
   */
  @JsonProperty("2")
  @JsonPropertyDescription("double")
  private double longitude;

  /**
   * The name of the location that this message wraps
   */
  @JsonProperty("3")
  @JsonPropertyDescription("string")
  private String name;

  /**
   * The address of the location that this message wraps
   */
  @JsonProperty("4")
  @JsonPropertyDescription("string")
  private String address;

  /**
   * A URL to visit the location that this message wraps in Google Maps
   */
  @JsonProperty("5")
  @JsonPropertyDescription("string")
  private String url;

  /**
   * Determines whether this message is a {@link LiveLocationMessage}
   */
  @JsonProperty("6")
  @JsonPropertyDescription("bool")
  private boolean live;

  /**
   * The accuracy in meters of the location that this message wraps
   */
  @JsonProperty("7")
  @JsonPropertyDescription("uint32")
  private int accuracy;

  /**
   * The speed in meters per second of the device that sent this live location message
   */
  @JsonProperty("8")
  @JsonPropertyDescription("float")
  private float speed;

  /**
   * Degrees Clockwise from Magnetic North
   */
  @JsonProperty("9")
  @JsonPropertyDescription("uint32")
  private int magneticNorthOffset;

  /**
   * The caption of this message
   */
  @JsonProperty("11")
  @JsonPropertyDescription("string")
  private String caption;

  /**
   * The thumbnail for this image message encoded as jpeg in an array of bytes
   */
  @JsonProperty("16")
  @JsonPropertyDescription("bytes")
  private byte[] thumbnail;
}
