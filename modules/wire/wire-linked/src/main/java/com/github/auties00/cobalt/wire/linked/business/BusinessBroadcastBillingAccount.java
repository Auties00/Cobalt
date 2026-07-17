package com.github.auties00.cobalt.wire.linked.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Billing account context returned by a WhatsApp Business broadcast billing
 * query.
 *
 * <p>Before a merchant pays for a broadcast (paid marketing) campaign,
 * WhatsApp surfaces the billing details that govern how the campaign will be
 * charged. Those details live on a billing account: the account itself
 * exposes its server-issued identifier and a server-defined type marker,
 * and carries the {@linkplain BillingInfo billing-info} sub-tree describing
 * the payment mode in force, the estimated taxes that will be added on top
 * of the budget, the payment-section presentation shown in the composer
 * panel, and any required action the merchant must take before the campaign
 * can be sent.
 *
 * <p>This model is the billing account exactly as the server reports it.
 * All fields are optional because the server may omit any of them. The
 * billing-info tree is exposed as a typed sub-graph rather than as flat
 * scalars so callers can navigate the payment, tax, and action sub-objects
 * without re-parsing.
 */
@ProtobufMessage(name = "BusinessBroadcastBillingAccount")
public final class BusinessBroadcastBillingAccount {
    /**
     * Server-defined type marker for this billing account. The full marker
     * set is not recoverable from the WhatsApp client, so the raw marker is
     * exposed as a string. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String typename;

    /**
     * Server-issued identifier of the billing account. {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String id;

    /**
     * Billing-info sub-tree describing the payment mode, estimated taxes,
     * payment-section presentation, and any required action. {@code null}
     * when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final BillingInfo billingInfo;

    /**
     * Constructs a new {@code BusinessBroadcastBillingAccount}. Any
     * reference argument may be {@code null} when the server omitted the
     * corresponding field.
     *
     * @param typename    the type marker, or {@code null}
     * @param id          the account identifier, or {@code null}
     * @param billingInfo the billing-info sub-tree, or {@code null}
     */
    BusinessBroadcastBillingAccount(String typename, String id, BillingInfo billingInfo) {
        this.typename = typename;
        this.id = id;
        this.billingInfo = billingInfo;
    }

    /**
     * Returns the server-defined type marker for this billing account.
     *
     * @return the type marker, or empty when the server omitted it
     */
    public Optional<String> typename() {
        return Optional.ofNullable(typename);
    }

    /**
     * Returns the server-issued identifier of the billing account.
     *
     * @return the account id, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the billing-info sub-tree.
     *
     * @return the billing info, or empty when the server omitted it
     */
    public Optional<BillingInfo> billingInfo() {
        return Optional.ofNullable(billingInfo);
    }

    /**
     * Returns a hash code derived from this billing account's identifier
     * and type marker.
     *
     * @return the hash code of the identifier and type marker
     */
    @Override
    public int hashCode() {
        return Objects.hash(typename, id);
    }

    /**
     * Returns whether this billing account is equal to the given object.
     *
     * <p>Two billing accounts are considered equal when they carry the
     * same identifier and type marker.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a
     *         {@code BusinessBroadcastBillingAccount} with the same id
     *         and type marker
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof BusinessBroadcastBillingAccount that
                && Objects.equals(this.typename, that.typename)
                && Objects.equals(this.id, that.id);
    }

    /**
     * Returns a debug string describing this billing account.
     *
     * @return a debug string
     */
    @Override
    public String toString() {
        return "BusinessBroadcastBillingAccount[" +
                "typename=" + typename +
                ", id=" + id +
                ", billingInfo=" + billingInfo +
                "]";
    }

