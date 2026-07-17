package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The payment state of the advertising account funding a WhatsApp Business
 * advertisement.
 *
 * <p>Before a merchant pays for a "Click-to-WhatsApp" ad (a paid promotion that
 * opens a chat with the business when tapped), the ad-creation surface shows
 * the current payment method and, when the account is not yet ready to be
 * charged, prompts the merchant to finish setting up billing. This model is
 * that state: the descriptive payment row the merchant sees and any setup step
 * they must complete first.
 *
 * <p>{@link #accountId()} identifies the billed account; {@link #label()},
 * {@link #description()}, and {@link #paymentMethodLogos()} are the descriptive
 * payment row; and {@link #requiredSetup()} carries the billing-setup step the
 * account must complete, present only when setup is outstanding. The merchant
 * may proceed to pay when {@link #requiredSetup()} is empty.
 */
@ProtobufMessage(name = "BusinessAdPaymentSection")
public final class BusinessAdPaymentSection {
    /**
     * Server-issued identifier of the billed advertising account. Empty when
     * the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String accountId;

    /**
     * Label of the payment row shown to the merchant (for example the active
     * payment method). Empty when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String label;

    /**
     * Secondary description shown under the payment row. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String description;

    /**
     * Locations of the payment-method logo images shown in the payment row, in
     * the order the server returned them. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final List<String> paymentMethodLogos;

    /**
     * Billing-setup step the account must complete before it can be charged, or
     * {@code null} when no setup is outstanding and the merchant may pay
     * directly.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final RequiredSetup requiredSetup;

    /**
     * Constructs a new {@code BusinessAdPaymentSection}. A {@code null}
     * {@code paymentMethodLogos} is coerced to an empty list, and the other
     * reference arguments may be {@code null} when the server omitted them.
     *
     * @param accountId          the billed account identifier, or {@code null}
     * @param label              the payment-row label, or {@code null}
     * @param description        the payment-row description, or {@code null}
     * @param paymentMethodLogos the payment-method logo locations; {@code null} treated as empty
     * @param requiredSetup      the outstanding setup step, or {@code null}
     */
    BusinessAdPaymentSection(String accountId,
                             String label,
                             String description,
                             List<String> paymentMethodLogos,
                             RequiredSetup requiredSetup) {
        this.accountId = accountId;
        this.label = label;
        this.description = description;
        this.paymentMethodLogos = paymentMethodLogos == null ? List.of() : paymentMethodLogos;
        this.requiredSetup = requiredSetup;
    }

    /**
     * Returns the server-issued identifier of the billed advertising account.
     *
     * @return the account id, or empty when the server omitted it
     */
    public Optional<String> accountId() {
        return Optional.ofNullable(accountId);
    }

    /**
     * Returns the label of the payment row shown to the merchant.
     *
     * @return the payment-row label, or empty when the server omitted it
     */
    public Optional<String> label() {
        return Optional.ofNullable(label);
    }

    /**
     * Returns the secondary description shown under the payment row.
     *
     * @return the payment-row description, or empty when the server omitted it
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the locations of the payment-method logo images.
     *
     * @return an unmodifiable view of the logo locations; never {@code null},
     *         possibly empty
     */
    public List<String> paymentMethodLogos() {
        return Collections.unmodifiableList(paymentMethodLogos);
    }

    /**
     * Returns the billing-setup step the account must complete before it can be
     * charged.
     *
     * @return the outstanding setup step, or empty when no setup is outstanding
     */
    public Optional<RequiredSetup> requiredSetup() {
        return Optional.ofNullable(requiredSetup);
    }

    /**
     * A billing-setup step the account must complete before it can be charged.
     *
     * <p>When an advertising account is not yet ready to pay, the server
     * describes a setup step the merchant must complete (for example adding a
     * payment method). The step names the setup screen to open and carries the
     * inputs that screen needs, plus a short message explaining why setup is
     * required.
     */
    @ProtobufMessage(name = "BusinessAdPaymentSection.RequiredSetup")
    public static final class RequiredSetup {
        /**
         * Label of the action button shown to the merchant for this setup step.
         * Empty when the server omitted it.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String label;

        /**
         * Server-defined name of the setup screen to open. The full name set is
         * not recoverable from the WhatsApp client, so the raw name is exposed
         * as a string. Empty when the server omitted it.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        final String setupName;

        /**
         * Opaque, server-defined inputs the setup screen needs, carried verbatim
         * as a JSON-encoded string. Empty when the server omitted it.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        final String setupPropertiesJson;

        /**
         * Short headline explaining why setup is required. Empty when the server
         * omitted it.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        final String messageHeadline;

        /**
         * Body text explaining why setup is required. Empty when the server
         * omitted it.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        final String messageBody;

        /**
         * Constructs a new {@code RequiredSetup}. The reference arguments may be
         * {@code null} when the server omitted them.
         *
         * @param label               the action-button label, or {@code null}
         * @param setupName           the setup-screen name, or {@code null}
         * @param setupPropertiesJson the opaque setup inputs JSON, or {@code null}
         * @param messageHeadline     the explanatory headline, or {@code null}
         * @param messageBody         the explanatory body, or {@code null}
         */
        RequiredSetup(String label,
                      String setupName,
                      String setupPropertiesJson,
                      String messageHeadline,
                      String messageBody) {
            this.label = label;
            this.setupName = setupName;
            this.setupPropertiesJson = setupPropertiesJson;
            this.messageHeadline = messageHeadline;
            this.messageBody = messageBody;
        }

        /**
         * Returns the label of the action button shown for this setup step.
         *
         * @return the action-button label, or empty when the server omitted it
         */
        public Optional<String> label() {
            return Optional.ofNullable(label);
        }

        /**
         * Returns the server-defined name of the setup screen to open.
         *
         * @return the setup-screen name, or empty when the server omitted it
         */
        public Optional<String> setupName() {
            return Optional.ofNullable(setupName);
        }

        /**
         * Returns the opaque, server-defined inputs the setup screen needs.
         *
         * <p>The value is an opaque JSON document carried verbatim; its shape is
         * not modelled.
         *
         * @return the opaque setup inputs JSON, or empty when the server omitted
         *         it
         */
        public Optional<String> setupPropertiesJson() {
            return Optional.ofNullable(setupPropertiesJson);
        }

        /**
         * Returns the short headline explaining why setup is required.
         *
         * @return the explanatory headline, or empty when the server omitted it
         */
        public Optional<String> messageHeadline() {
            return Optional.ofNullable(messageHeadline);
        }

        /**
         * Returns the body text explaining why setup is required.
         *
         * @return the explanatory body, or empty when the server omitted it
         */
        public Optional<String> messageBody() {
            return Optional.ofNullable(messageBody);
        }
    }
}
