package it.auties.whatsapp.protobuf.message.standard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.message.model.ContextualMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
import lombok.experimental.SuperBuilder;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a live location inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newLiveLocationMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class LiveLocationMessage extends ContextualMessage {
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
   * The accuracy in meters of the location that this message wraps
   */
  @JsonProperty("3")
  @JsonPropertyDescription("uint32")
  private int accuracy;

  /**
   * The speed in meters per second of the device that sent this live location message
   */
  @JsonProperty("4")
  @JsonPropertyDescription("float")
  private float speed;

  /**
   * Degrees Clockwise from Magnetic North
   */
  @JsonProperty("5")
  @JsonPropertyDescription("uint32")
  private int magneticNorthOffset;

  /**
   * The caption of this message
   */
  @JsonProperty("6")
  @JsonPropertyDescription("string")
  private String caption;

  /**
   * This property probably refers to the number of updates that this live location message.
   */
  @JsonProperty("7")
  @JsonPropertyDescription("uint64")
  private long sequenceNumber;

  /**
   * This offset probably refers to the endTimeStamp since the last update to this live location message.
   * In addition, it is measured in seconds since {@link java.time.Instant#EPOCH}.
   */
  @JsonProperty("8")
  @JsonPropertyDescription("uint32")
  private int timeOffset;

  /**
   * The thumbnail for this live location message encoded as jpeg in an array of bytes
   */
  @JsonProperty("16")
  @JsonPropertyDescription("bytes")
  private byte[] thumbnail;
}
