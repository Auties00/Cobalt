package it.auties.whatsapp.model.info;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

/**
 * A model class that holds the information related to a native flow.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class NativeFlowInfo implements Info {
  /**
   * The name of the flow
   */
  @ProtobufProperty(index = 1, type = STRING)
  private String name;

  /**
   * The params of the flow, encoded as json
   */
  @ProtobufProperty(index = 2, type = STRING)
  private String paramsJson;
}
