package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdSuccessScreen;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdSuccessScreenBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the success-modal query built by
 * {@link BizAdCreationSuccessModalFacebookGraphQlRequest} into a {@link BusinessAdSuccessScreen}.
 *
 * <p>Projects the {@code billable_account_by_asset_id} root onto the {@link BusinessAdSuccessScreen}
 * model: the billed account identifier and the payment-method description and icons shown after an ad
 * goes live.
 *
 * @see BizAdCreationSuccessModalFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationSuccessModalQuery")
public final class BizAdCreationSuccessModalFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected success screen, or {@code null} when the relay omitted the root.
     */
    private final BusinessAdSuccessScreen screen;

    /**
     * Constructs a response wrapping the projected success screen.
     *
     * <p>Reserved for the static parser.
     *
     * @param screen the projected success screen, or {@code null} when the relay omitted the root
     */
    private BizAdCreationSuccessModalFacebookGraphQlResponse(BusinessAdSuccessScreen screen) {
        this.screen = screen;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code billable_account_by_asset_id} root onto a {@link BusinessAdSuccessScreen}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationSuccessModalFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var account = data.getJSONObject("billable_account_by_asset_id");
        if (account == null) {
            return Optional.of(new BizAdCreationSuccessModalFacebookGraphQlResponse(null));
        }

        String paymentLabel = null;
        List<URI> paymentLogos = List.of();
        var billingInfo = account.getJSONObject("billing_info");
        if (billingInfo != null) {
            var details = billingInfo.getJSONObject("payment_section_details");
            if (details != null) {
                paymentLabel = details.getString("label");
                paymentLogos = logos(details.getJSONArray("logos"));
            }
        }

        var screen = new BusinessAdSuccessScreenBuilder()
                .billableAccountId(account.getString("id"))
                .paymentLabel(paymentLabel)
                .paymentLogos(paymentLogos)
                .build();
        return Optional.of(new BizAdCreationSuccessModalFacebookGraphQlResponse(screen));
    }

    /**
     * Projects the {@code logos} array onto a list of {@link URI} icon locations, skipping any entry
     * whose {@code uri} is absent or not syntactically valid.
     *
     * @param arr the {@code logos} array, or {@code null}
     * @return the icon locations, empty when {@code arr} is {@code null}
     */
    private static List<URI> logos(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<URI>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var node = arr.getJSONObject(i);
            if (node == null) {
                continue;
            }
            var uri = toUri(node.getString("uri"));
            if (uri != null) {
                result.add(uri);
            }
        }
        return result;
    }

    /**
     * Parses the given string into a {@link URI}, returning {@code null} when it is absent or not
     * syntactically valid.
     *
     * @param raw the candidate URI string, or {@code null}
     * @return the parsed {@link URI}, or {@code null} when absent or invalid
     */
    private static URI toUri(String raw) {
        if (raw == null) {
            return null;
        }

        try {
            return new URI(raw);
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    /**
     * Returns the projected success screen.
     *
     * @return the projected {@link BusinessAdSuccessScreen}, or empty when the relay omitted the root
     */
    public Optional<BusinessAdSuccessScreen> screen() {
        return Optional.ofNullable(screen);
    }
}
