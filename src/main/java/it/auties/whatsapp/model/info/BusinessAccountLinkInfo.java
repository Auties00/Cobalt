package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that holds a payload about a business link info.
 */
@ProtobufMessage(name = "BizAccountLinkInfo")
public final class BusinessAccountLinkInfo {
    @ProtobufProperty(index = 1, type = ProtobufType.UINT64)
    final long businessId;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String phoneNumber;

    @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
    final long issueTimeSeconds;

    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    final HostStorageType hostStorage;

    @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
    final AccountType accountType;

    BusinessAccountLinkInfo(long businessId, String phoneNumber, long issueTimeSeconds, HostStorageType hostStorage, AccountType accountType) {
        this.businessId = businessId;
        this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber cannot be null");
        this.issueTimeSeconds = issueTimeSeconds;
        this.hostStorage = Objects.requireNonNull(hostStorage, "hostStorage cannot be null");
        this.accountType = Objects.requireNonNull(accountType, "accountType cannot be null");
    }

    public long businessId() {
        return businessId;
    }

    public String phoneNumber() {
        return phoneNumber;
    }

    public long issueTimeSeconds() {
        return issueTimeSeconds;
    }

    public HostStorageType hostStorage() {
        return hostStorage;
    }

    public AccountType accountType() {
        return accountType;
    }

    /**
     * Returns this object's timestampSeconds
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> issueTime() {
        return Clock.parseSeconds(issueTimeSeconds);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BusinessAccountLinkInfo that
                && businessId == that.businessId
                && Objects.equals(phoneNumber, that.phoneNumber)
                && issueTimeSeconds == that.issueTimeSeconds
                && Objects.equals(hostStorage, that.hostStorage)
                && Objects.equals(accountType, that.accountType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(businessId, phoneNumber, issueTimeSeconds, hostStorage, accountType);
    }

    @Override
    public String toString() {
        return "BusinessAccountLinkInfo[" +
                "businessId=" + businessId +
                ", phoneNumber=" + phoneNumber +
                ", issueTimeSeconds=" + issueTimeSeconds +
                ", hostStorage=" + hostStorage +
                ", accountType=" + accountType +
                ']';
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
    }
}