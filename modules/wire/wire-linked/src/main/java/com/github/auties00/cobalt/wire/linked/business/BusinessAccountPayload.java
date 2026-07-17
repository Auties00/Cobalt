package com.github.auties00.cobalt.wire.linked.business;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Provisioning envelope returned by WhatsApp when a business account is linked
 * or registered, bundling a {@link BusinessVerifiedNameCertificate} together
 * with the serialized account-link record that ties the WhatsApp number to a
 * Facebook Business entity.
 *
 * <p>Whenever a phone number is registered as a WhatsApp Business account, the
 * server emits this envelope so the client can simultaneously persist the
 * verified-name certificate (used to prove the business's display name to chat
 * peers) and the account-link blob (used to prove ownership of the linked
 * Facebook Business Manager account). The two fields are independent and
 * either may be absent depending on the registration path.
 *
 * <p>The {@link #accountLinkInfo()} bytes are the protobuf-serialized form of
 * an {@link AccountLinkInfo} message and should be deserialized by callers
 * that need to inspect the Facebook Business ID, the linked phone number, the
 * issuance time, the hosting infrastructure, or the business account tier.
 */
@ProtobufMessage(name = "BizAccountPayload")
public final class BusinessAccountPayload {
    /**
     * Verified-name certificate for the business account, attesting the
     * business's approved display name and carrying the client and server
     * signatures that prove its authenticity. Populated whenever the
     * registration flow includes a verified-name issuance step; absent when
     * the payload only carries account-link data.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    BusinessVerifiedNameCertificate verifiedNameCertificate;

    /**
     * Serialized protobuf bytes of an {@link AccountLinkInfo} message
     * describing the link between this WhatsApp account and a Facebook
     * Business account. Populated whenever the registration flow established
     * or refreshed the Facebook link; callers must deserialize the bytes
     * separately to access the structured fields.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] accountLinkInfo;

    /**
     * Constructs a new {@code BusinessAccountPayload} bundling a verified-name
     * certificate and a serialized account-link blob. Either argument may be
     * {@code null} when the corresponding wire field is absent.
     *
     * @param verifiedNameCertificate the verified-name certificate, or {@code null}
     * @param accountLinkInfo         the serialized {@link AccountLinkInfo} bytes, or {@code null}
     */
    BusinessAccountPayload(BusinessVerifiedNameCertificate verifiedNameCertificate, byte[] accountLinkInfo) {
        this.verifiedNameCertificate = verifiedNameCertificate;
        this.accountLinkInfo = accountLinkInfo;
    }

    /**
     * Returns the verified-name certificate attesting the business's approved
     * display name.
     *
     * @return an {@code Optional} containing the {@link BusinessVerifiedNameCertificate},
     *         or empty when the payload did not include certificate data
     */
    public Optional<BusinessVerifiedNameCertificate> verifiedNameCertificate() {
        return Optional.ofNullable(verifiedNameCertificate);
    }

    /**
     * Returns the serialized protobuf bytes of the {@link AccountLinkInfo}
     * message describing the Facebook Business link for this account.
     *
     * @return an {@code Optional} containing the raw protobuf bytes, or empty
     *         when the payload did not include account-link data
     */
    public Optional<byte[]> accountLinkInfo() {
        return Optional.ofNullable(accountLinkInfo);
    }

    /**
     * Sets the verified-name certificate for this account payload.
     *
     * @param verifiedNameCertificate the certificate to set, or {@code null} to clear
     */
    public void setVerifiedNameCertificate(BusinessVerifiedNameCertificate verifiedNameCertificate) {
        this.verifiedNameCertificate = verifiedNameCertificate;
    }

    /**
     * Sets the serialized {@link AccountLinkInfo} bytes for this account payload.
     *
     * @param accountLinkInfo the raw protobuf bytes to set, or {@code null} to clear
     */
    public void setAccountLinkInfo(byte[] accountLinkInfo) {
        this.accountLinkInfo = accountLinkInfo;
    }

    /**
     * Describes the association between a WhatsApp Business phone number and
     * the Facebook Business account that owns it.
     *
     * <p>This message records the Facebook Business ID (FBID) that owns the
     * WhatsApp Business account, the registered phone number, the moment the
     * link was established, and the hosting and tier configuration of the
     * business account. WhatsApp uses these fields during business
     * verification, provisioning, and policy enforcement to confirm that a
     * given WhatsApp number is legitimately operated by a specific Facebook
     * Business entity.
     */
    @ProtobufMessage(name = "BizAccountLinkInfo")
    public static final class AccountLinkInfo {
        /**
         * Numeric Facebook Business ID (FBID) of the linked business account,
         * for example {@code 123456789012345}. This is the unique identifier
         * assigned by Meta to the business entity in Business Manager and
         * establishes the ownership relationship between the Facebook
         * organization and this WhatsApp Business account.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.UINT64)
        Long facebookBusinessId;

        /**
         * WhatsApp phone number registered as the business account, in
         * E.164-like form without the leading plus sign (for example
         * {@code "15551234567"}). The number is the public messaging address
         * that customers use to contact the business.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String phoneNumber;

        /**
         * Moment at which the link between this WhatsApp number and the
         * Facebook Business account was established. The wire encoding is
         * seconds since the Unix epoch, converted to {@link Instant} via
         * {@link InstantSecondsMixin}.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
        Instant issueTime;

        /**
         * Type of infrastructure that hosts the business's data and message
         * processing, distinguishing on-premises deployments from
         * Meta-hosted Cloud API deployments.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
        BusinessIdentityHostStorageType hostStorage;

        /**
         * Tier of the WhatsApp Business product this account belongs to,
         * which determines the kind of API access and feature set available
         * to the business.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
        AccountType accountType;

        /**
         * Constructs a new {@code AccountLinkInfo} with the given Facebook
         * link parameters. Any argument may be {@code null} to represent a
         * field that was absent on the wire.
         *
         * @param facebookBusinessId the Facebook Business ID, or {@code null}
         * @param phoneNumber        the registered WhatsApp phone number, or {@code null}
         * @param issueTime          the time the link was established, or {@code null}
         * @param hostStorage        the hosting infrastructure type, or {@code null}
         * @param accountType        the business account tier, or {@code null}
         */
        AccountLinkInfo(Long facebookBusinessId, String phoneNumber, Instant issueTime, BusinessIdentityHostStorageType hostStorage, AccountType accountType) {
            this.facebookBusinessId = facebookBusinessId;
            this.phoneNumber = phoneNumber;
            this.issueTime = issueTime;
            this.hostStorage = hostStorage;
            this.accountType = accountType;
        }

        /**
         * Returns the Facebook Business ID (FBID) of the linked business
         * account.
         *
         * @return an {@code OptionalLong} containing the FBID, or empty when
         *         the wire omitted the field
         */
        public OptionalLong facebookBusinessId() {
            return facebookBusinessId == null ? OptionalLong.empty() : OptionalLong.of(facebookBusinessId);
        }

        /**
         * Returns the WhatsApp phone number registered as this business
         * account.
         *
         * @return an {@code Optional} containing the phone number string
         *         (for example {@code "15551234567"}), or empty when the wire
         *         omitted the field
         */
        public Optional<String> phoneNumber() {
            return Optional.ofNullable(phoneNumber);
        }

        /**
         * Returns the moment at which the link between this WhatsApp number
         * and the Facebook Business account was established.
         *
         * @return an {@code Optional} containing the issue time as an
         *         {@link Instant}, or empty when the wire omitted the field
         */
        public Optional<Instant> issueTime() {
            return Optional.ofNullable(issueTime);
        }

        /**
         * Returns the type of infrastructure that hosts the business's data
         * and processes its messages.
         *
         * @return an {@code Optional} containing the {@link BusinessIdentityHostStorageType},
         *         or empty when the wire omitted the field
         */
        public Optional<BusinessIdentityHostStorageType> hostStorage() {
            return Optional.ofNullable(hostStorage);
        }

        /**
         * Returns the tier of the WhatsApp Business product that this
         * account belongs to.
         *
         * @return an {@code Optional} containing the {@link AccountType}, or
         *         empty when the wire omitted the field
         */
        public Optional<AccountType> accountType() {
            return Optional.ofNullable(accountType);
        }

        /**
         * Sets the Facebook Business ID for this account link.
         *
         * @param facebookBusinessId the FBID to set, or {@code null} to clear
         */
        public void setFacebookBusinessId(Long facebookBusinessId) {
            this.facebookBusinessId = facebookBusinessId;
        }

        /**
         * Sets the registered WhatsApp phone number for this account link.
         *
         * @param phoneNumber the phone number to set, or {@code null} to clear
         */
        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        /**
         * Sets the moment at which the account link was established.
         *
         * @param issueTime the issue time to set, or {@code null} to clear
         */
        public void setIssueTime(Instant issueTime) {
            this.issueTime = issueTime;
        }

        /**
         * Sets the hosting infrastructure type for this account link.
         *
         * @param hostStorage the {@link BusinessIdentityHostStorageType} to set, or {@code null} to clear
         */
        public void setHostStorage(BusinessIdentityHostStorageType hostStorage) {
            this.hostStorage = hostStorage;
        }

        /**
         * Sets the business account tier for this account link.
         *
         * @param accountType the {@link AccountType} to set, or {@code null} to clear
         */
        public void setAccountType(AccountType accountType) {
            this.accountType = accountType;
        }

        /**
         * Enumerates the tiers of WhatsApp Business products that an
         * {@link AccountLinkInfo} can identify. The tier dictates the level
         * of API access available to the business and the way it can
         * automate or delegate messaging.
         */
        @ProtobufEnum(name = "BizAccountLinkInfo.AccountType")
        public enum AccountType {
            /**
             * Enterprise-tier business account, typically using the WhatsApp
             * Business API (Cloud API or On-Premises API) for programmatic
             * messaging at scale through a Business Solution Provider or via
             * Meta's hosted infrastructure.
             */
            ENTERPRISE(0);

            /**
             * Protobuf wire index for this enum constant.
             */
            final int index;

            /**
             * Constructs an {@code AccountType} enum constant bound to the
             * given protobuf wire index.
             *
             * @param index the protobuf wire index
             */
            AccountType(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * Returns the protobuf wire index of this enum constant.
             *
             * @return the protobuf index
             */
            public int index() {
                return this.index;
            }
        }

    }
}
