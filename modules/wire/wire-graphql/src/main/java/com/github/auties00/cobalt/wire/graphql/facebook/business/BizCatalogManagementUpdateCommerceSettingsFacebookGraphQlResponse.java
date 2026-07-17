package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the update-commerce-settings mutation built by
 * {@link BizCatalogManagementUpdateCommerceSettingsFacebookGraphQlRequest}.
 *
 * <p>Reads the linked root {@code xfb_whatsapp_smb_commerce_settings} and projects its
 * {@code cart_enabled} flag, which echoes the resulting cart state for the business account.
 *
 * @see BizCatalogManagementUpdateCommerceSettingsFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementUpdateCommerceSettingsMutation")
public final class BizCatalogManagementUpdateCommerceSettingsFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the resulting cart-enabled flag.
     */
    private final boolean cartEnabled;

    /**
     * Constructs a response wrapping the resulting cart-enabled flag.
     *
     * <p>Reserved for the static parser.
     *
     * @param cartEnabled the resulting cart-enabled flag
     */
    private BizCatalogManagementUpdateCommerceSettingsFacebookGraphQlResponse(boolean cartEnabled) {
        this.cartEnabled = cartEnabled;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xfb_whatsapp_smb_commerce_settings} and projects its
     * {@code cart_enabled} flag; the returned {@link Optional} is empty when {@code data} or the root
     * is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null} or the root is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementUpdateCommerceSettingsMutation", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<BizCatalogManagementUpdateCommerceSettingsFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var root = data.getJSONObject("xfb_whatsapp_smb_commerce_settings");
        if (root == null) {
            return Optional.empty();
        }
        var cartEnabled = Boolean.TRUE.equals(root.getBoolean("cart_enabled"));
        return Optional.of(new BizCatalogManagementUpdateCommerceSettingsFacebookGraphQlResponse(cartEnabled));
    }

    /**
     * Returns the resulting cart-enabled flag echoed by the Meta graph endpoint.
     *
     * @return {@code true} when the business cart is enabled after the update, {@code false} otherwise
     */
    public boolean cartEnabled() {
        return cartEnabled;
    }
}
