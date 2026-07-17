package com.github.auties00.cobalt.wire.graphql.facebook.misc;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.AdEntryPointEntitlement;
import com.github.auties00.cobalt.wire.linked.business.ads.AdEntryPointEntitlementBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the ad-entry-points entitlement query (with localised copy) built by
 * {@link FetchAdEntryPointsConfigurationM1FacebookGraphQlRequest} into a list of {@link AdEntryPointEntitlement}.
 *
 * <p>Reads the plural linked root {@code ctwa_client_entry_point_entitlement} and projects each entry
 * onto the Cobalt domain model: the entry-point or experience identifier, whether it should be
 * surfaced, and the primary and secondary localised copy this richer surface additionally returns.
 *
 * @see FetchAdEntryPointsConfigurationM1FacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebFetchAdEntryPointsConfigurationM1Query")
public final class FetchAdEntryPointsConfigurationM1FacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed entry-point entitlements.
     */
    private final List<AdEntryPointEntitlement> entitlements;

    /**
     * Constructs a response wrapping the parsed entitlement entries.
     *
     * <p>Reserved for the static parser.
     *
     * @param entitlements the parsed entry-point entitlements
     */
    private FetchAdEntryPointsConfigurationM1FacebookGraphQlResponse(List<AdEntryPointEntitlement> entitlements) {
        this.entitlements = entitlements;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the plural linked root {@code ctwa_client_entry_point_entitlement} and projects each
     * entry, including its localised copy, onto an {@link AdEntryPointEntitlement}; the returned
     * {@link Optional} is empty when {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<FetchAdEntryPointsConfigurationM1FacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var entitlements = parseEntitlements(data.getJSONArray("ctwa_client_entry_point_entitlement"));
        return Optional.of(new FetchAdEntryPointsConfigurationM1FacebookGraphQlResponse(entitlements));
    }

    /**
     * Projects the {@code ctwa_client_entry_point_entitlement} array onto a list of
     * {@link AdEntryPointEntitlement}, carrying the localised copy.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<AdEntryPointEntitlement> parseEntitlements(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<AdEntryPointEntitlement>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            var shouldShow = obj.getBoolean("should_show");
            result.add(new AdEntryPointEntitlementBuilder()
                    .entryPointOrExperience(obj.getString("entry_point_or_experience"))
                    .shouldShow(shouldShow != null && shouldShow)
                    .content(obj.getString("content"))
                    .subContent(obj.getString("sub_content"))
                    .build());
        }
        return result;
    }

    /**
     * Returns the parsed entry-point entitlements.
     *
     * @return the parsed entitlements, empty when the Meta graph endpoint returned none
     */
    public List<AdEntryPointEntitlement> entitlements() {
        return entitlements;
    }
}
