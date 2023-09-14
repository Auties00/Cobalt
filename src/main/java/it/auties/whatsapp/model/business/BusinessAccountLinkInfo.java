package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * A model class that holds a payload about a business link info.
 */
public record BusinessAccountLinkInfo(
        @ProtobufProperty(index = 1, type = ProtobufType.UINT64)
        long businessId,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        @NonNull
        String phoneNumber,
        @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
        long issueTimeSeconds,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        @NonNull
        BusinessStorageType hostStorage,
        @ProtobufProperty(index = 5, type = ProtobufType.OBJECT)
        @NonNull
        BusinessAccountType accountType
) implements ProtobufMessage {
    /**
     * Returns this object's timestampSeconds
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> issueTime() {
        return Clock.parseSeconds(issueTimeSeconds);
    }
}