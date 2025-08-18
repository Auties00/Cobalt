package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A model clas that represents a subscription
 */
@ProtobufMessage(name = "SyncActionValue.SubscriptionAction")
public final class SubscriptionAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean deactivated;

    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    final boolean autoRenewing;

    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    final long expirationDateSeconds;

    SubscriptionAction(boolean deactivated, boolean autoRenewing, long expirationDateSeconds) {
        this.deactivated = deactivated;
        this.autoRenewing = autoRenewing;
        this.expirationDateSeconds = expirationDateSeconds;
    }

    @Override
    public String indexName() {
        return "subscription";
    }

    @Override
    public int actionVersion() {
        return 1;
    }

    public boolean deactivated() {
        return deactivated;
    }

    public boolean autoRenewing() {
        return autoRenewing;
    }

    public long expirationDateSeconds() {
        return expirationDateSeconds;
    }

    public Optional<ZonedDateTime> expirationDate() {
        return Clock.parseSeconds(expirationDateSeconds);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SubscriptionAction that
                && deactivated == that.deactivated
                && autoRenewing == that.autoRenewing
                && expirationDateSeconds == that.expirationDateSeconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deactivated, autoRenewing, expirationDateSeconds);
    }

    @Override
    public String toString() {
        return "SubscriptionAction[" +
                "deactivated=" + deactivated + ", " +
                "autoRenewing=" + autoRenewing + ", " +
                "expirationDateSeconds=" + expirationDateSeconds + ']';
    }
}
