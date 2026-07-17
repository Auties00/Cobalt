package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The sealed family of inbound reply variants produced by the relay
 * in response to a {@link SmaxGetSMBMeteredMessagingCheckoutRequest}.
 *
 * <p>The checkout flow fetches the cost projection, integrity-eligibility marker, account-balance
 * triple and optional quota state before a small business confirms a paid-conversation purchase.
 * The three variants split the wire outcome into {@link Success} (full checkout projection),
 * {@link ClientError} (relay rejected the lookup with a documented {@code 4xx} envelope) and
 * {@link ServerError} (transient {@code 5xx} relay failure).
 *
 * @implNote
 * This implementation tries each variant in priority order via {@link #of(Stanza, Stanza)} and returns
 * the first successful parse.
 */
public sealed interface SmaxGetSMBMeteredMessagingCheckoutResponse extends SmaxStanza.Response
        permits SmaxGetSMBMeteredMessagingCheckoutResponse.Success, SmaxGetSMBMeteredMessagingCheckoutResponse.ClientError, SmaxGetSMBMeteredMessagingCheckoutResponse.ServerError {

    /**
     * Tries each {@link SmaxGetSMBMeteredMessagingCheckoutResponse}
     * variant in priority order and returns the first that parses
     * cleanly.
     *
     * <p>Invoked by the smax reply pump after dispatching a
     * {@link SmaxGetSMBMeteredMessagingCheckoutRequest}. The priority order ensures a malformed
     * {@link Success} stanza falls through to {@link ClientError} rather than masking an error.
     *
     * @implNote
     * This implementation invokes {@link Success#of(Stanza, Stanza)}
     * first, then {@link ClientError#of(Stanza, Stanza)}, then
     * {@link ServerError#of(Stanza, Stanza)}; an unrecognised stanza
     * shape returns {@link Optional#empty()}.
     *
     * @param stanza    the inbound IQ stanza received from the relay;
     *                never {@code null}
     * @param request the original outbound stanza, used to validate
     *                echoed identifiers; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or
     *         {@link Optional#empty()} when no documented variant
     *         matched the stanza shape
     * @throws NullPointerException if either argument is
     *                              {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxSmbMeteredMessagingAccountGetSMBMeteredMessagingCheckoutRPC",
            exports = "sendGetSMBMeteredMessagingCheckoutRPC",
            adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGetSMBMeteredMessagingCheckoutResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * The {@code Success} reply variant carrying the full SMB
     * metered-messaging checkout projection.
     *
     * <p>Projected by {@link SmaxGetSMBMeteredMessagingCheckoutResponse#of(Stanza, Stanza)} when the
     * relay returns the documented {@code <cost>}, {@code <integrity>}, {@code <account_balance>}
     * and {@code <quota>} tree consumed by the checkout UI.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInSmbMeteredMessagingAccountGetSMBMeteredMessagingCheckoutResponseSuccess")
    final class Success implements SmaxGetSMBMeteredMessagingCheckoutResponse {
        /**
         * The mandatory cost projection drawn from the
         * {@code <cost>} child.
         */
        private final Cost cost;

        /**
         * The mandatory integrity-eligibility marker drawn from the
         * {@code <integrity is_eligible="..."/>} child.
         */
        private final SmaxGetSMBMeteredMessagingCheckoutIntegrityEligibility integrityIsEligible;

        /**
         * The mandatory account-balance projection drawn from the
         * {@code <account_balance>} child.
         */
        private final AccountBalance accountBalance;

        /**
         * The optional quota projection drawn from the {@code <quota>}
         * child; {@code null} when the relay omitted the child.
         */
        private final Quota quota;

        /**
         * Constructs a new successful reply.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} after all four child projections have been
         * validated.
         *
         * @param cost                the cost projection; never
         *                            {@code null}
         * @param integrityIsEligible the integrity-eligibility
         *                            marker; never {@code null}
         * @param accountBalance      the account-balance projection;
         *                            never {@code null}
         * @param quota               the optional quota projection;
         *                            may be {@code null}
         * @throws NullPointerException if any of {@code cost},
         *                              {@code integrityIsEligible}
         *                              or {@code accountBalance} is
         *                              {@code null}
         */
        public Success(Cost cost, SmaxGetSMBMeteredMessagingCheckoutIntegrityEligibility integrityIsEligible,
                       AccountBalance accountBalance, Quota quota) {
            this.cost = Objects.requireNonNull(cost, "cost cannot be null");
            this.integrityIsEligible = Objects.requireNonNull(integrityIsEligible,
                    "integrityIsEligible cannot be null");
            this.accountBalance = Objects.requireNonNull(accountBalance, "accountBalance cannot be null");
            this.quota = quota;
        }

        /**
         * Returns the cost projection.
         *
         * @return the cost; never {@code null}
         */
        public Cost cost() {
            return cost;
        }

        /**
         * Returns the integrity-eligibility marker.
         *
         * <p>Reflects whether the business satisfies the integrity checks required before allowing
         * a paid-conversation purchase.
         *
         * @return the marker; never {@code null}
         */
        public SmaxGetSMBMeteredMessagingCheckoutIntegrityEligibility integrityIsEligible() {
            return integrityIsEligible;
        }

        /**
         * Returns the account-balance projection.
         *
         * @return the balance; never {@code null}
         */
        public AccountBalance accountBalance() {
            return accountBalance;
        }

        /**
         * Returns the optional quota projection.
         *
         * @return an {@link Optional} carrying the projection, or
         *         empty when the relay omitted the
         *         {@code <quota/>} child
         */
        public Optional<Quota> quota() {
            return Optional.ofNullable(quota);
        }

        /**
         * Tries to parse a {@link Success} variant from the given
         * inbound stanza.
         *
         * @implNote
         * This implementation enforces the
         * {@link SmaxIqResultResponseMixin} envelope check, then
         * locates the {@code <cost>}, {@code <integrity>} and
         * {@code <account_balance>} children (any of which is
         * required) and the optional {@code <quota>} child. The
         * integrity attribute is admitted via
         * {@link SmaxGetSMBMeteredMessagingCheckoutIntegrityEligibility#of(String)}
         * (the {@code ENUM_FALSE_TRUE} dictionary); the per-child
         * parses delegate to {@link Cost#of(Stanza)},
         * {@link AccountBalance#of(Stanza)} and {@link Quota#of(Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant,
         *         or empty when the stanza does not match the
         *         success schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInSmbMeteredMessagingAccountGetSMBMeteredMessagingCheckoutResponseSuccess",
                exports = "parseGetSMBMeteredMessagingCheckoutResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            Objects.requireNonNull(request, "request cannot be null");
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var costNode = stanza.getChild("cost").orElse(null);
            if (costNode == null) {
                return Optional.empty();
            }
            var cost = Cost.of(costNode).orElse(null);
            if (cost == null) {
                return Optional.empty();
            }
            var integrityNode = stanza.getChild("integrity").orElse(null);
            if (integrityNode == null) {
                return Optional.empty();
            }
            var integrityStr = integrityNode.getAttributeAsString("is_eligible").orElse(null);
            var integrity = SmaxGetSMBMeteredMessagingCheckoutIntegrityEligibility.of(integrityStr).orElse(null);
            if (integrity == null) {
                return Optional.empty();
            }
            var balanceNode = stanza.getChild("account_balance").orElse(null);
            if (balanceNode == null) {
                return Optional.empty();
            }
            var balance = AccountBalance.of(balanceNode).orElse(null);
            if (balance == null) {
                return Optional.empty();
            }
            Quota quota = null;
            var quotaNode = stanza.getChild("quota").orElse(null);
            if (quotaNode != null) {
                var parsed = Quota.of(quotaNode);
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                quota = parsed.get();
            }
            return Optional.of(new Success(cost, integrity, balance, quota));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Success) obj;
            return Objects.equals(this.cost, that.cost)
                    && this.integrityIsEligible == that.integrityIsEligible
                    && Objects.equals(this.accountBalance, that.accountBalance)
                    && Objects.equals(this.quota, that.quota);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(cost, integrityIsEligible, accountBalance, quota);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "SmaxGetSMBMeteredMessagingCheckoutResponse.Success[cost=" + cost
                    + ", integrityIsEligible=" + integrityIsEligible
                    + ", accountBalance=" + accountBalance
                    + ", quota=" + quota + ']';
        }

        /**
         * The {@code <cost/>} child projection carrying the full
         * cost breakdown for the metered-messaging checkout.
         *
         * <p>Aggregates the mandatory pre-tax cost, tax amount, currency offset and currency
         * identifier with the optional pre-discount {@code base}, {@code base_formatted},
         * {@code discount_percent}, {@code before_discount}, {@code before_discount_formatted}
         * fields and the optional list of applied discounts.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInSmbMeteredMessagingAccountGetSMBMeteredMessagingCheckoutResponseSuccess")
        public static final class Cost {
            /**
             * The mandatory pre-tax cost expressed in
             * currency-minor units.
             */
            private final int beforeTax;

            /**
             * The mandatory tax amount expressed in currency-minor
             * units.
             */
            private final int tax;

            /**
             * The mandatory currency offset (number of decimal
             * places to shift the minor-unit integers by).
             */
            private final int offset;

            /**
             * The mandatory currency identifier (ISO-4217 code).
             */
            private final String currency;

            /**
             * The optional pre-discount base expressed in
             * currency-minor units; {@code null} when the relay
             * omitted the attribute.
             */
            private final Integer base;

            /**
             * The optional formatted pre-discount base for direct
             * UI display; {@code null} when the relay omitted the
             * attribute.
             */
            private final String baseFormatted;

            /**
             * The optional discount percent; {@code null} when the
             * relay omitted the attribute.
             */
            private final Integer discountPercent;

            /**
             * The optional pre-discount cost expressed in
             * currency-minor units; {@code null} when the relay
             * omitted the attribute.
             */
            private final Integer beforeDiscount;

            /**
             * The optional formatted pre-discount cost for direct
             * UI display; {@code null} when the relay omitted the
             * attribute.
             */
            private final String beforeDiscountFormatted;

            /**
             * The optional list of applied discount entries
             * ({@code 0..10} per the
             * {@code mapChildrenWithTag(..., 0, 10, ...)} contract);
             * empty when the relay omitted the
             * {@code <discounts/>} child or when the child was
             * empty.
             */
            private final List<Discount> discounts;

            /**
             * Constructs a new cost projection.
             *
             * <p>Invoked by {@link #of(Stanza)} after every mandatory attribute has been read and the
             * optional pre-discount and discount-list fields have been resolved.
             *
             * @param beforeTax               the pre-tax cost in
             *                                currency-minor units
             * @param tax                     the tax in
             *                                currency-minor units
             * @param offset                  the currency offset
             *                                (decimal places)
             * @param currency                the currency code;
             *                                never {@code null}
             * @param base                    the optional
             *                                pre-discount base; may
             *                                be {@code null}
             * @param baseFormatted           the optional formatted
             *                                base; may be
             *                                {@code null}
             * @param discountPercent         the optional discount
             *                                percent; may be
             *                                {@code null}
             * @param beforeDiscount          the optional
             *                                pre-discount cost; may
             *                                be {@code null}
             * @param beforeDiscountFormatted the optional formatted
             *                                pre-discount cost;
             *                                may be {@code null}
             * @param discounts               the optional list of
             *                                applied discounts; may
             *                                be {@code null}
             *                                (treated as empty)
             * @throws NullPointerException if {@code currency} is
             *                              {@code null}
             */
            public Cost(int beforeTax, int tax, int offset, String currency,
                        Integer base, String baseFormatted,
                        Integer discountPercent, Integer beforeDiscount,
                        String beforeDiscountFormatted,
                        List<Discount> discounts) {
                this.beforeTax = beforeTax;
                this.tax = tax;
                this.offset = offset;
                this.currency = Objects.requireNonNull(currency, "currency cannot be null");
                this.base = base;
                this.baseFormatted = baseFormatted;
                this.discountPercent = discountPercent;
                this.beforeDiscount = beforeDiscount;
                this.beforeDiscountFormatted = beforeDiscountFormatted;
                this.discounts = discounts == null ? List.of() : List.copyOf(discounts);
            }

            /**
             * Returns the pre-tax cost.
             *
             * @return the cost in currency-minor units
             */
            public int beforeTax() {
                return beforeTax;
            }

            /**
             * Returns the tax amount.
             *
             * @return the tax in currency-minor units
             */
            public int tax() {
                return tax;
            }

            /**
             * Returns the currency offset.
             *
             * <p>Apply by shifting the minor-unit integers {@link #beforeTax()} and {@link #tax()}
             * right by this many decimal places when rendering to the UI.
             *
             * @return the offset (number of decimal places)
             */
            public int offset() {
                return offset;
            }

            /**
             * Returns the currency identifier.
             *
             * @return the currency code (ISO-4217); never
             *         {@code null}
             */
            public String currency() {
                return currency;
            }

            /**
             * Returns the optional pre-discount base cost.
             *
             * @return an {@link OptionalInt} carrying the value, or
             *         empty when the relay omitted the attribute
             */
            public OptionalInt base() {
                return base == null ? OptionalInt.empty() : OptionalInt.of(base);
            }

            /**
             * Returns the optional formatted pre-discount base cost.
             *
             * @return an {@link Optional} carrying the value, or
             *         empty when the relay omitted the attribute
             */
            public Optional<String> baseFormatted() {
                return Optional.ofNullable(baseFormatted);
            }

            /**
             * Returns the optional discount percent.
             *
             * @return an {@link OptionalInt} carrying the value, or
             *         empty when the relay omitted the attribute
             */
            public OptionalInt discountPercent() {
                return discountPercent == null ? OptionalInt.empty() : OptionalInt.of(discountPercent);
            }

            /**
             * Returns the optional pre-discount cost.
             *
             * @return an {@link OptionalInt} carrying the value, or
             *         empty when the relay omitted the attribute
             */
            public OptionalInt beforeDiscount() {
                return beforeDiscount == null ? OptionalInt.empty() : OptionalInt.of(beforeDiscount);
            }

            /**
             * Returns the optional formatted pre-discount cost.
             *
             * @return an {@link Optional} carrying the value, or
             *         empty when the relay omitted the attribute
             */
            public Optional<String> beforeDiscountFormatted() {
                return Optional.ofNullable(beforeDiscountFormatted);
            }

            /**
             * Returns the list of applied discount entries.
             *
             * @return an unmodifiable list of {@code 0..10}
             *         entries; never {@code null}
             */
            public List<Discount> discounts() {
                return discounts;
            }

            /**
             * Tries to parse the cost projection from the given
             * {@code <cost/>} stanza, decoding the {@code <cost>} child of the checkout-response
             * stanza for {@link Success#of(Stanza, Stanza)}.
             *
             * @implNote
             * This implementation reads the four mandatory
             * attributes ({@code before_tax}, {@code tax},
             * {@code offset}, {@code currency}) followed by the
             * five optional attributes ({@code base},
             * {@code base_formatted}, {@code discount_percent},
             * {@code before_discount},
             * {@code before_discount_formatted}); any mandatory
             * attribute missing yields {@link Optional#empty()}.
             * The optional {@code <discounts>} child is decoded
             * via {@link Discount#of(Stanza)}, with the
             * {@code 0..10} cap from
             * {@code mapChildrenWithTag(t, "discount", 0, 10, e)}
             * enforced as a rejection of the parse when exceeded.
             *
             * @param stanza the {@code <cost/>} stanza
             * @return an {@link Optional} carrying the projection,
             *         or empty when the stanza does not match the
             *         documented schema
             */
            public static Optional<Cost> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("cost")) {
                    return Optional.empty();
                }
                var beforeTax = stanza.getAttributeAsInt("before_tax");
                if (beforeTax.isEmpty()) {
                    return Optional.empty();
                }
                var tax = stanza.getAttributeAsInt("tax");
                if (tax.isEmpty()) {
                    return Optional.empty();
                }
                var offset = stanza.getAttributeAsInt("offset");
                if (offset.isEmpty()) {
                    return Optional.empty();
                }
                var currency = stanza.getAttributeAsString("currency").orElse(null);
                if (currency == null) {
                    return Optional.empty();
                }
                Integer base = null;
                var baseOpt = stanza.getAttributeAsInt("base");
                if (baseOpt.isPresent()) {
                    base = baseOpt.getAsInt();
                }
                var baseFormatted = stanza.getAttributeAsString("base_formatted").orElse(null);
                Integer discountPercent = null;
                var discountPercentOpt = stanza.getAttributeAsInt("discount_percent");
                if (discountPercentOpt.isPresent()) {
                    discountPercent = discountPercentOpt.getAsInt();
                }
                Integer beforeDiscount = null;
                var beforeDiscountOpt = stanza.getAttributeAsInt("before_discount");
                if (beforeDiscountOpt.isPresent()) {
                    beforeDiscount = beforeDiscountOpt.getAsInt();
                }
                var beforeDiscountFormatted = stanza.getAttributeAsString("before_discount_formatted").orElse(null);
                var discounts = new ArrayList<Discount>();
                var discountsNode = stanza.getChild("discounts").orElse(null);
                if (discountsNode != null) {
                    var iter = discountsNode.streamChildren("discount").iterator();
                    while (iter.hasNext()) {
                        var parsed = Discount.of(iter.next());
                        if (parsed.isEmpty()) {
                            return Optional.empty();
                        }
                        discounts.add(parsed.get());
                    }
                    if (discounts.size() > 10) {
                        return Optional.empty();
                    }
                }
                return Optional.of(new Cost(beforeTax.getAsInt(), tax.getAsInt(), offset.getAsInt(),
                        currency, base, baseFormatted, discountPercent, beforeDiscount,
                        beforeDiscountFormatted, discounts));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (Cost) obj;
                return this.beforeTax == that.beforeTax
                        && this.tax == that.tax
                        && this.offset == that.offset
                        && Objects.equals(this.currency, that.currency)
                        && Objects.equals(this.base, that.base)
                        && Objects.equals(this.baseFormatted, that.baseFormatted)
                        && Objects.equals(this.discountPercent, that.discountPercent)
                        && Objects.equals(this.beforeDiscount, that.beforeDiscount)
                        && Objects.equals(this.beforeDiscountFormatted, that.beforeDiscountFormatted)
                        && Objects.equals(this.discounts, that.discounts);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public int hashCode() {
                return Objects.hash(beforeTax, tax, offset, currency, base, baseFormatted,
                        discountPercent, beforeDiscount, beforeDiscountFormatted, discounts);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "SmaxGetSMBMeteredMessagingCheckoutResponse.Success.Cost[beforeTax=" + beforeTax
                        + ", tax=" + tax
                        + ", offset=" + offset
                        + ", currency=" + currency
                        + ", base=" + base
                        + ", baseFormatted=" + baseFormatted
                        + ", discountPercent=" + discountPercent
                        + ", beforeDiscount=" + beforeDiscount
                        + ", beforeDiscountFormatted=" + beforeDiscountFormatted
                        + ", discounts=" + discounts + ']';
            }

            /**
             * A single applied discount entry under the
             * {@code <discounts><discount/></discounts>} subtree.
             *
             * <p>Projects the {@code (type, percentage, amount, amountFormatted)} record passed to
             * the checkout UI as one entry of its discounts array.
             */
            @WhatsAppWebModule(moduleName = "WASmaxInSmbMeteredMessagingAccountGetSMBMeteredMessagingCheckoutResponseSuccess")
            public static final class Discount {
                /**
                 * The mandatory discount type drawn from the
                 * {@code ENUM_FREEMSG_PERCENTAGE} dictionary
                 * ({@code "freemsg"} or {@code "percentage"}).
                 */
                private final SmaxGetSMBMeteredMessagingCheckoutDiscountType type;

                /**
                 * The optional percentage value; only meaningful
                 * when {@link #type} is
                 * {@link SmaxGetSMBMeteredMessagingCheckoutDiscountType#PERCENTAGE}.
                 */
                private final Integer percentage;

                /**
                 * The mandatory discount amount in
                 * currency-minor units.
                 */
                private final int amount;

                /**
                 * The mandatory formatted discount amount for
                 * direct UI display.
                 */
                private final String amountFormatted;

                /**
                 * Constructs a new discount entry.
                 *
                 * <p>Invoked by {@link #of(Stanza)} after every mandatory attribute has been read and
                 * the optional {@code percentage} has been resolved.
                 *
                 * @param type            the discount type; never
                 *                        {@code null}
                 * @param percentage      the optional percentage;
                 *                        may be {@code null}
                 * @param amount          the discount amount in
                 *                        currency-minor units
                 * @param amountFormatted the formatted amount;
                 *                        never {@code null}
                 * @throws NullPointerException if {@code type} or
                 *                              {@code amountFormatted}
                 *                              is {@code null}
                 */
                public Discount(SmaxGetSMBMeteredMessagingCheckoutDiscountType type, Integer percentage,
                                int amount, String amountFormatted) {
                    this.type = Objects.requireNonNull(type, "type cannot be null");
                    this.percentage = percentage;
                    this.amount = amount;
                    this.amountFormatted = Objects.requireNonNull(amountFormatted,
                            "amountFormatted cannot be null");
                }

                /**
                 * Returns the discount type.
                 *
                 * @return the type; never {@code null}
                 */
                public SmaxGetSMBMeteredMessagingCheckoutDiscountType type() {
                    return type;
                }

                /**
                 * Returns the optional percentage value.
                 *
                 * @return an {@link OptionalInt} carrying the
                 *         percentage, or empty when the relay
                 *         omitted the attribute
                 */
                public OptionalInt percentage() {
                    return percentage == null ? OptionalInt.empty() : OptionalInt.of(percentage);
                }

                /**
                 * Returns the discount amount.
                 *
                 * @return the amount in currency-minor units
                 */
                public int amount() {
                    return amount;
                }

                /**
                 * Returns the formatted discount amount.
                 *
                 * @return the formatted string; never {@code null}
                 */
                public String amountFormatted() {
                    return amountFormatted;
                }

                /**
                 * Tries to parse a discount entry from the given
                 * {@code <discount/>} stanza, decoding each child of the {@code <discounts>}
                 * grandchild for {@link Cost#of(Stanza)}.
                 *
                 * @implNote
                 * This implementation requires the {@code type}
                 * attribute to match the
                 * {@code ENUM_FREEMSG_PERCENTAGE} dictionary via
                 * {@link SmaxGetSMBMeteredMessagingCheckoutDiscountType#of(String)},
                 * reads the optional {@code percentage} attribute,
                 * the mandatory {@code amount} attribute, and the
                 * mandatory {@code amount_formatted} attribute;
                 * any mandatory attribute missing yields
                 * {@link Optional#empty()}.
                 *
                 * @param stanza the {@code <discount/>} stanza
                 * @return an {@link Optional} carrying the parsed
                 *         entry, or empty when the stanza does not
                 *         match the documented schema
                 */
                public static Optional<Discount> of(Stanza stanza) {
                    Objects.requireNonNull(stanza, "stanza cannot be null");
                    if (!stanza.hasDescription("discount")) {
                        return Optional.empty();
                    }
                    var typeStr = stanza.getAttributeAsString("type").orElse(null);
                    var type = SmaxGetSMBMeteredMessagingCheckoutDiscountType.of(typeStr).orElse(null);
                    if (type == null) {
                        return Optional.empty();
                    }
                    Integer percentage = null;
                    var percentageOpt = stanza.getAttributeAsInt("percentage");
                    if (percentageOpt.isPresent()) {
                        percentage = percentageOpt.getAsInt();
                    }
                    var amountOpt = stanza.getAttributeAsInt("amount");
                    if (amountOpt.isEmpty()) {
                        return Optional.empty();
                    }
                    var amountFormatted = stanza.getAttributeAsString("amount_formatted").orElse(null);
                    if (amountFormatted == null) {
                        return Optional.empty();
                    }
                    return Optional.of(new Discount(type, percentage, amountOpt.getAsInt(), amountFormatted));
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public boolean equals(Object obj) {
                    if (obj == this) {
                        return true;
                    }
                    if (obj == null || obj.getClass() != this.getClass()) {
                        return false;
                    }
                    var that = (Discount) obj;
                    return this.amount == that.amount
                            && this.type == that.type
                            && Objects.equals(this.percentage, that.percentage)
                            && Objects.equals(this.amountFormatted, that.amountFormatted);
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public int hashCode() {
                    return Objects.hash(type, percentage, amount, amountFormatted);
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public String toString() {
                    return "SmaxGetSMBMeteredMessagingCheckoutResponse.Success.Cost.Discount[type=" + type
                            + ", percentage=" + percentage
                            + ", amount=" + amount
                            + ", amountFormatted=" + amountFormatted + ']';
                }
            }
        }

        /**
         * The {@code <account_balance/>} child projection carrying
         * the billing, available and offset triple for the calling
         * business's metered-messaging wallet.
         *
         * <p>The available balance drives the "available balance" disclosure shown alongside the
         * cost breakdown; the billing and offset fields support consistent minor-unit rendering.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInSmbMeteredMessagingAccountGetSMBMeteredMessagingCheckoutResponseSuccess")
        public static final class AccountBalance {
            /**
             * The mandatory total billed-to-date balance in
             * currency-minor units.
             */
            private final int billing;

            /**
             * The mandatory currently-available balance in
             * currency-minor units.
             */
            private final int available;

            /**
             * The mandatory currency offset (decimal places).
             */
            private final int offset;

            /**
             * Constructs a new balance projection.
             *
             * <p>Invoked by {@link #of(Stanza)} after every mandatory attribute has been read.
             *
             * @param billing   the billed-to-date balance in
             *                  currency-minor units
             * @param available the available balance in
             *                  currency-minor units
             * @param offset    the currency offset (decimal
             *                  places)
             */
            public AccountBalance(int billing, int available, int offset) {
                this.billing = billing;
                this.available = available;
                this.offset = offset;
            }

            /**
             * Returns the billed-to-date balance.
             *
             * @return the balance in currency-minor units
             */
            public int billing() {
                return billing;
            }

            /**
             * Returns the available balance.
             *
             * @return the balance in currency-minor units
             */
            public int available() {
                return available;
            }

            /**
             * Returns the currency offset.
             *
             * @return the offset (number of decimal places)
             */
            public int offset() {
                return offset;
            }

            /**
             * Tries to parse the balance projection from the given
             * {@code <account_balance/>} stanza, decoding the {@code <account_balance>} child of the
             * checkout-response stanza for {@link Success#of(Stanza, Stanza)}.
             *
             * @implNote
             * This implementation requires the three mandatory
             * integer attributes ({@code billing},
             * {@code available}, {@code offset}); any attribute
             * missing yields {@link Optional#empty()}.
             *
             * @param stanza the {@code <account_balance/>} stanza
             * @return an {@link Optional} carrying the projection,
             *         or empty when the stanza does not match the
             *         documented schema
             */
            public static Optional<AccountBalance> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("account_balance")) {
                    return Optional.empty();
                }
                var billing = stanza.getAttributeAsInt("billing");
                if (billing.isEmpty()) {
                    return Optional.empty();
                }
                var available = stanza.getAttributeAsInt("available");
                if (available.isEmpty()) {
                    return Optional.empty();
                }
                var offset = stanza.getAttributeAsInt("offset");
                if (offset.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(new AccountBalance(billing.getAsInt(), available.getAsInt(), offset.getAsInt()));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (AccountBalance) obj;
                return this.billing == that.billing
                        && this.available == that.available
                        && this.offset == that.offset;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public int hashCode() {
                return Objects.hash(billing, available, offset);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "SmaxGetSMBMeteredMessagingCheckoutResponse.Success.AccountBalance[billing=" + billing
                        + ", available=" + available
                        + ", offset=" + offset + ']';
            }
        }

        /**
         * The optional {@code <quota/>} child projection carrying
         * the calling business's free-message quota state.
         *
         * <p>Projects the {@code (remaining, totalMonthly, singleCredits, totalAvailableCredits)}
         * quadruple forwarded to the checkout UI for the "free messages remaining this month"
         * disclosure.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInSmbMeteredMessagingAccountGetSMBMeteredMessagingCheckoutResponseSuccess")
        public static final class Quota {
            /**
             * The mandatory remaining-message quota.
             */
            private final int remaining;

            /**
             * The mandatory total monthly quota.
             */
            private final int totalMonthly;

            /**
             * The optional one-shot single-credits balance;
             * {@code null} when the relay omitted the attribute.
             */
            private final Integer singleCredits;

            /**
             * The optional total-available-credits projection;
             * {@code null} when the relay omitted the attribute.
             */
            private final Integer totalAvailableCredits;

            /**
             * Constructs a new quota projection.
             *
             * <p>Invoked by {@link #of(Stanza)} after the two mandatory attributes have been read and
             * the two optional attributes have been resolved.
             *
             * @param remaining             the remaining-message
             *                              quota
             * @param totalMonthly          the total monthly quota
             * @param singleCredits         the optional
             *                              single-credits balance;
             *                              may be {@code null}
             * @param totalAvailableCredits the optional
             *                              total-available-credits
             *                              projection; may be
             *                              {@code null}
             */
            public Quota(int remaining, int totalMonthly,
                         Integer singleCredits, Integer totalAvailableCredits) {
                this.remaining = remaining;
                this.totalMonthly = totalMonthly;
                this.singleCredits = singleCredits;
                this.totalAvailableCredits = totalAvailableCredits;
            }

            /**
             * Returns the remaining-message quota.
             *
             * @return the remaining count
             */
            public int remaining() {
                return remaining;
            }

            /**
             * Returns the total monthly quota.
             *
             * @return the total count
             */
            public int totalMonthly() {
                return totalMonthly;
            }

            /**
             * Returns the optional single-credits balance.
             *
             * @return an {@link OptionalInt} carrying the value,
             *         or empty when the relay omitted the
             *         attribute
             */
            public OptionalInt singleCredits() {
                return singleCredits == null ? OptionalInt.empty() : OptionalInt.of(singleCredits);
            }

            /**
             * Returns the optional total-available-credits
             * projection.
             *
             * @return an {@link OptionalInt} carrying the value,
             *         or empty when the relay omitted the
             *         attribute
             */
            public OptionalInt totalAvailableCredits() {
                return totalAvailableCredits == null
                        ? OptionalInt.empty()
                        : OptionalInt.of(totalAvailableCredits);
            }

            /**
             * Tries to parse the quota projection from the given
             * {@code <quota/>} stanza, decoding the optional {@code <quota>} child of the
             * checkout-response stanza for {@link Success#of(Stanza, Stanza)}.
             *
             * @implNote
             * This implementation reads the two mandatory integer
             * attributes ({@code remaining}, {@code total_monthly})
             * and the two optional integer attributes
             * ({@code single_credits},
             * {@code total_available_credits}); any mandatory
             * attribute missing yields {@link Optional#empty()}.
             *
             * @param stanza the {@code <quota/>} stanza
             * @return an {@link Optional} carrying the projection,
             *         or empty when the stanza does not match the
             *         documented schema
             */
            public static Optional<Quota> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("quota")) {
                    return Optional.empty();
                }
                var remaining = stanza.getAttributeAsInt("remaining");
                if (remaining.isEmpty()) {
                    return Optional.empty();
                }
                var totalMonthly = stanza.getAttributeAsInt("total_monthly");
                if (totalMonthly.isEmpty()) {
                    return Optional.empty();
                }
                Integer singleCredits = null;
                var singleCreditsOpt = stanza.getAttributeAsInt("single_credits");
                if (singleCreditsOpt.isPresent()) {
                    singleCredits = singleCreditsOpt.getAsInt();
                }
                Integer totalAvailableCredits = null;
                var totalAvailableCreditsOpt = stanza.getAttributeAsInt("total_available_credits");
                if (totalAvailableCreditsOpt.isPresent()) {
                    totalAvailableCredits = totalAvailableCreditsOpt.getAsInt();
                }
                return Optional.of(new Quota(remaining.getAsInt(), totalMonthly.getAsInt(),
                        singleCredits, totalAvailableCredits));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (Quota) obj;
                return this.remaining == that.remaining
                        && this.totalMonthly == that.totalMonthly
                        && Objects.equals(this.singleCredits, that.singleCredits)
                        && Objects.equals(this.totalAvailableCredits, that.totalAvailableCredits);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public int hashCode() {
                return Objects.hash(remaining, totalMonthly, singleCredits, totalAvailableCredits);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "SmaxGetSMBMeteredMessagingCheckoutResponse.Success.Quota[remaining=" + remaining
                        + ", totalMonthly=" + totalMonthly
                        + ", singleCredits=" + singleCredits
                        + ", totalAvailableCredits=" + totalAvailableCredits + ']';
            }
        }
    }

    /**
     * The {@code ClientError} reply variant carrying a documented
     * {@code 4xx} SMB-metered-messaging rejection.
     *
     * <p>Surfaced when the relay rejected the checkout lookup. The caller treats this branch as a
     * terminal status-code error and surfaces it to the checkout UI rather than retrying.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInSmbMeteredMessagingAccountGetSMBMeteredMessagingCheckoutResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInSmbMeteredMessagingAccountGetSmbMeteredMessagingCheckoutIqErrors")
    final class ClientError implements SmaxGetSMBMeteredMessagingCheckoutResponse {
        /**
         * The numeric server-side error code in the {@code 4xx}
         * range.
         */
        private final int errorCode;

        /**
         * The human-readable error text, when the relay supplied
         * one.
         */
        private final String errorText;

        /**
         * Constructs a new client-error reply.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} after the {@code 4xx} envelope has been validated.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may
         *                  be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or
         *         empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the
         * given inbound stanza.
         *
         * @implNote
         * This implementation routes the {@code <iq>}/{@code <error>}
         * extraction through
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}
         * and admits the full {@code 4xx} range as a catch-all,
         * matching WA Web's
         * {@code parseGetSmbMeteredMessagingCheckoutIqErrors}
         * disjunction.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant,
         *         or empty when the stanza does not match the
         *         client-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInSmbMeteredMessagingAccountGetSMBMeteredMessagingCheckoutResponseError",
                exports = "parseGetSMBMeteredMessagingCheckoutResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ClientError) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "SmaxGetSMBMeteredMessagingCheckoutResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * The {@code ServerError} reply variant carrying a transient
     * {@code 5xx} relay failure.
     *
     * <p>Surfaced when the relay returned a transient internal failure while computing the checkout
     * projection; the caller can re-issue the request with backoff.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInSmbMeteredMessagingAccountGetSMBMeteredMessagingCheckoutResponseError")
    final class ServerError implements SmaxGetSMBMeteredMessagingCheckoutResponse {
        /**
         * The numeric server-side error code in the {@code 5xx}
         * range.
         */
        private final int errorCode;

        /**
         * The human-readable error text, when the relay supplied
         * one.
         */
        private final String errorText;

        /**
         * Constructs a new server-error reply.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} after the {@code 5xx} envelope has been validated.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may
         *                  be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or
         *         empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the
         * given inbound stanza.
         *
         * @implNote
         * This implementation delegates the {@code 5xx} range
         * check to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)};
         * any stanza outside the {@code 5xx} range yields
         * {@link Optional#empty()}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant,
         *         or empty when the stanza does not match the
         *         server-error schema
         */
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ServerError) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "SmaxGetSMBMeteredMessagingCheckoutResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
