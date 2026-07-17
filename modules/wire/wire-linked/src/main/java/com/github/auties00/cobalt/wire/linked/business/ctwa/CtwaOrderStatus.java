package com.github.auties00.cobalt.wire.linked.business.ctwa;

/**
 * Enumerates the order lifecycle states that drive a Click-To-WhatsApp (CTWA) order conversion signal.
 *
 * <p>Each constant carries the two strings that the Click-To-WhatsApp third-party conversion telemetry
 * expects for an order: the conversion type ({@code order_created} for a freshly created order,
 * {@code order_updated} for any later state change) and the conversion subtype that classifies the
 * specific transition ({@code processing}, {@code shipped}, {@code completed}, {@code canceled}, or
 * {@code paid_change}).
 *
 * <p>The status-to-subtype mapping mirrors WhatsApp Web's order-status reducer: a created order, or any
 * status that is neither shipped, completed, delivered nor canceled, reports {@code processing}; shipped
 * reports {@code shipped}; complete and delivered both report {@code completed}; canceled reports
 * {@code canceled}. {@link #PAID_CHANGE} is the dedicated state for a payment-state toggle that did not
 * otherwise change the order status, and reports the {@code paid_change} subtype.
 */
public enum CtwaOrderStatus {
    /**
     * A newly created order; reports conversion type {@code order_created} and subtype {@code processing}.
     */
    CREATED("order_created", "processing"),

    /**
     * An order that has shipped; reports subtype {@code shipped}.
     */
    SHIPPED("order_updated", "shipped"),

    /**
     * An order that has completed; reports subtype {@code completed}.
     */
    COMPLETED("order_updated", "completed"),

    /**
     * An order that has been delivered; reports subtype {@code completed}, matching how WhatsApp Web
     * folds delivery into the completed transition.
     */
    DELIVERED("order_updated", "completed"),

    /**
     * An order that has been canceled; reports subtype {@code canceled}.
     */
    CANCELED("order_updated", "canceled"),

    /**
     * An order that is being processed; reports subtype {@code processing}.
     */
    PROCESSING("order_updated", "processing"),

    /**
     * An order awaiting confirmation; reports subtype {@code processing}.
     */
    PENDING("order_updated", "processing"),

    /**
     * An order that has been partially shipped; reports subtype {@code processing}.
     */
    PARTIALLY_SHIPPED("order_updated", "processing"),

    /**
     * An order being prepared for shipment; reports subtype {@code processing}.
     */
    PREPARING_TO_SHIP("order_updated", "processing"),

    /**
     * An order for which payment has been requested; reports subtype {@code processing}.
     */
    PAYMENT_REQUESTED("order_updated", "processing"),

    /**
     * A confirmed order; reports subtype {@code processing}.
     */
    CONFIRMED("order_updated", "processing"),

    /**
     * A delayed order; reports subtype {@code processing}.
     */
    DELAYED("order_updated", "processing"),

    /**
     * A failed order; reports subtype {@code processing}.
     */
    FAILED("order_updated", "processing"),

    /**
     * An order that is out for delivery; reports subtype {@code processing}.
     */
    OUT_FOR_DELIVERY("order_updated", "processing"),

    /**
     * A refunded order; reports subtype {@code processing}.
     */
    REFUNDED("order_updated", "processing"),

    /**
     * A payment-state toggle that did not change the order status; reports subtype {@code paid_change}.
     */
    PAID_CHANGE("order_updated", "paid_change");

    /**
     * The conversion type reported for this status, either {@code order_created} or {@code order_updated}.
     */
    private final String conversionType;

    /**
     * The conversion subtype reported for this status.
     */
    private final String subtype;

    /**
     * Constructs a status carrying its conversion type and subtype.
     *
     * @param conversionType the conversion type string
     * @param subtype        the conversion subtype string
     */
    CtwaOrderStatus(String conversionType, String subtype) {
        this.conversionType = conversionType;
        this.subtype = subtype;
    }

    /**
     * Returns the conversion type reported for this status.
     *
     * @return {@code order_created} for {@link #CREATED}, {@code order_updated} otherwise
     */
    public String conversionType() {
        return conversionType;
    }

    /**
     * Returns the conversion subtype reported for this status.
     *
     * @return the subtype string
     */
    public String subtype() {
        return subtype;
    }
}
