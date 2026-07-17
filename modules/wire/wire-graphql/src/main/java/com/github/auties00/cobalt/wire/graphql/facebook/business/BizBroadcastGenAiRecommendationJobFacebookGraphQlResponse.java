package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastGenAiRecommendation;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastGenAiRecommendationBuilder;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastGenAiRecommendationToneMessagePairBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the GenAI-recommendation query built by
 * {@link BizBroadcastGenAiRecommendationJobFacebookGraphQlRequest} into a
 * {@link BusinessBroadcastGenAiRecommendation}.
 *
 * <p>Reads the linked {@code xwa_business_broadcast_genai_recommendation} root, unwraps its single
 * {@code response} sub-object, and projects the success and error inline-fragment fields onto the
 * Cobalt domain model. Both branches are read unconditionally; the inactive branch's fields are
 * left empty.
 *
 * @see BizBroadcastGenAiRecommendationJobFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizBroadcastGenAIRecommendationJobQuery")
public final class BizBroadcastGenAiRecommendationJobFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed recommendation.
     */
    private final BusinessBroadcastGenAiRecommendation recommendation;

    /**
     * Constructs a response wrapping the parsed recommendation.
     *
     * <p>Reserved for the static parser.
     *
     * @param recommendation the parsed recommendation, or {@code null} when the Meta graph endpoint omitted the
     *                       field
     */
    private BizBroadcastGenAiRecommendationJobFacebookGraphQlResponse(BusinessBroadcastGenAiRecommendation recommendation) {
        this.recommendation = recommendation;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xwa_business_broadcast_genai_recommendation} and projects it onto
     * a {@link BusinessBroadcastGenAiRecommendation}; the returned {@link Optional} is empty when
     * {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizBroadcastGenAiRecommendationJobFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var recommendation = readRecommendation(data.getJSONObject("xwa_business_broadcast_genai_recommendation"));
        return Optional.of(new BizBroadcastGenAiRecommendationJobFacebookGraphQlResponse(recommendation));
    }

    /**
     * Returns the parsed recommendation.
     *
     * @return the parsed {@link BusinessBroadcastGenAiRecommendation}, or empty when the Meta graph endpoint
     *         omitted the field
     */
    public Optional<BusinessBroadcastGenAiRecommendation> recommendation() {
        return Optional.ofNullable(recommendation);
    }

    /**
     * Projects the {@code xwa_business_broadcast_genai_recommendation} sub-object onto a
     * {@link BusinessBroadcastGenAiRecommendation}, unwrapping the single nested {@code response}
     * sub-object.
     *
     * @param node the JSON object to read, possibly {@code null}
     * @return the projected recommendation, or {@code null} when {@code stanza} is {@code null} or no
     *         {@code response} sub-object is present
     */
    private static BusinessBroadcastGenAiRecommendation readRecommendation(JSONObject node) {
        if (node == null) {
            return null;
        }

        var response = node.getJSONObject("response");
        if (response == null) {
            return null;
        }

        return new BusinessBroadcastGenAiRecommendationBuilder()
                .typename(response.getString("__typename"))
                .toneMessagePair(readToneMessagePairs(response.getJSONArray("tone_message_pair")))
                .followUps(readStrings(response.getJSONArray("follow_ups")))
                .errorMessage(response.getString("error_message"))
                .errorCode(response.getString("error_code"))
                .build();
    }

    /**
     * Projects a {@code tone_message_pair} array onto a list of
     * {@link BusinessBroadcastGenAiRecommendation.ToneMessagePair}.
     *
     * @param arr the JSON array to read, possibly {@code null}
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<BusinessBroadcastGenAiRecommendation.ToneMessagePair> readToneMessagePairs(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessBroadcastGenAiRecommendation.ToneMessagePair>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var pair = readToneMessagePair(arr.getJSONObject(i));
            if (pair != null) {
                result.add(pair);
            }
        }
        return result;
    }

    /**
     * Projects a single {@code tone_message_pair} entry onto a
     * {@link BusinessBroadcastGenAiRecommendation.ToneMessagePair}.
     *
     * @param node the JSON object to read, possibly {@code null}
     * @return the projected pair, or {@code null} when {@code stanza} is {@code null}
     */
    private static BusinessBroadcastGenAiRecommendation.ToneMessagePair readToneMessagePair(JSONObject node) {
        if (node == null) {
            return null;
        }

        return new BusinessBroadcastGenAiRecommendationToneMessagePairBuilder()
                .tone(node.getString("tone"))
                .message(node.getString("message"))
                .build();
    }

    /**
     * Collects the string entries of the given JSON array.
     *
     * @param arr the JSON array to read, possibly {@code null}
     * @return the parsed list, empty when {@code arr} is {@code null}
     */
    private static List<String> readStrings(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<String>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var value = arr.getString(i);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }
}
