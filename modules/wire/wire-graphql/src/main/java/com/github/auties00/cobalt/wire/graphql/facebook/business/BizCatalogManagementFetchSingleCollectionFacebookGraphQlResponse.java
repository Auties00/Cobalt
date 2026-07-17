package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.github.auties00.cobalt.wire.graphql.whatsappWeb.business.CatalogProductInfoParser;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProductCollection;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProductCollectionBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the fetch-single-collection query built by
 * {@link BizCatalogManagementFetchSingleCollectionFacebookGraphQlRequest} into a
 * {@link BusinessProductCollection}.
 *
 * <p>Reads the linked root {@code xfb_whatsapp_catalog_collection}, projecting the collection identity,
 * its products, and its review verdict onto the domain model, and folding the sibling
 * {@code paging.after} cursor into the collection's next-page cursor.
 *
 * @see BizCatalogManagementFetchSingleCollectionFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementFetchSingleCollectionQuery")
public final class BizCatalogManagementFetchSingleCollectionFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed collection.
     */
    private final BusinessProductCollection collection;

    /**
     * Constructs a response wrapping the parsed collection.
     *
     * <p>Reserved for the static parser.
     *
     * @param collection the parsed collection, or {@code null} when the Meta graph endpoint omitted the field
     */
    private BizCatalogManagementFetchSingleCollectionFacebookGraphQlResponse(BusinessProductCollection collection) {
        this.collection = collection;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xfb_whatsapp_catalog_collection} and its {@code collection} and
     * {@code paging} children, projecting them onto a {@link BusinessProductCollection} whose
     * {@link BusinessProductCollection#afterCursor()} carries the {@code paging.after} cursor.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the collection projection is missing
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementFetchSingleCollection", exports = "fetchSingleCollection",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<BizCatalogManagementFetchSingleCollectionFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var root = data.getJSONObject("xfb_whatsapp_catalog_collection");
        if (root == null) {
            return Optional.empty();
        }
        var collectionObj = root.getJSONObject("collection");
        if (collectionObj == null) {
            return Optional.empty();
        }
        String after = null;
        var paging = root.getJSONObject("paging");
        if (paging != null) {
            after = paging.getString("after");
        }
        String moderationStatus = null;
        var canAppeal = false;
        String commerceUrl = null;
        String rejectReason = null;
        var statusInfo = collectionObj.getJSONObject("status_info");
        if (statusInfo != null) {
            moderationStatus = statusInfo.getString("status");
            canAppeal = Boolean.TRUE.equals(statusInfo.getBoolean("can_appeal"));
            commerceUrl = statusInfo.getString("commerce_url");
            rejectReason = statusInfo.getString("reject_reason");
        }
        var collection = new BusinessProductCollectionBuilder()
                .id(collectionObj.getString("id"))
                .name(collectionObj.getString("name"))
                .products(CatalogProductInfoParser.parseProducts(collectionObj.getJSONArray("products")))
                .moderationStatus(moderationStatus)
                .canAppeal(canAppeal)
                .commerceUrl(commerceUrl)
                .rejectReason(rejectReason)
                .afterCursor(after)
                .build();
        return Optional.of(new BizCatalogManagementFetchSingleCollectionFacebookGraphQlResponse(collection));
    }

    /**
     * Returns the parsed collection.
     *
     * @return the parsed {@link BusinessProductCollection}, never {@code null}
     */
    public BusinessProductCollection collection() {
        return collection;
    }
}
