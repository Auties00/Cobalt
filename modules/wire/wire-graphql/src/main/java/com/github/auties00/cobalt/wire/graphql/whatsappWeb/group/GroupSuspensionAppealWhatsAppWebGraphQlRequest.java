package com.github.auties00.cobalt.wire.graphql.whatsappWeb.group;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the relay mutation that files an appeal against a group suspension.
 *
 * <p>The single {@code input} GraphQL variable carries the suspended group {@link Jid}, the appeal
 * reason, and a debug-info blob. WhatsApp Web's {@code WAWebGroupSuspensionAppealMutation} builds it
 * from the group's user part ({@code group_jid}), an optional {@code appeal_reason}, and a
 * {@code debug_info} string (a JSON-encoded client-debug bundle gathered for the support tag). The
 * relay returns the appeal outcome under {@code wa_create_group_suspension_appeal}; the reply is
 * consumed through {@link GroupSuspensionAppealWhatsAppWebGraphQlResponse}.
 *
 * @see GroupSuspensionAppealWhatsAppWebGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebGroupSuspensionAppealMutation")
public final class GroupSuspensionAppealWhatsAppWebGraphQlRequest implements WhatsAppWebGraphQlOperation.Request {
    /**
     * The persisted document identifier the relay maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the url-encoded request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebGroupSuspensionAppealMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25946115325088226";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebGroupSuspensionAppealMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebGroupSuspensionAppealMutation";

    /**
     * The {@code group_jid} field of the {@code input} object naming the suspended group, or
     * {@code null} to omit it.
     */
    private final Jid groupJid;

    /**
     * The {@code appeal_reason} field of the {@code input} object carrying the free-text appeal
     * reason, or {@code null} to omit it.
     */
    private final String appealReason;

    /**
     * The {@code debug_info} field of the {@code input} object carrying the JSON-encoded client-debug
     * bundle, or {@code null} to omit it.
     */
    private final String debugInfo;

    /**
     * Constructs a group-suspension-appeal mutation request.
     *
     * <p>The {@code groupJid} names the suspended group. The {@code appealReason} is the optional
     * free-text reason. The {@code debugInfo} is the JSON-encoded client-debug bundle gathered for the
     * support tag. Each value that is {@code null} is omitted from the serialized object.
     *
     * @param groupJid     the suspended group {@link Jid}, or {@code null} to omit the field
     * @param appealReason the free-text appeal reason, or {@code null} to omit the field
     * @param debugInfo    the JSON-encoded client-debug bundle, or {@code null} to omit the field
     */
    public GroupSuspensionAppealWhatsAppWebGraphQlRequest(Jid groupJid, String appealReason, String debugInfo) {
        this.groupJid = groupJid;
        this.appealReason = appealReason;
        this.debugInfo = debugInfo;
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
     * {@code {"input": {"group_jid": <groupJid>, "appeal_reason": <appealReason>, "debug_info":
     * <debugInfo>}}}, writing each field only when its value is non-null and emitting
     * {@code {"input": {}}} when all are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebGroupSuspensionAppealMutation", exports = "submitGroupSuspensionAppeal",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (groupJid != null) {
                writer.writeName("group_jid");
                writer.writeColon();
                writer.writeString(groupJid.toString());
            }

            if (appealReason != null) {
                writer.writeName("appeal_reason");
                writer.writeColon();
                writer.writeString(appealReason);
            }

            if (debugInfo != null) {
                writer.writeName("debug_info");
                writer.writeColon();
                writer.writeString(debugInfo);
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
}
