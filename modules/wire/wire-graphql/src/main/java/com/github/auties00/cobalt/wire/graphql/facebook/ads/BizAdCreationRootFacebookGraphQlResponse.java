package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdCreationScreen;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdCreationScreenBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the ad-creation root query built by
 * {@link BizAdCreationRootFacebookGraphQlRequest} into a {@link BusinessAdCreationScreen}.
 *
 * <p>Projects the embedder-useful fields of the WhatsApp Business ad-creation opening screen onto the
 * {@link BusinessAdCreationScreen} model: the selectable budget steps and eligible publisher
 * platforms from the {@code lwi} boosted-component surface, the promoted page header from the
 * {@code page} root, the linked ad-account count from the {@code wa_ctwa_ad_accounts} root, and the
 * onboarding email from the {@code wa_ad_account_onboarding_data} root.
 *
 * @see BizAdCreationRootFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationRootQuery")
public final class BizAdCreationRootFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected creation screen.
     */
    private final BusinessAdCreationScreen screen;

    /**
     * Constructs a response wrapping the projected creation screen.
     *
     * <p>Reserved for the static parser.
     *
     * @param screen the projected creation screen
     */
    private BizAdCreationRootFacebookGraphQlResponse(BusinessAdCreationScreen screen) {
        this.screen = screen;
    }

    /**
     * Parses the response from the unwrapped GraphQL {@code data} object and projects the
     * embedder-useful fields onto a {@link BusinessAdCreationScreen}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationRootFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        List<String> budgetOptions = List.of();
        List<String> publisherPlatforms = List.of();
        var lwi = data.getJSONObject("lwi");
        if (lwi != null) {
            var boostedComponent = lwi.getJSONObject("boostedComponent");
            if (boostedComponent != null) {
                budgetOptions = budgetOptions(boostedComponent.getJSONObject("boostedComponentOptions"));
                publisherPlatforms = publisherPlatforms(boostedComponent.getJSONObject("options"));
            }
        }

        String pageName = null;
        var pageVerified = false;
        var page = data.getJSONObject("page");
        if (page != null) {
            pageName = page.getString("name");
            pageVerified = Boolean.TRUE.equals(page.getBoolean("is_verified"));
        }

        Long linkedAdAccountCount = null;
        var adAccounts = data.getJSONObject("wa_ctwa_ad_accounts");
        if (adAccounts != null) {
            linkedAdAccountCount = adAccounts.getLong("count");
        }

        String onboardingEmail = null;
        var onboardingData = data.getJSONObject("wa_ad_account_onboarding_data");
        if (onboardingData != null) {
            onboardingEmail = onboardingData.getString("email");
        }

        var screen = new BusinessAdCreationScreenBuilder()
                .budgetOptions(budgetOptions)
                .publisherPlatforms(publisherPlatforms)
                .pageName(pageName)
                .pageVerified(pageVerified)
                .linkedAdAccountCount(linkedAdAccountCount)
                .onboardingEmail(onboardingEmail)
                .build();
        return Optional.of(new BizAdCreationRootFacebookGraphQlResponse(screen));
    }

    /**
     * Projects the {@code budgetOptions} array of the boosted-component options onto the selectable
     * spend amounts, reading each entry's {@code offsetAmount} scalar.
     *
     * @param options the {@code boostedComponentOptions} object, or {@code null}
     * @return the selectable spend amounts, empty when {@code options} is {@code null}
     */
    private static List<String> budgetOptions(JSONObject options) {
        if (options == null) {
            return List.of();
        }

        var arr = options.getJSONArray("budgetOptions");
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<String>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var node = arr.getJSONObject(i);
            if (node == null) {
                continue;
            }
            var amount = node.getString("offsetAmount");
            if (amount != null) {
                result.add(amount);
            }
        }
        return result;
    }

    /**
     * Projects the {@code eligible_publisher_platforms} array onto the platform labels.
     *
     * @param options the eligible-publisher-platform {@code options} object, or {@code null}
     * @return the platform labels, empty when {@code options} is {@code null}
     */
    private static List<String> publisherPlatforms(JSONObject options) {
        if (options == null) {
            return List.of();
        }

        var arr = options.getJSONArray("eligible_publisher_platforms");
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<String>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var node = arr.getJSONObject(i);
            if (node == null) {
                continue;
            }
            var label = node.getString("label");
            if (label != null) {
                result.add(label);
            }
        }
        return result;
    }

    /**
     * Returns the projected creation screen.
     *
     * @return the projected {@link BusinessAdCreationScreen}
     */
    public BusinessAdCreationScreen screen() {
        return screen;
    }
}
