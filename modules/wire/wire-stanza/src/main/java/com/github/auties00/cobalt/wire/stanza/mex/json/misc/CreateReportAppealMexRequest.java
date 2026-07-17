package com.github.auties00.cobalt.wire.stanza.mex.json.misc;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Optional;

/**
 * Lodges an appeal against a previously submitted channel report.
 *
 * <p>This mutation is issued when a channel admin contests a report enforcement notice. The relay
 * produces a fresh channel-report envelope (report id, status, channel name and JID,
 * reported-content data, plus the appeal sub-object carrying the appeal id and state) which the
 * caller parses through {@link CreateReportAppealMexResponse} and renders as an updated report
 * record. The two declared GraphQL variables, {@link #reason} and {@link #reportId}, are always
 * materialised on the wire and forwarded verbatim.
 *
 * @implNote This implementation surfaces a missing
 * {@code data.xwa2_create_channel_report_appeal_v2} envelope as
 * {@link CreateReportAppealMexResponse#of(Stanza)} returning {@link Optional#empty()} rather than
 * raising a synthetic server error.
 */
@WhatsAppWebModule(moduleName = "WAWebMexCreateReportAppealJob")
public final class CreateReportAppealMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled GraphQL query identifier for the report-appeal mutation document.
     *
     * <p>The relay maps this identifier to a server-side persisted mutation and never sees the
     * GraphQL text on the wire.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexCreateReportAppealJobMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "27103316329328467";

    /**
     * Holds the GraphQL operation name reported to the MEX perf tracker when this mutation is
     * dispatched.
     *
     * <p>The name tags the mutation in latency and error metrics; it is kept on the request so
     * embedders mirroring that telemetry surface can emit the same tag.
     */
    public static final String OPERATION_NAME = "createReportAppeal";

    /**
     * Holds the free-form appeal justification bound to the {@code reason} GraphQL variable.
     */
    private final String reason;

    /**
     * Holds the identifier of the report being contested, bound to the {@code report_id} GraphQL
     * variable.
     */
    private final String reportId;

    /**
     * Constructs a new request with the two GraphQL variables.
     *
     * <p>The {@code reason} is the user-typed appeal justification and {@code reportId} identifies
     * the original channel report. Both variables are always emitted on the wire.
     *
     * @param reason   the free-form appeal justification
     * @param reportId the identifier of the report being contested
     */
    public CreateReportAppealMexRequest(String reason, String reportId) {
        this.reason = reason;
        this.reportId = reportId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String id() {
        return QUERY_ID;
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
     * @implNote This implementation streams the GraphQL variables through fastjson2's
     * {@link JSONWriter}, always materialising both declared {@code reason} and {@code report_id}
     * variables, then wraps the payload through
     * {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexCreateReportAppealJob", exports = "createReportAppeal",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
            writer.writeName("reason");
            writer.writeColon();
            writer.writeString(reason);
            writer.writeName("report_id");
            writer.writeColon();
            writer.writeString(reportId);
            writer.endObject();
            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return Json.createMexNode(QUERY_ID, output.toString());
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
