package it.auties.whatsapp.model.action;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("AgentAction")
public final class AgentAction
    implements Action {

  @ProtobufProperty(index = 1, name = "name", type = ProtobufType.STRING)
  private String name;

  @ProtobufProperty(index = 2, name = "deviceID", type = ProtobufType.INT32)
  private Integer deviceId;

  @ProtobufProperty(index = 3, name = "isDeleted", type = ProtobufType.BOOL)
  private boolean deleted;

  /**
   * Always throws an exception as this action cannot be serialized
   *
   * @return an exception
   */
  @Override
  public String indexName() {
    throw new UnsupportedOperationException("Cannot send action: no index name");
  }
}
