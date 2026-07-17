package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.github.auties00.cobalt.wire.graphql.whatsappWeb.business.CatalogProductInfoParser;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogPage;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogPageBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the fetch-catalog query built by
 * {@link BizCatalogManagementFetchCatalogFacebookGraphQlRequest} into a {@link BusinessCatalogPage}.
 *
 * <p>Reads the linked chain {@code xfb_whatsapp_catalog -> product_catalog} and projects the catalog
 * identity, the page of products, and the pagination cursors onto the Cobalt domain model.
 *
 * @see BizCatalogManagementFetchCatalogFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementFetchCatalogQuery")
public final class BizCatalogManagementFetchCatalogFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed catalog page.
     */
    private final BusinessCatalogPage catalog;

    /**
     * Constructs a response wrapping the parsed catalog page.
     *
     * <p>Reserved for the static parser.
     *
     * @param catalog the parsed catalog page, or {@code null} when the Meta graph endpoint omitted the field
     */
    private BizCatalogManagementFetchCatalogFacebookGraphQlResponse(BusinessCatalogPage catalog) {
        this.catalog = catalog;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked chain {@code xfb_whatsapp_catalog -> product_catalog} and projects the
     * catalog identity, products, and cursors onto a {@link BusinessCatalogPage}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the catalog projection is missing
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementFetchCatalog", exports = "fetchCatalog",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<BizCatalogManagementFetchCatalogFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var root = data.getJSONObject("xfb_whatsapp_catalog");
        if (root == null) {
            return Optional.empty();
        }
        var productCatalog = root.getJSONObject("product_catalog");
        if (productCatalog == null) {
            return Optional.empty();
        }
        String before = null;
        String after = null;
        var paging = productCatalog.getJSONObject("paging");
        if (paging != null) {
            before = paging.getString("before");
            after = paging.getString("after");
        }
        var products = CatalogProductInfoParser.parseProducts(productCatalog.getJSONArray("products"));
        var page = new BusinessCatalogPageBuilder()
                .catalogId(productCatalog.getString("catalog_id"))
                .catalogType(productCatalog.getString("catalog_type"))
                .catalogName(productCatalog.getString("catalog_name"))
                .products(products)
                .beforeCursor(before)
                .afterCursor(after)
                .build();
        return Optional.of(new BizCatalogManagementFetchCatalogFacebookGraphQlResponse(page));
    }

    /**
     * Returns the parsed catalog page.
     *
     * <p>The returned {@link BusinessCatalogPage} carries the catalog identity, one page of products,
     * and the pagination cursors.
     *
     * @return the parsed catalog page, never {@code null}
     */
    public BusinessCatalogPage catalog() {
        return catalog;
    }
}
