package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessPriceTier;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessPriceTierBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the price-tiers query built by {@link BizGetPriceTiersWhatsAppGraphQlRequest}
 * into a list of {@link BusinessPriceTier}.
 *
 * <p>Reads the linked {@code xwa_whatsapp_get_pricing_tiers} root and projects its {@code price_tiers}
 * list, each carrying a tier id, a localised description, and a currency symbol, onto the Cobalt domain
 * model.
 *
 * @see BizGetPriceTiersWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizGetPriceTiersQuery")
public final class BizGetPriceTiersWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the parsed price tiers.
     */
    private final List<BusinessPriceTier> priceTiers;

    /**
     * Constructs a response wrapping the parsed price tiers.
     *
     * <p>Reserved for the static parser.
     *
     * @param priceTiers the parsed price tiers
     */
    private BizGetPriceTiersWhatsAppGraphQlResponse(List<BusinessPriceTier> priceTiers) {
        this.priceTiers = priceTiers;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xwa_whatsapp_get_pricing_tiers} and projects its
     * {@code price_tiers} list onto {@link BusinessPriceTier} entries; the returned {@link Optional} is
     * empty when {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebBizGetPriceTiersQuery", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<BizGetPriceTiersWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var root = data.getJSONObject("xwa_whatsapp_get_pricing_tiers");
        var priceTiers = root == null ? List.<BusinessPriceTier>of() : parseTiers(root.getJSONArray("price_tiers"));
        return Optional.of(new BizGetPriceTiersWhatsAppGraphQlResponse(priceTiers));
    }

    /**
     * Projects a {@code price_tiers} JSON array onto a list of {@link BusinessPriceTier}.
     *
     * @param array the JSON array to parse, or {@code null}
     * @return the projected tiers, empty when {@code array} is {@code null}
     */
    private static List<BusinessPriceTier> parseTiers(JSONArray array) {
        if (array == null) {
            return List.of();
        }
        var result = new ArrayList<BusinessPriceTier>(array.size());
        for (var i = 0; i < array.size(); i++) {
            var entry = array.getJSONObject(i);
            if (entry == null) {
                continue;
            }
            result.add(new BusinessPriceTierBuilder()
                    .id(entry.getString("id"))
                    .description(entry.getString("description"))
                    .currencySymbol(entry.getString("symbol"))
                    .build());
        }
        return result;
    }

    /**
     * Returns the parsed price tiers.
     *
     * <p>Each {@link BusinessPriceTier} pairs a tier id with a localised description and currency
     * symbol.
     *
     * @return the parsed tiers, empty when the graph.whatsapp.com endpoint returned none
     */
    public List<BusinessPriceTier> priceTiers() {
        return priceTiers;
    }
}
