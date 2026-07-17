package com.github.auties00.cobalt.wire.graphql.whatsapp.misc;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the Waffle FX WAMO update-UOOM mutation built by
 * {@link WaffleFxwamoUpdateUoomWhatsAppGraphQlRequest}.
 *
 * <p>Exposes the single boolean scalar field {@code xfb_waffle_fx_wamo_update_uoom}, the outcome of
 * propagating the caller's universal opt-out into the linked Meta advertising surfaces. WhatsApp Web
 * marks the GPC-completed user preference only when this outcome is {@code true}.
 *
 * @see WaffleFxwamoUpdateUoomWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebWaffleFXWAMOUpdateUOOMMutation")
public final class WaffleFxwamoUpdateUoomWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the update outcome returned under {@code xfb_waffle_fx_wamo_update_uoom}, or {@code null}
     * when the graph.whatsapp.com endpoint omitted the field.
     */
    private final Boolean updated;

    /**
     * Constructs a response wrapping the parsed update outcome.
     *
     * <p>Reserved for the static parser.
     *
     * @param updated the update outcome, or {@code null} when the graph.whatsapp.com endpoint omitted the field
     */
    private WaffleFxwamoUpdateUoomWhatsAppGraphQlResponse(Boolean updated) {
        this.updated = updated;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the scalar root {@code xfb_waffle_fx_wamo_update_uoom}; the returned {@link Optional}
     * is empty when {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<WaffleFxwamoUpdateUoomWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var updated = data.getBoolean("xfb_waffle_fx_wamo_update_uoom");
        return Optional.of(new WaffleFxwamoUpdateUoomWhatsAppGraphQlResponse(updated));
    }

    /**
     * Returns whether the universal opt-out update succeeded.
     *
     * @return {@code true} when the graph.whatsapp.com endpoint reported a successful update, {@code false} when it
     *         reported failure or omitted the field
     */
    public boolean updated() {
        return updated != null && updated;
    }
}
