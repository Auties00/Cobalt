package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiAgentHome;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiAgentHomeBuilder;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiExampleResponse;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiExampleResponseBuilder;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiKnowledgeEntry;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiKnowledgeEntryBuilder;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiProductImage;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiProductImageBuilder;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiProductInfo;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiProductInfoBuilder;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiWebsite;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiWebsiteBuilder;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiWebsiteKnowledge;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiWebsiteKnowledgeBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the fetch-example-responses query built by
 * {@link BizAiExampleResponsesFacebookGraphQlRequest} into a {@link BusinessAiAgentHome}.
 *
 * <p>Projects the linked {@code xfb_meta_ai_biz_agent_wa_ai_home} field onto the knowledge facets of
 * the home view: the ordered knowledge entries (free-text and frequently-asked-question entries), the
 * website-backed material and featured products, the structured product entries, and the
 * product-information eligibility flag. The remaining facets are left empty because this query does
 * not request them.
 *
 * @see BizAiExampleResponsesFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiExampleResponsesQuery")
public final class BizAiExampleResponsesFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected agent-home view, or {@code null} when the Meta graph endpoint omitted the field.
     */
    private final BusinessAiAgentHome agentHome;

    /**
     * Constructs a response wrapping the projected agent-home view.
     *
     * <p>Reserved for the static parser.
     *
     * @param agentHome the projected agent-home view, or {@code null} when the Meta graph endpoint omitted the field
     */
    private BizAiExampleResponsesFacebookGraphQlResponse(BusinessAiAgentHome agentHome) {
        this.agentHome = agentHome;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * knowledge facets onto a {@link BusinessAiAgentHome}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAiExampleResponsesFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var home = data.getJSONObject("xfb_meta_ai_biz_agent_wa_ai_home");
        if (home == null) {
            return Optional.of(new BizAiExampleResponsesFacebookGraphQlResponse(null));
        }

        var knowledgeEntries = parseKnowledgeEntries(home.getJSONArray("ordered_knowledge"));
        var websiteKnowledge = parseWebsiteKnowledge(home.getJSONObject("knowledge"));
        var productInfo = parseProductInfo(home.getJSONArray("product_info_knowledge"));
        var productInfoEligible = parseEligible(home.getJSONObject("product_info_eligibility"));
        var agentHome = new BusinessAiAgentHomeBuilder()
                .knowledgeEntries(knowledgeEntries)
                .websiteKnowledge(websiteKnowledge)
                .productInfo(productInfo)
                .productInfoEligible(productInfoEligible)
                .build();
        return Optional.of(new BizAiExampleResponsesFacebookGraphQlResponse(agentHome));
    }

    /**
     * Projects the {@code ordered_knowledge} array onto a list of {@link BusinessAiKnowledgeEntry}
     * values.
     *
     * @param arr the {@code ordered_knowledge} array, possibly {@code null}
     * @return the projected entries, never {@code null}
     */
    private static List<BusinessAiKnowledgeEntry> parseKnowledgeEntries(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessAiKnowledgeEntry>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }
            var lastUpdate = obj.getLong("last_update_ts");
            result.add(new BusinessAiKnowledgeEntryBuilder()
                    .knowledgeType(obj.getString("knowledge_type"))
                    .dataType(obj.getString("data_type"))
                    .text(obj.getString("string_data"))
                    .exampleResponse(parseExampleResponse(obj.getJSONObject("faq_data")))
                    .lastUpdated(lastUpdate != null ? Instant.ofEpochSecond(lastUpdate) : null)
                    .build());
        }
        return result;
    }

    /**
     * Projects a {@code faq_data} stanza onto a {@link BusinessAiExampleResponse}.
     *
     * @param obj the {@code faq_data} stanza, possibly {@code null}
     * @return the projected example response, or {@code null} when {@code obj} is {@code null}
     */
    private static BusinessAiExampleResponse parseExampleResponse(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        return new BusinessAiExampleResponseBuilder()
                .id(obj.getString("id"))
                .question(obj.getString("question"))
                .answer(obj.getString("answer"))
                .entryType(obj.getString("faq_type"))
                .build();
    }

    /**
     * Projects the {@code knowledge} stanza onto a {@link BusinessAiWebsiteKnowledge}.
     *
     * @param obj the {@code knowledge} stanza, possibly {@code null}
     * @return the projected website knowledge, or {@code null} when {@code obj} is {@code null}
     */
    private static BusinessAiWebsiteKnowledge parseWebsiteKnowledge(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        return new BusinessAiWebsiteKnowledgeBuilder()
                .website(obj.getString("website"))
                .websites(parseWebsites(obj.getJSONArray("websites")))
                .bestsellerProductIds(parseBestsellers(obj.getJSONArray("bestsellers")))
                .build();
    }

    /**
     * Projects the {@code websites} array onto a list of {@link BusinessAiWebsite} values.
     *
     * @param arr the {@code websites} array, possibly {@code null}
     * @return the projected websites, never {@code null}
     */
    private static List<BusinessAiWebsite> parseWebsites(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessAiWebsite>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }
            result.add(new BusinessAiWebsiteBuilder()
                    .type(obj.getString("website_type"))
                    .url(obj.getString("website_url"))
                    .build());
        }
        return result;
    }

    /**
     * Projects the {@code bestsellers} array onto a list of catalog product ids.
     *
     * @param arr the {@code bestsellers} array, possibly {@code null}
     * @return the projected product ids, never {@code null}
     */
    private static List<String> parseBestsellers(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<String>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }
            var id = obj.getString("id");
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    /**
     * Projects the {@code product_info_knowledge} array onto a list of {@link BusinessAiProductInfo}
     * values.
     *
     * @param arr the {@code product_info_knowledge} array, possibly {@code null}
     * @return the projected products, never {@code null}
     */
    private static List<BusinessAiProductInfo> parseProductInfo(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessAiProductInfo>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }
            result.add(new BusinessAiProductInfoBuilder()
                    .productId(obj.getString("product_id"))
                    .title(obj.getString("title"))
                    .description(obj.getString("description"))
                    .price(obj.getString("price"))
                    .images(parseImages(obj.getJSONArray("images")))
                    .build());
        }
        return result;
    }

    /**
     * Projects a product's {@code images} array onto a list of {@link BusinessAiProductImage} values.
     *
     * @param arr the {@code images} array, possibly {@code null}
     * @return the projected images, never {@code null}
     */
    private static List<BusinessAiProductImage> parseImages(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessAiProductImage>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }
            result.add(new BusinessAiProductImageBuilder()
                    .id(obj.getString("image_id"))
                    .originalUrl(obj.getString("original_url"))
                    .thumbnailUrl(obj.getString("thumbnail_url"))
                    .build());
        }
        return result;
    }

    /**
     * Reads the {@code eligible} flag from the {@code product_info_eligibility} stanza.
     *
     * @param obj the {@code product_info_eligibility} stanza, possibly {@code null}
     * @return {@code true} when the stanza reports eligibility, {@code false} otherwise
     */
    private static boolean parseEligible(JSONObject obj) {
        if (obj == null) {
            return false;
        }

        var eligible = obj.getBoolean("eligible");
        return eligible != null && eligible;
    }

    /**
     * Returns the projected agent-home view carrying the agent's knowledge.
     *
     * @return the projected {@link BusinessAiAgentHome}, or empty when the Meta graph endpoint omitted the field
     */
    public Optional<BusinessAiAgentHome> agentHome() {
        return Optional.ofNullable(agentHome);
    }
}
