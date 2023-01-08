package it.auties.whatsapp.model.message.standard;

import static it.auties.protobuf.base.ProtobufType.BOOL;
import static it.auties.protobuf.base.ProtobufType.BYTES;
import static it.auties.protobuf.base.ProtobufType.DOUBLE;
import static it.auties.protobuf.base.ProtobufType.FLOAT;
import static it.auties.protobuf.base.ProtobufType.STRING;
import static it.auties.protobuf.base.ProtobufType.UINT32;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * A model class that represents a message holding a location inside
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Jacksonized
@Accessors(fluent = true)
public final class LocationMessage
    extends ContextualMessage {

  /**
   * The latitude of the location that this message wraps
   */
  @ProtobufProperty(index = 1, type = DOUBLE)
  private Double latitude;

  /**
   * The longitude of the location that this message wraps
   */
  @ProtobufProperty(index = 2, type = DOUBLE)
  private Double longitude;

  /**
   * The name of the location that this message wraps
   */
  @ProtobufProperty(index = 3, type = STRING)
  private String name;

  /**
   * The address of the location that this message wraps
   */
  @ProtobufProperty(index = 4, type = STRING)
  private String address;

  /**
   * A URL to visit the location that this message wraps in Google Maps
   */
  @ProtobufProperty(index = 5, type = STRING)
  private String url;

  /**
   * Determines whether this message is a {@link LiveLocationMessage}
   */
  @ProtobufProperty(index = 6, type = BOOL)
  private boolean live;

  /**
   * The accuracy in meters of the location that this message wraps
   */
  @ProtobufProperty(index = 7, type = UINT32)
  private Integer accuracy;

  /**
   * The speed in meters per second of the device that sent this live location message
   */
  @ProtobufProperty(index = 8, type = FLOAT)
  private Float speed;

  /**
   * Degrees Clockwise from Magnetic North
   */
  @ProtobufProperty(index = 9, type = UINT32)
  private Integer magneticNorthOffset;

  /**
   * The caption of this message
   */
  @ProtobufProperty(index = 11, type = STRING)
  private String caption;

  /**
   * The thumbnail for this image message encoded as jpeg in an array of bytes
   */
  @ProtobufProperty(index = 16, type = BYTES)
  private byte[] thumbnail;

  @Override
  public MessageType type() {
    return MessageType.LOCATION;
  }

  @Override
  public MessageCategory category() {
    return MessageCategory.STANDARD;
  }
}
