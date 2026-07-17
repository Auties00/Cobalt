package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The cost summary a merchant reviews before publishing a WhatsApp Business
 * advertisement.
 *
 * <p>On the final review step of a "Click-to-WhatsApp" ad (a paid promotion that
 * opens a chat with the business when tapped), the server returns the billing
 * breakdown shown to the merchant: which advertising account will be billed,
 * the estimated taxes itemised by name, and the estimated grand total. This
 * model gathers those review fields so the merchant can confirm what the ad will
 * cost before committing.
 *
 * <p>{@link #billableAccountId()} is the advertising account that will be billed;
 * {@link #taxes()} lists the estimated tax line items; and
 * {@link #estimatedTotal()} is the formatted estimated grand total.
 */
@ProtobufMessage(name = "BusinessAdCreationSummary")
public final class BusinessAdCreationSummary {
    /**
     * Identifier of the advertising account that will be billed. A numeric
     * advertising identifier, not a WhatsApp address. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String billableAccountId;

    /**
     * Estimated tax line items, each pairing a tax name with its formatted
     * amount, in the order the server returned them. Never {@code null}, possibly
     * empty.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final List<TaxLine> taxes;

    /**
     * Formatted estimated grand total of the ad, including taxes. Empty when the
     * server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String estimatedTotal;

    /**
     * Constructs a new {@code BusinessAdCreationSummary}. A {@code null}
     * {@code taxes} is coerced to an empty list, and the other reference
     * arguments may be {@code null} when the server omitted them.
     *
     * @param billableAccountId the billable advertising-account identifier, or {@code null}
     * @param taxes             the estimated tax line items; {@code null} treated as empty
     * @param estimatedTotal    the formatted estimated grand total, or {@code null}
     */
    BusinessAdCreationSummary(String billableAccountId, List<TaxLine> taxes, String estimatedTotal) {
        this.billableAccountId = billableAccountId;
        this.taxes = taxes == null ? List.of() : taxes;
        this.estimatedTotal = estimatedTotal;
    }

    /**
     * Returns the identifier of the advertising account that will be billed.
     *
     * @return the billable advertising-account id, or empty when the server
     *         omitted it
     */
    public Optional<String> billableAccountId() {
        return Optional.ofNullable(billableAccountId);
    }

    /**
     * Returns the estimated tax line items.
     *
     * @return an unmodifiable view of the estimated tax line items; never
     *         {@code null}, possibly empty
     */
    public List<TaxLine> taxes() {
        return Collections.unmodifiableList(taxes);
    }

    /**
     * Returns the formatted estimated grand total of the ad.
     *
     * @return the formatted estimated grand total, or empty when the server
     *         omitted it
     */
    public Optional<String> estimatedTotal() {
        return Optional.ofNullable(estimatedTotal);
    }

    /**
     * One estimated tax applied to a WhatsApp Business advertisement.
     *
     * <p>The cost summary itemises the estimated taxes so the merchant can see
     * what makes up the grand total. This is one such item: a named tax and its
     * formatted amount.
     */
    @ProtobufMessage(name = "BusinessAdCreationSummary.TaxLine")
    public static final class TaxLine {
        /**
         * Name of the tax shown to the merchant. Empty when the server omitted
         * it.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String name;

        /**
         * Formatted amount of the tax. Empty when the server omitted it.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        final String amount;

        /**
         * Constructs a new {@code TaxLine}. The reference arguments may be
         * {@code null} when the server omitted them.
         *
         * @param name   the tax name, or {@code null}
         * @param amount the formatted tax amount, or {@code null}
         */
        TaxLine(String name, String amount) {
            this.name = name;
            this.amount = amount;
        }

        /**
         * Returns the name of the tax shown to the merchant.
         *
         * @return the tax name, or empty when the server omitted it
         */
        public Optional<String> name() {
            return Optional.ofNullable(name);
        }

        /**
         * Returns the formatted amount of the tax.
         *
         * @return the formatted tax amount, or empty when the server omitted it
         */
        public Optional<String> amount() {
            return Optional.ofNullable(amount);
        }
    }
}
