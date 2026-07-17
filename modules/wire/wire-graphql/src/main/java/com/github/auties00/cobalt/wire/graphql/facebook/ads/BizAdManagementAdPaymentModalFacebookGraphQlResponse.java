package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdMutationResult;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdMutationResultBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the ad-management payment-notification mutation built by
 * {@link BizAdManagementAdPaymentModalFacebookGraphQlRequest} into a {@link BusinessAdMutationResult}.
 *
 * <p>Projects the single root scalar {@code xfb_wa_biz_send_payment_hub_notification}, which reports
 * whether the payment-hub notification was dispatched, onto the shared mutation-result shape. Only an
 * explicit {@code true} is treated as a successful dispatch.
 *
 * @implNote This implementation reads {@code xfb_wa_biz_send_payment_hub_notification} as a nullable
 * {@code Boolean} dispatch outcome. The compiled {@code WAWebBizAdManagementAdPaymentModalMutation}
 * document of snapshot {@code 1040120866} declares the field as a {@code ScalarField} without a
 * concrete type; the {@code send_*}-style field name follows WhatsApp Web's success-flag convention,
 * and the lenient {@link JSONObject#getBoolean(String)} coercion accepts a boolean, the strings
 * {@code "true"} and {@code "false"}, and numeric truthiness without throwing on an unexpected shape.
 *
 * @see BizAdManagementAdPaymentModalFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdManagementAdPaymentModalMutation")
public final class BizAdManagementAdPaymentModalFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected mutation result, or {@code null} when {@code data} was {@code null}.
     */
    private final BusinessAdMutationResult result;

    /**
     * Constructs a response wrapping the projected mutation result.
     *
     * <p>Reserved for the static parser.
     *
     * @param result the projected mutation result
     */
    private BizAdManagementAdPaymentModalFacebookGraphQlResponse(BusinessAdMutationResult result) {
        this.result = result;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code xfb_wa_biz_send_payment_hub_notification} scalar onto a {@link BusinessAdMutationResult}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdManagementAdPaymentModalFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var result = new BusinessAdMutationResultBuilder()
                .success(Boolean.TRUE.equals(data.getBoolean("xfb_wa_biz_send_payment_hub_notification")))
                .build();
        return Optional.of(new BizAdManagementAdPaymentModalFacebookGraphQlResponse(result));
    }

    /**
     * Returns the projected mutation result.
     *
     * @return the projected {@link BusinessAdMutationResult}, or empty when {@code data} was
     *         {@code null}
     */
    public Optional<BusinessAdMutationResult> result() {
        return Optional.ofNullable(result);
    }
}
