package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.github.auties00.cobalt.wire.graphql.whatsappWeb.business.CatalogProductParser;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalog;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the product-collections query built by
 * {@link QueryProductCollectionsWhatsAppGraphQlRequest}.
 *
 * <p>Carries the {@code xwa_product_catalog_get_collections.collections} array projected onto Cobalt
 * {@link BusinessCatalog} values, paired with the {@code paging.after} cursor needed to drive
 * subsequent pages, decoded from the unwrapped GraphQL {@code data} object the graph.whatsapp.com endpoint returns.
 *
 * @implNote This implementation drops the WA Web {@code status_info}, {@code can_appeal},
 * {@code reject_reason} and {@code commerce_url} fields because the Cobalt {@link BusinessCatalog}
 * model does not represent the collection review surface; only {@code id}, {@code name} and the nested
 * products are projected.
 *
 * @see QueryProductCollectionsWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebQueryProductCollectionsQuery")
public final class QueryProductCollectionsWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the business catalogs returned by this page.
     */
    private final List<BusinessCatalog> collections;

    /**
     * Holds the {@code paging.after} cursor, or the empty string when the graph.whatsapp.com endpoint reported no further
     * pages.
     */
    private final String afterCursor;

    /**
     * Constructs a parsed collections response.
     *
     * <p>Instances are produced by {@link #of(JSONObject)} after projecting the graph.whatsapp.com endpoint payload.
     *
     * @param collections the parsed business catalogs
     * @param afterCursor the {@code paging.after} cursor, or the empty string when the graph.whatsapp.com endpoint reported
     *                    no further pages
     */
    private QueryProductCollectionsWhatsAppGraphQlResponse(List<BusinessCatalog> collections, String afterCursor) {
        this.collections = collections;
        this.afterCursor = afterCursor;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xwa_product_catalog_get_collections} directly off {@code data},
     * projecting each collection through {@link #parseCollections(JSONArray)}.
     *
     * @implNote This implementation matches WA Web's empty-result fallback: a reply where
     * {@code xwa_product_catalog_get_collections} is {@code null} returns an empty collections list
     * with an empty cursor rather than {@link Optional#empty()}. Only a {@code null} {@code data}
     * object yields {@link Optional#empty()}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryProductCollections", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<QueryProductCollectionsWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var getResult = data.getJSONObject("xwa_product_catalog_get_collections");
        if (getResult == null) {
            return Optional.of(new QueryProductCollectionsWhatsAppGraphQlResponse(List.of(), ""));
        }
        var paging = getResult.getJSONObject("paging");
        var cursor = paging == null ? "" : Optional.ofNullable(paging.getString("after")).orElse("");
        var collectionsArray = getResult.getJSONArray("collections");
        var collections = parseCollections(collectionsArray);
        return Optional.of(new QueryProductCollectionsWhatsAppGraphQlResponse(collections, cursor));
    }

    /**
     * Returns the collections carried by this page of the query.
     *
     * <p>Each {@link BusinessCatalog} pairs the collection's id and name with the products nested
     * inside it, projected through {@link CatalogProductParser#parseProducts(JSONArray)}.
     *
     * @return an unmodifiable list of {@link BusinessCatalog} values, never {@code null}
     */
    public List<BusinessCatalog> collections() {
        return collections;
    }

    /**
     * Returns the {@code paging.after} cursor usable to request the next page of collections.
     *
     * <p>Pass the returned value as the {@code after} argument of the next
     * {@link QueryProductCollectionsWhatsAppGraphQlRequest}. An empty {@link Optional} means the graph.whatsapp.com endpoint did not
     * advertise a continuation cursor, so callers should stop pagination.
     *
     * @return an {@link Optional} carrying the cursor when the graph.whatsapp.com endpoint returned a non-empty value, or
     *         empty otherwise
     */
    public Optional<String> afterCursor() {
        return afterCursor == null || afterCursor.isEmpty() ? Optional.empty() : Optional.of(afterCursor);
    }

    /**
     * Parses an array of GraphQL collection objects into a list of {@link BusinessCatalog} values.
     *
     * <p>Invoked once per response to project the {@code collections} array onto the Cobalt domain
     * model.
     *
     * @implNote This implementation skips entries that {@link #parseCollection(JSONObject)} rejects
     * (currently only {@code null} entries) and wraps the result in
     * {@link List#copyOf(java.util.Collection)} so callers receive an unmodifiable list.
     *
     * @param array the GraphQL collections array, possibly {@code null}
     * @return the parsed collections, never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryProductCollections", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static List<BusinessCatalog> parseCollections(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        var out = new ArrayList<BusinessCatalog>(array.size());
        for (var i = 0; i < array.size(); i++) {
            parseCollection(array.getJSONObject(i)).ifPresent(out::add);
        }
        return List.copyOf(out);
    }

    /**
     * Parses a single GraphQL collection object into a {@link BusinessCatalog}.
     *
     * <p>Invoked by {@link #parseCollections(JSONArray)} once per array element.
     *
     * @implNote This implementation defaults missing {@code id} and {@code name} fields to the empty
     * string, mirroring the WA Web {@code n || ""} and {@code r || ""} fall-throughs.
     *
     * @param obj the GraphQL collection object, possibly {@code null}
     * @return the parsed collection, or empty when {@code obj} is {@code null}
     */
    private static Optional<BusinessCatalog> parseCollection(JSONObject obj) {
        if (obj == null) {
            return Optional.empty();
        }
        var id = Optional.ofNullable(obj.getString("id")).orElse("");
        var name = Optional.ofNullable(obj.getString("name")).orElse("");
        var products = CatalogProductParser.parseProducts(obj.getJSONArray("products"));
        var collection = new BusinessCatalogBuilder()
                .id(id)
                .name(name)
                .products(products)
                .build();
        return Optional.of(collection);
    }
}
