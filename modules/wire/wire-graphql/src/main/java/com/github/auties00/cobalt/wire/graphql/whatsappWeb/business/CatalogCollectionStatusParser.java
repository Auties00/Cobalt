package com.github.auties00.cobalt.wire.graphql.whatsappWeb.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProductCollection;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProductCollectionBuilder;

/**
 * Projects the status-only collection stanza returned by the catalog-management create-collection and
 * update-collection mutations onto Cobalt's {@link BusinessProductCollection} model.
 *
 * <p>Both mutations echo only the collection identifier and its moderation status; this stateless
 * utility centralises that projection so the two response decoders build the model identically. The
 * mutations do not return the collection's products or display name, so those fields are left empty.
 *
 * @implNote This implementation strips the {@code STATUS_} prefix the create-collection mutation
 * carries on {@code status_info.status} so the resulting
 * {@link BusinessProductCollection#moderationStatus()} matches the bare {@code APPROVED},
 * {@code PENDING}, or {@code REJECTED} tokens the read and update paths surface. The appealable flag is
 * read from {@code status_info.can_appeal} when present, tolerating both the boolean and the
 * stringified-boolean encodings.
 */
@WhatsAppWebModule(moduleName = "WAWebProductTypes")
public final class CatalogCollectionStatusParser {
    /**
     * Prevents instantiation of this stateless utility class.
     *
     * <p>The class is a namespace for the {@code static} parsing helper and holds no per-instance
     * state.
     *
     * @throws AssertionError always
     */
    private CatalogCollectionStatusParser() {
        throw new AssertionError("No CatalogCollectionStatusParser instances for you!");
    }

    /**
     * Projects a status-only collection stanza onto a {@link BusinessProductCollection}.
     *
     * <p>Reads the collection identifier and its {@code status_info.status} moderation verdict and
     * {@code status_info.can_appeal} appeal flag.
     *
     * @param collection the collection stanza, never {@code null}
     * @return the parsed collection, never {@code null}
     */
    public static BusinessProductCollection parseCollection(JSONObject collection) {
        String moderationStatus = null;
        var canAppeal = false;
        var statusInfo = collection.getJSONObject("status_info");
        if (statusInfo != null) {
            moderationStatus = stripStatusPrefix(statusInfo.getString("status"));
            canAppeal = Boolean.TRUE.equals(statusInfo.getBoolean("can_appeal"));
        }
        return new BusinessProductCollectionBuilder()
                .id(collection.getString("id"))
                .moderationStatus(moderationStatus)
                .canAppeal(canAppeal)
                .build();
    }

    /**
     * Strips the {@code STATUS_} prefix the create-collection mutation carries on its moderation
     * status token.
     *
     * @param status the raw status token, possibly {@code null}
     * @return the bare status token, or {@code null} when the input is {@code null}
     */
    private static String stripStatusPrefix(String status) {
        if (status == null) {
            return null;
        }
        return status.startsWith("STATUS_") ? status.substring("STATUS_".length()) : status;
    }
}