    /**
     * Billing-info sub-tree carried by a {@link BusinessBroadcastBillingAccount}.
     *
     * <p>Describes how the broadcast will be billed: the payment mode in
     * force, the estimated taxes that will be added on top of the budget,
     * the payment-section presentation shown in the broadcast composer, and
     * any required action the merchant must take before the campaign can be
     * sent.
     */
    @ProtobufMessage(name = "BusinessBroadcastBillingAccount.BillingInfo")
    public static final class BillingInfo {
        /**
         * Payment-mode marker. The full marker set is not recoverable from
         * the WhatsApp client, so the raw marker is exposed as a string.
         * {@code null} when the server omitted it.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String billingPaymentMode;

        /**
         * Estimated-tax breakdown computed against the broadcast budget, or
         * {@code null} when the server omitted it.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        final EstimatedTax estimatedTax;

        /**
         * Presentation details for the payment section shown in the
         * broadcast composer, or {@code null} when the server omitted it.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        final PaymentSectionDetails paymentSectionDetails;

        /**
         * Action the merchant must take before the campaign can be sent, or
         * {@code null} when no action is required.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
        final RequiredAction requiredAction;

        /**
         * Constructs a new {@code BillingInfo}. Any reference argument may
         * be {@code null} when the server omitted the corresponding field.
         *
         * @param billingPaymentMode    the payment-mode marker, or {@code null}
         * @param estimatedTax          the estimated-tax breakdown, or {@code null}
         * @param paymentSectionDetails the payment-section details, or {@code null}
         * @param requiredAction        the required action, or {@code null}
         */
        BillingInfo(String billingPaymentMode, EstimatedTax estimatedTax,
                    PaymentSectionDetails paymentSectionDetails, RequiredAction requiredAction) {
            this.billingPaymentMode = billingPaymentMode;
            this.estimatedTax = estimatedTax;
            this.paymentSectionDetails = paymentSectionDetails;
            this.requiredAction = requiredAction;
        }

        /**
         * Returns the payment-mode marker.
         *
         * @return the payment-mode marker, or empty when the server omitted
         *         it
         */
        public Optional<String> billingPaymentMode() {
            return Optional.ofNullable(billingPaymentMode);
        }

        /**
         * Returns the estimated-tax breakdown.
         *
         * @return the estimated-tax breakdown, or empty when the server
         *         omitted it
         */
        public Optional<EstimatedTax> estimatedTax() {
            return Optional.ofNullable(estimatedTax);
        }

        /**
         * Returns the payment-section presentation details.
         *
         * @return the payment-section details, or empty when the server
         *         omitted it
         */
        public Optional<PaymentSectionDetails> paymentSectionDetails() {
            return Optional.ofNullable(paymentSectionDetails);
        }

        /**
         * Returns the action the merchant must take before the campaign can
         * be sent.
         *
         * @return the required action, or empty when no action is required
         */
        public Optional<RequiredAction> requiredAction() {
            return Optional.ofNullable(requiredAction);
        }
    }

    /**
     * Estimated-tax breakdown computed against a broadcast budget.
     *
     * <p>Pairs the {@linkplain #budget() budget} the estimate was computed
     * against, the per-line {@linkplain #taxes() tax breakdown}, and the
     * estimated {@linkplain #total() total} the merchant will be charged.
     */
    @ProtobufMessage(name = "BusinessBroadcastBillingAccount.EstimatedTax")
    public static final class EstimatedTax {
        /**
         * Budget amount the estimate was computed against, or {@code null}
         * when the server omitted it.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        final CurrencyAmount budget;

        /**
         * Per-line tax breakdown, or {@code null} when the server omitted
         * it.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        final List<TaxLineItem> taxes;

        /**
         * Estimated total the merchant will be charged, or {@code null}
         * when the server omitted it.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        final CurrencyAmount total;

        /**
         * Constructs a new {@code EstimatedTax}. Any reference argument may
         * be {@code null} when the server omitted the corresponding field.
         *
         * @param budget the budget the estimate was computed against, or {@code null}
         * @param taxes  the per-line tax breakdown, or {@code null}
         * @param total  the estimated total, or {@code null}
         */
        EstimatedTax(CurrencyAmount budget, List<TaxLineItem> taxes, CurrencyAmount total) {
            this.budget = budget;
            this.taxes = taxes;
            this.total = total;
        }

        /**
         * Returns the budget the estimate was computed against.
         *
         * @return the budget amount, or empty when the server omitted it
         */
        public Optional<CurrencyAmount> budget() {
            return Optional.ofNullable(budget);
        }

        /**
         * Returns an unmodifiable view of the per-line tax breakdown.
         *
         * @return the tax line items, empty when the server reported none
         */
        public List<TaxLineItem> taxes() {
            return taxes == null ? List.of() : Collections.unmodifiableList(taxes);
        }

