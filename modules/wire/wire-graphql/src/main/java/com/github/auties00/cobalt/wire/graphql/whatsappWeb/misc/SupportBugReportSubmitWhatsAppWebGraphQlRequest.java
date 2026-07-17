package com.github.auties00.cobalt.wire.graphql.whatsappWeb.misc;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.support.SupportBugReportSubmissionRequest;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the relay mutation that submits a WhatsApp support bug report.
 *
 * <p>The single {@code input} GraphQL variable is the server-side bug-report payload carried as a
 * {@link SupportBugReportSubmissionRequest}: a category, a title, a free-text description, a
 * pre-serialized diagnostic blob, a device-log handle, a client-server join key, and uploaded media
 * handles. WhatsApp Web's {@code WAWebSupportBugReportSubmitMutation.submitBugReportGraphQL(input)}
 * forwards the object to the relay, which returns the submission outcome under
 * {@code xwa_wa_support_bug_report_submit}; the reply is consumed through
 * {@link SupportBugReportSubmitWhatsAppWebGraphQlResponse}.
 *
 * @see SupportBugReportSubmitWhatsAppWebGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebSupportBugReportSubmitMutation")
public final class SupportBugReportSubmitWhatsAppWebGraphQlRequest implements WhatsAppWebGraphQlOperation.Request {
    /**
     * The persisted document identifier the relay maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the url-encoded request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebSupportBugReportSubmitMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25952242091096312";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebSupportBugReportSubmitMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebSupportBugReportSubmitMutation";

    /**
     * The {@code input} GraphQL variable carrying the bug-report payload, or {@code null} to omit it.
     */
    private final SupportBugReportSubmissionRequest input;

    /**
     * Constructs a submit-bug-report mutation request.
     *
     * <p>The {@code input} holds the bug-report payload. A {@code null} value omits the variable from
     * the serialized object.
     *
     * @param input the bug-report payload, or {@code null} to omit the variable
     */
    public SupportBugReportSubmitWhatsAppWebGraphQlRequest(SupportBugReportSubmissionRequest input) {
        this.input = input;
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
     * @implNote This implementation emits {@code {"input": {"category": ..., "description": ...,
     * "title": ..., "debug_info_json": ..., "device_log_handle": ..., "client_server_join_key": ...,
     * "media": [{"cipher_key": ..., "element_value": ..., "iv": ..., "type": ..., "file_name": ...},
     * ...]}}}, writing each field only when its value is present (and {@code media} only when
     * non-empty), and emitting {@code "{}"} when the input is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebSupportBugReportSubmitMutation", exports = "submitBugReportGraphQL",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (input != null) {
                writer.writeName("input");
                writer.writeColon();
                writer.startObject();
                input.category().ifPresent(value -> {
                    writer.writeName("category");
                    writer.writeColon();
                    writer.writeString(value);
                });
                input.description().ifPresent(value -> {
                    writer.writeName("description");
                    writer.writeColon();
                    writer.writeString(value);
                });
                input.title().ifPresent(value -> {
                    writer.writeName("title");
                    writer.writeColon();
                    writer.writeString(value);
                });
                input.debugInfoJson().ifPresent(value -> {
                    writer.writeName("debug_info_json");
                    writer.writeColon();
                    writer.writeString(value);
                });
                input.deviceLogHandle().ifPresent(value -> {
                    writer.writeName("device_log_handle");
                    writer.writeColon();
                    writer.writeString(value);
                });
                input.clientServerJoinKey().ifPresent(value -> {
                    writer.writeName("client_server_join_key");
                    writer.writeColon();
                    writer.writeString(value);
                });
                var media = input.media();
                if (!media.isEmpty()) {
                    writer.writeName("media");
                    writer.writeColon();
                    writer.startArray();
                    for (var i = 0; i < media.size(); i++) {
                        if (i > 0) {
                            writer.writeComma();
                        }
                        var attachment = media.get(i);
                        writer.startObject();
                        attachment.cipherKey().ifPresent(value -> {
                            writer.writeName("cipher_key");
                            writer.writeColon();
                            writer.writeString(value);
                        });
                        attachment.elementValue().ifPresent(value -> {
                            writer.writeName("element_value");
                            writer.writeColon();
                            writer.writeString(value);
                        });
                        attachment.iv().ifPresent(value -> {
                            writer.writeName("iv");
                            writer.writeColon();
                            writer.writeString(value);
                        });
                        attachment.type().ifPresent(value -> {
                            writer.writeName("type");
                            writer.writeColon();
                            writer.writeString(value);
                        });
                        attachment.fileName().ifPresent(value -> {
                            writer.writeName("file_name");
                            writer.writeColon();
                            writer.writeString(value);
                        });
                        writer.endObject();
                    }
                    writer.endArray();
                }
                writer.endObject();
            }
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
