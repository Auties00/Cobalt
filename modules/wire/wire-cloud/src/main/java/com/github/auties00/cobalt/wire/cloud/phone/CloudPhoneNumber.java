package com.github.auties00.cobalt.wire.cloud.phone;

import java.util.Objects;
import java.util.Optional;

/**
 * The Cloud API view of a WhatsApp Business phone number.
 *
 * <p>Projects the fields of a phone-number node: the server id used to address the messaging and
 * profile edges, the human-readable display number, the verified business name, the messaging quality
 * rating, the registration and code-verification status, the display-name review status, the messaging
 * limit tier, the throughput level, the hosting platform type, the SMB-onboarding certificate, the
 * pending new SMB-onboarding certificate, the official-business-account flag, and the account mode.
 */
public final class CloudPhoneNumber {
    /**
     * The phone number id used to address the messaging and profile edges.
     */
    private final String id;

    /**
     * The display phone number in international form, or {@code null} when absent.
     */
    private final String displayPhoneNumber;

    /**
     * The verified business name, or {@code null} when not yet verified.
     */
    private final String verifiedName;

    /**
     * The messaging quality rating, or {@code null} when absent.
     */
    private final CloudPhoneNumberQualityRating qualityRating;

    /**
     * The code-verification status, or {@code null} when absent.
     */
    private final CloudPhoneNumberCodeVerificationStatus codeVerificationStatus;

    /**
     * The platform registration status, or {@code null} when absent.
     */
    private final CloudPhoneNumberStatus status;

    /**
     * The display-name review status of the current verified name, or {@code null} when absent.
     */
    private final CloudPhoneNumberNameStatus nameStatus;

    /**
     * The review status of a requested new display name, or {@code null} when absent.
     */
    private final CloudPhoneNumberNameStatus newNameStatus;

    /**
     * The messaging limit tier, or {@code null} when absent.
     */
    private final CloudMessagingLimitTier messagingLimitTier;

    /**
     * The throughput level, or {@code null} when absent.
     */
    private final CloudThroughputLevel throughputLevel;

    /**
     * The hosting platform type, or {@code null} when absent.
     */
    private final CloudPhoneNumberPlatformType platformType;

    /**
     * The base64-encoded SMB-onboarding certificate, or {@code null} when absent.
     */
    private final String certificate;

    /**
     * The base64-encoded SMB-onboarding certificate issued for a pending display-name change, or
     * {@code null} when no new certificate is available.
     */
    private final String newCertificate;

    /**
     * Whether the number belongs to an official business account, or {@code null} when absent.
     */
    private final Boolean officialBusinessAccount;

    /**
     * The account mode, or {@code null} when absent.
     */
    private final CloudPhoneNumberAccountMode accountMode;

    /**
     * Constructs a new phone-number view.
     *
     * @param id                     the phone number id
     * @param displayPhoneNumber     the display number, or {@code null}
     * @param verifiedName           the verified business name, or {@code null}
     * @param qualityRating          the messaging quality rating, or {@code null}
     * @param codeVerificationStatus the code-verification status, or {@code null}
     * @param status                 the platform registration status, or {@code null}
     * @param nameStatus             the display-name review status, or {@code null}
     * @param newNameStatus          the review status of a requested new display name, or {@code null}
     * @param messagingLimitTier     the messaging limit tier, or {@code null}
     * @param throughputLevel        the throughput level, or {@code null}
     * @param platformType           the hosting platform type, or {@code null}
     * @param certificate            the base64-encoded SMB-onboarding certificate, or {@code null}
     * @param officialBusinessAccount whether the number is an official business account, or {@code null}
     * @param accountMode            the account mode, or {@code null}
     * @param newCertificate         the base64-encoded SMB-onboarding certificate issued for a pending
     *                               display-name change, or {@code null}
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public CloudPhoneNumber(String id, String displayPhoneNumber, String verifiedName,
                            CloudPhoneNumberQualityRating qualityRating,
                            CloudPhoneNumberCodeVerificationStatus codeVerificationStatus,
                            CloudPhoneNumberStatus status, CloudPhoneNumberNameStatus nameStatus,
                            CloudPhoneNumberNameStatus newNameStatus, CloudMessagingLimitTier messagingLimitTier,
                            CloudThroughputLevel throughputLevel, CloudPhoneNumberPlatformType platformType,
                            String certificate, Boolean officialBusinessAccount,
                            CloudPhoneNumberAccountMode accountMode, String newCertificate) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.displayPhoneNumber = displayPhoneNumber;
        this.verifiedName = verifiedName;
        this.qualityRating = qualityRating;
        this.codeVerificationStatus = codeVerificationStatus;
        this.status = status;
        this.nameStatus = nameStatus;
        this.newNameStatus = newNameStatus;
        this.messagingLimitTier = messagingLimitTier;
        this.throughputLevel = throughputLevel;
        this.platformType = platformType;
        this.certificate = certificate;
        this.officialBusinessAccount = officialBusinessAccount;
        this.accountMode = accountMode;
        this.newCertificate = newCertificate;
    }

    /**
     * Returns the phone number id.
     *
     * @return the phone number id
     */
    public String id() {
        return id;
    }

