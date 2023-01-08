package it.auties.whatsapp.model.action;

import static it.auties.protobuf.base.ProtobufType.BOOL;

import it.auties.protobuf.base.ProtobufProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * A model clas that represents unsupported actions for android
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder(access = AccessLevel.PROTECTED)
@Jacksonized
@Accessors(fluent = true)
public final class AndroidUnsupportedActions
    implements Action {

  /**
   * Whether unsupported actions are allowed
   */
  @ProtobufProperty(index = 1, type = BOOL)
  private boolean allowed;

  /**
   * The name of this action
   *
   * @return a non-null string
   */
  @Override
  public String indexName() {
    return "android_unsupported_actions";
  }
}
