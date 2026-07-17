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
 * Parses the Facebook GraphQL response of the add-product mutation built by
 * {@link BizCatalogManagementAddProductFacebookGraphQlRequest} into a {@link BusinessProduct}.
 *
 * <p>Reads the linked chain {@code xfb_whatsapp_catalog_add_product -> product} and projects the
 * created product onto the Cobalt domain model.
 *
 * @see BizCatalogManagementAddProductFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementAddProductMutation")
public final class BizCatalogManagementAddProductFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed created product.
     */
    private final BusinessProduct product;

    /**
     * Constructs a response wrapping the parsed created product.
     *
     * <p>Reserved for the static parser.
     *
     * @param product the parsed created product, or {@code null} when the Meta graph endpoint omitted the field
     */
    private BizCatalogManagementAddProductFacebookGraphQlResponse(BusinessProduct product) {
        this.product = product;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked chain {@code xfb_whatsapp_catalog_add_product -> product} and projects the
     * created product onto a {@link BusinessProduct}; the returned {@link Optional} is empty when
     * {@code data} or the product projection is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null} or the product is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementAddProductMutation", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<BizCatalogManagementAddProductFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var root = data.getJSONObject("xfb_whatsapp_catalog_add_product");
        if (root == null) {
            return Optional.empty();
        }
        var product = CatalogProductInfoParser.parseProduct(root.getJSONObject("product")).orElse(null);
        if (product == null) {
            return Optional.empty();
        }
        return Optional.of(new BizCatalogManagementAddProductFacebookGraphQlResponse(product));
    }

    /**
     * Returns the parsed created product.
     *
     * @return the parsed {@link BusinessProduct}, never {@code null}
     */
    public BusinessProduct product() {
        return product;
    }
}
