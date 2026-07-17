package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiMutationResult;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiMutationResultBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the delete-website-source mutation built by
 * {@link BizAiKnowledgeSourceDeleteMutationWebsiteFacebookGraphQlRequest} into a {@link BusinessAiMutationResult}.
 *
 * <p>Projects the linked {@code maiba_trigger_website_deletion} field, whose single {@code success}
 * scalar reports whether the website deletion was triggered, onto the shared mutation-result shape.
 * WhatsApp Web treats only an explicit {@code true} as a successful deletion.
 *
 * @see BizAiKnowledgeSourceDeleteMutationWebsiteFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiKnowledgeSourceDeleteMutationWebsiteMutation")
public final class BizAiKnowledgeSourceDeleteMutationWebsiteFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected mutation result, or {@code null} when the Meta graph endpoint omitted the
     * field.
     */
    private final BusinessAiMutationResult result;

    /**
     * Constructs a response wrapping the projected mutation result.
     *
     * <p>Reserved for the static parser.
     *
     * @param result the projected mutation result, or {@code null} when the Meta graph endpoint
     *               omitted the field
     */
    private BizAiKnowledgeSourceDeleteMutationWebsiteFacebookGraphQlResponse(BusinessAiMutationResult result) {
        this.result = result;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code success} scalar onto a {@link BusinessAiMutationResult}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAiKnowledgeSourceDeleteMutationWebsiteFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("maiba_trigger_website_deletion");
        if (node == null) {
            return Optional.of(new BizAiKnowledgeSourceDeleteMutationWebsiteFacebookGraphQlResponse(null));
        }

        var result = new BusinessAiMutationResultBuilder()
                .success(Boolean.TRUE.equals(node.getBoolean("success")))
                .build();
        return Optional.of(new BizAiKnowledgeSourceDeleteMutationWebsiteFacebookGraphQlResponse(result));
    }

    /**
     * Returns the projected mutation result.
     *
     * @return the projected {@link BusinessAiMutationResult}, or empty when the Meta graph endpoint
     *         omitted the field
     */
    public Optional<BusinessAiMutationResult> result() {
        return Optional.ofNullable(result);
    }
}
