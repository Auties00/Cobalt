package com.github.auties00.cobalt.wire.graphql.whatsapp.misc;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlEnvironment;
import java.util.Optional;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the graph.whatsapp.com GraphQL query that maps a batch of custom chat labels to their click-to-WhatsApp (CTWA)
 * third-party conversion events.
 *
 * <p>The query takes two GraphQL variables. The {@code custom_labels} list carries the custom-label
 * objects to resolve, each a single-field object {@code {"custom_label": <name>}}; WhatsApp Web's
 * {@code WAWebCustomLabels3pdSignalUtils} collects these from the chat's predefined-id-free labels.
 * The {@code expt_group} carries the CTWA custom-label algorithm experiment group, sourced from the
 * {@code ctwa_custom_label_algorithm} AB-prop config value. The graph.whatsapp.com endpoint returns the matching conversion
 * events under {@code xwa_get_3pd_event}; the reply is consumed through
 * {@link CustomLabel3PdEventWhatsAppGraphQlResponse}.
 *
 * @see CustomLabel3PdEventWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebCustomLabel3pdEventQuery")
public final class CustomLabel3PdEventWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebCustomLabel3pdEventQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "24247439618185103";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebCustomLabel3pdEventQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebCustomLabel3pdEventQuery";

    /**
     * The custom-label names populating the {@code custom_labels} variable, or {@code null} to omit
     * the variable.
     *
     * <p>Each name is serialized as a single-field object {@code {"custom_label": <name>}} matching the
     * {@code XWACustomLabelInput} shape WhatsApp Web sends.
     */
    private final List<String> customLabels;

    /**
     * The {@code expt_group} variable carrying the CTWA custom-label algorithm experiment group, or
     * {@code null} to omit the variable.
     */
    private final String exptGroup;

    /**
     * Constructs a custom-label-to-3PD-event query request.
     *
     * <p>The {@code customLabels} are the custom-label names to resolve, each serialized as a
     * {@code {"custom_label": <name>}} object. The {@code exptGroup} is the CTWA custom-label
     * algorithm experiment group. Each value that is {@code null} omits its variable from the
     * serialized object.
     *
     * @param customLabels the custom-label names to resolve, or {@code null} to omit the variable
     * @param exptGroup    the CTWA custom-label algorithm experiment group, or {@code null} to omit
     *                     the variable
     */
    public CustomLabel3PdEventWhatsAppGraphQlRequest(List<String> customLabels, String exptGroup) {
        this.customLabels = customLabels;
        this.exptGroup = exptGroup;
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
     * @implNote This implementation emits {@code {"custom_labels": [{"custom_label": <name>}, ...],
     * "expt_group": <exptGroup>}}, writing each variable only when its value is non-null, rendering
     * every label name as a single-field {@code custom_label} object, and emitting {@code "{}"} when
     * both variables are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebCustomLabel3pdEventQuery", exports = "getCustomLabel3pdEvent",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (customLabels != null) {
                writer.writeName("custom_labels");
                writer.writeColon();
                writer.startArray();
                for (var label : customLabels) {
                    writer.startObject();
                    writer.writeName("custom_label");
                    writer.writeColon();
                    writer.writeString(label);
                    writer.endObject();
                }
                writer.endArray();
            }

            if (exptGroup != null) {
                writer.writeName("expt_group");
                writer.writeColon();
                writer.writeString(exptGroup);
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
