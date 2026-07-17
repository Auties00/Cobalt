package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessCategory;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessCategoryBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the flat category-typeahead query built by
 * {@link BizGetCategoriesWhatsAppGraphQlRequest} into a flat list of {@link BusinessCategory}.
 *
 * <p>Reads the linked {@code whatsapp_catkit_typeahead_proxy} field and projects its {@code categories}
 * list, each pairing a server category id with a localised display name, onto the Cobalt domain model.
 *
 * @see BizGetCategoriesWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizGetCategoriesQuery")
public final class BizGetCategoriesWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the parsed flat categories.
     */
    private final List<BusinessCategory> categories;

    /**
     * Constructs a response wrapping the parsed categories.
     *
     * <p>Reserved for the static parser.
     *
     * @param categories the parsed categories
     */
    private BizGetCategoriesWhatsAppGraphQlResponse(List<BusinessCategory> categories) {
        this.categories = categories;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code whatsapp_catkit_typeahead_proxy} and projects its
     * {@code categories} list onto {@link BusinessCategory} entries; the returned {@link Optional} is
     * empty when {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebBizGetCategoriesQuery", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<BizGetCategoriesWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var root = data.getJSONObject("whatsapp_catkit_typeahead_proxy");
        var categories = root == null ? List.<BusinessCategory>of() : parseCategories(root.getJSONArray("categories"));
        return Optional.of(new BizGetCategoriesWhatsAppGraphQlResponse(categories));
    }

    /**
     * Projects a {@code categories} JSON array onto a list of {@link BusinessCategory}.
     *
     * @param array the JSON array to parse, or {@code null}
     * @return the projected categories, empty when {@code array} is {@code null}
     */
    private static List<BusinessCategory> parseCategories(JSONArray array) {
        if (array == null) {
            return List.of();
        }
        var result = new ArrayList<BusinessCategory>(array.size());
        for (var i = 0; i < array.size(); i++) {
            var entry = array.getJSONObject(i);
            if (entry == null) {
                continue;
            }
            result.add(new BusinessCategoryBuilder()
                    .id(entry.getString("id"))
                    .name(entry.getString("display_name"))
                    .build());
        }
        return result;
    }

    /**
     * Returns the parsed flat categories.
     *
     * <p>Each {@link BusinessCategory} pairs a server category id with its localised display name.
     *
     * @return the parsed categories, empty when the graph.whatsapp.com endpoint returned none
     */
    public List<BusinessCategory> categories() {
        return categories;
    }
}
