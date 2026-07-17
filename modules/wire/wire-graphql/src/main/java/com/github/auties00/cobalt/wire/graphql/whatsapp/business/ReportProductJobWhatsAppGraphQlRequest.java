package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlEnvironment;
import java.util.Optional;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the graph.whatsapp.com GraphQL mutation that reports a catalog product for policy violation.
 *
 * <p>The single {@code input} GraphQL variable carries the business catalog {@link Jid}, the reported
 * product id, and an optional free-text reason. WhatsApp Web's {@code WAWebReportProductJob} builds it
 * from the catalog wid ({@code jid}), the {@code product_id}, and the {@code reason}, which it omits
 * entirely when the caller passes the empty string. The graph.whatsapp.com endpoint returns the outcome under
 * {@code xwa_whatsapp_catalog_report_product}; the reply is consumed through
 * {@link ReportProductJobWhatsAppGraphQlResponse}.
 *
 * @see ReportProductJobWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebReportProductJobMutation")
public final class ReportProductJobWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     *
     * @implNote This implementation ships the resolved numeric document id rather than the operation
     * name. The operation's own {@code params.id} in {@code WAWebReportProductJobMutation.graphql} is
     * the literal document name {@code "WAWebReportProductJobMutation"} rather than a numeric id; the
     * live numeric id is the value WhatsApp Web resolves at dispatch time from
     * {@code WAWebGraphQLPersistedQueries.PersistedQueries["WAWebReportProductJobMutation"]}, which is
     * {@value #DOC_ID} in snapshot {@code 1040120866}.
     */
    @WhatsAppWebExport(moduleName = "WAWebReportProductJobMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static final String DOC_ID = "8473660082655001";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebReportProductJobMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebReportProductJobMutation";

    /**
     * The {@code jid} field of the {@code input} object naming the business catalog of the reported
     * product, or {@code null} to omit it.
     */
    private final Jid catalogJid;

    /**
     * The {@code product_id} field of the {@code input} object naming the reported product, or
     * {@code null} to omit it.
     */
    private final String productId;

    /**
     * The {@code reason} field of the {@code input} object carrying the free-text report reason, or
     * {@code null} to omit it.
     */
    private final String reason;

    /**
     * Constructs a report-product mutation request.
     *
     * <p>The {@code catalogJid} names the business catalog and {@code productId} the reported product.
     * The {@code reason} is the optional free-text report reason. Each value that is {@code null} is
     * omitted from the serialized object.
     *
     * @param catalogJid the business catalog {@link Jid} of the reported product, or {@code null} to
     *                   omit the field
     * @param productId  the reported product id, or {@code null} to omit the field
     * @param reason     the free-text report reason, or {@code null} to omit the field
     */
    public ReportProductJobWhatsAppGraphQlRequest(Jid catalogJid, String productId, String reason) {
        this.catalogJid = catalogJid;
        this.productId = productId;
        this.reason = reason;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String docId() {
        return DOC_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation emits
     * {@code {"input": {"jid": <jid>, "product_id": <productId>, "reason": <reason>}}}, writing each
     * field only when its value is non-null and emitting {@code {"input": {}}} when all are
     * {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebReportProductJob", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (catalogJid != null) {
                writer.writeName("jid");
                writer.writeColon();
                writer.writeString(catalogJid.toString());
            }

            if (productId != null) {
                writer.writeName("product_id");
                writer.writeColon();
                writer.writeString(productId);
            }

            if (reason != null) {
                writer.writeName("reason");
                writer.writeColon();
                writer.writeString(reason);
            }
            writer.endObject();
            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return output.toString();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WhatsAppGraphQlEnvironment environment() {
        return WhatsAppGraphQlEnvironment.CATALOG;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> accessToken() {
        return Optional.empty();
    }
}
