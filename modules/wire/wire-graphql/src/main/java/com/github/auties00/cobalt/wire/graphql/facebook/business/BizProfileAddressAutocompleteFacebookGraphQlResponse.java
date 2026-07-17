package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessAddressSuggestion;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessAddressSuggestionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the address-autocomplete query built by
 * {@link BizProfileAddressAutocompleteFacebookGraphQlRequest} into a list of {@link BusinessAddressSuggestion}.
 *
 * <p>Reads the linked {@code whatsapp_maps_typeahead} field and projects its ranked {@code items} list,
 * flattening each match's place id, geo coordinates, structured address, and display title onto the
 * Cobalt domain model.
 *
 * @see BizProfileAddressAutocompleteFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizProfileAddressAutocompleteQuery")
public final class BizProfileAddressAutocompleteFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed address suggestions.
     */
    private final List<BusinessAddressSuggestion> suggestions;

    /**
     * Constructs a response wrapping the parsed address suggestions.
     *
     * <p>Reserved for the static parser.
     *
     * @param suggestions the parsed address suggestions
     */
    private BizProfileAddressAutocompleteFacebookGraphQlResponse(List<BusinessAddressSuggestion> suggestions) {
        this.suggestions = suggestions;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code whatsapp_maps_typeahead} and projects its {@code items} list onto
     * {@link BusinessAddressSuggestion} entries; the returned {@link Optional} is empty when
     * {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebBizProfileAddressAutocompleteQuery", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<BizProfileAddressAutocompleteFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var root = data.getJSONObject("whatsapp_maps_typeahead");
        var suggestions = root == null ? List.<BusinessAddressSuggestion>of() : parseItems(root.getJSONArray("items"));
        return Optional.of(new BizProfileAddressAutocompleteFacebookGraphQlResponse(suggestions));
    }

    /**
     * Projects an {@code items} JSON array onto a list of {@link BusinessAddressSuggestion}.
     *
     * @param array the JSON array to parse, or {@code null}
     * @return the projected suggestions, empty when {@code array} is {@code null}
     */
    private static List<BusinessAddressSuggestion> parseItems(JSONArray array) {
        if (array == null) {
            return List.of();
        }
        var result = new ArrayList<BusinessAddressSuggestion>(array.size());
        for (var i = 0; i < array.size(); i++) {
            var entry = array.getJSONObject(i);
            if (entry == null) {
                continue;
            }
            var location = entry.getJSONObject("location");
            var address = entry.getJSONObject("address");
            result.add(new BusinessAddressSuggestionBuilder()
                    .placeId(entry.getString("id"))
                    .title(entry.getString("title"))
                    .latitude(location == null ? null : location.getString("latitude"))
                    .longitude(location == null ? null : location.getString("longitude"))
                    .streetAddress(address == null ? null : address.getString("streetaddress"))
                    .city(address == null ? null : address.getString("city"))
                    .stateOrProvince(address == null ? null : address.getString("stateprovince"))
                    .postalCode(address == null ? null : address.getString("postalcode"))
                    .country(address == null ? null : address.getString("country"))
                    .build());
        }
        return result;
    }

    /**
     * Returns the parsed address suggestions.
     *
     * <p>Each {@link BusinessAddressSuggestion} flattens one ranked place match into its place id,
     * coordinates, structured address, and display title.
     *
     * @return the parsed suggestions, empty when the Meta graph endpoint returned none
     */
    public List<BusinessAddressSuggestion> suggestions() {
        return suggestions;
    }
}
