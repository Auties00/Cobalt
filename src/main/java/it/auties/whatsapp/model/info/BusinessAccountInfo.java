package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.business.BusinessAccountType;
import it.auties.whatsapp.model.business.BusinessStorageType;
import it.auties.whatsapp.util.Clock;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.ZonedDateTime;
import java.util.Optional;


/**
 * A model class that holds the information related to a business account.
 */
public record BusinessAccountInfo(
        @ProtobufProperty(index = 1, type = ProtobufType.UINT64)
        long facebookId,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        @NonNull
        String accountNumber,
        @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
        long timestampSeconds,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        @NonNull
        BusinessStorageType hostStorage,
        @ProtobufProperty(index = 5, type = ProtobufType.OBJECT)
        @NonNull
        BusinessAccountType accountType
) implements Info, ProtobufMessage {
    /**
     * Returns the timestampSeconds for this message
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> timestamp() {
        return Clock.parseSeconds(timestampSeconds);
    }
}
