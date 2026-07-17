package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.WhatsAppAdsIdentityPage;
import com.github.auties00.cobalt.wire.linked.business.ads.WhatsAppAdsIdentityPageBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the create-WhatsApp-Ads-identity mutation built by
 * {@link CreateWhatsAppAdsIdentityFacebookGraphQlRequest} into a {@link WhatsAppAdsIdentityPage}.
 *
 * <p>Reads the linked root {@code create_or_update_whatsapp_ads_identity} and projects its
 * {@code id} scalar onto the Cobalt domain model. WhatsApp Web treats the absence of that id as a
 * provisioning failure.
 *
 * @see CreateWhatsAppAdsIdentityFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebCreateWhatsAppAdsIdentityMutation")
public final class CreateWhatsAppAdsIdentityFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed advertising-platform page.
     */
    private final WhatsAppAdsIdentityPage page;

    /**
     * Constructs a response wrapping the parsed advertising page.
     *
     * <p>Reserved for the static parser.
     *
     * @param page the parsed advertising page, or {@code null} when the Meta graph endpoint omitted the field
     */
    private CreateWhatsAppAdsIdentityFacebookGraphQlResponse(WhatsAppAdsIdentityPage page) {
        this.page = page;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code create_or_update_whatsapp_ads_identity} and projects it onto a
     * {@link WhatsAppAdsIdentityPage}; the returned {@link Optional} is empty when {@code data} or
     * the page object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the page object is missing
     */
    public static Optional<CreateWhatsAppAdsIdentityFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("create_or_update_whatsapp_ads_identity");
        if (node == null) {
            return Optional.empty();
        }

        var page = new WhatsAppAdsIdentityPageBuilder()
                .id(node.getString("id"))
                .build();
        return Optional.of(new CreateWhatsAppAdsIdentityFacebookGraphQlResponse(page));
    }

    /**
     * Returns the parsed advertising-platform page.
     *
     * @return the parsed {@link WhatsAppAdsIdentityPage}, never {@code null}
     */
    public WhatsAppAdsIdentityPage page() {
        return page;
    }
}
