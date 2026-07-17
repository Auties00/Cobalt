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
 * Identity and verification record attached to a WhatsApp Business account,
 * describing how thoroughly the business has been verified, whether the
 * account has been signed or revoked, and how the business operates its
 * messaging pipeline.
 *
 * <p>WhatsApp issues each business account a verification level (none, low,
 * or high) reflecting the depth of identity checks the business has passed,
 * and pairs that level with a verified-name certificate that attests the
 * business's display name. Two booleans complete the verification picture:
 * {@code signed} indicates the business itself has cryptographically
 * acknowledged its identity, while {@code revoked} indicates WhatsApp has
 * subsequently withdrawn the verification.
 *
 * <p>The record also carries the "privacy-mode triplet" that determines the
 * messaging-privacy badge shown in chats with this business: {@code hostStorage}
 * (where the data lives), {@code actualActors} (who actually reads and writes
 * messages), and {@code privacyModeTimestamp} (when the configuration was
 * last changed). When the server pushes a newer privacy-mode timestamp the
 * client renders an in-chat system message announcing the change.
 *
 * <p>Finally, {@code featureControls} is a server-driven bitmask that toggles
 * business-only features for the account.
 */
@ProtobufMessage(name = "BizIdentityInfo")
public final class BusinessIdentityInfo {
    /**
     * Verification level reached by this business identity, indicating how
     * thoroughly WhatsApp has confirmed the business's real-world identity.
     * Higher levels unlock the verification badge in the chat header and
     * contact info.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    VerificationLevel verificationLevel;

    /**
     * Verified-name certificate associated with this identity, carrying the
     * approved business display name, the certificate serial number, the
     * issuer (enterprise or small-business), and the cryptographic
     * signatures used to authenticate the certificate.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    BusinessVerifiedNameCertificate verifiedNameCertificate;

    /**
     * Whether the business has cryptographically signed its identity
     * information. A signed identity indicates the business has acknowledged
     * and confirmed the data; absence of a signature is treated as
     * {@code false}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    Boolean signed;

    /**
     * Whether WhatsApp has revoked the business's verification status. A
     * revoked identity is no longer trusted by the client and typically
     * indicates a policy violation or an identity dispute. Absence is
     * treated as {@code false}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    Boolean revoked;

    /**
     * Type of infrastructure that hosts the business's data and message
     * processing. Together with {@link #actualActors} and
     * {@link #privacyModeTimestamp} it forms the privacy-mode triplet that
     * determines the messaging-privacy level advertised to chat peers.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
    BusinessIdentityHostStorageType hostStorage;

    /**
     * Entity that actually reads and writes messages on behalf of the
     * business: the business itself or a third-party Business Solution
     * Provider. Together with {@link #hostStorage} and
     * {@link #privacyModeTimestamp} it forms the privacy-mode triplet.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.ENUM)
    ActualActorsType actualActors;

    /**
     * Moment at which the privacy-mode configuration of this business was
     * last changed. Wire encoding is seconds since the Unix epoch, converted
     * to {@link Instant} via {@link InstantSecondsMixin}. When a newer
     * timestamp is received from the server the client emits an in-chat
     * system message disclosing the privacy-mode change.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant privacyModeTimestamp;

    /**
     * Bitmask of business-feature toggles enabled or disabled server-side
     * for this account. Individual bit positions are defined by the
     * WhatsApp backend and gate optional business behaviours such as
     * extended catalog features or experimental capabilities.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.UINT64)
    Long featureControls;

    /**
     * Constructs a new {@code BusinessIdentityInfo} from individual identity
     * and privacy-mode fields. Any argument may be {@code null} when the
     * corresponding wire field is absent.
     *
     * @param verificationLevel       the verification level, or {@code null}
     * @param verifiedNameCertificate the verified-name certificate, or {@code null}
     * @param signed                  {@code true} if the identity is signed, or {@code null}
     * @param revoked                 {@code true} if the identity is revoked, or {@code null}
     * @param hostStorage             the hosting infrastructure type, or {@code null}
     * @param actualActors            the message-processing entity, or {@code null}
     * @param privacyModeTimestamp    the privacy-mode change timestamp, or {@code null}
     * @param featureControls         the feature-controls bitmask, or {@code null}
     */
    BusinessIdentityInfo(VerificationLevel verificationLevel, BusinessVerifiedNameCertificate verifiedNameCertificate, Boolean signed, Boolean revoked, BusinessIdentityHostStorageType hostStorage, ActualActorsType actualActors, Instant privacyModeTimestamp, Long featureControls) {
        this.verificationLevel = verificationLevel;
        this.verifiedNameCertificate = verifiedNameCertificate;
        this.signed = signed;
        this.revoked = revoked;
        this.hostStorage = hostStorage;
        this.actualActors = actualActors;
        this.privacyModeTimestamp = privacyModeTimestamp;
        this.featureControls = featureControls;
    }

