package com.github.auties00.cobalt.wire.linked.business.marketing;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Cost breakdown for an SMB metered-messaging campaign quote.
 *
 * <p>All monetary amounts are denominated in currency-minor units
 * (cents/paise/centavos) and are interpreted using the
 * {@linkplain #offset() decimal offset} the relay reports alongside
 * them; for currencies with two decimal places the offset is
 * {@code 2}, so a {@code beforeTax} value of {@code 1234} prints as
 * {@code 12.34}. The optional {@linkplain #base() base} and
 * {@linkplain #beforeDiscount() pre-discount} fields record the
 * undiscounted amounts when the quote includes promotional discounts;
 * the {@linkplain #discountPercent() discount percent} restates the
 * applied discount as a whole-number percentage.
 */
@ProtobufMessage(name = "BusinessMeteredMessagingCost")
public final class BusinessMeteredMessagingCost {
    /**
     * Pre-tax cost (currency-minor units).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    int beforeTax;

    /**
     * Tax amount (currency-minor units).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    int tax;

    /**
     * Decimal offset for the currency (number of fractional digits).
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    int offset;

    /**
     * ISO 4217-style currency identifier.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String currency;

    /**
     * Optional pre-discount base cost (currency-minor units).
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT32)
    Integer base;

    /**
     * Optional human-formatted pre-discount base cost (e.g.
     * {@code "$0.50"}).
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String baseFormatted;

    /**
     * Optional applied-discount percent (whole-number percentage).
     */
    @ProtobufProperty(index = 7, type = ProtobufType.INT32)
    Integer discountPercent;

    /**
     * Optional pre-discount cost (currency-minor units).
     */
    @ProtobufProperty(index = 8, type = ProtobufType.INT32)
    Integer beforeDiscount;

    /**
     * Optional human-formatted pre-discount cost.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    String beforeDiscountFormatted;

    /**
     * Full protobuf constructor invoked by the generated builder and the
     * deserializer.
     *
     * @param beforeTax               the pre-tax cost
     * @param tax                     the tax amount
     * @param offset                  the decimal offset
     * @param currency                the currency code
     * @param base                    the optional base cost
     * @param baseFormatted           the optional formatted base cost
     * @param discountPercent         the optional discount percent
     * @param beforeDiscount          the optional pre-discount cost
     * @param beforeDiscountFormatted the optional formatted pre-discount
     *                                cost
     */
    BusinessMeteredMessagingCost(int beforeTax, int tax, int offset, String currency,
                            Integer base, String baseFormatted,
                            Integer discountPercent, Integer beforeDiscount,
                            String beforeDiscountFormatted) {
        this.beforeTax = beforeTax;
        this.tax = tax;
        this.offset = offset;
        this.currency = currency;
        this.base = base;
        this.baseFormatted = baseFormatted;
        this.discountPercent = discountPercent;
        this.beforeDiscount = beforeDiscount;
        this.beforeDiscountFormatted = beforeDiscountFormatted;
    }

    /**
     * Returns the pre-tax cost in currency-minor units.
     *
     * @return the pre-tax cost
     */
    public int beforeTax() {
        return beforeTax;
    }

    /**
     * Returns the tax amount in currency-minor units.
     *
     * @return the tax amount
     */
    public int tax() {
        return tax;
    }

    /**
     * Returns the decimal offset for the currency.
     *
     * @return the offset
     */
    public int offset() {
        return offset;
    }

    /**
     * Returns the currency identifier.
     *
     * @return the currency code; never {@code null} for a parsed cost
     */
    public String currency() {
        return currency;
    }

    /**
     * Returns the optional base cost.
     *
     * @return an {@link OptionalInt} carrying the cost, or empty when
     *         the relay omitted it
     */
    public OptionalInt base() {
        return base == null ? OptionalInt.empty() : OptionalInt.of(base);
    }

    /**
     * Returns the optional formatted base cost.
     *
     * @return an {@link Optional} carrying the formatted cost, or
     *         empty when the relay omitted it
     */
    public Optional<String> baseFormatted() {
        return Optional.ofNullable(baseFormatted);
    }

    /**
     * Returns the optional applied-discount percent.
     *
     * @return an {@link OptionalInt} carrying the percent, or empty
     *         when the relay omitted it
     */
    public OptionalInt discountPercent() {
        return discountPercent == null ? OptionalInt.empty() : OptionalInt.of(discountPercent);
    }

    /**
     * Returns the optional pre-discount cost.
     *
     * @return an {@link OptionalInt} carrying the cost, or empty when
     *         the relay omitted it
     */
    public OptionalInt beforeDiscount() {
        return beforeDiscount == null ? OptionalInt.empty() : OptionalInt.of(beforeDiscount);
    }

    /**
     * Returns the optional formatted pre-discount cost.
     *
     * @return an {@link Optional} carrying the formatted cost, or
     *         empty when the relay omitted it
     */
    public Optional<String> beforeDiscountFormatted() {
        return Optional.ofNullable(beforeDiscountFormatted);
    }
}
