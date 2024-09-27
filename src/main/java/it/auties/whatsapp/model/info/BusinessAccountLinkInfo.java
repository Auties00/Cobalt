package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * A model class that holds a payload about a business link info.
 */
@ProtobufMessage(name = "BizAccountLinkInfo")
public record BusinessAccountLinkInfo(
        @ProtobufProperty(index = 1, type = ProtobufType.UINT64)
        long businessId,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String phoneNumber,
        @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
        long issueTimeSeconds,
        @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
        HostStorageType hostStorage,
        @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
        AccountType accountType
) {
    /**
     * Returns this object's timestampSeconds
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> issueTime() {
        return Clock.parseSeconds(issueTimeSeconds);
    }

    /**
     * The constants of this enumerated type describe the various types of business accounts
     */
    @ProtobufEnum(name = "BizAccountLinkInfo.AccountType")
    public enum AccountType {
        /**
         * Enterprise
         */
        ENTERPRISE(0),
        /**
         * Page
         */
        PAGE(1);

        final int index;

        AccountType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }

    @ProtobufEnum(name = "BizAccountLinkInfo.HostStorageType")
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

        public int index() {
            return index;
        }
    }
}