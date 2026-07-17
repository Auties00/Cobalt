package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiMutationResult;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiMutationResultBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the example-response-update mutation built by
 * {@link BizAiExampleResponseUpdateFacebookGraphQlRequest} into a {@link BusinessAiMutationResult}.
 *
 * <p>Projects the linked {@code xfb_meta_ai_biz_agent_wa_update_knowledge} field onto the shared
 * mutation-result shape. The overall {@code success} flag becomes
 * {@link BusinessAiMutationResult#success()}; any website-backed knowledge that failed validation has
 * its URL collected into {@link BusinessAiMutationResult#affectedIds()} and the first reported
 * validation error code becomes {@link BusinessAiMutationResult#errorMessage()}.
 *
 * @see BizAiExampleResponseUpdateFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiExampleResponseUpdateMutation")
public final class BizAiExampleResponseUpdateFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected mutation result, or {@code null} when the Meta graph endpoint omitted the field.
     */
    private final BusinessAiMutationResult result;

    /**
     * Constructs a response wrapping the projected mutation result.
     *
     * <p>Reserved for the static parser.
     *
     * @param result the projected mutation result, or {@code null} when the Meta graph endpoint omitted the field
     */
    private BizAiExampleResponseUpdateFacebookGraphQlResponse(BusinessAiMutationResult result) {
        this.result = result;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code success} flag and the per-website validation statuses onto a
     * {@link BusinessAiMutationResult}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiExampleResponseUpdateMutation", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<BizAiExampleResponseUpdateFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("xfb_meta_ai_biz_agent_wa_update_knowledge");
        if (node == null) {
            return Optional.of(new BizAiExampleResponseUpdateFacebookGraphQlResponse(null));
        }

        var invalidUrls = new ArrayList<String>();
        var firstError = collectInvalidWebsites(node.getJSONArray("website_statuses"), invalidUrls);
        var result = new BusinessAiMutationResultBuilder()
                .success(Boolean.TRUE.equals(node.getBoolean("success")))
                .affectedIds(invalidUrls)
                .errorMessage(firstError)
                .build();
        return Optional.of(new BizAiExampleResponseUpdateFacebookGraphQlResponse(result));
    }

    /**
     * Collects the URLs of the websites that failed validation and returns the first reported
     * validation error code.
     *
     * @param arr         the {@code website_statuses} array, possibly {@code null}
     * @param invalidUrls the mutable list the invalid website URLs are appended to
     * @return the first reported validation error code, or {@code null} when none was reported
     */
    private static String collectInvalidWebsites(JSONArray arr, List<String> invalidUrls) {
        if (arr == null) {
            return null;
        }

        String firstError = null;
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }
            var valid = Boolean.TRUE.equals(obj.getBoolean("is_valid"));
            if (!valid) {
                var url = obj.getString("website_url");
                if (url != null) {
                    invalidUrls.add(url);
                }
                var errorCode = obj.getString("error_code");
                if (firstError == null && errorCode != null) {
                    firstError = errorCode;
                }
            }
        }
        return firstError;
    }

    /**
     * Returns the projected mutation result.
     *
     * @return the projected {@link BusinessAiMutationResult}, or empty when the Meta graph endpoint omitted the field
     */
    public Optional<BusinessAiMutationResult> result() {
        return Optional.ofNullable(result);
    }
}
