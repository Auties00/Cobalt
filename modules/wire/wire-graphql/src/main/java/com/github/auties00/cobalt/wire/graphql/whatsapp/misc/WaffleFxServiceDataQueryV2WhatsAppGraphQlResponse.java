package com.github.auties00.cobalt.wire.graphql.whatsapp.misc;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingAdditionalFeatureSet;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingAdditionalFeatureSetBuilder;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingDestinationAccount;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingDestinationAccountBuilder;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingLinkEligibility;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingLinkEligibilityBuilder;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingServiceData;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingServiceDataBuilder;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingSurfaceSetting;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingSurfaceSettingBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the Meta cross-posting and account-linking service-data mutation
 * built by {@link WaffleFxServiceDataQueryV2WhatsAppGraphQlRequest} into a
 * {@link CrossPostingServiceData}.
 *
 * <p>Reads the linked root {@code waffle_fx_service_data} and projects its {@code services} child
 * tree (linked destination accounts and per-surface settings, additional-feature-set eligibility,
 * and per-destination link-eligibility flags) onto the Cobalt domain model.
 *
 * @see WaffleFxServiceDataQueryV2WhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebWaffleFXServiceDataQueryV2Mutation")
public final class WaffleFxServiceDataQueryV2WhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the parsed service-data view.
     */
    private final CrossPostingServiceData serviceData;

    /**
     * Constructs a response wrapping the parsed service-data view.
     *
     * <p>Reserved for the static parser.
     *
     * @param serviceData the parsed service-data view, or {@code null} when the graph.whatsapp.com endpoint omitted the
     *                    field
     */
    private WaffleFxServiceDataQueryV2WhatsAppGraphQlResponse(CrossPostingServiceData serviceData) {
        this.serviceData = serviceData;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code waffle_fx_service_data} and projects its {@code services}
     * child onto a {@link CrossPostingServiceData}; the returned {@link Optional} is empty when
     * {@code data}, the root, or the services object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data}, the root, or the services object is
     *         missing
     */
    public static Optional<WaffleFxServiceDataQueryV2WhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("waffle_fx_service_data");
        if (root == null) {
            return Optional.empty();
        }

        var services = root.getJSONObject("services");
        if (services == null) {
            return Optional.empty();
        }

        var serviceData = new CrossPostingServiceDataBuilder()
                .destinationAccounts(parseDestinationAccounts(services.getJSONArray("waffle_sxs")))
                .additionalFeatureSet(parseAdditionalFeatureSet(services.getJSONObject("waffle_afs")))
                .linkEligibility(parseLinkEligibility(services.getJSONObject("foa_to_wa_link_eligibility")))
                .build();
        return Optional.of(new WaffleFxServiceDataQueryV2WhatsAppGraphQlResponse(serviceData));
    }

    /**
     * Projects the {@code waffle_sxs} array onto a list of {@link CrossPostingDestinationAccount}.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<CrossPostingDestinationAccount> parseDestinationAccounts(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<CrossPostingDestinationAccount>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            result.add(new CrossPostingDestinationAccountBuilder()
                    .destinationId(obj.getString("waffle_di"))
                    .destinationKind(obj.getString("waffle_da"))
                    .surfaceSettings(parseSurfaceSettings(obj.getJSONArray("waffle_xss")))
                    .build());
        }
        return result;
    }

    /**
     * Projects the {@code waffle_xss} array onto a list of {@link CrossPostingSurfaceSetting}.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<CrossPostingSurfaceSetting> parseSurfaceSettings(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<CrossPostingSurfaceSetting>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            result.add(new CrossPostingSurfaceSettingBuilder()
                    .eligibility(obj.getString("waffle_iaxe"))
                    .surface(obj.getString("waffle_x_surface"))
                    .build());
        }
        return result;
    }

    /**
     * Projects the {@code waffle_afs} object onto a {@link CrossPostingAdditionalFeatureSet}.
     *
     * @param obj the JSON object to project
     * @return the projected feature-set, or {@code null} when {@code obj} is {@code null}
     */
    private static CrossPostingAdditionalFeatureSet parseAdditionalFeatureSet(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        return new CrossPostingAdditionalFeatureSetBuilder()
                .eligibility(obj.getString("waffle_wes"))
                .build();
    }

    /**
     * Projects the {@code foa_to_wa_link_eligibility} object onto a
     * {@link CrossPostingLinkEligibility}.
     *
     * @param obj the JSON object to project
     * @return the projected link-eligibility, or {@code null} when {@code obj} is {@code null}
     */
    private static CrossPostingLinkEligibility parseLinkEligibility(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        var linkedFb = obj.getBoolean("is_eligible_to_link_to_linked_fb");
        var linkedIg = obj.getBoolean("is_eligible_to_link_to_linked_ig");
        var linkedRl = obj.getBoolean("is_eligible_to_link_to_linked_rl");
        var unlinkedFb = obj.getBoolean("is_eligible_to_link_to_unlinked_fb");
        var unlinkedIg = obj.getBoolean("is_eligible_to_link_to_unlinked_ig");
        var unlinkedRl = obj.getBoolean("is_eligible_to_link_to_unlinked_rl");
        return new CrossPostingLinkEligibilityBuilder()
                .linkedFacebook(linkedFb != null && linkedFb)
                .linkedInstagram(linkedIg != null && linkedIg)
                .linkedReels(linkedRl != null && linkedRl)
                .unlinkedFacebook(unlinkedFb != null && unlinkedFb)
                .unlinkedInstagram(unlinkedIg != null && unlinkedIg)
                .unlinkedReels(unlinkedRl != null && unlinkedRl)
                .build();
    }

    /**
     * Returns the parsed service-data view.
     *
     * @return the parsed {@link CrossPostingServiceData}, never {@code null}
     */
    public CrossPostingServiceData serviceData() {
        return serviceData;
    }
}
