package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessBoostedComponent;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessBoostedComponentAudienceBuilder;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessBoostedComponentBuilder;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessBoostedComponentPlacementBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the create-boosted-component mutation built by
 * {@link CometCreateBoostedComponentFacebookGraphQlRequest} into a {@link BusinessBoostedComponent}.
 *
 * <p>Projects the {@code create_boosted_component} root onto the {@link BusinessBoostedComponent}
 * model: the created ad's identifier and delivery status, its {@code context_spec} placement, and its
 * {@code spec} budget, duration, objective, audience, creatives, and targeting-editable flag.
 *
 * @see CometCreateBoostedComponentFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "LWICometCreateBoostedComponentMutation")
public final class CometCreateBoostedComponentFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected boosted component, or {@code null} when the relay omitted the field.
     */
    private final BusinessBoostedComponent component;

    /**
     * Constructs a response wrapping the projected boosted component.
     *
     * <p>Reserved for the static parser.
     *
     * @param component the projected boosted component, or {@code null} when the relay omitted the
     *                  field
     */
    private CometCreateBoostedComponentFacebookGraphQlResponse(BusinessBoostedComponent component) {
        this.component = component;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code create_boosted_component} root onto a {@link BusinessBoostedComponent}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<CometCreateBoostedComponentFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var component = component(data.getJSONObject("create_boosted_component"));
        return Optional.of(new CometCreateBoostedComponentFacebookGraphQlResponse(component));
    }

    /**
     * Projects the {@code create_boosted_component} root object onto a {@link BusinessBoostedComponent}.
     *
     * @param node the {@code create_boosted_component} object, or {@code null}
     * @return the projected component, or {@code null} when {@code stanza} is {@code null}
     */
    private static BusinessBoostedComponent component(JSONObject node) {
        if (node == null) {
            return null;
        }

        var spec = node.getJSONObject("spec");
        var placement = placement(node.getJSONObject("context_spec"));
        var audience = spec == null ? null : audience(spec.getJSONObject("audience"));
        var builder = new BusinessBoostedComponentBuilder()
                .id(node.getString("id"))
                .placement(placement)
                .audience(audience)
                .creativeBodies(creativeBodies(spec))
                .targetingEditable(spec != null && targetingEditable(spec.getJSONObject("ad_target_spec")));
        if (spec != null) {
            builder.status(spec.getString("boosting_status"))
                    .budget(budget(spec.getJSONObject("budget")))
                    .budgetKind(spec.getString("budget_type"))
                    .durationInDays(spec.getLong("duration_in_days"))
                    .objective(objective(spec.getJSONObject("objective_spec")));
        }
        return builder.build();
    }

    /**
     * Projects the {@code context_spec} object onto a {@link BusinessBoostedComponent.Placement}.
     *
     * @param node the {@code context_spec} object, or {@code null}
     * @return the projected placement, or {@code null} when {@code stanza} is {@code null}
     */
    private static BusinessBoostedComponent.Placement placement(JSONObject node) {
        if (node == null) {
            return null;
        }

        return new BusinessBoostedComponentPlacementBuilder()
                .promotedSurface(node.getString("product"))
                .targetId(node.getString("target_id"))
                .pageId(node.getString("page_id"))
                .build();
    }

    /**
     * Projects the {@code audience} object onto a {@link BusinessBoostedComponent.Audience}.
     *
     * @param node the {@code audience} object, or {@code null}
     * @return the projected audience, or {@code null} when {@code stanza} is {@code null}
     */
    private static BusinessBoostedComponent.Audience audience(JSONObject node) {
        if (node == null) {
            return null;
        }

        return new BusinessBoostedComponentAudienceBuilder()
                .name(node.getString("name"))
                .id(node.getString("id"))
                .build();
    }

    /**
     * Reads the {@code offset_amount} of a {@code CurrencyQuantity} budget object.
     *
     * @param node the {@code budget} object, or {@code null}
     * @return the budget amount string, or {@code null} when {@code stanza} is {@code null}
     */
    private static String budget(JSONObject node) {
        return node == null ? null : node.getString("offset_amount");
    }

    /**
     * Reads the {@code title} of an {@code objective_spec} object.
     *
     * @param node the {@code objective_spec} object, or {@code null}
     * @return the objective title, or {@code null} when {@code stanza} is {@code null}
     */
    private static String objective(JSONObject node) {
        return node == null ? null : node.getString("title");
    }

    /**
     * Reads the {@code client_can_edit} flag of an {@code ad_target_spec} object.
     *
     * @param node the {@code ad_target_spec} object, or {@code null}
     * @return {@code true} when the targeting is editable, {@code false} otherwise
     */
    private static boolean targetingEditable(JSONObject node) {
        return node != null && Boolean.TRUE.equals(node.getBoolean("client_can_edit"));
    }

    /**
     * Collects the creative {@code body} text of each {@code adgroup_spec} entry.
     *
     * @param spec the {@code spec} object, or {@code null}
     * @return the creative body texts in array order, empty when none are present
     */
    private static List<String> creativeBodies(JSONObject spec) {
        if (spec == null) {
            return List.of();
        }

        var adgroups = spec.getJSONArray("adgroup_spec");
        if (adgroups == null) {
            return List.of();
        }

        var result = new ArrayList<String>(adgroups.size());
        for (var i = 0; i < adgroups.size(); i++) {
            var adgroup = adgroups.getJSONObject(i);
            if (adgroup == null) {
                continue;
            }

            var creative = adgroup.getJSONObject("creative");
            var body = creative == null ? null : creative.getString("body");
            if (body != null) {
                result.add(body);
            }
        }
        return result;
    }

    /**
     * Returns the projected boosted component.
     *
     * @return the projected {@link BusinessBoostedComponent}, or empty when the relay omitted the field
     */
    public Optional<BusinessBoostedComponent> component() {
        return Optional.ofNullable(component);
    }
}
