package com.github.auties00.cobalt.wire.cloud.waba;

import java.util.Objects;
import java.util.Optional;

/**
 * The management view of a WhatsApp Business Account.
 *
 * <p>This model projects the node returned by a {@code GET} on a WhatsApp Business Account id and by
 * the owned/client account listing edges of a business portfolio. Beyond the always-present id, every
 * field is optional because the listing edges and the node read can each populate a different subset
 * depending on the requested fields and the caller's access.
 */
public final class CloudWaba {
    /**
     * The WhatsApp Business Account id.
     */
    private final String id;

    /**
     * The account name, or {@code null} when not projected.
     */
    private final String name;

    /**
     * The ISO-4217 billing currency, or {@code null} when not projected.
     */
    private final String currency;

    /**
     * The numeric timezone id, or {@code null} when not projected.
     */
    private final String timezoneId;

    /**
     * The message-template namespace, or {@code null} when not projected.
     */
    private final String messageTemplateNamespace;

    /**
     * The two-letter country code, or {@code null} when not projected.
     */
    private final String country;

    /**
     * The business verification status (for example {@code verified}, {@code pending}), or
     * {@code null} when not projected.
     */
    private final String businessVerificationStatus;

    /**
     * The account review status, or {@code null} when not projected.
     */
    private final CloudWabaReviewStatus accountReviewStatus;

    /**
     * The account status (for example {@code ACTIVE}), or {@code null} when not projected.
     */
    private final String status;

    /**
     * The ownership type, or {@code null} when not projected.
     */
    private final CloudWabaOwnershipType ownershipType;

    /**
     * Constructs a new WhatsApp Business Account view.
     *
     * @param id                         the account id
     * @param name                       the account name, or {@code null}
     * @param currency                   the billing currency, or {@code null}
     * @param timezoneId                 the timezone id, or {@code null}
     * @param messageTemplateNamespace   the message-template namespace, or {@code null}
     * @param country                    the country code, or {@code null}
     * @param businessVerificationStatus the business verification status, or {@code null}
     * @param accountReviewStatus        the account review status, or {@code null}
     * @param status                     the account status, or {@code null}
     * @param ownershipType              the ownership type, or {@code null}
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public CloudWaba(String id, String name, String currency, String timezoneId,
                     String messageTemplateNamespace, String country, String businessVerificationStatus,
                     CloudWabaReviewStatus accountReviewStatus, String status,
                     CloudWabaOwnershipType ownershipType) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = name;
        this.currency = currency;
        this.timezoneId = timezoneId;
        this.messageTemplateNamespace = messageTemplateNamespace;
        this.country = country;
        this.businessVerificationStatus = businessVerificationStatus;
        this.accountReviewStatus = accountReviewStatus;
        this.status = status;
        this.ownershipType = ownershipType;
    }

    /**
     * Returns the WhatsApp Business Account id.
     *
     * @return the id
     */
    public String id() {
        return id;
    }

    /**
     * Returns the account name.
     *
     * @return an {@link Optional} carrying the name, or empty when not projected
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the ISO-4217 billing currency.
     *
     * @return an {@link Optional} carrying the currency, or empty when not projected
     */
    public Optional<String> currency() {
        return Optional.ofNullable(currency);
    }

    /**
     * Returns the numeric timezone id.
     *
     * @return an {@link Optional} carrying the timezone id, or empty when not projected
     */
    public Optional<String> timezoneId() {
        return Optional.ofNullable(timezoneId);
    }

    /**
     * Returns the message-template namespace.
     *
     * @return an {@link Optional} carrying the namespace, or empty when not projected
     */
    public Optional<String> messageTemplateNamespace() {
        return Optional.ofNullable(messageTemplateNamespace);
    }

    /**
     * Returns the two-letter country code.
     *
     * @return an {@link Optional} carrying the country code, or empty when not projected
     */
    public Optional<String> country() {
        return Optional.ofNullable(country);
    }

    /**
     * Returns the business verification status.
     *
     * @return an {@link Optional} carrying the status, or empty when not projected
     */
    public Optional<String> businessVerificationStatus() {
        return Optional.ofNullable(businessVerificationStatus);
    }

    /**
     * Returns the account review status.
     *
     * @return an {@link Optional} carrying the {@link CloudWabaReviewStatus}, or empty when not
     *         projected
     */
    public Optional<CloudWabaReviewStatus> accountReviewStatus() {
        return Optional.ofNullable(accountReviewStatus);
    }

    /**
     * Returns the account status.
     *
     * @return an {@link Optional} carrying the status, or empty when not projected
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the ownership type.
     *
     * @return an {@link Optional} carrying the {@link CloudWabaOwnershipType}, or empty when not
     *         projected
     */
    public Optional<CloudWabaOwnershipType> ownershipType() {
        return Optional.ofNullable(ownershipType);
    }
}
