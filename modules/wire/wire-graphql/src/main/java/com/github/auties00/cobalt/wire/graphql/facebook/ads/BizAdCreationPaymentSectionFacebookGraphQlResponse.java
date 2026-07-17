package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdPaymentSection;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdPaymentSectionBuilder;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdPaymentSectionRequiredSetupBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the payment-section query built by
 * {@link BizAdCreationPaymentSectionFacebookGraphQlRequest} into a {@link BusinessAdPaymentSection}.
 *
 * <p>Projects the {@code billable_account_by_asset_id} root onto the {@link BusinessAdPaymentSection}
 * model: the account identifier and the descriptive payment row (label, description, payment-method
 * logos) from {@code billing_info.payment_section_details}, plus the outstanding billing-setup step
 * from {@code billing_info.required_action}.
 *
 * @see BizAdCreationPaymentSectionFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationPaymentSectionQuery")
public final class BizAdCreationPaymentSectionFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
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
    private BizAdCreationPaymentSectionFacebookGraphQlResponse(BusinessAdPaymentSection paymentSection) {
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
    public static Optional<BizAdCreationPaymentSectionFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var account = data.getJSONObject("billable_account_by_asset_id");
        if (account == null) {
            return Optional.of(new BizAdCreationPaymentSectionFacebookGraphQlResponse(null));
        }

        var billingInfo = account.getJSONObject("billing_info");
        var details = billingInfo == null ? null : billingInfo.getJSONObject("payment_section_details");
        var requiredAction = billingInfo == null ? null : billingInfo.getJSONObject("required_action");
        var paymentSection = new BusinessAdPaymentSectionBuilder()
                .accountId(account.getString("id"))
                .label(details == null ? null : details.getString("label"))
                .description(details == null ? null : details.getString("description"))
                .paymentMethodLogos(details == null ? List.of() : logos(details))
                .requiredSetup(requiredSetup(requiredAction))
                .build();
        return Optional.of(new BizAdCreationPaymentSectionFacebookGraphQlResponse(paymentSection));
    }

    /**
     * Reads the {@code uri} of each {@code logos} entry of a {@code payment_section_details} object.
     *
     * @param details the {@code payment_section_details} object
     * @return the logo locations in array order, empty when none are present
     */
    private static List<String> logos(JSONObject details) {
        var array = details.getJSONArray("logos");
        if (array == null) {
            return List.of();
        }

        var result = new ArrayList<String>(array.size());
        for (var i = 0; i < array.size(); i++) {
            var logo = array.getJSONObject(i);
            var uri = logo == null ? null : logo.getString("uri");
            if (uri != null) {
                result.add(uri);
            }
        }
        return result;
    }

    /**
     * Projects a {@code required_action} object onto a {@link BusinessAdPaymentSection.RequiredSetup}.
     *
     * <p>The unaliased wizard wire keys {@code wizard_name} and {@code wizard_props_json} carried by
     * the {@code required_action.action} row are mapped onto the model's setup-name and setup-inputs
     * fields, and the {@code message.headline}/{@code message.body} onto its explanatory message.
     *
     * @param node the {@code required_action} object, or {@code null}
     * @return the projected setup step, or {@code null} when {@code stanza} is {@code null}
     */
    private static BusinessAdPaymentSection.RequiredSetup requiredSetup(JSONObject node) {
        if (node == null) {
            return null;
        }

        var action = node.getJSONObject("action");
        var message = node.getJSONObject("message");
        return new BusinessAdPaymentSectionRequiredSetupBuilder()
                .label(action == null ? null : action.getString("label"))
                .setupName(action == null ? null : action.getString("wizard_name"))
                .setupPropertiesJson(action == null ? null : action.getString("wizard_props_json"))
                .messageHeadline(message == null ? null : message.getString("headline"))
                .messageBody(message == null ? null : message.getString("body"))
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
