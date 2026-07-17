package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiKnowledgeReview;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiKnowledgeReviewBuilder;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiPendingKnowledge;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiPendingKnowledgeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the knowledge-review query built by
 * {@link BizAiKnowledgeReviewFacebookGraphQlRequest} into a {@link BusinessAiKnowledgeReview}.
 *
 * <p>Projects the linked {@code xfb_maiba_load_potential_knowledge_for_review} field onto the review
 * queue: the list of knowledge items the WhatsApp Business AI agent has inferred and that await the
 * operator's approval.
 *
 * @see BizAiKnowledgeReviewFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiKnowledgeReviewQuery")
public final class BizAiKnowledgeReviewFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected review queue, or {@code null} when the Meta graph endpoint omitted the
     * field.
     */
    private final BusinessAiKnowledgeReview knowledgeReview;

    /**
     * Constructs a response wrapping the projected review queue.
     *
     * <p>Reserved for the static parser.
     *
     * @param knowledgeReview the projected review queue, or {@code null} when the Meta graph endpoint
     *                        omitted the field
     */
    private BizAiKnowledgeReviewFacebookGraphQlResponse(BusinessAiKnowledgeReview knowledgeReview) {
        this.knowledgeReview = knowledgeReview;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the pending
     * knowledge items onto a {@link BusinessAiKnowledgeReview}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAiKnowledgeReviewFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var review = data.getJSONObject("xfb_maiba_load_potential_knowledge_for_review");
        if (review == null) {
            return Optional.of(new BizAiKnowledgeReviewFacebookGraphQlResponse(null));
        }

        var pendingItems = parsePendingItems(review.getJSONArray("items"));
        var knowledgeReview = new BusinessAiKnowledgeReviewBuilder()
                .pendingItems(pendingItems)
                .build();
        return Optional.of(new BizAiKnowledgeReviewFacebookGraphQlResponse(knowledgeReview));
    }

    /**
     * Projects the {@code items} array onto a list of {@link BusinessAiPendingKnowledge} values.
     *
     * @param arr the {@code items} array, possibly {@code null}
     * @return the projected items, never {@code null}
     */
    private static List<BusinessAiPendingKnowledge> parsePendingItems(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessAiPendingKnowledge>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }
            result.add(new BusinessAiPendingKnowledgeBuilder()
                    .id(obj.getString("id"))
                    .type(obj.getString("type"))
                    .question(obj.getString("faq_question"))
                    .answer(obj.getString("faq_answer"))
                    .attributeName(obj.getString("biz_info_attribute"))
                    .attributeValue(obj.getString("biz_info_value"))
                    .build());
        }
        return result;
    }

    /**
     * Returns the projected review queue carrying the pending knowledge items.
     *
     * @return the projected {@link BusinessAiKnowledgeReview}, or empty when the Meta graph endpoint
     *         omitted the field
     */
    public Optional<BusinessAiKnowledgeReview> knowledgeReview() {
        return Optional.ofNullable(knowledgeReview);
    }
}