        /**
         * Returns the estimated total the merchant will be charged.
         *
         * @return the total amount, or empty when the server omitted it
         */
        public Optional<CurrencyAmount> total() {
            return Optional.ofNullable(total);
        }
    }

    /**
     * Single line of an estimated-tax breakdown.
     *
     * <p>Pairs a tax {@linkplain #taxName() name}, the
     * {@linkplain #taxRate() rate} the line was computed at, and the
     * resulting {@linkplain #taxAmount() amount}.
     */
    @ProtobufMessage(name = "BusinessBroadcastBillingAccount.TaxLineItem")
    public static final class TaxLineItem {
        /**
         * Tax name (for example a localized VAT label), or {@code null}
         * when the server omitted it.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String taxName;

        /**
         * Tax rate, as a server-defined numeric marker exposed as a raw
         * string because its encoding is not recoverable from the WhatsApp
         * client. {@code null} when the server omitted it.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        final String taxRate;

        /**
         * Tax amount computed for this line, or {@code null} when the
         * server omitted it.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        final CurrencyAmount taxAmount;

        /**
         * Constructs a new {@code TaxLineItem}. Any reference argument may
         * be {@code null} when the server omitted the corresponding field.
         *
         * @param taxName   the tax name, or {@code null}
         * @param taxRate   the tax-rate marker, or {@code null}
         * @param taxAmount the tax amount, or {@code null}
         */
        TaxLineItem(String taxName, String taxRate, CurrencyAmount taxAmount) {
            this.taxName = taxName;
            this.taxRate = taxRate;
            this.taxAmount = taxAmount;
        }

        /**
         * Returns the tax name.
         *
         * @return the tax name, or empty when the server omitted it
         */
        public Optional<String> taxName() {
            return Optional.ofNullable(taxName);
        }

        /**
         * Returns the tax-rate marker.
         *
         * @return the tax rate, or empty when the server omitted it
         */
        public Optional<String> taxRate() {
            return Optional.ofNullable(taxRate);
        }

        /**
         * Returns the tax amount computed for this line.
         *
         * @return the tax amount, or empty when the server omitted it
         */
        public Optional<CurrencyAmount> taxAmount() {
            return Optional.ofNullable(taxAmount);
        }
    }

    /**
     * Currency amount returned by a billing account.
     *
     * <p>Carries the {@linkplain #amount() amount} as a raw numeric marker,
     * the ISO {@linkplain #currency() currency code}, and the localized
     * {@linkplain #formattedAmount() formatted amount} the composer
     * displays to the merchant. Shared by the budget, total, and per-line
     * tax amounts.
     */
    @ProtobufMessage(name = "BusinessBroadcastBillingAccount.CurrencyAmount")
    public static final class CurrencyAmount {
        /**
         * Numeric amount as a server-defined marker exposed as a raw string
         * because its encoding is not recoverable from the WhatsApp client.
         * {@code null} when the server omitted it.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String amount;

        /**
         * ISO currency code (for example {@code "USD"}), or {@code null}
         * when the server omitted it.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        final String currency;

        /**
         * Localized formatted amount the composer displays to the merchant,
         * or {@code null} when the server omitted it.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        final String formattedAmount;

        /**
         * Constructs a new {@code CurrencyAmount}. Any reference argument
         * may be {@code null} when the server omitted the corresponding
         * field.
         *
         * @param amount          the numeric amount marker, or {@code null}
         * @param currency        the ISO currency code, or {@code null}
         * @param formattedAmount the localized formatted amount, or {@code null}
         */
        CurrencyAmount(String amount, String currency, String formattedAmount) {
            this.amount = amount;
            this.currency = currency;
            this.formattedAmount = formattedAmount;
        }

        /**
         * Returns the numeric amount marker.
         *
         * @return the amount, or empty when the server omitted it
         */
        public Optional<String> amount() {
            return Optional.ofNullable(amount);
        }

        /**
         * Returns the ISO currency code.
         *
         * @return the currency code, or empty when the server omitted it
         */
        public Optional<String> currency() {
            return Optional.ofNullable(currency);
        }

        /**
         * Returns the localized formatted amount.
         *
         * @return the formatted amount, or empty when the server omitted it
         */
        public Optional<String> formattedAmount() {
            return Optional.ofNullable(formattedAmount);
        }
    }

