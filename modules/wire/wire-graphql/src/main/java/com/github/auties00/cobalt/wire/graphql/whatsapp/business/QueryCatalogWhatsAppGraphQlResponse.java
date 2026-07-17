package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.github.auties00.cobalt.wire.graphql.whatsappWeb.business.CatalogProductParser;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogEntry;

import java.util.List;
import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the catalog-products query built by {@link QueryCatalogWhatsAppGraphQlRequest}.
 *
 * <p>Carries the {@code xwa_product_catalog_get_product_catalog.product_catalog} projection paired
 * with the {@code paging.after} cursor needed to drive subsequent pages, decoded from the unwrapped
 * GraphQL {@code data} object the graph.whatsapp.com endpoint returns. Each product is projected onto the Cobalt
 * {@link BusinessCatalogEntry} domain model through {@link CatalogProductParser}.
 *
 * @implNote This implementation tolerates a graph.whatsapp.com endpoint reply that omits either
 * {@code xwa_product_catalog_get_product_catalog} or its {@code product_catalog} child by returning an
 * empty result, mirroring WA Web's empty {@code data: []} fallback. A {@code null} {@code data} object
 * collapses to {@link Optional#empty()}.
 *
 * @see QueryCatalogWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebQueryCatalogQuery")
public final class QueryCatalogWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the catalog entries returned by this page.
     */
    private final List<BusinessCatalogEntry> products;

    /**
     * Holds the {@code paging.after} cursor, or the empty string when the graph.whatsapp.com endpoint reported no further
     * pages.
     */
    private final String afterCursor;

    /**
     * Constructs a parsed catalog response.
     *
     * <p>Instances are produced by the {@link #of(JSONObject)} factory after projecting the graph.whatsapp.com endpoint
     * payload.
     *
     * @param products    the catalog entries returned by this page
     * @param afterCursor the {@code paging.after} cursor, or the empty string when the graph.whatsapp.com endpoint reported
     *                    no further pages
     */
    private QueryCatalogWhatsAppGraphQlResponse(List<BusinessCatalogEntry> products, String afterCursor) {
        this.products = products;
        this.afterCursor = afterCursor;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked chain {@code xwa_product_catalog_get_product_catalog -> product_catalog}
     * directly off {@code data}, projecting the contained products through
     * {@link CatalogProductParser#parseProducts(com.alibaba.fastjson2.JSONArray)}.
     *
     * @implNote This implementation matches WA Web's empty-result fallback: a reply where
     * {@code xwa_product_catalog_get_product_catalog} or {@code product_catalog} is {@code null}
     * returns a response with an empty product list and an empty cursor rather than
     * {@link Optional#empty()}. Only a {@code null} {@code data} object yields {@link Optional#empty()}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryCatalog", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<QueryCatalogWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var getResult = data.getJSONObject("xwa_product_catalog_get_product_catalog");
        if (getResult == null) {
            return Optional.of(new QueryCatalogWhatsAppGraphQlResponse(List.of(), ""));
        }
        var catalog = getResult.getJSONObject("product_catalog");
        if (catalog == null) {
            return Optional.of(new QueryCatalogWhatsAppGraphQlResponse(List.of(), ""));
        }
        var paging = catalog.getJSONObject("paging");
        var cursor = paging == null ? "" : Optional.ofNullable(paging.getString("after")).orElse("");
        var products = CatalogProductParser.parseProducts(catalog.getJSONArray("products"));
        return Optional.of(new QueryCatalogWhatsAppGraphQlResponse(products, cursor));
    }

    /**
     * Returns the products carried by this page of the catalog.
     *
     * <p>Each entry is the projection produced by
     * {@link CatalogProductParser#parseProducts(com.alibaba.fastjson2.JSONArray)}.
     *
     * @return an unmodifiable list of {@link BusinessCatalogEntry} values, never {@code null}
     */
    public List<BusinessCatalogEntry> products() {
        return products;
    }

    /**
     * Returns the {@code paging.after} cursor usable to request the next page of products.
     *
     * <p>Pass the returned value as the {@code after} argument of the next
     * {@link QueryCatalogWhatsAppGraphQlRequest}. An empty {@link Optional} means the graph.whatsapp.com endpoint did not advertise a
     * continuation cursor, so callers should stop pagination.
     *
     * @return an {@link Optional} carrying the cursor when the graph.whatsapp.com endpoint returned a non-empty value, or
     *         empty otherwise
     */
    public Optional<String> afterCursor() {
        return afterCursor == null || afterCursor.isEmpty() ? Optional.empty() : Optional.of(afterCursor);
    }
}
