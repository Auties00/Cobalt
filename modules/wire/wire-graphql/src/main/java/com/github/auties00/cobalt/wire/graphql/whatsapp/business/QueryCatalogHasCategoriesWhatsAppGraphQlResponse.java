package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the has-categories query built by
 * {@link QueryCatalogHasCategoriesWhatsAppGraphQlRequest}.
 *
 * <p>Exposes the linked root {@code xwa_product_catalog_get_categories} of type {@link Categories},
 * whose only selection is the plural {@code categories} array. Each category carries solely its
 * GraphQL {@code __typename} discriminator: WhatsApp Web reduces the whole reply to "does the catalog
 * expose at least one category", a question {@link #hasCategories()} answers directly.
 *
 * @see QueryCatalogHasCategoriesWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebQueryCatalogHasCategoriesQuery")
public final class QueryCatalogHasCategoriesWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the parsed {@code xwa_product_catalog_get_categories} root, or {@code null} when the graph.whatsapp.com endpoint
     * omitted it.
     */
    private final Categories categories;

    /**
     * Constructs a response wrapping the parsed categories root.
     *
     * <p>Reserved for the static parser.
     *
     * @param categories the parsed categories root, or {@code null} when the graph.whatsapp.com endpoint omitted the field
     */
    private QueryCatalogHasCategoriesWhatsAppGraphQlResponse(Categories categories) {
        this.categories = categories;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xwa_product_catalog_get_categories}; the returned
     * {@link Optional} is empty when {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<QueryCatalogHasCategoriesWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var categories = Categories.of(data.getJSONObject("xwa_product_catalog_get_categories")).orElse(null);
        return Optional.of(new QueryCatalogHasCategoriesWhatsAppGraphQlResponse(categories));
    }

    /**
     * Returns the parsed categories root.
     *
     * @return the parsed {@link Categories}, or empty when the graph.whatsapp.com endpoint omitted the field
     */
    public Optional<Categories> categories() {
        return Optional.ofNullable(categories);
    }

    /**
     * Returns whether the catalog exposes at least one product category.
     *
     * <p>Mirrors the WhatsApp Web reduction of this query to a single boolean: the catalog "has
     * categories" when the root is present and its {@code categories} array is non-empty.
     *
     * @return {@code true} when the graph.whatsapp.com endpoint returned at least one category, {@code false} otherwise
     */
    public boolean hasCategories() {
        return categories != null && !categories.categories().isEmpty();
    }

    /**
     * Wraps the {@code xwa_product_catalog_get_categories} root of type
     * {@code XWAProductCatalogGetCategoriesResponseSuccess}.
     *
     * <p>Carries the plural {@code categories} array, whose entries hold only the GraphQL
     * {@code __typename} discriminator.
     */
    public static final class Categories {
        /**
         * Holds the parsed category entries returned by the graph.whatsapp.com endpoint.
         */
        private final List<Category> categories;

        /**
         * Constructs a categories wrapper from the parsed entries.
         *
         * <p>Reserved for the static parser.
         *
         * @param categories the parsed category entries
         */
        private Categories(List<Category> categories) {
            this.categories = categories;
        }

        /**
         * Returns the parsed category entries.
         *
         * @return the parsed entries, empty when the graph.whatsapp.com endpoint returned none
         */
        public List<Category> categories() {
            return categories;
        }

        /**
         * Parses a {@link Categories} from the given JSON object.
         *
         * <p>Used by {@link QueryCatalogHasCategoriesWhatsAppGraphQlResponse#of(JSONObject)} to hydrate the
         * nested {@code xwa_product_catalog_get_categories} entry.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link Categories}, or empty when {@code obj} is {@code null}
         */
        static Optional<Categories> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var categories = Category.ofArray(obj.getJSONArray("categories"));
            return Optional.of(new Categories(categories));
        }
    }

    /**
     * Wraps one {@code categories} entry of type
     * {@code XWAProductCatalogGetCategoriesResponseSuccessCategoryWithSubCategories}.
     *
     * <p>The query selects only the GraphQL {@code __typename} discriminator on each category; no
     * other field is requested, so this entry exists solely to be counted.
     */
    public static final class Category {
        /**
         * Holds the GraphQL {@code __typename} discriminator of this category entry.
         */
        private final String typename;

        /**
         * Constructs a category wrapper from the parsed discriminator.
         *
         * <p>Reserved for the static parser.
         *
         * @param typename the {@code __typename} discriminator, or {@code null} when the graph.whatsapp.com endpoint
         *                 omitted it
         */
        private Category(String typename) {
            this.typename = typename;
        }

        /**
         * Returns the GraphQL {@code __typename} discriminator of this category entry.
         *
         * @return the discriminator, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> typename() {
            return Optional.ofNullable(typename);
        }

        /**
         * Parses a {@link Category} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link Category}, or empty when {@code obj} is {@code null}
         */
        static Optional<Category> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            return Optional.of(new Category(WhatsAppGraphQlOperation.Response.getTypename(obj).orElse(null)));
        }

        /**
         * Parses a list of {@link Category} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<Category> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<Category>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }
}
