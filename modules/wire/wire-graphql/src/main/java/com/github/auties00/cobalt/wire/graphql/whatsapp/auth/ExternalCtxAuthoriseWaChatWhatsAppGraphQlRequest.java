package com.github.auties00.cobalt.wire.graphql.whatsapp.auth;

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
 * Builds the graph.whatsapp.com GraphQL mutation that authorises an external-context (deep-link) chat partner.
 *
 * <p>The single {@code input} GraphQL variable is the external-context authorisation object. WhatsApp
 * Web fills it from {@code WAWebExternalCtxAuthoriseWAChat} with the recipient chat
 * {@code recipient_jid}, the {@code deeplink_type} that opened the chat, the {@code deeplink_source}
 * (the literal {@code "1"} for an external entry point or {@code "2"} for an internal one), the
 * {@code deeplink_platform} (always {@code "Web"} for this client), and the {@code partner_token}
 * carried by the deep link. The graph.whatsapp.com endpoint returns the authorisation outcome under
 * {@code xwa_external_ctx_authorise_wa_chat}; the reply is consumed through
 * {@link ExternalCtxAuthoriseWaChatWhatsAppGraphQlResponse}.
 *
 * @see ExternalCtxAuthoriseWaChatWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebExternalCtxAuthoriseWAChatMutation")
public final class ExternalCtxAuthoriseWaChatWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The {@code deeplink_source} value WhatsApp Web sends when the deep link originated from an
     * external entry point.
     */
    private static final String DEEPLINK_SOURCE_EXTERNAL = "1";

    /**
     * The {@code deeplink_source} value WhatsApp Web sends when the deep link originated from an
     * internal entry point.
     */
    private static final String DEEPLINK_SOURCE_INTERNAL = "2";

    /**
     * The {@code deeplink_platform} value this client always reports.
     */
    private static final String DEEPLINK_PLATFORM = "Web";

    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebExternalCtxAuthoriseWAChatMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9790465291023292";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebExternalCtxAuthoriseWAChatMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebExternalCtxAuthoriseWAChatMutation";

    /**
     * The {@code recipient_jid} field of the {@code input} object naming the chat being authorised, or
     * {@code null} to omit it.
     */
    private final Jid recipientJid;

    /**
     * The {@code deeplink_type} field of the {@code input} object describing the deep link that opened
     * the chat, or {@code null} to omit it.
     */
    private final String deeplinkType;

    /**
     * Whether the deep link originated from an external entry point, or {@code null} to omit the
     * {@code deeplink_source} field.
     *
     * <p>When {@code true} the serialized {@code deeplink_source} is {@value #DEEPLINK_SOURCE_EXTERNAL};
     * when {@code false} it is {@value #DEEPLINK_SOURCE_INTERNAL}.
     */
    private final Boolean external;

    /**
     * The {@code partner_token} field of the {@code input} object carried by the deep link, or
     * {@code null} to omit it.
     */
    private final String partnerToken;

    /**
     * Constructs an external-context authorise-chat mutation request.
     *
     * <p>Each value populates the {@code input} GraphQL object; a {@code null} value omits its field
     * from the serialized object. The {@code external} flag selects the {@code deeplink_source} token
     * ({@value #DEEPLINK_SOURCE_EXTERNAL} when external, {@value #DEEPLINK_SOURCE_INTERNAL} when
     * internal). The {@code deeplink_platform} is always emitted as {@value #DEEPLINK_PLATFORM}.
     *
     * @param recipientJid the chat {@link Jid} being authorised, or {@code null} to omit the field
     * @param deeplinkType the deep-link type that opened the chat, or {@code null} to omit the field
     * @param external     whether the deep link is external, or {@code null} to omit
     *                     {@code deeplink_source}
     * @param partnerToken the partner token carried by the deep link, or {@code null} to omit the
     *                     field
     */
    public ExternalCtxAuthoriseWaChatWhatsAppGraphQlRequest(Jid recipientJid, String deeplinkType, Boolean external, String partnerToken) {
        this.recipientJid = recipientJid;
        this.deeplinkType = deeplinkType;
        this.external = external;
        this.partnerToken = partnerToken;
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
     * @implNote This implementation emits {@code {"input": {"recipient_jid": <recipientJid>,
     * "deeplink_type": <deeplinkType>, "deeplink_source": <"1"|"2">, "deeplink_platform": "Web",
     * "partner_token": <partnerToken>}}}, writing each field only when its backing value is non-null,
     * always emitting {@code deeplink_platform} as {@value #DEEPLINK_PLATFORM}, and rendering
     * {@code deeplink_source} from the {@code external} flag.
     */
    @WhatsAppWebExport(moduleName = "WAWebExternalCtxAuthoriseWAChat", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (recipientJid != null) {
                writer.writeName("recipient_jid");
                writer.writeColon();
                writer.writeString(recipientJid.toString());
            }

            if (deeplinkType != null) {
                writer.writeName("deeplink_type");
                writer.writeColon();
                writer.writeString(deeplinkType);
            }

            if (external != null) {
                writer.writeName("deeplink_source");
                writer.writeColon();
                writer.writeString(external ? DEEPLINK_SOURCE_EXTERNAL : DEEPLINK_SOURCE_INTERNAL);
            }

            writer.writeName("deeplink_platform");
            writer.writeColon();
            writer.writeString(DEEPLINK_PLATFORM);

            if (partnerToken != null) {
                writer.writeName("partner_token");
                writer.writeColon();
                writer.writeString(partnerToken);
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
