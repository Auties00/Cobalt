package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiMutationResult;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiMutationResultBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the knowledge-review-commit mutation built by
 * {@link BizAiKnowledgeReviewCommitFacebookGraphQlRequest} into a {@link BusinessAiMutationResult}.
 *
 * <p>Projects the linked {@code xfb_maiba_approve_potential_knowledge} field, whose single
 * {@code success} scalar reports whether the requested knowledge items were approved, onto the shared
 * mutation-result shape.
 *
 * @see BizAiKnowledgeReviewCommitFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiKnowledgeReviewCommitMutation")
public final class BizAiKnowledgeReviewCommitFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
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
    private BizAiKnowledgeReviewCommitFacebookGraphQlResponse(BusinessAiMutationResult result) {
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
    public static Optional<BizAiKnowledgeReviewCommitFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("xfb_maiba_approve_potential_knowledge");
        if (node == null) {
            return Optional.of(new BizAiKnowledgeReviewCommitFacebookGraphQlResponse(null));
        }

        var result = new BusinessAiMutationResultBuilder()
                .success(Boolean.TRUE.equals(node.getBoolean("success")))
                .build();
        return Optional.of(new BizAiKnowledgeReviewCommitFacebookGraphQlResponse(result));
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
