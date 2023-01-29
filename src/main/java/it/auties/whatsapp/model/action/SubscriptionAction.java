package it.auties.whatsapp.model.action;

import static it.auties.protobuf.base.ProtobufType.BOOL;
import static it.auties.protobuf.base.ProtobufType.INT64;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
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
@ProtobufName("SubscriptionAction")
public final class SubscriptionAction
    implements Action {

  @ProtobufProperty(index = 1, name = "isDeactivated", type = BOOL)
  private boolean deactivated;

  @ProtobufProperty(index = 2, name = "isAutoRenewing", type = BOOL)
  private boolean autoRenewing;

  @ProtobufProperty(index = 3, name = "expirationDate", type = INT64)
  private long expirationDate;

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
