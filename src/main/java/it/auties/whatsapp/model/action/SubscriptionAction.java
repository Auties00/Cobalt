package it.auties.whatsapp.model.action;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.binary.PatchType;
import it.auties.whatsapp.util.Clock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.time.ZonedDateTime;

import static it.auties.protobuf.base.ProtobufType.BOOL;
import static it.auties.protobuf.base.ProtobufType.INT64;

/**
 * A model clas that represents a subscription
 */
@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("SubscriptionAction")
public final class SubscriptionAction implements Action {
    @ProtobufProperty(index = 1, name = "isDeactivated", type = BOOL)
    private boolean deactivated;

    @ProtobufProperty(index = 2, name = "isAutoRenewing", type = BOOL)
    private boolean autoRenewing;

    @ProtobufProperty(index = 3, name = "expirationDate", type = INT64)
    private long expirationDateSeconds;

    /**
     * Returns when the subscription ends
     *
     * @return an optional
     */
    public ZonedDateTime messageTimestamp() {
        return Clock.parseSeconds(expirationDateSeconds);
    }

    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "subscription";
    }

    /**
     * The version of this action
     *
     * @return a non-null string
     */
    @Override
    public int version() {
        return 1;
    }

    /**
     * The type of this action
     *
     * @return a non-null string
     */
    @Override
    public PatchType type() {
        return null;
    }
}
