package com.github.auties00.cobalt.wire.graphql.whatsappWeb.business;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogEntry;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogEntryBuilder;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessItemAvailability;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessReviewStatus;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses GraphQL product objects into Cobalt {@link BusinessCatalogEntry} values.
 *
 * <p>This stateless utility centralises the product projection shared by
 * {@link QueryCatalogWhatsAppGraphQlResponse} and {@link QueryProductCollectionsWhatsAppGraphQlResponse},
 * so the two decoders read identical fields without duplicating the defensive
 * null handling.
 *
 * @implNote This implementation reads only the fields {@code id},
 * {@code retailer_id}, {@code name}, {@code description}, {@code url},
 * {@code currency}, {@code price}, {@code is_hidden},
 * {@code product_availability}, {@code status_info.status} and the first
 * image's {@code original_image_url}. The remaining WA Web business surface
 * (compliance info, variant info, sale-price metadata, video media, signed
 * shimmed URLs and {@code is_sanctioned}) is never used at the Cobalt layer
 * and is silently ignored.
 */
@WhatsAppWebModule(moduleName = "WAWebBizParseProductGraphql")
@WhatsAppWebModule(moduleName = "WAWebBizParseProductGraphql_product.graphql")
public final class CatalogProductParser {
    /**
     * Prevents instantiation of this stateless utility class.
     *
     * <p>The class is a namespace for the {@code static} parsing helpers and
     * holds no per-instance state.
     *
     * @throws AssertionError always
     */
    private CatalogProductParser() {
        throw new AssertionError("No CatalogProductParser instances for you!");
    }

    /**
     * Parses an array of GraphQL product objects into a list of
     * {@link BusinessCatalogEntry} values.
     *
     * <p>Projects the {@code product_catalog.products} and
     * {@code collections[i].products} arrays returned by the catalog and
     * product-collections GraphQL queries, as surfaced through
     * {@link QueryCatalogWhatsAppGraphQlResponse} and
     * {@link QueryProductCollectionsWhatsAppGraphQlResponse}.
     *
     * @implNote This implementation discards entries that
     * {@link #parseProduct(JSONObject)} cannot project (currently only
     * {@code null} entries) and wraps the result in
     * {@link List#copyOf(java.util.Collection)} so callers receive an
     * unmodifiable list.
     *
     * @param array the GraphQL products array, possibly {@code null}
     * @return the parsed entries, never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebBizParseProductGraphql", exports = "parseProductGraphQL",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static List<BusinessCatalogEntry> parseProducts(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        var out = new ArrayList<BusinessCatalogEntry>(array.size());
        for (var i = 0; i < array.size(); i++) {
            parseProduct(array.getJSONObject(i)).ifPresent(out::add);
        }
        return List.copyOf(out);
    }

    /**
     * Parses a single GraphQL product object into a
     * {@link BusinessCatalogEntry}.
     *
     * <p>Invoked by {@link #parseProducts(JSONArray)} once per array element.
     *
     * @implNote This implementation tolerates malformed scalar fields: a
     * {@code url} or image {@code original_image_url} that does not parse as a
     * {@link URI} leaves the corresponding field {@code null}, and a
     * {@code price} string that is not a decimal long leaves the field at
     * {@code 0}. The {@code is_hidden} field is encoded by WA Web as the enum
     * literal {@code "ISHIDDEN_TRUE"} or {@code "ISHIDDEN_FALSE"}; any other
     * value is treated as not hidden.
     *
     * @param obj the GraphQL product object, possibly {@code null}
     * @return the parsed entry, or empty when {@code obj} is {@code null}
     */
    private static Optional<BusinessCatalogEntry> parseProduct(JSONObject obj) {
        if (obj == null) {
            return Optional.empty();
        }
        var id = obj.getString("id");
        var retailerId = obj.getString("retailer_id");
        var name = obj.getString("name");
        var description = obj.getString("description");
        var urlString = obj.getString("url");
        URI url = null;
        if (urlString != null && !urlString.isEmpty()) {
            try {
                url = URI.create(urlString);
            } catch (IllegalArgumentException _) {
            }
        }
        var currency = obj.getString("currency");
        var price = 0L;
        var priceString = obj.getString("price");
        if (priceString != null && !priceString.isEmpty()) {
            try {
                price = Long.parseLong(priceString);
            } catch (NumberFormatException _) {
            }
        }
        var hidden = "ISHIDDEN_TRUE".equals(obj.getString("is_hidden"));
        var availability = parseAvailability(obj.getString("product_availability"));
        BusinessReviewStatus reviewStatus = null;
        var statusInfo = obj.getJSONObject("status_info");
        if (statusInfo != null) {
            var status = statusInfo.getString("status");
            if (status != null) {
                reviewStatus = BusinessReviewStatus.ofName(status).orElse(null);
            }
        }
        URI encryptedImage = null;
        var media = obj.getJSONObject("media");
        if (media != null) {
            var images = media.getJSONArray("images");
            if (images != null && !images.isEmpty()) {
                var first = images.getJSONObject(0);
                if (first != null) {
                    var originalUrl = first.getString("original_image_url");
                    if (originalUrl != null && !originalUrl.isEmpty()) {
                        try {
                            encryptedImage = URI.create(originalUrl);
                        } catch (IllegalArgumentException _) {
                        }
                    }
                }
            }
        }
        var entry = new BusinessCatalogEntryBuilder()
                .id(id)
                .encryptedImage(encryptedImage)
                .reviewStatus(reviewStatus)
                .availability(availability)
                .name(name)
                .sellerId(retailerId)
                .uri(url)
                .description(description)
                .price(price)
                .currency(currency)
                .hidden(hidden)
                .build();
        return Optional.of(entry);
    }

    /**
     * Maps the WA Web {@code product_availability} enum string to the Cobalt
     * {@link BusinessItemAvailability} constant.
     *
     * <p>Invoked by {@link #parseProduct(JSONObject)} to project the GraphQL
     * scalar onto the Cobalt domain enum.
     *
     * @implNote This implementation strips the WA Web {@code PRODUCTAVAILABILITY_}
     * prefix and lower-cases the remainder with underscores rewritten as
     * spaces, matching the pretty-printed form expected by
     * {@link BusinessItemAvailability#ofName(String)} (for example
     * {@code "in stock"}). The WA Web source enumerates {@code IN_STOCK},
     * {@code OUT_OF_STOCK}, {@code AVAILABLE_FOR_ANOTHER_POSTCODE} and
     * {@code UNKNOWN}; anything outside these collapses to {@code null} via
     * {@link Optional#orElse(Object)}.
     *
     * @param raw the raw enum string, may be {@code null}
     * @return the matched constant, or {@code null} when the input is absent
     *         or unknown
     */
    @WhatsAppWebExport(moduleName = "WAWebProductTypes.flow", exports = "ProductAvailability",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static BusinessItemAvailability parseAvailability(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        var stripped = raw.startsWith("PRODUCTAVAILABILITY_")
                ? raw.substring("PRODUCTAVAILABILITY_".length())
                : raw;
        var pretty = stripped.toLowerCase().replace('_', ' ');
        return BusinessItemAvailability.ofName(pretty).orElse(null);
    }
}
