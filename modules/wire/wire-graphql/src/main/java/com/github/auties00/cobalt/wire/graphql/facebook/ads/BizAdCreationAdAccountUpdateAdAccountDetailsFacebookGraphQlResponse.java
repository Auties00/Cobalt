package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdAccount;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdAccountBuilder;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdAccountPaymentMethodBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the ad-account-details query built by
 * {@link BizAdCreationAdAccountUpdateAdAccountDetailsFacebookGraphQlRequest} into a {@link BusinessAdAccount}.
 *
 * <p>Projects the linked {@code adAccount} field (the wire alias of the {@code ad_account} selection)
 * onto the {@link BusinessAdAccount} model: the account identifiers, display name, billing currency,
 * the spend-attribution-reporting capability flag, and the linked payment method.
 *
 * @see BizAdCreationAdAccountUpdateAdAccountDetailsFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationAdAccountUpdate_AdAccountDetailsQuery")
public final class BizAdCreationAdAccountUpdateAdAccountDetailsFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected ad account, or {@code null} when the relay omitted the field.
     */
    private final BusinessAdAccount adAccount;

    /**
     * Constructs a response wrapping the projected ad account.
     *
     * <p>Reserved for the static parser.
     *
     * @param adAccount the projected ad account, or {@code null} when the relay omitted the field
     */
    private BizAdCreationAdAccountUpdateAdAccountDetailsFacebookGraphQlResponse(BusinessAdAccount adAccount) {
        this.adAccount = adAccount;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code adAccount} field onto a {@link BusinessAdAccount}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationAdAccountUpdateAdAccountDetailsFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var adAccount = adAccount(data.getJSONObject("adAccount"));
        return Optional.of(new BizAdCreationAdAccountUpdateAdAccountDetailsFacebookGraphQlResponse(adAccount));
    }

    /**
     * Projects the {@code adAccount} object onto a {@link BusinessAdAccount}, reading each scalar
     * under its wire alias.
     *
     * @param node the {@code adAccount} object, or {@code null}
     * @return the projected account, or {@code null} when {@code stanza} is {@code null}
     */
    private static BusinessAdAccount adAccount(JSONObject node) {
        if (node == null) {
            return null;
        }

        return new BusinessAdAccountBuilder()
                .id(node.getString("id"))
                .legacyAccountId(node.getString("legacyAccountID"))
                .currencyCode(node.getString("currencyCodeEnum"))
                .name(node.getString("name"))
                .spendAttributionReportingEnabled(Boolean.TRUE.equals(node.getBoolean("canSeeSAFRV3")))
                .paymentMethod(paymentMethod(node.getJSONObject("paymentAccount")))
                .build();
    }

    /**
     * Projects the {@code paymentAccount} object onto a {@link BusinessAdAccount.PaymentMethod}.
     *
     * @param node the {@code paymentAccount} object, or {@code null}
     * @return the projected payment method, or {@code null} when {@code stanza} is {@code null}
     */
    private static BusinessAdAccount.PaymentMethod paymentMethod(JSONObject node) {
        if (node == null) {
            return null;
        }

        return new BusinessAdAccountPaymentMethodBuilder()
                .kind(node.getString("__typename"))
                .id(node.getString("id"))
                .build();
    }

    /**
     * Returns the projected ad account.
     *
     * @return the projected {@link BusinessAdAccount}, or empty when the relay omitted the field
     */
    public Optional<BusinessAdAccount> adAccount() {
        return Optional.ofNullable(adAccount);
    }
}
