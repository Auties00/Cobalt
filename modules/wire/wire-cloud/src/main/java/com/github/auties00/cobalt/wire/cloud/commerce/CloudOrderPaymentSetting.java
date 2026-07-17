package com.github.auties00.cobalt.wire.cloud.commerce;

import java.util.Objects;
import java.util.Optional;

/**
 * A payment gateway setting attached to a Cloud API {@code order_details} message.
 *
 * <p>Each setting selects a payment method by type and, for a {@code payment_gateway} setting, the
 * gateway provider and the named gateway configuration registered in WhatsApp Business Manager. The
 * gateway configuration is referenced by name only; its credentials are managed in the Business
 * Manager UI rather than over the Graph API.
 */
public final class CloudOrderPaymentSetting {
    /**
     * The setting type, for example {@code payment_gateway}.
     */
    private final String type;

    /**
     * The gateway provider type (for example {@code razorpay}, {@code payu}), or {@code null} when
     * the setting carries no gateway sub-object.
     */
    private final String gatewayType;

    /**
     * The name of the gateway configuration registered in Business Manager, or {@code null} when the
     * setting carries no gateway sub-object.
     */
    private final String configurationName;

    /**
     * Constructs a new payment setting.
     *
     * @param type              the setting type
     * @param gatewayType       the gateway provider type, or {@code null}
     * @param configurationName the gateway configuration name, or {@code null}
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public CloudOrderPaymentSetting(String type, String gatewayType, String configurationName) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.gatewayType = gatewayType;
        this.configurationName = configurationName;
    }

    /**
     * Returns the setting type.
     *
     * @return the type
     */
    public String type() {
        return type;
    }

    /**
     * Returns the gateway provider type.
     *
     * @return an {@link Optional} carrying the gateway type, or empty when no gateway sub-object is set
     */
    public Optional<String> gatewayType() {
        return Optional.ofNullable(gatewayType);
    }

    /**
     * Returns the name of the gateway configuration registered in Business Manager.
     *
     * @return an {@link Optional} carrying the configuration name, or empty when no gateway sub-object
     *         is set
     */
    public Optional<String> configurationName() {
        return Optional.ofNullable(configurationName);
    }
}