    /**
     * Presentation details for the payment section shown in the broadcast
     * composer.
     *
     * <p>Carries the labels and description that introduce the section, the
     * logos rendered next to them, the section type marker, and the primary
     * action the merchant can trigger from the section.
     */
    @ProtobufMessage(name = "BusinessBroadcastBillingAccount.PaymentSectionDetails")
    public static final class PaymentSectionDetails {
        /**
         * Section description text, or {@code null} when the server omitted
         * it.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String description;

        /**
         * Section label text, or {@code null} when the server omitted it.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        final String label;

        /**
         * Accessibility label paired with the section label, or {@code null}
         * when the server omitted it.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        final String labelAx;

        /**
         * Logos rendered next to the section labels, or {@code null} when
         * the server omitted them.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
        final List<Image> logos;

        /**
         * Primary action the merchant can trigger from the section, or
         * {@code null} when the server omitted it.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
        final BillingInfoAction primaryAction;

        /**
         * Section type marker. The full marker set is not recoverable from
         * the WhatsApp client, so the raw marker is exposed as a string.
         * {@code null} when the server omitted it.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        final String type;

        /**
         * Constructs a new {@code PaymentSectionDetails}. Any reference
         * argument may be {@code null} when the server omitted the
         * corresponding field.
         *
         * @param description   the section description, or {@code null}
         * @param label         the section label, or {@code null}
         * @param labelAx       the accessibility label, or {@code null}
         * @param logos         the logos, or {@code null}
         * @param primaryAction the primary action, or {@code null}
         * @param type          the section type marker, or {@code null}
         */
        PaymentSectionDetails(String description, String label, String labelAx, List<Image> logos,
                              BillingInfoAction primaryAction, String type) {
            this.description = description;
            this.label = label;
            this.labelAx = labelAx;
            this.logos = logos;
            this.primaryAction = primaryAction;
            this.type = type;
        }

        /**
         * Returns the section description text.
         *
         * @return the description, or empty when the server omitted it
         */
        public Optional<String> description() {
            return Optional.ofNullable(description);
        }

        /**
         * Returns the section label text.
         *
         * @return the label, or empty when the server omitted it
         */
        public Optional<String> label() {
            return Optional.ofNullable(label);
        }

        /**
         * Returns the accessibility label paired with the section label.
         *
         * @return the accessibility label, or empty when the server omitted
         *         it
         */
        public Optional<String> labelAx() {
            return Optional.ofNullable(labelAx);
        }

        /**
         * Returns an unmodifiable view of the section logos.
         *
         * @return the logos, empty when the server reported none
         */
        public List<Image> logos() {
            return logos == null ? List.of() : Collections.unmodifiableList(logos);
        }

        /**
         * Returns the primary action the merchant can trigger.
         *
         * @return the primary action, or empty when the server omitted it
         */
        public Optional<BillingInfoAction> primaryAction() {
            return Optional.ofNullable(primaryAction);
        }

        /**
         * Returns the section type marker.
         *
         * @return the section type, or empty when the server omitted it
         */
        public Optional<String> type() {
            return Optional.ofNullable(type);
        }
    }

    /**
     * Logo image rendered in a payment section.
     *
     * <p>Carries only the URI the composer should load to render the image.
     */
    @ProtobufMessage(name = "BusinessBroadcastBillingAccount.Image")
    public static final class Image {
        /**
         * URI of the logo image, or {@code null} when the server omitted
         * it.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String uri;

        /**
         * Constructs a new {@code Image}. The reference argument may be
         * {@code null} when the server omitted the field.
         *
         * @param uri the image URI, or {@code null}
         */
        Image(String uri) {
            this.uri = uri;
        }

        /**
         * Returns the URI of the logo image.
         *
         * @return the image URI, or empty when the server omitted it
         */
        public Optional<String> uri() {
            return Optional.ofNullable(uri);
        }
    }

