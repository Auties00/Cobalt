package it.auties.whatsapp.model.action;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Jacksonized
@Builder
@ProtobufName("SubscriptionAction")
public final class SubscriptionAction implements Action {
    @ProtobufProperty(index = 1, name = "isDeactivated", type = ProtobufType.BOOL)
    private boolean deactivated;

    @ProtobufProperty(index = 2, name = "isAutoRenewing", type = ProtobufType.BOOL)
    private boolean autoRenewing;

    @ProtobufProperty(index = 3, name = "expirationDate", type = ProtobufType.INT64)
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
