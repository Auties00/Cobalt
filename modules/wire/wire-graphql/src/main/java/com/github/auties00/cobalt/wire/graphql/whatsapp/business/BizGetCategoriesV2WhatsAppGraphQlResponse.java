package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessCategoryNode;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessCategoryNodeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the hierarchical category-typeahead query built by
 * {@link BizGetCategoriesV2WhatsAppGraphQlRequest} into a tree of {@link BusinessCategoryNode}.
 *
 * <p>Reads the linked {@code whatsapp_catkit_typeahead_proxy} field and projects its nested
 * {@code categories} tree, where each stanza carries an id, a localised display name, and its own
 * children, onto the Cobalt domain model.
 *
 * @see BizGetCategoriesV2WhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizGetCategoriesV2Query")
public final class BizGetCategoriesV2WhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the parsed top level of the category tree.
     */
    private final List<BusinessCategoryNode> categories;

    /**
     * Constructs a response wrapping the parsed category tree.
     *
     * <p>Reserved for the static parser.
     *
     * @param categories the parsed top level of the category tree
     */
    private BizGetCategoriesV2WhatsAppGraphQlResponse(List<BusinessCategoryNode> categories) {
        this.categories = categories;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code whatsapp_catkit_typeahead_proxy} and recursively projects its
     * nested {@code categories} tree onto {@link BusinessCategoryNode} entries; the returned
     * {@link Optional} is empty when {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebBizGetCategoriesV2Query", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<BizGetCategoriesV2WhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var root = data.getJSONObject("whatsapp_catkit_typeahead_proxy");
        var categories = root == null ? List.<BusinessCategoryNode>of() : parseNodes(root.getJSONArray("categories"));
        return Optional.of(new BizGetCategoriesV2WhatsAppGraphQlResponse(categories));
    }

    /**
     * Recursively projects a {@code categories} JSON array onto a list of {@link BusinessCategoryNode}.
     *
     * @param array the JSON array to parse, or {@code null}
     * @return the projected nodes, empty when {@code array} is {@code null}
     */
    private static List<BusinessCategoryNode> parseNodes(JSONArray array) {
        if (array == null) {
            return List.of();
        }
        var result = new ArrayList<BusinessCategoryNode>(array.size());
        for (var i = 0; i < array.size(); i++) {
            var entry = array.getJSONObject(i);
            if (entry == null) {
                continue;
            }
            result.add(new BusinessCategoryNodeBuilder()
                    .id(entry.getString("id"))
                    .displayName(entry.getString("display_name"))
                    .children(parseNodes(entry.getJSONArray("categories")))
                    .build());
        }
        return result;
    }

    /**
     * Returns the parsed top level of the category tree.
     *
     * <p>Each {@link BusinessCategoryNode} carries its own children, so the returned list is the root
     * of the full nested tree.
     *
     * @return the parsed top-level categories, empty when the graph.whatsapp.com endpoint returned none
     */
    public List<BusinessCategoryNode> categories() {
        return categories;
    }
}
