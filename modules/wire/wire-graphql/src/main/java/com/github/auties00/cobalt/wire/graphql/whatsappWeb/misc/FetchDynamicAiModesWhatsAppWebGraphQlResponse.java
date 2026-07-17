package com.github.auties00.cobalt.wire.graphql.whatsappWeb.misc;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ai.MetaAiMode;
import com.github.auties00.cobalt.wire.linked.business.ai.MetaAiModeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the WhatsApp Web GraphQL response of the dynamic Meta AI modes query built by
 * {@link FetchDynamicAiModesWhatsAppWebGraphQlRequest} into a list of {@link MetaAiMode}.
 *
 * <p>Reads the plural linked root {@code xfb_meta_ai_modes} and projects each entry onto the Cobalt
 * domain model: the numeric mode id, the mode type discriminator, the experimental flag, and the
 * localised title and subtitle the AI mode selector renders.
 *
 * @see FetchDynamicAiModesWhatsAppWebGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebFetchDynamicAIModesQuery")
public final class FetchDynamicAiModesWhatsAppWebGraphQlResponse implements WhatsAppWebGraphQlOperation.Response {
    /**
     * Holds the parsed Meta AI modes.
     */
    private final List<MetaAiMode> modes;

    /**
     * Constructs a response wrapping the parsed mode entries.
     *
     * <p>Reserved for the static parser.
     *
     * @param modes the parsed Meta AI modes
     */
    private FetchDynamicAiModesWhatsAppWebGraphQlResponse(List<MetaAiMode> modes) {
        this.modes = modes;
    }

    /**
     * Parses the WhatsApp Web GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the plural linked root {@code xfb_meta_ai_modes} and projects each entry onto a
     * {@link MetaAiMode}; the returned {@link Optional} is empty when {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppWebGraphQlClient#send(WhatsAppWebGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<FetchDynamicAiModesWhatsAppWebGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var modes = parseModes(data.getJSONArray("xfb_meta_ai_modes"));
        return Optional.of(new FetchDynamicAiModesWhatsAppWebGraphQlResponse(modes));
    }

    /**
     * Projects the {@code xfb_meta_ai_modes} array onto a list of {@link MetaAiMode}.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<MetaAiMode> parseModes(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<MetaAiMode>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            var experimental = obj.getBoolean("is_experimental");
            result.add(new MetaAiModeBuilder()
                    .modeId(obj.getLong("mode_id"))
                    .type(obj.getString("type"))
                    .experimental(experimental != null && experimental)
                    .title(obj.getString("title"))
                    .subtitle(obj.getString("subtitle"))
                    .build());
        }
        return result;
    }

    /**
     * Returns the parsed Meta AI modes.
     *
     * @return the parsed modes, empty when the relay returned none
     */
    public List<MetaAiMode> modes() {
        return modes;
    }
}
