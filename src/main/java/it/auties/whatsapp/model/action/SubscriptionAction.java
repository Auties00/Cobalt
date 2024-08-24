package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.PatchType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * A model clas that represents a subscription
 */
@ProtobufMessage(name = "SyncActionValue.SubscriptionAction")
public record SubscriptionAction(
        @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
        boolean deactivated,
        @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
        boolean autoRenewing,
        @ProtobufProperty(index = 3, type = ProtobufType.INT64)
        long expirationDateSeconds
) implements Action {
    /**
     * Returns when the subscription ends
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> expirationDate() {
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
    public int actionVersion() {
        return 1;
    }

    /**
     * The type of this action
     *
     * @return a non-null string
     */
    @Override
    public PatchType actionType() {
        return null;
    }
}
