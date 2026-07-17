package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdLocation;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdLocationBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the search-local-locations query built by
 * {@link BizAdCreationSearchLocalLocationsFacebookGraphQlRequest} into a list of {@link BusinessAdLocation}.
 *
 * <p>Projects the plural {@code geo_locations_search} root onto {@link BusinessAdLocation} entries,
 * carrying each matched place's targeting key, label, kind, country and region identity, primary city,
 * and map point.
 *
 * @see BizAdCreationSearchLocalLocationsFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationSearchLocalLocationsQuery")
public final class BizAdCreationSearchLocalLocationsFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected matched places.
     */
    private final List<BusinessAdLocation> locations;

    /**
     * Constructs a response wrapping the projected matched places.
     *
     * <p>Reserved for the static parser.
     *
     * @param locations the projected matched places
     */
    private BizAdCreationSearchLocalLocationsFacebookGraphQlResponse(List<BusinessAdLocation> locations) {
        this.locations = locations;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the plural
     * {@code geo_locations_search} root onto {@link BusinessAdLocation} entries.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationSearchLocalLocationsFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var locations = locations(data.getJSONArray("geo_locations_search"));
        return Optional.of(new BizAdCreationSearchLocalLocationsFacebookGraphQlResponse(locations));
    }

    /**
     * Projects the {@code geo_locations_search} array onto a list of {@link BusinessAdLocation}
     * entries.
     *
     * @param arr the {@code geo_locations_search} array, or {@code null}
     * @return the projected places, empty when {@code arr} is {@code null}
     */
    private static List<BusinessAdLocation> locations(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessAdLocation>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var node = arr.getJSONObject(i);
            if (node == null) {
                continue;
            }
            var coordinates = node.getJSONObject("coordinates");
            var latitude = coordinates == null ? null : coordinates.getDouble("latitude");
            var longitude = coordinates == null ? null : coordinates.getDouble("longitude");
            result.add(new BusinessAdLocationBuilder()
                    .key(node.getString("key"))
                    .name(node.getString("name"))
                    .type(node.getString("type"))
                    .countryCode(node.getString("country_code"))
                    .countryName(node.getString("country_name"))
                    .region(node.getString("region"))
                    .primaryCityId(node.getString("primary_city_id"))
                    .latitude(latitude)
                    .longitude(longitude)
                    .build());
        }
        return result;
    }

    /**
     * Returns the projected matched places.
     *
     * @return an unmodifiable view of the projected places; never {@code null}, possibly empty
     */
    public List<BusinessAdLocation> locations() {
        return locations;
    }
}
