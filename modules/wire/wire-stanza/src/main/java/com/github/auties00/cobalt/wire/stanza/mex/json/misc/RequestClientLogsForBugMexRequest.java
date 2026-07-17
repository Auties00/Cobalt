package com.github.auties00.cobalt.wire.stanza.mex.json.misc;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the MEX mutation that asks a set of participants to upload their client logs for a bug
 * report.
 *
 * <p>This mutation drives remote-log collection: given a bug identifier, the reporter's own
 * identifier, an optional cut-off timestamp and the participants to solicit, the relay instructs
 * each participant's client to upload its logs up to that timestamp. The reply is a single boolean
 * scalar parsed by {@link RequestClientLogsForBugMexResponse}. All four fields are nested under the
 * single {@code input} GraphQL variable.
 *
 * @see RequestClientLogsForBugMexResponse
 *
 * @deprecated not wired: bug-report peer-log solicitation has no headless surface.
 */
@Deprecated
@WhatsAppWebModule(moduleName = "WAWebMexRequestClientLogsForBugJob")
public final class RequestClientLogsForBugMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of the
     * {@code WAWebMexRequestClientLogsForBugJobMutation} document.
     *
     * <p>The relay maps this identifier to its persisted operation; it is emitted as the
     * {@code query_id} attribute of the outgoing {@code <query>} child and the GraphQL text is never
     * sent on the wire.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexRequestClientLogsForBugJobMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "27135500612803533";

    /**
     * Holds the GraphQL operation name reported alongside this mutation when it is dispatched.
     *
     * <p>The name tags the operation in latency and error metrics; it is kept on the request so
     * embedders mirroring that telemetry surface can emit the same tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexRequestClientLogsForBugJobMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebMexRequestClientLogsForBugJobMutation";

    /**
     * Holds the {@code bug_id} field of the {@code input} object: the identifier of the bug report
     * the solicited logs are attached to.
     */
    private final String bugId;

    /**
     * Holds the {@code participant_ids} field of the {@code input} object: the participants whose
     * clients are asked to upload their logs.
     */
    private final List<String> participantIds;

    /**
     * Holds the {@code reporter_id} field of the {@code input} object: the identifier of the account
     * that filed the bug report (the reporter's own LID).
     */
    private final String reporterId;

    /**
     * Holds the {@code up_to_timestamp_secs} field of the {@code input} object: the Unix epoch
     * second up to which logs are requested, or {@code null} to request all available logs.
     */
    private final Long upToTimestampSecs;

    /**
     * Constructs a request-client-logs-for-bug mutation request.
     *
     * <p>The {@code bugId} ties the solicited logs to a bug report, the {@code participantIds} names
     * the clients to solicit, the {@code reporterId} identifies the filing account, and
     * {@code upToTimestampSecs} bounds how far back logs are requested. Each field whose value is
     * {@code null} is omitted from the nested {@code input} object.
     *
     * @param bugId             the bug-report identifier, or {@code null} to omit the field
     * @param participantIds    the participants to solicit, or {@code null} to omit the field
     * @param reporterId        the reporter's own identifier, or {@code null} to omit the field
     * @param upToTimestampSecs the upper-bound Unix epoch second, or {@code null} to omit the field
     */
    public RequestClientLogsForBugMexRequest(String bugId, List<String> participantIds, String reporterId, Long upToTimestampSecs) {
        this.bugId = bugId;
        this.participantIds = participantIds;
        this.reporterId = reporterId;
        this.upToTimestampSecs = upToTimestampSecs;
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
     * {@link JSONWriter}, nesting the {@code bug_id}, {@code participant_ids}, {@code reporter_id}
     * and {@code up_to_timestamp_secs} fields under a single {@code input} object and emitting each
     * field only when its corresponding constructor argument is non-null. The
     * {@code up_to_timestamp_secs} field is written as the raw epoch second supplied by the caller.
     * The wrapped envelope is built through
     * {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexRequestClientLogsForBugJob", exports = "requestClientLogsForBugJob",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();

            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (bugId != null) {
                writer.writeName("bug_id");
                writer.writeColon();
                writer.writeString(bugId);
            }
            if (participantIds != null) {
                writer.writeName("participant_ids");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < participantIds.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    writer.writeString(participantIds.get(i));
                }
                writer.endArray();
            }
            if (reporterId != null) {
                writer.writeName("reporter_id");
                writer.writeColon();
                writer.writeString(reporterId);
            }
            if (upToTimestampSecs != null) {
                writer.writeName("up_to_timestamp_secs");
                writer.writeColon();
                writer.writeInt64(upToTimestampSecs);
            }
            writer.endObject();

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