    /**
     * Returns the verification level reached by this business identity.
     *
     * @return an {@code Optional} containing the {@link VerificationLevel},
     *         or empty when no level has been assigned
     */
    public Optional<VerificationLevel> verificationLevel() {
        return Optional.ofNullable(verificationLevel);
    }

    /**
     * Returns the verified-name certificate associated with this identity.
     *
     * @return an {@code Optional} containing the {@link BusinessVerifiedNameCertificate},
     *         or empty when no certificate is present
     */
    public Optional<BusinessVerifiedNameCertificate> verifiedNameCertificate() {
        return Optional.ofNullable(verifiedNameCertificate);
    }

    /**
     * Returns whether the business has cryptographically signed its
     * identity information.
     *
     * @return {@code true} if the identity is signed, {@code false} when
     *         the field is absent or explicitly negative
     */
    public boolean signed() {
        return signed != null && signed;
    }

    /**
     * Returns whether WhatsApp has revoked this business's verification.
     *
     * @return {@code true} if the identity is revoked, {@code false} when
     *         the field is absent or explicitly negative
     */
    public boolean revoked() {
        return revoked != null && revoked;
    }

    /**
     * Returns the type of infrastructure that hosts the business's data
     * and message processing.
     *
     * @return an {@code Optional} containing the {@link BusinessIdentityHostStorageType},
     *         or empty when the field is absent
     */
    public Optional<BusinessIdentityHostStorageType> hostStorage() {
        return Optional.ofNullable(hostStorage);
    }

    /**
     * Returns the entity that actually reads and writes messages on
     * behalf of the business.
     *
     * @return an {@code Optional} containing the {@link ActualActorsType},
     *         or empty when the field is absent
     */
    public Optional<ActualActorsType> actualActors() {
        return Optional.ofNullable(actualActors);
    }

    /**
     * Returns the moment at which the privacy-mode configuration was last
     * changed.
     *
     * @return an {@code Optional} containing the privacy-mode timestamp,
     *         or empty when the field is absent
     */
    public Optional<Instant> privacyModeTimestamp() {
        return Optional.ofNullable(privacyModeTimestamp);
    }

    /**
     * Returns the bitmask of business-feature toggles enabled for this
     * account.
     *
     * @return an {@code OptionalLong} containing the bitmask, or empty when
     *         no feature controls have been configured
     */
    public OptionalLong featureControls() {
        return featureControls == null ? OptionalLong.empty() : OptionalLong.of(featureControls);
    }

    /**
     * Sets the verification level for this business identity.
     *
     * @param verificationLevel the {@link VerificationLevel} to set, or {@code null} to clear
     */
    public void setVerificationLevel(VerificationLevel verificationLevel) {
        this.verificationLevel = verificationLevel;
    }

