package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * A model class that represents a verified name
 */
@ProtobufMessage(name = "VerifiedNameCertificate.Details")
public final class BusinessVerifiedNameDetails {
    @ProtobufProperty(index = 1, type = ProtobufType.UINT64)
    final long serial;
    
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String issuer;
    
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String name;
    
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    final List<BusinessLocalizedName> localizedNames;
    
    @ProtobufProperty(index = 10, type = ProtobufType.UINT64)
    final long issueTimeSeconds;

    BusinessVerifiedNameDetails(long serial, String issuer, String name, List<BusinessLocalizedName> localizedNames, long issueTimeSeconds) {
        this.serial = serial;
        this.issuer = issuer;
        this.name = name;
        this.localizedNames = localizedNames;
        this.issueTimeSeconds = issueTimeSeconds;
    }

    public long serial() {
        return serial;
    }

    public Optional<String> issuer() {
        return Optional.ofNullable(issuer);
    }

    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    public List<BusinessLocalizedName> localizedNames() {
        return Collections.unmodifiableList(localizedNames);
    }

    public long issueTimeSeconds() {
        return issueTimeSeconds;
    }

    public Optional<ZonedDateTime> issueTime() {
        return Clock.parseSeconds(issueTimeSeconds);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BusinessVerifiedNameDetails that
                && serial == that.serial
                && Objects.equals(issuer, that.issuer)
                && Objects.equals(name, that.name)
                && Objects.equals(localizedNames, that.localizedNames)
                && issueTimeSeconds == that.issueTimeSeconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(serial, issuer, name, localizedNames, issueTimeSeconds);
    }

    @Override
    public String toString() {
        return "BusinessVerifiedNameDetails[" +
                "serial=" + serial +
                ", issuer=" + issuer +
                ", name=" + name +
                ", localizedNames=" + localizedNames +
                ", issueTimeSeconds=" + issueTimeSeconds + ']';
    }
}
