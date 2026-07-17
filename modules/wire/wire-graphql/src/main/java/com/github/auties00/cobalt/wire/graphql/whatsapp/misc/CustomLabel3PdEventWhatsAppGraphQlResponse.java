package com.github.auties00.cobalt.wire.graphql.whatsapp.misc;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ctwa.CtwaConversionEvent;
import com.github.auties00.cobalt.wire.linked.business.ctwa.CtwaConversionEventBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the custom-label-to-conversion-event query built by
 * {@link CustomLabel3PdEventWhatsAppGraphQlRequest} into a list of {@link CtwaConversionEvent}.
 *
 * <p>Reads the plural linked {@code xwa_get_3pd_event} field and projects each entry onto the Cobalt
 * domain model: the custom label paired with its Click-to-WhatsApp conversion type, conversion
 * subtype, and an opaque conversion-metadata blob.
 *
 * @see CustomLabel3PdEventWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebCustomLabel3pdEventQuery")
public final class CustomLabel3PdEventWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the parsed conversion events.
     */
    private final List<CtwaConversionEvent> events;

    /**
     * Constructs a response wrapping the parsed event list.
     *
     * <p>Reserved for the static parser.
     *
     * @param events the parsed conversion events
     */
    private CustomLabel3PdEventWhatsAppGraphQlResponse(List<CtwaConversionEvent> events) {
        this.events = events;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the plural linked root {@code xwa_get_3pd_event} and projects each entry onto a
     * {@link CtwaConversionEvent}; the returned {@link Optional} is empty when {@code data} is
     * {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<CustomLabel3PdEventWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var events = parseEvents(data.getJSONArray("xwa_get_3pd_event"));
        return Optional.of(new CustomLabel3PdEventWhatsAppGraphQlResponse(events));
    }

    /**
     * Projects the {@code xwa_get_3pd_event} array onto a list of {@link CtwaConversionEvent}.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<CtwaConversionEvent> parseEvents(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<CtwaConversionEvent>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            result.add(new CtwaConversionEventBuilder()
                    .customLabel(obj.getString("custom_label"))
                    .conversionType(obj.getString("ctwa_3pd_conversion_type"))
                    .conversionSubtype(obj.getString("ctwa_3pd_conversion_subtype"))
                    .conversionMetadata(obj.getString("ctwa_3pd_conversion_metadata"))
                    .build());
        }
        return result;
    }

    /**
     * Returns the parsed conversion events.
     *
     * @return the parsed events, empty when the graph.whatsapp.com endpoint returned none
     */
    public List<CtwaConversionEvent> events() {
        return events;
    }
}
