package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdCreationSummary;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdCreationSummaryBuilder;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdCreationSummaryTaxLineBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the summary-content query built by
 * {@link BizAdCreationSummaryContentFacebookGraphQlRequest} into a {@link BusinessAdCreationSummary}.
 *
 * <p>Projects the {@code billable_account_by_asset_id} root onto the {@link BusinessAdCreationSummary}
 * model: the billable account identifier, the estimated per-tax line items, and the estimated grand
 * total shown on the ad-creation review step.
 *
 * @see BizAdCreationSummaryContentFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationSummaryContentQuery")
public final class BizAdCreationSummaryContentFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected creation summary, or {@code null} when the relay omitted the root.
     */
    private final BusinessAdCreationSummary summary;

    /**
     * Constructs a response wrapping the projected creation summary.
     *
     * <p>Reserved for the static parser.
     *
     * @param summary the projected creation summary, or {@code null} when the relay omitted the root
     */
    private BizAdCreationSummaryContentFacebookGraphQlResponse(BusinessAdCreationSummary summary) {
        this.summary = summary;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code billable_account_by_asset_id} root onto a {@link BusinessAdCreationSummary}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationSummaryContentFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var account = data.getJSONObject("billable_account_by_asset_id");
        if (account == null) {
            return Optional.of(new BizAdCreationSummaryContentFacebookGraphQlResponse(null));
        }

        List<BusinessAdCreationSummary.TaxLine> taxes = List.of();
        String estimatedTotal = null;
        var billingInfo = account.getJSONObject("billing_info");
        if (billingInfo != null) {
            var estimatedTax = billingInfo.getJSONObject("estimated_tax");
            if (estimatedTax != null) {
                taxes = taxes(estimatedTax.getJSONArray("taxes"));
                var total = estimatedTax.getJSONObject("total");
                if (total != null) {
                    estimatedTotal = total.getString("formattedAmount");
                }
            }
        }

        var summary = new BusinessAdCreationSummaryBuilder()
                .billableAccountId(account.getString("id"))
                .taxes(taxes)
                .estimatedTotal(estimatedTotal)
                .build();
        return Optional.of(new BizAdCreationSummaryContentFacebookGraphQlResponse(summary));
    }

    /**
     * Projects the {@code taxes} array onto a list of {@link BusinessAdCreationSummary.TaxLine}
     * entries, reading each entry's {@code taxName} and {@code taxAmount.formattedAmount}.
     *
     * @param arr the {@code taxes} array, or {@code null}
     * @return the projected tax line items, empty when {@code arr} is {@code null}
     */
    private static List<BusinessAdCreationSummary.TaxLine> taxes(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessAdCreationSummary.TaxLine>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var node = arr.getJSONObject(i);
            if (node == null) {
                continue;
            }
            var taxAmount = node.getJSONObject("taxAmount");
            var amount = taxAmount == null ? null : taxAmount.getString("formattedAmount");
            result.add(new BusinessAdCreationSummaryTaxLineBuilder()
                    .name(node.getString("taxName"))
                    .amount(amount)
                    .build());
        }
        return result;
    }

    /**
     * Returns the projected creation summary.
     *
     * @return the projected {@link BusinessAdCreationSummary}, or empty when the relay omitted the root
     */
    public Optional<BusinessAdCreationSummary> summary() {
        return Optional.ofNullable(summary);
    }
}
