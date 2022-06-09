package it.auties.whatsapp.model.info;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

/**
 * A model class that holds the information related to a Whatsapp call.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class CallInfo implements Info {
  /**
   * The key of this call
   */
  @ProtobufProperty(index = 1, type = BYTES)
  private byte[] key;

  /**
   * The source of this call
   */
  @ProtobufProperty(index = 2, type = STRING)
  private String source;

  /**
   * The data of this call
   */
  @ProtobufProperty(index = 3, type = BYTES)
  private byte[] data;

  /**
   * The delay of this call in seconds
   */
  @ProtobufProperty(index = 4, type = UINT32)
  private int delay;
}
