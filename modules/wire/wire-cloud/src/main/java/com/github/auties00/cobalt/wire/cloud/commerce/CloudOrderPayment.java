package com.github.auties00.cobalt.wire.cloud.commerce;

import java.util.List;
import java.util.Objects;

/**
 * The payment configuration of a Cloud API {@code order_details} message.
 *
 * <p>An order-details message offers exactly one payment path, and the path is a closed two-variant
 * union: a {@link Gateway} carries the generic gateway {@linkplain Gateway#settings() settings} used
 * outside India, while an {@link India} carries the India-specific
 * {@linkplain India#configurationName() configuration name} and {@linkplain India#paymentType() payment
 * type}. Pattern matching on the variant recovers the path-specific data; the two paths are mutually
 * exclusive on the wire.
 */
public sealed interface CloudOrderPayment permits CloudOrderPayment.Gateway, CloudOrderPayment.India {
    /**
     * The generic gateway payment path used outside India.
     *
     * <p>The path carries one or more {@linkplain CloudOrderPaymentSetting payment settings}, each
     * selecting a gateway provider and the named configuration registered in Business Manager.
     */
    final class Gateway implements CloudOrderPayment {
        /**
         * The gateway payment settings.
         */
        private final List<CloudOrderPaymentSetting> settings;

        /**
         * Constructs a new gateway payment path.
         *
         * @param settings the gateway payment settings, or {@code null} for none
         */
        public Gateway(List<CloudOrderPaymentSetting> settings) {
            this.settings = settings == null ? List.of() : List.copyOf(settings);
        }

        /**
         * Returns the gateway payment settings.
         *
         * @return an unmodifiable list of payment settings, empty when none were declared
         */
        public List<CloudOrderPaymentSetting> settings() {
            return settings;
        }
    }

    /**
     * The India-specific payment path.
     *
     * <p>The path references a payment configuration by name and selects the India payment type, for
     * example {@code upi}; the gateway settings used by the {@link Gateway} path are not supplied.
     */
    final class India implements CloudOrderPayment {
        /**
         * The India payment configuration name.
         */
        private final String configurationName;

        /**
         * The India payment type, for example {@code upi}.
         */
        private final String paymentType;

        /**
         * Constructs a new India payment path.
         *
         * @param configurationName the India payment configuration name
         * @param paymentType       the India payment type, for example {@code upi}
         * @throws NullPointerException if {@code configurationName} or {@code paymentType} is
         *                              {@code null}
         */
        public India(String configurationName, String paymentType) {
            this.configurationName = Objects.requireNonNull(configurationName, "configurationName must not be null");
            this.paymentType = Objects.requireNonNull(paymentType, "paymentType must not be null");
        }

        /**
         * Returns the India payment configuration name.
         *
         * @return the configuration name
         */
        public String configurationName() {
            return configurationName;
        }

        /**
         * Returns the India payment type.
         *
         * @return the payment type, for example {@code upi}
         */
        public String paymentType() {
            return paymentType;
        }
    }
}
