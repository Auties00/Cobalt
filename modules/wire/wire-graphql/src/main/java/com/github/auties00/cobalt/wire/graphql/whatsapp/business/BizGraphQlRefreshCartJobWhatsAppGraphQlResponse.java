package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.cart.BusinessCartPrice;
import com.github.auties00.cobalt.wire.linked.business.cart.BusinessCartPriceBuilder;
import com.github.auties00.cobalt.wire.linked.business.cart.BusinessCartProduct;
import com.github.auties00.cobalt.wire.linked.business.cart.BusinessCartProductBuilder;
import com.github.auties00.cobalt.wire.linked.business.cart.BusinessCartProductImageBuilder;
import com.github.auties00.cobalt.wire.linked.business.cart.BusinessCartProductMedia;
import com.github.auties00.cobalt.wire.linked.business.cart.BusinessCartProductMediaBuilder;
import com.github.auties00.cobalt.wire.linked.business.cart.BusinessCartProductSalePrice;
import com.github.auties00.cobalt.wire.linked.business.cart.BusinessCartProductSalePriceBuilder;
import com.github.auties00.cobalt.wire.linked.business.cart.BusinessRefreshedCart;
import com.github.auties00.cobalt.wire.linked.business.cart.BusinessRefreshedCartBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the refresh-cart query built by
 * {@link BizGraphQlRefreshCartJobWhatsAppGraphQlRequest} into a {@link BusinessRefreshedCart}.
 *
 * <p>Reads the linked {@code xwa_checkout_refresh_cart} root and projects its {@code cart} child onto
 * the Cobalt domain model: each refreshed product's identity, price, currency, thumbnail, sale window,
 * and per-line cap, plus the cart-level price breakdown. The graph.whatsapp.com endpoint's richer per-product detail
 * (variant matrices, compliance, moderation, full media galleries) is not surfaced by this projection.
 *
 * @see BizGraphQlRefreshCartJobWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizGraphQLRefreshCartJobQuery")
public final class BizGraphQlRefreshCartJobWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Default per-line cart-item cap WhatsApp applies when the graph.whatsapp.com endpoint omits a per-product override.
     */
    private static final int DEFAULT_MAX_AVAILABLE = 99;

    /**
     * Holds the parsed refreshed cart.
     */
    private final BusinessRefreshedCart cart;

    /**
     * Constructs a response wrapping the parsed refreshed cart.
     *
     * <p>Reserved for the static parser.
     *
     * @param cart the parsed refreshed cart, or {@code null} when the graph.whatsapp.com endpoint omitted the field
     */
    private BizGraphQlRefreshCartJobWhatsAppGraphQlResponse(BusinessRefreshedCart cart) {
        this.cart = cart;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xwa_checkout_refresh_cart} and projects its {@code cart} child
     * onto a {@link BusinessRefreshedCart}; the returned {@link Optional} is empty when {@code data} or
     * the cart projection is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the cart projection is missing
     */
    @WhatsAppWebExport(moduleName = "WAWebBizGraphQLRefreshCartJob", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<BizGraphQlRefreshCartJobWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var root = data.getJSONObject("xwa_checkout_refresh_cart");
        if (root == null) {
            return Optional.empty();
        }
        var cartObject = root.getJSONObject("cart");
        if (cartObject == null) {
            return Optional.empty();
        }
        var cart = new BusinessRefreshedCartBuilder()
                .price(parsePrice(cartObject.getJSONObject("price_details")))
                .products(parseProducts(cartObject.getJSONArray("products")))
                .build();
        return Optional.of(new BizGraphQlRefreshCartJobWhatsAppGraphQlResponse(cart));
    }

    /**
     * Projects the {@code price_details} object onto a {@link BusinessCartPrice}.
     *
     * @param obj the price-details object to parse, or {@code null}
     * @return the cart-level price summary, never {@code null}
     */
    private static BusinessCartPrice parsePrice(JSONObject obj) {
        if (obj == null) {
            return new BusinessCartPriceBuilder().build();
        }
        return new BusinessCartPriceBuilder()
                .subtotal(obj.getString("subtotal_amount"))
                .total(obj.getString("total_amount"))
                .currency(obj.getString("currency"))
                .priceStatus(obj.getString("price_status"))
                .build();
    }

    /**
     * Projects a {@code products} JSON array onto a list of {@link BusinessCartProduct}.
     *
     * @param array the JSON array to parse, or {@code null}
     * @return the projected products, empty when {@code array} is {@code null}
     */
    private static List<BusinessCartProduct> parseProducts(JSONArray array) {
        if (array == null) {
            return List.of();
        }
        var result = new ArrayList<BusinessCartProduct>(array.size());
        for (var i = 0; i < array.size(); i++) {
            var entry = array.getJSONObject(i);
            if (entry == null) {
                continue;
            }
            var id = entry.getString("id");
            if (id == null) {
                continue;
            }
            result.add(new BusinessCartProductBuilder()
                    .id(id)
                    .name(entry.getString("name"))
                    .price(entry.getString("price"))
                    .currency(entry.getString("currency"))
                    .media(parseMedia(entry.getJSONObject("media")))
                    .maxAvailable(parseMaxAvailable(entry.getString("max_available")))
                    .salePrice(parseSalePrice(entry.getJSONObject("sale_price")))
                    .status(entry.getString("status"))
                    .build());
        }
        return result;
    }

    /**
     * Projects the first gallery image of a {@code media} object onto a
     * {@link BusinessCartProductMedia} thumbnail wrapper.
     *
     * @param obj the media object to parse, or {@code null}
     * @return the thumbnail wrapper, or {@code null} when no image is present
     */
    private static BusinessCartProductMedia parseMedia(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        var images = obj.getJSONArray("images");
        if (images == null || images.isEmpty()) {
            return null;
        }
        var image = images.getJSONObject(0);
        if (image == null) {
            return null;
        }
        return new BusinessCartProductMediaBuilder()
                .image(new BusinessCartProductImageBuilder()
                        .id(image.getString("id"))
                        .requestImageUrl(image.getString("request_image_url"))
                        .build())
                .build();
    }

    /**
     * Projects a {@code sale_price} object onto a {@link BusinessCartProductSalePrice}.
     *
     * @param obj the sale-price object to parse, or {@code null}
     * @return the sale-price block, or {@code null} when no priced sale is present
     */
    private static BusinessCartProductSalePrice parseSalePrice(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        var price = obj.getString("price");
        if (price == null) {
            return null;
        }
        return new BusinessCartProductSalePriceBuilder()
                .price(price)
                .startDate(obj.getString("start_date"))
                .endDate(obj.getString("end_date"))
                .build();
    }

    /**
     * Parses the graph.whatsapp.com endpoint's stringified {@code max_available} into the per-line cap.
     *
     * @param raw the raw {@code max_available} string, or {@code null}
     * @return the parsed cap, or {@value #DEFAULT_MAX_AVAILABLE} when the value is missing or not a
     *         valid integer
     */
    private static int parseMaxAvailable(String raw) {
        if (raw == null) {
            return DEFAULT_MAX_AVAILABLE;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            return DEFAULT_MAX_AVAILABLE;
        }
    }

    /**
     * Returns the parsed refreshed cart.
     *
     * <p>The returned {@link BusinessRefreshedCart} carries the cart-level price summary and the
     * refreshed product lines.
     *
     * @return the parsed refreshed cart, never {@code null}
     */
    public BusinessRefreshedCart cart() {
        return cart;
    }
}
