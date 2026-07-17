package com.github.auties00.cobalt.wire.graphql.whatsapp.misc;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.marketing.BrandPhoneNumberMapping;
import com.github.auties00.cobalt.wire.linked.business.marketing.BrandPhoneNumberMappingBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the brand-id resolution query built by
 * {@link GetNumbersForBrandIdsJobWhatsAppGraphQlRequest} into a list of {@link BrandPhoneNumberMapping}.
 *
 * <p>Reads the linked root {@code xwa_get_numbers_for_brand_ids -> brand_ids_data} and projects each
 * per-brand record onto the Cobalt domain model: the echoed brand id, an optional error marking a
 * brand that failed to resolve, and the resolved phone numbers and LIDs.
 *
 * @see GetNumbersForBrandIdsJobWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebGetNumbersForBrandIdsJobQuery")
public final class GetNumbersForBrandIdsJobWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the parsed per-brand mappings.
     */
    private final List<BrandPhoneNumberMapping> mappings;

    /**
     * Constructs a response wrapping the parsed mapping list.
     *
     * <p>Reserved for the static parser.
     *
     * @param mappings the parsed per-brand mappings
     */
    private GetNumbersForBrandIdsJobWhatsAppGraphQlResponse(List<BrandPhoneNumberMapping> mappings) {
        this.mappings = mappings;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xwa_get_numbers_for_brand_ids -> brand_ids_data} and projects each
     * record onto a {@link BrandPhoneNumberMapping}; the returned {@link Optional} is empty when
     * {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<GetNumbersForBrandIdsJobWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("xwa_get_numbers_for_brand_ids");
        var brandIdsData = node == null ? null : node.getJSONArray("brand_ids_data");
        return Optional.of(new GetNumbersForBrandIdsJobWhatsAppGraphQlResponse(parseMappings(brandIdsData)));
    }

    /**
     * Projects the {@code brand_ids_data} array onto a list of {@link BrandPhoneNumberMapping}.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<BrandPhoneNumberMapping> parseMappings(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BrandPhoneNumberMapping>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            result.add(new BrandPhoneNumberMappingBuilder()
                    .brandId(obj.getString("brand_id"))
                    .error(obj.getString("error"))
                    .phoneNumbers(parseStrings(obj.getJSONArray("phone_numbers")))
                    .lids(parseStrings(obj.getJSONArray("lids")))
                    .build());
        }
        return result;
    }

    /**
     * Projects a JSON array of string tokens into an unmodifiable string list, skipping {@code null}
     * elements.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<String> parseStrings(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<String>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var value = arr.getString(i);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * Returns the parsed per-brand mappings.
     *
     * @return the parsed mappings, empty when the graph.whatsapp.com endpoint returned none
     */
    public List<BrandPhoneNumberMapping> mappings() {
        return mappings;
    }
}
