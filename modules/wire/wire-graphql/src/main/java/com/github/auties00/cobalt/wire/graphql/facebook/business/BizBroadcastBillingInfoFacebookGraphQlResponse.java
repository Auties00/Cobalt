package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastBillingAccount;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastBillingAccountBuilder;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastBillingAccountBillingInfoBuilder;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastBillingAccountBillingInfoActionBuilder;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastBillingAccountCurrencyAmountBuilder;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastBillingAccountEstimatedTaxBuilder;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastBillingAccountImageBuilder;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastBillingAccountMessageBuilder;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastBillingAccountPaymentSectionDetailsBuilder;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastBillingAccountRequiredActionBuilder;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastBillingAccountTaxLineItemBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the broadcast-billing-info query built by
 * {@link BizBroadcastBillingInfoFacebookGraphQlRequest} into a {@link BusinessBroadcastBillingAccount}.
 *
 * <p>Reads the linked {@code billable_account_by_asset_id} root and projects the billable account
 * (type marker, identifier, and the nested billing-info sub-tree covering payment mode, estimated
 * taxes, payment-section presentation, and any required action) onto the Cobalt domain model.
 *
 * @see BizBroadcastBillingInfoFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebBizBroadcastBillingInfoQuery")
public final class BizBroadcastBillingInfoFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed billable account.
     */
    private final BusinessBroadcastBillingAccount billableAccount;

    /**
     * Constructs a response wrapping the parsed billable account.
     *
     * <p>Reserved for the static parser.
     *
     * @param billableAccount the parsed billable account, or {@code null} when the Meta graph endpoint omitted the
     *                        field
     */
    private BizBroadcastBillingInfoFacebookGraphQlResponse(BusinessBroadcastBillingAccount billableAccount) {
        this.billableAccount = billableAccount;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code billable_account_by_asset_id} and projects it onto a
     * {@link BusinessBroadcastBillingAccount}; the returned {@link Optional} is empty when {@code data}
     * is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizBroadcastBillingInfoFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var billableAccount = readBillableAccount(data.getJSONObject("billable_account_by_asset_id"));
        return Optional.of(new BizBroadcastBillingInfoFacebookGraphQlResponse(billableAccount));
    }

    /**
     * Returns the parsed billable account.
     *
     * @return the parsed {@link BusinessBroadcastBillingAccount}, or empty when the Meta graph endpoint omitted the
     *         field
     */
    public Optional<BusinessBroadcastBillingAccount> billableAccount() {
        return Optional.ofNullable(billableAccount);
    }

    /**
     * Projects the {@code billable_account_by_asset_id} sub-object onto a
     * {@link BusinessBroadcastBillingAccount}.
     *
     * @param node the JSON object to read, possibly {@code null}
     * @return the projected billable account, or {@code null} when {@code stanza} is {@code null}
     */
    private static BusinessBroadcastBillingAccount readBillableAccount(JSONObject node) {
        if (node == null) {
            return null;
        }

        return new BusinessBroadcastBillingAccountBuilder()
                .typename(node.getString("__typename"))
                .id(node.getString("id"))
                .billingInfo(readBillingInfo(node.getJSONObject("billing_info")))
                .build();
    }

    /**
     * Projects the {@code billing_info} sub-object onto a
     * {@link BusinessBroadcastBillingAccount.BillingInfo}.
     *
     * @param node the JSON object to read, possibly {@code null}
     * @return the projected billing info, or {@code null} when {@code stanza} is {@code null}
     */
    private static BusinessBroadcastBillingAccount.BillingInfo readBillingInfo(JSONObject node) {
        if (node == null) {
            return null;
        }

        return new BusinessBroadcastBillingAccountBillingInfoBuilder()
                .billingPaymentMode(node.getString("billing_payment_mode"))
                .estimatedTax(readEstimatedTax(node.getJSONObject("estimated_tax")))
                .paymentSectionDetails(readPaymentSectionDetails(node.getJSONObject("payment_section_details")))
                .requiredAction(readRequiredAction(node.getJSONObject("required_action")))
                .build();
    }

    /**
     * Projects the {@code estimated_tax} sub-object onto a
     * {@link BusinessBroadcastBillingAccount.EstimatedTax}.
     *
     * @param node the JSON object to read, possibly {@code null}
     * @return the projected estimated tax, or {@code null} when {@code stanza} is {@code null}
     */
    private static BusinessBroadcastBillingAccount.EstimatedTax readEstimatedTax(JSONObject node) {
        if (node == null) {
            return null;
        }

        return new BusinessBroadcastBillingAccountEstimatedTaxBuilder()
                .budget(readCurrencyAmount(node.getJSONObject("budget")))
                .taxes(readTaxLineItems(node.getJSONArray("taxes")))
                .total(readCurrencyAmount(node.getJSONObject("total")))
                .build();
    }

    /**
     * Projects a {@code taxes} array onto a list of
     * {@link BusinessBroadcastBillingAccount.TaxLineItem}.
     *
     * @param arr the JSON array to read, possibly {@code null}
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<BusinessBroadcastBillingAccount.TaxLineItem> readTaxLineItems(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessBroadcastBillingAccount.TaxLineItem>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var item = readTaxLineItem(arr.getJSONObject(i));
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Projects a single {@code taxes} entry onto a
     * {@link BusinessBroadcastBillingAccount.TaxLineItem}.
     *
     * @param node the JSON object to read, possibly {@code null}
     * @return the projected tax line item, or {@code null} when {@code stanza} is {@code null}
     */
    private static BusinessBroadcastBillingAccount.TaxLineItem readTaxLineItem(JSONObject node) {
        if (node == null) {
            return null;
        }

        return new BusinessBroadcastBillingAccountTaxLineItemBuilder()
                .taxName(node.getString("tax_name"))
                .taxRate(node.getString("tax_rate"))
                .taxAmount(readCurrencyAmount(node.getJSONObject("tax_amount")))
                .build();
    }

    /**
     * Projects a currency-amount sub-object onto a
     * {@link BusinessBroadcastBillingAccount.CurrencyAmount}.
     *
     * @param node the JSON object to read, possibly {@code null}
     * @return the projected currency amount, or {@code null} when {@code stanza} is {@code null}
     */
    private static BusinessBroadcastBillingAccount.CurrencyAmount readCurrencyAmount(JSONObject node) {
        if (node == null) {
            return null;
        }

        return new BusinessBroadcastBillingAccountCurrencyAmountBuilder()
                .amount(node.getString("amount"))
                .currency(node.getString("currency"))
                .formattedAmount(node.getString("formatted_amount"))
                .build();
    }

    /**
     * Projects the {@code payment_section_details} sub-object onto a
     * {@link BusinessBroadcastBillingAccount.PaymentSectionDetails}.
     *
     * @param node the JSON object to read, possibly {@code null}
     * @return the projected payment-section details, or {@code null} when {@code stanza} is {@code null}
     */
    private static BusinessBroadcastBillingAccount.PaymentSectionDetails readPaymentSectionDetails(JSONObject node) {
        if (node == null) {
            return null;
        }

        return new BusinessBroadcastBillingAccountPaymentSectionDetailsBuilder()
                .description(node.getString("description"))
                .label(node.getString("label"))
                .labelAx(node.getString("label_ax"))
                .logos(readImages(node.getJSONArray("logos")))
                .primaryAction(readBillingInfoAction(node.getJSONObject("primary_action")))
                .type(node.getString("type"))
                .build();
    }

    /**
     * Projects a {@code logos} array onto a list of
     * {@link BusinessBroadcastBillingAccount.Image}.
     *
     * @param arr the JSON array to read, possibly {@code null}
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<BusinessBroadcastBillingAccount.Image> readImages(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessBroadcastBillingAccount.Image>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var image = readImage(arr.getJSONObject(i));
            if (image != null) {
                result.add(image);
            }
        }
        return result;
    }

    /**
     * Projects a single {@code logos} entry onto a
     * {@link BusinessBroadcastBillingAccount.Image}.
     *
     * @param node the JSON object to read, possibly {@code null}
     * @return the projected image, or {@code null} when {@code stanza} is {@code null}
     */
    private static BusinessBroadcastBillingAccount.Image readImage(JSONObject node) {
        if (node == null) {
            return null;
        }

        return new BusinessBroadcastBillingAccountImageBuilder()
                .uri(node.getString("uri"))
                .build();
    }

    /**
     * Projects the {@code required_action} sub-object onto a
     * {@link BusinessBroadcastBillingAccount.RequiredAction}.
     *
     * @param node the JSON object to read, possibly {@code null}
     * @return the projected required action, or {@code null} when {@code stanza} is {@code null}
     */
    private static BusinessBroadcastBillingAccount.RequiredAction readRequiredAction(JSONObject node) {
        if (node == null) {
            return null;
        }

        return new BusinessBroadcastBillingAccountRequiredActionBuilder()
                .action(readBillingInfoAction(node.getJSONObject("action")))
                .message(readMessage(node.getJSONObject("message")))
                .build();
    }

    /**
     * Projects a billing-info-action sub-object onto a
     * {@link BusinessBroadcastBillingAccount.BillingInfoAction}.
     *
     * @param node the JSON object to read, possibly {@code null}
     * @return the projected action, or {@code null} when {@code stanza} is {@code null}
     */
    private static BusinessBroadcastBillingAccount.BillingInfoAction readBillingInfoAction(JSONObject node) {
        if (node == null) {
            return null;
        }

        return new BusinessBroadcastBillingAccountBillingInfoActionBuilder()
                .label(node.getString("label"))
                .wizardName(node.getString("wizard_name"))
                .wizardPropsJson(node.getString("wizard_props_json"))
                .build();
    }

    /**
     * Projects the {@code message} sub-object onto a
     * {@link BusinessBroadcastBillingAccount.Message}.
     *
     * @param node the JSON object to read, possibly {@code null}
     * @return the projected message, or {@code null} when {@code stanza} is {@code null}
     */
    private static BusinessBroadcastBillingAccount.Message readMessage(JSONObject node) {
        if (node == null) {
            return null;
        }

        return new BusinessBroadcastBillingAccountMessageBuilder()
                .headline(node.getString("headline"))
                .body(node.getString("body"))
                .build();
    }
}
