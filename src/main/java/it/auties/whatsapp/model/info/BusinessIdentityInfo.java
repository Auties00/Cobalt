package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.business.BusinessActorsType;
import it.auties.whatsapp.model.business.BusinessStorageType;
import it.auties.whatsapp.model.business.BusinessVerifiedLevel;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificate;
import it.auties.whatsapp.util.Clock;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.ZonedDateTime;
import java.util.Optional;


/**
 * A model class that holds the information related to the identity of a business account.
 */
public record BusinessIdentityInfo(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        @NonNull
        BusinessVerifiedLevel level,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        @NonNull
        BusinessVerifiedNameCertificate certificate,
        @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
        boolean signed,
        @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
        boolean revoked,
        @ProtobufProperty(index = 5, type = ProtobufType.OBJECT)
        @NonNull
        BusinessStorageType hostStorage,
        @ProtobufProperty(index = 6, type = ProtobufType.OBJECT)
        @NonNull
        BusinessActorsType actualActors,
        @ProtobufProperty(index = 7, type = ProtobufType.UINT64)
        long privacyModeTimestampSeconds,
        @ProtobufProperty(index = 8, type = ProtobufType.UINT64)
        long featureControls
) implements Info, ProtobufMessage {
    /**
     * Returns the privacy mode timestampSeconds
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> privacyModeTimestamp() {
        return Clock.parseSeconds(privacyModeTimestampSeconds);
    }
}