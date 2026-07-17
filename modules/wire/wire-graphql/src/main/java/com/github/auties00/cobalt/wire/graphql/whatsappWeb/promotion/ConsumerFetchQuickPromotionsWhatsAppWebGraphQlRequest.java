package com.github.auties00.cobalt.wire.graphql.whatsappWeb.promotion;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the relay query that fetches the eligible quick-promotion banners for a set of consumer
 * (non-Business) WhatsApp surfaces.
 *
 * <p>The query takes two GraphQL variables. The {@code nux_ids} variable is the list of surface NUX
 * identifiers to evaluate, forwarded to the server-side field argument {@code surface_nux_ids}. The
 * {@code trigger_context} variable carries the client context the eligibility engine matches against;
 * WhatsApp Web's {@code WAWebConsumerFetchQuickPromotions.fetchConsumerQuickPromotions()} fills it
 * with the {@code wa_smb_trigger_context} sub-object holding the {@code is_from_wa_smb} flag (set to
 * {@code false} on the consumer build), the {@code app_version} base string, the caller's
 * {@code country} short-code, and the user {@code locale}. The relay also pins two server-side literal
 * arguments ({@code include_holdouts} and {@code supports_client_side_filters}, both {@code true}) that
 * are baked into the persisted document and carry no client variable. The relay returns one
 * surface-keyed batch under {@code quick_promotion_multiverse_batch_fetch_root}; the reply is consumed
 * through {@link ConsumerFetchQuickPromotionsWhatsAppWebGraphQlResponse}.
 *
 * @see ConsumerFetchQuickPromotionsWhatsAppWebGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebConsumerFetchQuickPromotionsQuery")
public final class ConsumerFetchQuickPromotionsWhatsAppWebGraphQlRequest implements WhatsAppWebGraphQlOperation.Request {
    /**
     * The persisted document identifier the relay maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the url-encoded request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebConsumerFetchQuickPromotionsQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "35462584533386409";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebConsumerFetchQuickPromotionsQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebConsumerFetchQuickPromotionsQuery";

    /**
     * The {@code nux_ids} GraphQL variable listing the surface NUX identifiers to evaluate, or
     * {@code null} to omit it.
     *
     * <p>Forwarded to the server-side field argument {@code surface_nux_ids}.
     */
    private final List<String> nuxIds;

    /**
     * The {@code trigger_context} GraphQL variable carrying the client context the eligibility engine
     * matches against, or {@code null} to omit it.
     */
    private final TriggerContext triggerContext;

    /**
     * Constructs a consumer fetch-quick-promotions query request carrying the surface identifiers to
     * evaluate and the trigger context.
     *
     * <p>Each value that is {@code null} is omitted from the serialized variables object.
     *
     * @param nuxIds         the surface NUX identifiers to evaluate, or {@code null} to omit the
     *                       variable
     * @param triggerContext the client trigger context, or {@code null} to omit the variable
     */
    public ConsumerFetchQuickPromotionsWhatsAppWebGraphQlRequest(List<String> nuxIds, TriggerContext triggerContext) {
        this.nuxIds = nuxIds;
        this.triggerContext = triggerContext;
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
     * @implNote This implementation emits {@code {"nux_ids": [<nuxIds>...], "trigger_context":
     * {"wa_smb_trigger_context": {...}}}}, writing each variable only when its value is non-null and
     * emitting {@code "{}"} when both are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebConsumerFetchQuickPromotions", exports = "fetchConsumerQuickPromotions",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (nuxIds != null) {
                writer.writeName("nux_ids");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < nuxIds.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    writer.writeString(nuxIds.get(i));
                }
                writer.endArray();
            }

            if (triggerContext != null) {
                writer.writeName("trigger_context");
                writer.writeColon();
                triggerContext.write(writer);
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
     * Models the {@code trigger_context} GraphQL variable.
     *
     * <p>Wraps the single {@code wa_smb_trigger_context} sub-object the eligibility engine matches
     * against. Every field is optional; a {@code null} field is omitted from the serialized object.
     *
     * @param isFromWaSmb whether the request originates from the WhatsApp Business build, or
     *                    {@code null} to omit the field
     * @param appVersion  the client app version base string, or {@code null} to omit the field
     * @param country     the caller's ISO country short-code, or {@code null} to omit the field
     * @param locale      the user locale, or {@code null} to omit the field
     */
    public record TriggerContext(Boolean isFromWaSmb, String appVersion, String country, String locale) {
        /**
         * Writes this trigger context onto the given JSON writer as the
         * {@code {"wa_smb_trigger_context": {...}}} object.
         *
         * <p>Reserved for {@link ConsumerFetchQuickPromotionsWhatsAppWebGraphQlRequest#variables()}; writes each
         * field only when its value is non-null.
         *
         * @param writer the JSON writer to emit the object onto
         */
        void write(JSONWriter writer) {
            writer.startObject();
            writer.writeName("wa_smb_trigger_context");
            writer.writeColon();
            writer.startObject();
            if (isFromWaSmb != null) {
                writer.writeName("is_from_wa_smb");
                writer.writeColon();
                writer.writeBool(isFromWaSmb);
            }

            if (appVersion != null) {
                writer.writeName("app_version");
                writer.writeColon();
                writer.writeString(appVersion);
            }

            if (country != null) {
                writer.writeName("country");
                writer.writeColon();
                writer.writeString(country);
            }

            if (locale != null) {
                writer.writeName("locale");
                writer.writeColon();
                writer.writeString(locale);
            }
            writer.endObject();
            writer.endObject();
        }
    }
}
