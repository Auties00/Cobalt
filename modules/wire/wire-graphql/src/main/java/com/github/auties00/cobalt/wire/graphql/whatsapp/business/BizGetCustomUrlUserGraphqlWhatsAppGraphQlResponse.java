package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessCustomUrlIdentity;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessCustomUrlIdentityBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the custom-url resolution query built by
 * {@link BizGetCustomUrlUserGraphqlWhatsAppGraphQlRequest} into a {@link BusinessCustomUrlIdentity}.
 *
 * <p>Reads the linked {@code xwa_custom_url_get_user} field and projects its {@code success} flag, the
 * resolved {@code user.jid}, and the {@code error_code} plus {@code error_text} failure pair onto the
 * Cobalt domain model. This query resolves the slug to the owner's primary contact identifier.
 *
 * @see BizGetCustomUrlUserGraphqlWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizGetCustomUrlUserGraphqlQuery")
public final class BizGetCustomUrlUserGraphqlWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the parsed resolution outcome.
     */
    private final BusinessCustomUrlIdentity identity;

    /**
     * Constructs a response wrapping the parsed resolution outcome.
     *
     * <p>Reserved for the static parser.
     *
     * @param identity the parsed resolution outcome, or {@code null} when the graph.whatsapp.com endpoint omitted the field
     */
    private BizGetCustomUrlUserGraphqlWhatsAppGraphQlResponse(BusinessCustomUrlIdentity identity) {
        this.identity = identity;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xwa_custom_url_get_user} and projects its resolution flag, the
     * resolved {@code user.jid} as the primary identifier, and the failure pair onto a
     * {@link BusinessCustomUrlIdentity}; the returned {@link Optional} is empty when {@code data} or the
     * resolution field is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the resolution field is missing
     */
    @WhatsAppWebExport(moduleName = "WAWebBizGetCustomUrlUserGraphqlQuery", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<BizGetCustomUrlUserGraphqlWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var root = data.getJSONObject("xwa_custom_url_get_user");
        if (root == null) {
            return Optional.empty();
        }
        var success = root.getBoolean("success");
        Jid jid = null;
        var user = root.getJSONObject("user");
        if (user != null) {
            var jidString = user.getString("jid");
            jid = jidString == null ? null : Jid.of(jidString);
        }
        var identity = new BusinessCustomUrlIdentityBuilder()
                .resolved(success != null && success)
                .identifier(jid)
                .errorCode(root.getString("error_code"))
                .errorText(root.getString("error_text"))
                .build();
        return Optional.of(new BizGetCustomUrlUserGraphqlWhatsAppGraphQlResponse(identity));
    }

    /**
     * Returns the parsed resolution outcome.
     *
     * <p>The returned {@link BusinessCustomUrlIdentity} reports whether the slug resolved and, on
     * success, carries the owner's primary contact identifier.
     *
     * @return the parsed resolution outcome, never {@code null}
     */
    public BusinessCustomUrlIdentity identity() {
        return identity;
    }
}
