package it.auties.whatsapp.model.action;

import static it.auties.protobuf.base.ProtobufType.BOOL;
import static it.auties.protobuf.base.ProtobufType.INT64;

import it.auties.protobuf.base.ProtobufProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * A model clas that represents a message deleted for this client
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder(access = AccessLevel.PROTECTED)
@Jacksonized
@Accessors(fluent = true)
public final class DeleteMessageForMeAction
    implements Action {

  /**
   * Whether the media should be removed from the memory of the client
   */
  @ProtobufProperty(index = 1, type = BOOL)
  private boolean deleteMedia;

  /**
   * The timestamp of the message
   */
  @ProtobufProperty(index = 2, type = INT64)
  private Long messageTimestamp;

  /**
   * The name of this action
   *
   * @return a non-null string
   */
  @Override
  public String indexName() {
    return "deleteMessageForMe";
  }
}
