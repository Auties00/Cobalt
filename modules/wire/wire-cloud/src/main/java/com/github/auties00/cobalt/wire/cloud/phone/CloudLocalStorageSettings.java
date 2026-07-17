package com.github.auties00.cobalt.wire.cloud.phone;

import java.util.Optional;

/**
 * The local-storage (data localization) configuration of a WhatsApp Business phone number.
 *
 * <p>Local storage pins the country in which the account's message data is stored at rest. The
 * configuration carries the {@link CloudLocalStorageStatus storage status}, the two-letter ISO 3166-1
 * region code identifying the storage country (required when storage is enabled in-country), and the
 * retention window in minutes (required when no storage is enabled). Documented region codes include
 * {@code AU}, {@code ID}, {@code IN}, {@code JP}, {@code SG}, {@code KR}, {@code DE}, {@code CH}, and
 * {@code GB}.
 */
public final class CloudLocalStorageSettings {
    /**
     * The storage status, or {@code null} when unset.
     */
    private final CloudLocalStorageStatus status;

    /**
     * The two-letter ISO 3166-1 region code, or {@code null} when no in-country region is set.
     */
    private final String dataLocalizationRegion;

    /**
     * The retention window in minutes used when no storage is enabled, or {@code null} when unset.
     */
    private final Integer retentionMinutes;

    /**
     * Constructs a new local-storage configuration.
     *
     * @param status                 the storage status, or {@code null} when unset
     * @param dataLocalizationRegion the two-letter ISO 3166-1 region code, or {@code null}
     * @param retentionMinutes       the retention window in minutes, or {@code null} when unset
     */
    public CloudLocalStorageSettings(CloudLocalStorageStatus status, String dataLocalizationRegion,
                                     Integer retentionMinutes) {
        this.status = status;
        this.dataLocalizationRegion = dataLocalizationRegion;
        this.retentionMinutes = retentionMinutes;
    }

    /**
     * Returns the storage status.
     *
     * @return an {@link Optional} carrying the {@link CloudLocalStorageStatus}, or empty when unset
     */
    public Optional<CloudLocalStorageStatus> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns whether in-country storage is enabled.
     *
     * @return {@code true} if the status is {@link CloudLocalStorageStatus#IN_COUNTRY_STORAGE_ENABLED}
     */
    public boolean enabled() {
        return status == CloudLocalStorageStatus.IN_COUNTRY_STORAGE_ENABLED;
    }

    /**
     * Returns the two-letter ISO 3166-1 region code.
     *
     * @return an {@link Optional} carrying the region code, or empty when absent
     */
    public Optional<String> dataLocalizationRegion() {
        return Optional.ofNullable(dataLocalizationRegion);
    }

    /**
     * Returns the retention window in minutes used when no storage is enabled.
     *
     * @return an {@link Optional} carrying the retention window, or empty when unset
     */
    public Optional<Integer> retentionMinutes() {
        return Optional.ofNullable(retentionMinutes);
    }
}