    /**
     * Required action a merchant must take on a billing account before a
     * broadcast can be sent.
     *
     * <p>Pairs the {@linkplain #action() action} the merchant must trigger
     * with the {@linkplain #message() message} describing why the action is
     * required.
     */
    @ProtobufMessage(name = "BusinessBroadcastBillingAccount.RequiredAction")
    public static final class RequiredAction {
        /**
         * Action the merchant must trigger, or {@code null} when the server
         * omitted it.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        final BillingInfoAction action;

        /**
         * Message describing why the action is required, or {@code null}
         * when the server omitted it.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        final Message message;

        /**
         * Constructs a new {@code RequiredAction}. Any reference argument
         * may be {@code null} when the server omitted the corresponding
         * field.
         *
         * @param action  the action, or {@code null}
         * @param message the explanatory message, or {@code null}
         */
        RequiredAction(BillingInfoAction action, Message message) {
            this.action = action;
            this.message = message;
        }

        /**
         * Returns the action the merchant must trigger.
         *
         * @return the action, or empty when the server omitted it
         */
        public Optional<BillingInfoAction> action() {
            return Optional.ofNullable(action);
        }

        /**
         * Returns the message describing why the action is required.
         *
         * @return the message, or empty when the server omitted it
         */
        public Optional<Message> message() {
            return Optional.ofNullable(message);
        }
    }

    /**
     * Action shown on a billing account that opens a follow-up wizard.
     *
     * <p>Carries the button label, the name of the wizard the action opens,
     * and the JSON-encoded props the wizard is initialised with. Shared by
     * the {@link PaymentSectionDetails#primaryAction() primary action} and
     * the {@link RequiredAction#action() required-action button}.
     */
    @ProtobufMessage(name = "BusinessBroadcastBillingAccount.BillingInfoAction")
    public static final class BillingInfoAction {
        /**
         * Button label shown to the merchant, or {@code null} when the
         * server omitted it.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String label;

        /**
         * Name of the wizard the action opens, or {@code null} when the
         * server omitted it.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        final String wizardName;

        /**
         * JSON-encoded props the wizard is initialised with. Exposed as the
         * raw JSON string because the props shape is wizard-specific.
         * {@code null} when the server omitted it.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        final String wizardPropsJson;

        /**
         * Constructs a new {@code BillingInfoAction}. Any reference
         * argument may be {@code null} when the server omitted the
         * corresponding field.
         *
         * @param label           the button label, or {@code null}
         * @param wizardName      the wizard name, or {@code null}
         * @param wizardPropsJson the JSON-encoded wizard props, or {@code null}
         */
        BillingInfoAction(String label, String wizardName, String wizardPropsJson) {
            this.label = label;
            this.wizardName = wizardName;
            this.wizardPropsJson = wizardPropsJson;
        }

        /**
         * Returns the button label.
         *
         * @return the label, or empty when the server omitted it
         */
        public Optional<String> label() {
            return Optional.ofNullable(label);
        }

        /**
         * Returns the name of the wizard the action opens.
         *
         * @return the wizard name, or empty when the server omitted it
         */
        public Optional<String> wizardName() {
            return Optional.ofNullable(wizardName);
        }

        /**
         * Returns the JSON-encoded wizard props.
         *
         * @return the JSON-encoded wizard props, or empty when the server
         *         omitted it
         */
        public Optional<String> wizardPropsJson() {
            return Optional.ofNullable(wizardPropsJson);
        }
    }

    /**
     * Explanatory message paired with a {@link RequiredAction}.
     *
     * <p>Pairs a short {@linkplain #headline() headline} with a longer
     * {@linkplain #body() body} that explains why the merchant must
     * complete the paired action.
     */
    @ProtobufMessage(name = "BusinessBroadcastBillingAccount.Message")
    public static final class Message {
        /**
         * Short headline summarising the message, or {@code null} when the
         * server omitted it.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String headline;

        /**
         * Longer body text expanding on the headline, or {@code null} when
         * the server omitted it.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        final String body;

        /**
         * Constructs a new {@code Message}. Any reference argument may be
         * {@code null} when the server omitted the corresponding field.
         *
         * @param headline the headline, or {@code null}
         * @param body     the body, or {@code null}
         */
        Message(String headline, String body) {
            this.headline = headline;
            this.body = body;
        }

        /**
         * Returns the short headline.
         *
         * @return the headline, or empty when the server omitted it
         */
        public Optional<String> headline() {
            return Optional.ofNullable(headline);
        }

        /**
         * Returns the longer body text.
         *
         * @return the body, or empty when the server omitted it
         */
        public Optional<String> body() {
            return Optional.ofNullable(body);
        }
    }
}