    /**
     * Returns the display phone number.
     *
     * @return an {@link Optional} carrying the display number, or empty when absent
     */
    public Optional<String> displayPhoneNumber() {
        return Optional.ofNullable(displayPhoneNumber);
    }

    /**
     * Returns the verified business name.
     *
     * @return an {@link Optional} carrying the verified name, or empty when not yet verified
     */
    public Optional<String> verifiedName() {
        return Optional.ofNullable(verifiedName);
    }

    /**
     * Returns the messaging quality rating.
     *
     * @return an {@link Optional} carrying the {@link CloudPhoneNumberQualityRating}, or empty when
     *         absent
     */
    public Optional<CloudPhoneNumberQualityRating> qualityRating() {
        return Optional.ofNullable(qualityRating);
    }

    /**
     * Returns the code-verification status.
     *
     * @return an {@link Optional} carrying the {@link CloudPhoneNumberCodeVerificationStatus}, or empty
     *         when absent
     */
    public Optional<CloudPhoneNumberCodeVerificationStatus> codeVerificationStatus() {
        return Optional.ofNullable(codeVerificationStatus);
    }

    /**
     * Returns the platform registration status.
     *
     * @return an {@link Optional} carrying the {@link CloudPhoneNumberStatus}, or empty when absent
     */
    public Optional<CloudPhoneNumberStatus> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the display-name review status of the current verified name.
     *
     * @return an {@link Optional} carrying the {@link CloudPhoneNumberNameStatus}, or empty when absent
     */
    public Optional<CloudPhoneNumberNameStatus> nameStatus() {
        return Optional.ofNullable(nameStatus);
    }

    /**
     * Returns the review status of a requested new display name.
     *
     * @return an {@link Optional} carrying the {@link CloudPhoneNumberNameStatus}, or empty when absent
     */
    public Optional<CloudPhoneNumberNameStatus> newNameStatus() {
        return Optional.ofNullable(newNameStatus);
    }

    /**
     * Returns the messaging limit tier.
     *
     * @return an {@link Optional} carrying the {@link CloudMessagingLimitTier}, or empty when absent
     */
    public Optional<CloudMessagingLimitTier> messagingLimitTier() {
        return Optional.ofNullable(messagingLimitTier);
    }

    /**
     * Returns the throughput level.
     *
     * @return an {@link Optional} carrying the {@link CloudThroughputLevel}, or empty when absent
     */
    public Optional<CloudThroughputLevel> throughputLevel() {
        return Optional.ofNullable(throughputLevel);
    }

    /**
     * Returns the hosting platform type.
     *
     * @return an {@link Optional} carrying the {@link CloudPhoneNumberPlatformType}, or empty when absent
     */
    public Optional<CloudPhoneNumberPlatformType> platformType() {
        return Optional.ofNullable(platformType);
    }

    /**
     * Returns the base64-encoded SMB-onboarding certificate.
     *
     * @return an {@link Optional} carrying the certificate, or empty when absent
     */
    public Optional<String> certificate() {
        return Optional.ofNullable(certificate);
    }

    /**
     * Returns the base64-encoded SMB-onboarding certificate issued for a pending display-name change.
     *
     * @return an {@link Optional} carrying the new certificate, or empty when no new certificate is
     *         available
     */
    public Optional<String> newCertificate() {
        return Optional.ofNullable(newCertificate);
    }

    /**
     * Returns whether the number belongs to an official business account.
     *
     * @return an {@link Optional} carrying the flag, or empty when absent
     */
    public Optional<Boolean> officialBusinessAccount() {
        return Optional.ofNullable(officialBusinessAccount);
    }

    /**
     * Returns the account mode.
     *
     * @return an {@link Optional} carrying the {@link CloudPhoneNumberAccountMode}, or empty when absent
     */
    public Optional<CloudPhoneNumberAccountMode> accountMode() {
        return Optional.ofNullable(accountMode);
    }
}
