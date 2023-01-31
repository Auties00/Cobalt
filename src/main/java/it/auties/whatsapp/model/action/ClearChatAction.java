package it.auties.whatsapp.model.action;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.sync.ActionMessageRangeSync;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * A model clas that represents a cleared chat
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder(access = AccessLevel.PROTECTED)
@Jacksonized
@Accessors(fluent = true)
public final class ClearChatAction
    implements Action {

  /**
   * The message range on which this action has effect
   */
  @ProtobufProperty(index = 1, type = MESSAGE, implementation = ActionMessageRangeSync.class)
  private ActionMessageRangeSync messageRange;

  /**
   * Returns the range of messages that were cleared
   *
   * @return an optional
   */
  public Optional<ActionMessageRangeSync> messageRange(){
    return Optional.ofNullable(messageRange);
  }

  /**
   * The name of this action
   *
   * @return a non-null string
   */
  @Override
  public String indexName() {
    return "clearChat";
  }
}
