package it.auties.whatsapp.model.message.standard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

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
@Jacksonized
@Accessors(fluent = true)
public final class LiveLocationMessage extends ContextualMessage {
  /**
   * The latitude of the location that this message wraps
   */
  @ProtobufProperty(index = 1, type = DOUBLE)
  private double latitude;
  
  /**
   * The longitude of the location that this message wraps
   */
  @ProtobufProperty(index = 2, type = DOUBLE)
  private double longitude;

  /**
   * The accuracy in meters of the location that this message wraps
   */
  @ProtobufProperty(index = 3, type = UINT32)
  private int accuracy;

  /**
   * The speed in meters per second of the device that sent this live location message
   */
  @ProtobufProperty(index = 4, type = FLOAT)
  private float speed;

  /**
   * Degrees Clockwise from Magnetic North
   */
  @ProtobufProperty(index = 5, type = UINT32)
  private int magneticNorthOffset;

  /**
   * The caption of this message
   */
  @ProtobufProperty(index = 6, type = STRING)
  private String caption;

  /**
   * This property probably refers to the number of updates that this live location message.
   */
  @ProtobufProperty(index = 7, type = UINT64)
  private long sequenceNumber;

  /**
   * This offset probably refers to the endTimeStamp since the last update to this live location message.
   * In addition, it is measured in seconds since {@link java.time.Instant#EPOCH}.
   */
  @ProtobufProperty(index = 8, type = UINT32)
  private int timeOffset;

  /**
   * The thumbnail for this live location message encoded as jpeg in an array of bytes
   */
  @ProtobufProperty(index = 16, type = BYTES)
  private byte[] thumbnail;
}