    /**
     * Sets the verified-name certificate associated with this identity.
     *
     * @param verifiedNameCertificate the {@link BusinessVerifiedNameCertificate} to set, or {@code null} to clear
     */
    public void setVerifiedNameCertificate(BusinessVerifiedNameCertificate verifiedNameCertificate) {
        this.verifiedNameCertificate = verifiedNameCertificate;
    }

    /**
     * Sets whether this identity is cryptographically signed.
     *
     * @param signed {@code true} if signed, {@code false} or {@code null} otherwise
     */
    public void setSigned(Boolean signed) {
        this.signed = signed;
    }

    /**
     * Sets whether this identity has been revoked by WhatsApp.
     *
     * @param revoked {@code true} if revoked, {@code false} or {@code null} otherwise
     */
    public void setRevoked(Boolean revoked) {
        this.revoked = revoked;
    }

    /**
     * Sets the hosting infrastructure type for this identity.
     *
     * @param hostStorage the {@link BusinessIdentityHostStorageType} to set, or {@code null} to clear
     */
    public void setHostStorage(BusinessIdentityHostStorageType hostStorage) {
        this.hostStorage = hostStorage;
    }

    /**
     * Sets the entity that processes messages on behalf of the business.
     *
     * @param actualActors the {@link ActualActorsType} to set, or {@code null} to clear
     */
    public void setActualActors(ActualActorsType actualActors) {
        this.actualActors = actualActors;
    }

    /**
     * Sets the moment at which the privacy-mode configuration was last
     * changed.
     *
     * @param privacyModeTimestamp the timestamp to set, or {@code null} to clear
     */
    public void setPrivacyModeTimestamp(Instant privacyModeTimestamp) {
        this.privacyModeTimestamp = privacyModeTimestamp;
    }

    /**
     * Sets the bitmask of business-feature toggles for this account.
     *
     * @param featureControls the bitmask to set, or {@code null} to clear
     */
    public void setFeatureControls(Long featureControls) {
        this.featureControls = featureControls;
    }

    /**
     * Identifies the entity that actually reads and writes messages on
     * behalf of a WhatsApp Business account, which directly determines the
     * messaging-privacy guarantees displayed to chat peers.
     */
    @ProtobufEnum(name = "BizIdentityInfo.ActualActorsType")
    public enum ActualActorsType {
        /**
         * The business itself processes messages directly, preserving
         * end-to-end encryption between the customer and the business.
         */
        SELF(0),

        /**
         * A Business Solution Provider (BSP) processes messages on behalf
         * of the business, meaning a third party has access to the message
         * content as part of its hosting role.
         */
        BSP(1);

        /**
         * Protobuf wire index for this enum constant.
         */
        final int index;

        /**
         * Constructs an {@code ActualActorsType} enum constant bound to
         * the given protobuf wire index.
         *
         * @param index the protobuf wire index
         */
        ActualActorsType(@ProtobufEnumIndex int index) {
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

    /**
     * Identifies how thoroughly WhatsApp has verified the real-world
     * identity of a business account. Higher verification levels unlock
     * the verification badge displayed in the chat header and contact
     * info, signalling to chat peers that the business has passed
     * stricter identity checks.
     */
    @ProtobufEnum(name = "BizIdentityInfo.VerifiedLevelValue")
    public enum VerificationLevel {
        /**
         * Verification level is unknown or has not yet been determined by
         * WhatsApp.
         */
        UNKNOWN(0),

        /**
         * Low verification level: only basic identity checks (such as
         * phone-number verification) have been completed.
         */
        LOW(1),

        /**
         * High verification level: thorough identity validation has been
         * completed and the business displays the verified-business badge
         * in chat surfaces.
         */
        HIGH(2);

        /**
         * Protobuf wire index for this enum constant.
         */
        final int index;

        /**
         * Constructs a {@code VerificationLevel} enum constant bound to
         * the given protobuf wire index.
         *
         * @param index the protobuf wire index
         */
        VerificationLevel(@ProtobufEnumIndex int index) {
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
