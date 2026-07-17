package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.github.auties00.cobalt.wire.graphql.whatsappWeb.business.CatalogProductInfoParser;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogCollections;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogCollectionsBuilder;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProductCollection;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProductCollectionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the fetch-collections query built by
 * {@link BizCatalogManagementFetchCollectionsFacebookGraphQlRequest} into a {@link BusinessCatalogCollections}.
 *
 * <p>Reads the linked root {@code xfb_whatsapp_catalog_collections} and projects the catalog type, the
 * page of collections (each with its products), and the forward cursor onto the Cobalt domain model.
 *
 * @see BizCatalogManagementFetchCollectionsFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementFetchCollectionsQuery")
public final class BizCatalogManagementFetchCollectionsFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed collections page.
     */
    private final BusinessCatalogCollections collections;

    /**
     * Constructs a response wrapping the parsed collections page.
     *
     * <p>Reserved for the static parser.
     *
     * @param collections the parsed collections page, or {@code null} when the Meta graph endpoint omitted the field
     */
    private BizCatalogManagementFetchCollectionsFacebookGraphQlResponse(BusinessCatalogCollections collections) {
        this.collections = collections;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xfb_whatsapp_catalog_collections} and projects the catalog type,
     * collections, and forward cursor onto a {@link BusinessCatalogCollections}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the collections projection is missing
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementFetchCollections", exports = "fetchCollections",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<BizCatalogManagementFetchCollectionsFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var root = data.getJSONObject("xfb_whatsapp_catalog_collections");
        if (root == null) {
            return Optional.empty();
        }
        String after = null;
        var paging = root.getJSONObject("paging");
        if (paging != null) {
            after = paging.getString("after");
        }
        var collections = parseCollections(root.getJSONArray("collections"));
        var page = new BusinessCatalogCollectionsBuilder()
                .catalogType(root.getString("catalog_type"))
                .collections(collections)
                .afterCursor(after)
                .build();
        return Optional.of(new BizCatalogManagementFetchCollectionsFacebookGraphQlResponse(page));
    }

    /**
     * Returns the parsed collections page.
     *
     * <p>The returned {@link BusinessCatalogCollections} carries the catalog type, the page of
     * collections, and the forward cursor.
     *
     * @return the parsed collections page, never {@code null}
     */
    public BusinessCatalogCollections collections() {
        return collections;
    }

    /**
     * Parses the {@code collections} array into a list of {@link BusinessProductCollection} values.
     *
     * <p>Each entry projects the collection identity, its products through
     * {@link CatalogProductInfoParser}, and the collection-level review verdict onto the domain model.
     *
     * @param array the collections array, possibly {@code null}
     * @return the parsed collections, never {@code null}
     */
    private static List<BusinessProductCollection> parseCollections(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        var out = new ArrayList<BusinessProductCollection>(array.size());
        for (var i = 0; i < array.size(); i++) {
            var obj = array.getJSONObject(i);
            if (obj == null) {
                continue;
            }
            String moderationStatus = null;
            var canAppeal = false;
            String commerceUrl = null;
            String rejectReason = null;
            var statusInfo = obj.getJSONObject("status_info");
            if (statusInfo != null) {
                moderationStatus = statusInfo.getString("status");
                canAppeal = "ISCANAPPEAL_TRUE".equals(statusInfo.getString("can_appeal"))
                        || Boolean.TRUE.equals(statusInfo.getBoolean("can_appeal"));
                commerceUrl = statusInfo.getString("commerce_url");
                rejectReason = statusInfo.getString("reject_reason");
            }
            out.add(new BusinessProductCollectionBuilder()
                    .id(obj.getString("id"))
                    .name(obj.getString("name"))
                    .products(CatalogProductInfoParser.parseProducts(obj.getJSONArray("products")))
                    .moderationStatus(moderationStatus)
                    .canAppeal(canAppeal)
                    .commerceUrl(commerceUrl)
                    .rejectReason(rejectReason)
                    .build());
        }
        return List.copyOf(out);
    }
}
