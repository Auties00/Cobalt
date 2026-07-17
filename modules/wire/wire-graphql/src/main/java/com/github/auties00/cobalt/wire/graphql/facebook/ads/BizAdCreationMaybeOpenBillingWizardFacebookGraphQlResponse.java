package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdPaymentSection;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdPaymentSectionBuilder;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdPaymentSectionRequiredSetupBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the billing-setup-required query built by
 * {@link BizAdCreationMaybeOpenBillingWizardFacebookGraphQlRequest} into a {@link BusinessAdPaymentSection}.
 *
 * <p>Projects the {@code billable_account_by_asset_id} root onto the {@link BusinessAdPaymentSection}
 * model: the account identifier and the outstanding billing-setup step from
 * {@code billing_info.required_action}. This query carries only the setup step, so the descriptive
 * payment-row fields of the model are left empty. A non-empty
 * {@link BusinessAdPaymentSection#requiredSetup()} signals that billing setup must be completed before
 * the ad can be paid for.
 *
 * @see BizAdCreationMaybeOpenBillingWizardFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationMaybeOpenBillingWizardQuery")
public final class BizAdCreationMaybeOpenBillingWizardFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected payment section, or {@code null} when the relay omitted the field.
     */
    private final BusinessAdPaymentSection paymentSection;

    /**
     * Constructs a response wrapping the projected payment section.
     *
     * <p>Reserved for the static parser.
     *
     * @param paymentSection the projected payment section, or {@code null} when the relay omitted the
     *                       field
     */
    private BizAdCreationMaybeOpenBillingWizardFacebookGraphQlResponse(BusinessAdPaymentSection paymentSection) {
        this.paymentSection = paymentSection;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code billable_account_by_asset_id} root onto a {@link BusinessAdPaymentSection}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationMaybeOpenBillingWizardFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var account = data.getJSONObject("billable_account_by_asset_id");
        if (account == null) {
            return Optional.of(new BizAdCreationMaybeOpenBillingWizardFacebookGraphQlResponse(null));
        }

        var billingInfo = account.getJSONObject("billing_info");
        var requiredAction = billingInfo == null ? null : billingInfo.getJSONObject("required_action");
        var paymentSection = new BusinessAdPaymentSectionBuilder()
                .accountId(account.getString("id"))
                .requiredSetup(requiredSetup(requiredAction))
                .build();
        return Optional.of(new BizAdCreationMaybeOpenBillingWizardFacebookGraphQlResponse(paymentSection));
    }

    /**
     * Projects a {@code required_action} object onto a {@link BusinessAdPaymentSection.RequiredSetup}.
     *
     * <p>The {@code action} row carries the unaliased wizard wire keys {@code wizard_name} and
     * {@code wizard_props_json}, which are mapped onto the model's setup-name and setup-inputs fields.
     *
     * @param node the {@code required_action} object, or {@code null}
     * @return the projected setup step, or {@code null} when {@code stanza} or its {@code action} is
     *         {@code null}
     */
    private static BusinessAdPaymentSection.RequiredSetup requiredSetup(JSONObject node) {
        if (node == null) {
            return null;
        }

        var action = node.getJSONObject("action");
        if (action == null) {
            return null;
        }

        return new BusinessAdPaymentSectionRequiredSetupBuilder()
                .setupName(action.getString("wizard_name"))
                .setupPropertiesJson(action.getString("wizard_props_json"))
                .build();
    }

    /**
     * Returns the projected payment section.
     *
     * @return the projected {@link BusinessAdPaymentSection}, or empty when the relay omitted the field
     */
    public Optional<BusinessAdPaymentSection> paymentSection() {
        return Optional.ofNullable(paymentSection);
    }
}
