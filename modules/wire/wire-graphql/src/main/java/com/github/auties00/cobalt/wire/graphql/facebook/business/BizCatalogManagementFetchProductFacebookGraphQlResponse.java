package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.github.auties00.cobalt.wire.graphql.whatsappWeb.business.CatalogProductInfoParser;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProduct;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the fetch-product query built by
 * {@link BizCatalogManagementFetchProductFacebookGraphQlRequest} into a {@link BusinessProduct}.
 *
 * <p>Reads the linked chain {@code xfb_whatsapp_catalog_product -> product_catalog -> product} and
 * projects the single product onto the Cobalt domain model.
 *
 * @see BizCatalogManagementFetchProductFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementFetchProductQuery")
public final class BizCatalogManagementFetchProductFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed product.
     */
    private final BusinessProduct product;

    /**
     * Constructs a response wrapping the parsed product.
     *
     * <p>Reserved for the static parser.
     *
     * @param product the parsed product, or {@code null} when the Meta graph endpoint omitted the field
     */
    private BizCatalogManagementFetchProductFacebookGraphQlResponse(BusinessProduct product) {
        this.product = product;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked chain {@code xfb_whatsapp_catalog_product -> product_catalog -> product} and
     * projects the single product onto a {@link BusinessProduct}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the product projection is missing
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementFetchProduct", exports = "fetchProduct",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<BizCatalogManagementFetchProductFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var root = data.getJSONObject("xfb_whatsapp_catalog_product");
        if (root == null) {
            return Optional.empty();
        }
        var productCatalog = root.getJSONObject("product_catalog");
        if (productCatalog == null) {
            return Optional.empty();
        }
        var product = CatalogProductInfoParser.parseProduct(productCatalog.getJSONObject("product")).orElse(null);
        if (product == null) {
            return Optional.empty();
        }
        return Optional.of(new BizCatalogManagementFetchProductFacebookGraphQlResponse(product));
    }

    /**
     * Returns the parsed product.
     *
     * @return the parsed {@link BusinessProduct}, never {@code null}
     */
    public BusinessProduct product() {
        return product;
    }
}
