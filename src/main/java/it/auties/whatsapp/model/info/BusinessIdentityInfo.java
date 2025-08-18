package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificate;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that holds the information related to the identity of a business account.
 */
@ProtobufMessage(name = "BizIdentityInfo")
public final class BusinessIdentityInfo implements Info {
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    final VerifiedLevel level;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final BusinessVerifiedNameCertificate certificate;

    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    final boolean signed;

    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    final boolean revoked;

    @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
    final HostStorageType hostStorage;

    @ProtobufProperty(index = 6, type = ProtobufType.ENUM)
    final ActorsType actualActors;

    @ProtobufProperty(index = 7, type = ProtobufType.UINT64)
    final long privacyModeTimestampSeconds;

    @ProtobufProperty(index = 8, type = ProtobufType.UINT64)
    final long featureControls;

    BusinessIdentityInfo(VerifiedLevel level, BusinessVerifiedNameCertificate certificate, boolean signed, boolean revoked, HostStorageType hostStorage, ActorsType actualActors, long privacyModeTimestampSeconds, long featureControls) {
        this.level = Objects.requireNonNullElse(level, VerifiedLevel.UNKNOWN);
        this.certificate = Objects.requireNonNull(certificate, "certificate cannot be null");
        this.signed = signed;
        this.revoked = revoked;
        this.hostStorage = Objects.requireNonNull(hostStorage, "hostStorage cannot be null");
        this.actualActors = Objects.requireNonNull(actualActors, "actualActors cannot be null");
        this.privacyModeTimestampSeconds = privacyModeTimestampSeconds;
        this.featureControls = featureControls;
    }

    public VerifiedLevel level() {
        return level;
    }

    public BusinessVerifiedNameCertificate certificate() {
        return certificate;
    }

    public boolean signed() {
        return signed;
    }

    public boolean revoked() {
        return revoked;
    }

    public HostStorageType hostStorage() {
        return hostStorage;
    }

    public ActorsType actualActors() {
        return actualActors;
    }

    public long privacyModeTimestampSeconds() {
        return privacyModeTimestampSeconds;
    }

    public long featureControls() {
        return featureControls;
    }

    /**
     * Returns the privacy mode timestampSeconds
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> privacyModeTimestamp() {
        return Clock.parseSeconds(privacyModeTimestampSeconds);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BusinessIdentityInfo that
                && Objects.equals(level, that.level)
                && Objects.equals(certificate, that.certificate)
                && signed == that.signed
                && revoked == that.revoked
                && Objects.equals(hostStorage, that.hostStorage)
                && Objects.equals(actualActors, that.actualActors)
                && privacyModeTimestampSeconds == that.privacyModeTimestampSeconds
                && featureControls == that.featureControls;
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, certificate, signed, revoked, hostStorage, actualActors,
                privacyModeTimestampSeconds, featureControls);
    }

    @Override
    public String toString() {
        return "BusinessIdentityInfo[" +
                "level=" + level +
                ", certificate=" + certificate +
                ", signed=" + signed +
                ", revoked=" + revoked +
                ", hostStorage=" + hostStorage +
                ", actualActors=" + actualActors +
                ", privacyModeTimestampSeconds=" + privacyModeTimestampSeconds +
                ", featureControls=" + featureControls +
                ']';
    }

    /**
     * The constants of this enumerated type describe the various types of actors of a business account
     */
    @ProtobufEnum(name = "BizIdentityInfo.ActualActorsType")
    public enum ActorsType {
        /**
         * Self
         */
        SELF(0),
        /**
         * Bsp
         */
        BSP(1);

        final int index;

        ActorsType(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }

    /**
     * The constants of this enumerated type describe the various types of verification that a business
     * account can have
     */
    @ProtobufEnum(name = "BizIdentityInfo.VerifiedLevelValue")
    public enum VerifiedLevel {
        /**
         * Unknown
         */
        UNKNOWN(0),

        /**
         * Low
         */
        LOW(1),

        /**
         * High
         */
        HIGH(2);

        final int index;

        VerifiedLevel(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }

    @ProtobufEnum(name = "BizIdentityInfo.HostStorageType")
    public enum HostStorageType {
        /**
         * Hosted on a private server ("On-Premise")
         */
        ON_PREMISE(0),

        /**
         * Hosted by facebook
         */
        FACEBOOK(1);

        final int index;

        HostStorageType(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }
}