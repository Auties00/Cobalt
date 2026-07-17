package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the report-product mutation built by
 * {@link ReportProductJobWhatsAppGraphQlRequest} into the report-accepted flag.
 *
 * <p>Reads the linked {@code xwa_whatsapp_catalog_report_product} root and projects its
 * {@code success} flag, which is the only payload the report carries. WhatsApp Web treats any value
 * other than a {@code true} {@code success} as a server failure.
 *
 * @see ReportProductJobWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebReportProductJobMutation")
public final class ReportProductJobWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds whether the report was accepted.
     */
    private final boolean accepted;

    /**
     * Constructs a response wrapping the report-accepted flag.
     *
     * <p>Reserved for the static parser.
     *
     * @param accepted whether the report was accepted
     */
    private ReportProductJobWhatsAppGraphQlResponse(boolean accepted) {
        this.accepted = accepted;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xwa_whatsapp_catalog_report_product} and projects its
     * {@code success} flag; the returned {@link Optional} is empty when {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebReportProductJobMutation", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<ReportProductJobWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var root = data.getJSONObject("xwa_whatsapp_catalog_report_product");
        var success = root == null ? null : root.getBoolean("success");
        return Optional.of(new ReportProductJobWhatsAppGraphQlResponse(success != null && success));
    }

    /**
     * Returns whether the report was accepted.
     *
     * @return {@code true} when the graph.whatsapp.com endpoint reported the report accepted, {@code false} otherwise
     */
    public boolean accepted() {
        return accepted;
    }
}
