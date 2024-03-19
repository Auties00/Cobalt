package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificate;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;


/**
 * A model class that holds the information related to the identity of a business account.
 */
@ProtobufMessageName("BizIdentityInfo")
public record BusinessIdentityInfo(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        VerifiedLevel level,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        BusinessVerifiedNameCertificate certificate,
        @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
        boolean signed,
        @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
        boolean revoked,
        @ProtobufProperty(index = 5, type = ProtobufType.OBJECT)
        HostStorageType hostStorage,
        @ProtobufProperty(index = 6, type = ProtobufType.OBJECT)
        ActorsType actualActors,
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

    /**
     * The constants of this enumerated type describe the various types of actors of a business account
     */
    @ProtobufMessageName("BizIdentityInfo.ActualActorsType")
    public enum ActorsType implements ProtobufEnum {
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

        public int index() {
            return index;
        }
    }

    /**
     * The constants of this enumerated type describe the various types of verification that a business
     * account can have
     */
    @ProtobufMessageName("BizIdentityInfo.VerifiedLevelValue")
    public enum VerifiedLevel implements ProtobufEnum {
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

        public int index() {
            return index;
        }
    }

    @ProtobufMessageName("BizIdentityInfo.HostStorageType")
    public enum HostStorageType implements ProtobufEnum {
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

        public int index() {
            return index;
        }
    }
}