package com.github.auties00.cobalt.wire.graphql.whatsappWeb.promotion;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.promotion.QuickPromotion;
import com.github.auties00.cobalt.wire.linked.business.promotion.QuickPromotionAction;
import com.github.auties00.cobalt.wire.linked.business.promotion.QuickPromotionActionBuilder;
import com.github.auties00.cobalt.wire.linked.business.promotion.QuickPromotionBuilder;
import com.github.auties00.cobalt.wire.linked.business.promotion.QuickPromotionCreative;
import com.github.auties00.cobalt.wire.linked.business.promotion.QuickPromotionCreativeBuilder;
import com.github.auties00.cobalt.wire.linked.business.promotion.QuickPromotionMediaVariant;
import com.github.auties00.cobalt.wire.linked.business.promotion.QuickPromotionMediaVariantBuilder;
import com.github.auties00.cobalt.wire.linked.business.promotion.QuickPromotionSurfaceBatch;
import com.github.auties00.cobalt.wire.linked.business.promotion.QuickPromotionSurfaceBatchBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the WhatsApp Web GraphQL response of the consumer fetch-quick-promotions query built by
 * {@link ConsumerFetchQuickPromotionsWhatsAppWebGraphQlRequest} into a list of
 * {@link QuickPromotionSurfaceBatch}.
 *
 * <p>Reads the plural root {@code quick_promotion_multiverse_batch_fetch_root} and projects each
 * entry onto a {@link QuickPromotionSurfaceBatch}: the surface identifier and the ordered list
 * of eligible {@link QuickPromotion banners}, each with their validity window, rendering
 * {@linkplain QuickPromotionCreative creatives}, and opaque logging blob.
 *
 * @see ConsumerFetchQuickPromotionsWhatsAppWebGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebConsumerFetchQuickPromotionsQuery")
public final class ConsumerFetchQuickPromotionsWhatsAppWebGraphQlResponse implements WhatsAppWebGraphQlOperation.Response {
    /**
     * Holds the parsed per-surface batches.
     */
    private final List<QuickPromotionSurfaceBatch> batches;

    /**
     * Constructs a response wrapping the parsed per-surface batches.
     *
     * <p>Reserved for the static parser.
     *
     * @param batches the parsed per-surface batches
     */
    private ConsumerFetchQuickPromotionsWhatsAppWebGraphQlResponse(List<QuickPromotionSurfaceBatch> batches) {
        this.batches = batches;
    }

    /**
     * Parses the WhatsApp Web GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the plural root {@code quick_promotion_multiverse_batch_fetch_root} and projects
     * each entry onto a {@link QuickPromotionSurfaceBatch}; the returned {@link Optional} is
     * empty when {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppWebGraphQlClient#send(WhatsAppWebGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<ConsumerFetchQuickPromotionsWhatsAppWebGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var batches = parseBatches(data.getJSONArray("quick_promotion_multiverse_batch_fetch_root"));
        return Optional.of(new ConsumerFetchQuickPromotionsWhatsAppWebGraphQlResponse(batches));
    }

    /**
     * Projects the {@code quick_promotion_multiverse_batch_fetch_root} array onto a list of
     * {@link QuickPromotionSurfaceBatch}.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<QuickPromotionSurfaceBatch> parseBatches(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<QuickPromotionSurfaceBatch>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            result.add(new QuickPromotionSurfaceBatchBuilder()
                    .surfaceId(obj.getString("surface_nux_id"))
                    .banners(parseBanners(obj.getJSONObject("eligible_promotions")))
                    .build());
        }
        return result;
    }

    /**
     * Projects an {@code eligible_promotions} connection onto a list of {@link QuickPromotion}.
     *
     * @param obj the JSON object to project
     * @return the projected list, empty when {@code obj} or its {@code edges} array is missing
     */
    private static List<QuickPromotion> parseBanners(JSONObject obj) {
        if (obj == null) {
            return List.of();
        }

        var edges = obj.getJSONArray("edges");
        if (edges == null) {
            return List.of();
        }

        var result = new ArrayList<QuickPromotion>(edges.size());
        for (var i = 0; i < edges.size(); i++) {
            var edge = edges.getJSONObject(i);
            if (edge == null) {
                continue;
            }

            var node = edge.getJSONObject("node");
            var timeRange = edge.getJSONObject("time_range");
            var nodeObj = node == null ? new JSONObject() : node;
            result.add(new QuickPromotionBuilder()
                    .promotionId(nodeObj.getString("promotion_id"))
                    .creatives(parseCreatives(nodeObj.getJSONArray("creatives")))
                    .validFromEpochSecond(timeRange == null ? null : timeRange.getLong("start"))
                    .validUntilEpochSecond(timeRange == null ? null : timeRange.getLong("end"))
                    .loggingBlob(nodeObj.getString("encrypted_logging_data"))
                    .build());
        }
        return result;
    }

    /**
     * Projects the {@code creatives} array onto a list of {@link QuickPromotionCreative}.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<QuickPromotionCreative> parseCreatives(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<QuickPromotionCreative>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            var dismissible = obj.getBoolean("is_dismissible");
            result.add(new QuickPromotionCreativeBuilder()
                    .title(parseText(obj.getJSONObject("title")))
                    .body(parseText(obj.getJSONObject("content")))
                    .primaryAction(parseAction(obj.getJSONObject("primary_action")))
                    .lightModeMedia(parseMedia(obj.getJSONObject("wa_light_mode_media_details")))
                    .darkModeMedia(parseMedia(obj.getJSONObject("wa_dark_mode_media_details")))
                    .imageAccessibilityText(obj.getString("accessibility_text_for_image"))
                    .dismissible(dismissible != null && dismissible)
                    .build());
        }
        return result;
    }

    /**
     * Projects a {@code TextWithEntities} sub-object onto its rendered {@code text}.
     *
     * @param obj the JSON object to project
     * @return the rendered text, or {@code null} when {@code obj} is {@code null}
     */
    private static String parseText(JSONObject obj) {
        return obj == null ? null : obj.getString("text");
    }

    /**
     * Projects a {@code primary_action} sub-object onto a {@link QuickPromotionAction}.
     *
     * @param obj the JSON object to project
     * @return the projected action, or {@code null} when {@code obj} is {@code null}
     */
    private static QuickPromotionAction parseAction(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        return new QuickPromotionActionBuilder()
                .label(parseText(obj.getJSONObject("title")))
                .destinationUrl(obj.getString("url"))
                .build();
    }

    /**
     * Projects a {@code WAQPMediaDetails} sub-object onto a {@link QuickPromotionMediaVariant}.
     *
     * @param obj the JSON object to project
     * @return the projected media variant, or {@code null} when {@code obj} is {@code null}
     */
    private static QuickPromotionMediaVariant parseMedia(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        return new QuickPromotionMediaVariantBuilder()
                .jpegThumbnail(obj.getString("jpeg_thumbnail"))
                .build();
    }

    /**
     * Returns the parsed per-surface batches.
     *
     * @return the parsed batches, empty when the relay returned none
     */
    public List<QuickPromotionSurfaceBatch> batches() {
        return batches;
    }
}
