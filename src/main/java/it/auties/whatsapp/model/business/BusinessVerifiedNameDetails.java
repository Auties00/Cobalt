package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;


/**
 * A model class that represents a verified name
 */
@ProtobufMessageName("VerifiedNameCertificate.Details")
public record BusinessVerifiedNameDetails(
        @ProtobufProperty(index = 1, type = ProtobufType.UINT64)
        long serial,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String issuer,
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        String name,
        @ProtobufProperty(index = 8, type = ProtobufType.OBJECT)
        List<BusinessLocalizedName> localizedNames,
        @ProtobufProperty(index = 10, type = ProtobufType.UINT64)
        long issueTimeSeconds
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
