package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdBudgetOptions;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdBudgetOptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the budget-options query built by
 * {@link BizAdCreationAdAccountUpdate_BudgetFacebookGraphQlRequest} into a {@link BusinessAdBudgetOptions}.
 *
 * <p>Projects the {@code lwi.boostedComponent} sub-tree onto the {@link BusinessAdBudgetOptions}
 * model: the currently selected budget, the selectable budget amounts, and the minimum daily budget,
 * each a {@code CurrencyQuantity} amount in the billing currency's minor units.
 *
 * @see BizAdCreationAdAccountUpdate_BudgetFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationAdAccountUpdate_BudgetQuery")
public final class BizAdCreationAdAccountUpdate_BudgetFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected budget options, or {@code null} when the relay omitted the field.
     */
    private final BusinessAdBudgetOptions budgetOptions;

    /**
     * Constructs a response wrapping the projected budget options.
     *
     * <p>Reserved for the static parser.
     *
     * @param budgetOptions the projected budget options, or {@code null} when the relay omitted the
     *                      field
     */
    private BizAdCreationAdAccountUpdate_BudgetFacebookGraphQlResponse(BusinessAdBudgetOptions budgetOptions) {
        this.budgetOptions = budgetOptions;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code lwi.boostedComponent} sub-tree onto a {@link BusinessAdBudgetOptions}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationAdAccountUpdate_BudgetFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var lwi = data.getJSONObject("lwi");
        var boostedComponent = lwi == null ? null : lwi.getJSONObject("boostedComponent");
        if (boostedComponent == null) {
            return Optional.of(new BizAdCreationAdAccountUpdate_BudgetFacebookGraphQlResponse(null));
        }

        var spec = boostedComponent.getJSONObject("spec");
        var options = boostedComponent.getJSONObject("boostedComponentOptions");
        var constraints = boostedComponent.getJSONObject("constraints");
        var budgetOptions = new BusinessAdBudgetOptionsBuilder()
                .currentBudget(spec == null ? null : amount(spec.getJSONObject("budget")))
                .amountOptions(options == null ? List.of() : amounts(options))
                .minimumDailyBudget(constraints == null ? null : amount(constraints.getJSONObject("minDailyBudget")))
                .build();
        return Optional.of(new BizAdCreationAdAccountUpdate_BudgetFacebookGraphQlResponse(budgetOptions));
    }

    /**
     * Reads the {@code offsetAmount} of a {@code CurrencyQuantity} object.
     *
     * @param node the currency-quantity object, or {@code null}
     * @return the amount string, or {@code null} when {@code stanza} is {@code null}
     */
    private static String amount(JSONObject node) {
        return node == null ? null : node.getString("offsetAmount");
    }

    /**
     * Reads the {@code offsetAmount} of each {@code budgetOptions} currency-quantity entry.
     *
     * @param options the {@code boostedComponentOptions} object holding the {@code budgetOptions} array
     * @return the amount strings in array order, empty when none are present
     */
    private static List<String> amounts(JSONObject options) {
        var array = options.getJSONArray("budgetOptions");
        if (array == null) {
            return List.of();
        }

        var result = new ArrayList<String>(array.size());
        for (var i = 0; i < array.size(); i++) {
            var amount = amount(array.getJSONObject(i));
            if (amount != null) {
                result.add(amount);
            }
        }
        return result;
    }

    /**
     * Returns the projected budget options.
     *
     * @return the projected {@link BusinessAdBudgetOptions}, or empty when the relay omitted the field
     */
    public Optional<BusinessAdBudgetOptions> budgetOptions() {
        return Optional.ofNullable(budgetOptions);
    }
}
