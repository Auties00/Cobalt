package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiProductInfo;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiProductInfoBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the update-product-info mutation built by
 * {@link BizAiProductInfoMutationUpdateFacebookGraphQlRequest} into a {@link BusinessAiProductInfo}.
 *
 * <p>The Meta graph endpoint confirms the update with a single {@code success} scalar under
 * {@code xfb_maiba_update_product_info_knowledge} without echoing the product's contents, so the
 * projected {@link BusinessAiProductInfo} is present only when the Meta graph endpoint reported success and carries
 * no fields beyond representing the updated entry.
 *
 * @see BizAiProductInfoMutationUpdateFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiProductInfoMutationUpdateMutation")
public final class BizAiProductInfoMutationUpdateFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected updated product entry, or {@code null} when the Meta graph endpoint reported failure or
     * omitted the field.
     */
    private final BusinessAiProductInfo productInfo;

    /**
     * Constructs a response wrapping the projected updated product entry.
     *
     * <p>Reserved for the static parser.
     *
     * @param productInfo the projected updated product entry, or {@code null} when the Meta graph endpoint reported
     *                    failure or omitted the field
     */
    private BizAiProductInfoMutationUpdateFacebookGraphQlResponse(BusinessAiProductInfo productInfo) {
        this.productInfo = productInfo;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * update outcome onto a {@link BusinessAiProductInfo}.
     *
     * <p>Reads the linked root {@code xfb_maiba_update_product_info_knowledge}; the returned
     * {@link Optional} is empty when {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAiProductInfoMutationUpdateFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("xfb_maiba_update_product_info_knowledge");
        var success = node != null && Boolean.TRUE.equals(node.getBoolean("success"));
        var productInfo = success ? new BusinessAiProductInfoBuilder().build() : null;
        return Optional.of(new BizAiProductInfoMutationUpdateFacebookGraphQlResponse(productInfo));
    }

    /**
     * Returns the projected updated product entry.
     *
     * @return the projected {@link BusinessAiProductInfo}, or empty when the Meta graph endpoint reported failure or
     *         omitted the field
     */
    public Optional<BusinessAiProductInfo> productInfo() {
        return Optional.ofNullable(productInfo);
    }
}
