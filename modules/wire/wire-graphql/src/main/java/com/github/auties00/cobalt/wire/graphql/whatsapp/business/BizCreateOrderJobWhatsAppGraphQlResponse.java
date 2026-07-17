package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.order.BusinessOrder;
import com.github.auties00.cobalt.wire.linked.business.order.BusinessOrderBuilder;

import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the create-order mutation built by
 * {@link BizCreateOrderJobWhatsAppGraphQlRequest} into a {@link BusinessOrder}.
 *
 * <p>Reads the placed order returned under {@code xwa_checkout_place_order} and projects its
 * {@code order} child, including the linked {@code price} breakdown, onto the Cobalt domain model.
 * WhatsApp Web treats a missing {@code order} as a server failure.
 *
 * @see BizCreateOrderJobWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizCreateOrderJobMutation")
public final class BizCreateOrderJobWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the parsed placed order.
     */
    private final BusinessOrder order;

    /**
     * Constructs a response wrapping the parsed placed order.
     *
     * <p>Reserved for the static parser.
     *
     * @param order the parsed placed order, or {@code null} when the graph.whatsapp.com endpoint omitted the field
     */
    private BizCreateOrderJobWhatsAppGraphQlResponse(BusinessOrder order) {
        this.order = order;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xwa_checkout_place_order} and its {@code order} child, projects
     * the {@code price} breakdown's currency, subtotal, and total onto a {@link BusinessOrder}; the
     * returned {@link Optional} is empty when {@code data} or the order projection is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the order projection is missing
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCreateOrderJob", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<BizCreateOrderJobWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var root = data.getJSONObject("xwa_checkout_place_order");
        if (root == null) {
            return Optional.empty();
        }
        var orderObject = root.getJSONObject("order");
        if (orderObject == null) {
            return Optional.empty();
        }
        String currency = null;
        Long subtotal = null;
        Long total = null;
        var price = orderObject.getJSONObject("price");
        if (price != null) {
            currency = price.getString("currency");
            subtotal = price.getLong("subtotal_amount");
            total = price.getLong("total_amount");
        }
        var order = new BusinessOrderBuilder()
                .referenceId(orderObject.getString("order_id"))
                .token(orderObject.getString("token"))
                .currency(currency)
                .subtotal(subtotal)
                .total(total)
                .build();
        return Optional.of(new BizCreateOrderJobWhatsAppGraphQlResponse(order));
    }

    /**
     * Returns the parsed placed order.
     *
     * <p>The returned {@link BusinessOrder} carries the order's reference id, access token, currency
     * and the subtotal and total amounts; the line items are not present in the placed-order
     * projection.
     *
     * @return the parsed placed order, never {@code null}
     */
    public BusinessOrder order() {
        return order;
    }
}
