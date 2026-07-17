package com.github.auties00.cobalt.wire.graphql.facebook.misc;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.AdBudgetEstimate;
import com.github.auties00.cobalt.wire.linked.business.ads.AdBudgetEstimateBuilder;
import com.github.auties00.cobalt.wire.linked.business.ads.AdBudgetEstimatePoint;
import com.github.auties00.cobalt.wire.linked.business.ads.AdBudgetEstimatePointBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the estimated-daily-reach query built by
 * {@link EstimatedDailyReachFacebookGraphQlRequest} into an {@link AdBudgetEstimate}.
 *
 * <p>Reads the linked {@code lwi.budget_estimate_data_v2.daily_outcomes_curve} array and projects
 * each entry onto an {@link AdBudgetEstimatePoint} (the estimated actions, reach, impressions,
 * spend, and bid plus the lower and upper bounds for actions and reach), wrapping them into a
 * single {@link AdBudgetEstimate} curve.
 *
 * @see EstimatedDailyReachFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebEstimatedDailyReachQuery")
public final class EstimatedDailyReachFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed budget estimate.
     */
    private final AdBudgetEstimate estimate;

    /**
     * Constructs a response wrapping the parsed budget estimate.
     *
     * <p>Reserved for the static parser.
     *
     * @param estimate the parsed budget estimate
     */
    private EstimatedDailyReachFacebookGraphQlResponse(AdBudgetEstimate estimate) {
        this.estimate = estimate;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked {@code lwi.budget_estimate_data_v2.daily_outcomes_curve} array and
     * projects each entry onto an {@link AdBudgetEstimatePoint}; the returned {@link Optional} is
     * empty when {@code data}, the {@code lwi} root, or the {@code budget_estimate_data_v2}
     * sub-object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data}, the {@code lwi} root, or the
     *         {@code budget_estimate_data_v2} sub-object is missing
     */
    public static Optional<EstimatedDailyReachFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("lwi");
        if (root == null) {
            return Optional.empty();
        }

        var budget = root.getJSONObject("budget_estimate_data_v2");
        if (budget == null) {
            return Optional.empty();
        }

        var estimate = new AdBudgetEstimateBuilder()
                .curve(parseCurve(budget.getJSONArray("daily_outcomes_curve")))
                .build();
        return Optional.of(new EstimatedDailyReachFacebookGraphQlResponse(estimate));
    }

    /**
     * Projects the {@code daily_outcomes_curve} array onto a list of {@link AdBudgetEstimatePoint}.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<AdBudgetEstimatePoint> parseCurve(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<AdBudgetEstimatePoint>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            result.add(new AdBudgetEstimatePointBuilder()
                    .actions(obj.getDouble("actions"))
                    .actionsLowerBound(obj.getDouble("actions_lower_bound"))
                    .actionsUpperBound(obj.getDouble("actions_upper_bound"))
                    .bid(obj.getDouble("bid"))
                    .impressions(obj.getDouble("impressions"))
                    .reach(obj.getDouble("reach"))
                    .reachLowerBound(obj.getDouble("reach_lower_bound"))
                    .reachUpperBound(obj.getDouble("reach_upper_bound"))
                    .spend(obj.getDouble("spend"))
                    .build());
        }
        return result;
    }

    /**
     * Returns the parsed budget estimate.
     *
     * @return the parsed {@link AdBudgetEstimate}, never {@code null}
     */
    public AdBudgetEstimate estimate() {
        return estimate;
    }
}
