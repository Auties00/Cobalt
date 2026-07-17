package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.github.auties00.cobalt.wire.graphql.whatsappWeb.business.CatalogCollectionStatusParser;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProductCollection;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the update-collection mutation built by
 * {@link BizCatalogManagementUpdateCollectionFacebookGraphQlRequest} into a {@link BusinessProductCollection}.
 *
 * <p>Reads the linked chain {@code xfb_whatsapp_catalog_update_collection -> collection} and projects
 * the updated collection's identifier and its moderation status onto the Cobalt domain model. The
 * mutation does not echo the collection's products, so {@link BusinessProductCollection#products()} is
 * empty.
 *
 * @see BizCatalogManagementUpdateCollectionFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementUpdateCollectionMutation")
public final class BizCatalogManagementUpdateCollectionFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed updated collection.
     */
    private final BusinessProductCollection collection;

    /**
     * Constructs a response wrapping the parsed updated collection.
     *
     * <p>Reserved for the static parser.
     *
     * @param collection the parsed updated collection, or {@code null} when the Meta graph endpoint omitted the field
     */
    private BizCatalogManagementUpdateCollectionFacebookGraphQlResponse(BusinessProductCollection collection) {
        this.collection = collection;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked chain {@code xfb_whatsapp_catalog_update_collection -> collection} and
     * projects the updated collection onto a {@link BusinessProductCollection}; the returned
     * {@link Optional} is empty when {@code data} or the collection is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null} or the collection is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementUpdateCollectionMutation", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<BizCatalogManagementUpdateCollectionFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var root = data.getJSONObject("xfb_whatsapp_catalog_update_collection");
        if (root == null) {
            return Optional.empty();
        }
        var collectionNode = root.getJSONObject("collection");
        if (collectionNode == null) {
            return Optional.empty();
        }
        var collection = CatalogCollectionStatusParser.parseCollection(collectionNode);
        return Optional.of(new BizCatalogManagementUpdateCollectionFacebookGraphQlResponse(collection));
    }

    /**
     * Returns the parsed updated collection.
     *
     * @return the parsed {@link BusinessProductCollection}, never {@code null}
     */
    public BusinessProductCollection collection() {
        return collection;
    }
}
