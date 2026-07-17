package com.github.auties00.cobalt.wire.cloud.commerce;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A named payment gateway configuration of a WhatsApp Business Account, read from or written to the
 * payment-configuration edge.
 *
 * <p>A payment configuration binds a business to a payment provider under a caller-chosen name. It
 * carries the configuration name and, when reported, the payment provider name, the provider merchant
 * id, the configuration status, and the creation and update instants. The same shape is reported on a
 * {@code payment_configuration_update} webhook change, which is delivered as this type.
 *
 * <p>Payments are region-gated; the request and response field set used by the CRUD edges is taken from
 * the published payment-configuration edge and the {@code payment_configuration_update} webhook value
 * rather than a captured response, so fields beyond the configuration name may be absent.
 */
public final class CloudPaymentConfiguration {
    /**
     * The name of the payment configuration.
     */
    private final String configurationName;

    /**
     * The payment provider name, or {@code null} when not reported.
     */
    private final String providerName;

    /**
     * The provider merchant id, or {@code null} when not reported.
     */
    private final String providerMerchantId;

    /**
     * The configuration status, or {@code null} when not reported.
     */
    private final String status;

    /**
     * The instant the configuration was created, or {@code null} when not reported.
     */
    private final Instant createdTimestamp;

    /**
     * The instant the configuration was last updated, or {@code null} when not reported.
     */
    private final Instant updatedTimestamp;

    /**
     * Constructs a new payment configuration.
     *
     * @param configurationName  the name of the payment configuration
     * @param providerName       the payment provider name, or {@code null}
     * @param providerMerchantId the provider merchant id, or {@code null}
     * @param status             the configuration status, or {@code null}
     * @param createdTimestamp   the creation instant, or {@code null}
     * @param updatedTimestamp   the last-update instant, or {@code null}
     * @throws NullPointerException if {@code configurationName} is {@code null}
     */
    public CloudPaymentConfiguration(String configurationName, String providerName, String providerMerchantId,
                                     String status, Instant createdTimestamp, Instant updatedTimestamp) {
        this.configurationName = Objects.requireNonNull(configurationName, "configurationName must not be null");
        this.providerName = providerName;
        this.providerMerchantId = providerMerchantId;
        this.status = status;
        this.createdTimestamp = createdTimestamp;
        this.updatedTimestamp = updatedTimestamp;
    }

    /**
     * Constructs a new payment configuration carrying only its name and provider.
     *
     * @param configurationName  the name of the payment configuration
     * @param providerName       the payment provider name, or {@code null}
     * @param providerMerchantId the provider merchant id, or {@code null}
     * @throws NullPointerException if {@code configurationName} is {@code null}
     */
    public CloudPaymentConfiguration(String configurationName, String providerName, String providerMerchantId) {
        this(configurationName, providerName, providerMerchantId, null, null, null);
    }

    /**
     * Returns the name of the payment configuration.
     *
     * @return the configuration name
     */
    public String configurationName() {
        return configurationName;
    }

    /**
     * Returns the payment provider name.
     *
     * @return an {@link Optional} carrying the provider name, or empty when not reported
     */
    public Optional<String> providerName() {
        return Optional.ofNullable(providerName);
    }

    /**
     * Returns the provider merchant id.
     *
     * @return an {@link Optional} carrying the merchant id, or empty when not reported
     */
    public Optional<String> providerMerchantId() {
        return Optional.ofNullable(providerMerchantId);
    }

    /**
     * Returns the configuration status.
     *
     * @return an {@link Optional} carrying the status, or empty when not reported
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the instant the configuration was created.
     *
     * @return an {@link Optional} carrying the creation instant, or empty when not reported
     */
    public Optional<Instant> createdTimestamp() {
        return Optional.ofNullable(createdTimestamp);
    }

    /**
     * Returns the instant the configuration was last updated.
     *
     * @return an {@link Optional} carrying the last-update instant, or empty when not reported
     */
    public Optional<Instant> updatedTimestamp() {
        return Optional.ofNullable(updatedTimestamp);
    }
}
