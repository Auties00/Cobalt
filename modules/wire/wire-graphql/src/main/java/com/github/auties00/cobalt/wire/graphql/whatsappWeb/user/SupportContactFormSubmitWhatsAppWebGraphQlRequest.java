package com.github.auties00.cobalt.wire.graphql.whatsappWeb.user;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.reporting.SupportContactFormContextFlow;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the relay mutation that submits a WhatsApp support contact form.
 *
 * <p>The mutation takes one {@code input} GraphQL variable. WhatsApp Web's
 * {@code WAWebSupportContactFormSubmitMutation.submitContactFormGraphQL(input)} forwards the object
 * built by {@code WAWebSendSupportRequestJob} straight to the relay; the recovered fields are the
 * free-text issue {@code description}, the {@code debug_info_json} diagnostic blob, and the
 * {@code context_flow} discriminator naming the originating support flow (for example
 * {@code "GENERAL"}). The relay returns the submission outcome under
 * {@code xwa_wa_support_contact_form_submit}; the reply is consumed through
 * {@link SupportContactFormSubmitWhatsAppWebGraphQlResponse}.
 *
 * @implNote This implementation models {@code contextFlow} as the typed
 * {@link SupportContactFormContextFlow}: WA Web's sole caller {@code WAWebSendSupportRequestJob} emits
 * the literal {@code "GENERAL"} ({@link SupportContactFormContextFlow#GENERAL}). The server-side
 * context-flow type may define further members, but they are not declared in the client bundle, so
 * only the client-emitted value is modelled.
 *
 * @see SupportContactFormSubmitWhatsAppWebGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebSupportContactFormSubmitMutation")
public final class SupportContactFormSubmitWhatsAppWebGraphQlRequest implements WhatsAppWebGraphQlOperation.Request {
    /**
     * The persisted document identifier the relay maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the url-encoded request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebSupportContactFormSubmitMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26494666453460666";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebSupportContactFormSubmitMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebSupportContactFormSubmitMutation";

    /**
     * The {@code description} field of the {@code input} object carrying the free-text issue
     * description, or {@code null} to omit it.
     */
    private final String description;

    /**
     * The {@code debug_info_json} field of the {@code input} object carrying the diagnostic blob, or
     * {@code null} to omit it.
     *
     * <p>A pre-serialized diagnostic string emitted as a JSON string value, not a nested object, so it
     * is carried as a plain {@link String}.
     */
    private final String debugInfo;

    /**
     * The {@code context_flow} field of the {@code input} object naming the originating support flow
     * (for example {@link SupportContactFormContextFlow#GENERAL}), or {@code null} to omit it.
     */
    private final SupportContactFormContextFlow contextFlow;

    /**
     * Constructs a support-contact-form-submit mutation request.
     *
     * <p>The values populate the {@code input} GraphQL object; each value that is {@code null} is
     * omitted from the serialized object.
     *
     * @param description the free-text issue description, or {@code null} to omit the field
     * @param debugInfo   the diagnostic blob, or {@code null} to omit the field
     * @param contextFlow the originating support flow, or {@code null} to omit the field
     */
    public SupportContactFormSubmitWhatsAppWebGraphQlRequest(String description, String debugInfo, SupportContactFormContextFlow contextFlow) {
        this.description = description;
        this.debugInfo = debugInfo;
        this.contextFlow = contextFlow;
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
     * @implNote This implementation emits {@code {"input": {"description": <description>,
     * "debug_info_json": <debugInfo>, "context_flow": <contextFlow>}}}, writing each field only
     * when its value is non-null and emitting {@code {"input": {}}} when every field is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebSupportContactFormSubmitMutation", exports = "submitContactFormGraphQL",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (description != null) {
                writer.writeName("description");
                writer.writeColon();
                writer.writeString(description);
            }

            if (debugInfo != null) {
                writer.writeName("debug_info_json");
                writer.writeColon();
                writer.writeString(debugInfo);
            }

            if (contextFlow != null) {
                writer.writeName("context_flow");
                writer.writeColon();
                writer.writeString(contextFlow.wireValue());
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
