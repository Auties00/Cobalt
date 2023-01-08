package it.auties.whatsapp.model.message.model;

import static it.auties.protobuf.base.ProtobufType.STRING;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * An action link for a button
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ActionLink
    implements ProtobufMessage {

  /**
   * The url of the action
   */
  @ProtobufProperty(index = 1, type = STRING)
  private String url;

  /**
   * The title of the action
   */
  @ProtobufProperty(index = 2, type = STRING)
  private String buttonTitle;
}