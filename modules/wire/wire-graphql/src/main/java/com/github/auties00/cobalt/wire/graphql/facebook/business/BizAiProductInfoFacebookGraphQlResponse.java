package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiProductInfo;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiProductInfoBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the create-product-info mutation built by
 * {@link BizAiProductInfoFacebookGraphQlRequest} into a {@link BusinessAiProductInfo}.
 *
 * <p>Projects the linked {@code xfb_maiba_create_product_info_knowledge} field: when the Meta graph endpoint reports
 * success and echoes the created product entries under {@code product_items}, the first echoed entry is
 * projected onto a {@link BusinessAiProductInfo} carrying the product's catalog id, title, description,
 * and price. The projection is absent when the Meta graph endpoint reported failure or echoed no entry.
 *
 * @see BizAiProductInfoFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiProductInfoMutation")
public final class BizAiProductInfoFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected created product entry, or {@code null} when the Meta graph endpoint reported
     * failure or echoed no entry.
     */
    private final BusinessAiProductInfo productInfo;

    /**
     * Constructs a response wrapping the projected created product entry.
     *
     * <p>Reserved for the static parser.
     *
     * @param productInfo the projected created product entry, or {@code null} when the Meta graph endpoint reported
     *                    failure or echoed no entry
     */
    private BizAiProductInfoFacebookGraphQlResponse(BusinessAiProductInfo productInfo) {
        this.productInfo = productInfo;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the first
     * echoed product entry onto a {@link BusinessAiProductInfo}.
     *
     * <p>Reads the linked root {@code xfb_maiba_create_product_info_knowledge}; the returned
     * {@link Optional} is empty when {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAiProductInfoFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("xfb_maiba_create_product_info_knowledge");
        if (node == null || !Boolean.TRUE.equals(node.getBoolean("success"))) {
            return Optional.of(new BizAiProductInfoFacebookGraphQlResponse(null));
        }

        var productInfo = parseFirstProduct(node.getJSONArray("product_items"));
        return Optional.of(new BizAiProductInfoFacebookGraphQlResponse(productInfo));
    }

    /**
     * Projects the first {@code product_items} entry onto a {@link BusinessAiProductInfo}.
     *
     * @param arr the {@code product_items} array, possibly {@code null}
     * @return the projected product entry, or {@code null} when the array is absent or empty
     */
    private static BusinessAiProductInfo parseFirstProduct(JSONArray arr) {
        if (arr == null || arr.isEmpty()) {
            return null;
        }

        var obj = arr.getJSONObject(0);
        if (obj == null) {
            return null;
        }
        return new BusinessAiProductInfoBuilder()
                .productId(obj.getString("product_id"))
                .title(obj.getString("title"))
                .description(obj.getString("description"))
                .price(obj.getString("price"))
                .build();
    }

    /**
     * Returns the projected created product entry.
     *
     * @return the projected {@link BusinessAiProductInfo}, or empty when the Meta graph endpoint reported failure or
     *         echoed no entry
     */
    public Optional<BusinessAiProductInfo> productInfo() {
        return Optional.ofNullable(productInfo);
    }
}
