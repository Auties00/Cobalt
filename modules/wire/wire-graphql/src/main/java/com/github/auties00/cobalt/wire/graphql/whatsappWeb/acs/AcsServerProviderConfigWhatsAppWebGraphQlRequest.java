package com.github.auties00.cobalt.wire.graphql.whatsappWeb.acs;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the relay query that fetches an ACS (Anonymous Credential Service) server-provider
 * configuration for the named project.
 *
 * <p>The operation takes a single {@code project_name} GraphQL variable identifying the ACS project
 * whose token-issuance parameters are requested. WhatsApp Web's
 * {@code WAWebACSServerProvider.getPublicParameters(projectName)} forwards it straight to the relay
 * via {@code WAWebRelayClient.fetchQuery}. The relay returns the configuration under
 * {@code xwa_wa_acs_config}; the reply is consumed through {@link AcsServerProviderConfigWhatsAppWebGraphQlResponse}.
 *
 * @see AcsServerProviderConfigWhatsAppWebGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebACSServerProviderConfigQuery")
public final class AcsServerProviderConfigWhatsAppWebGraphQlRequest implements WhatsAppWebGraphQlOperation.Request {
    /**
     * The persisted document identifier the relay maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the url-encoded request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebACSServerProviderConfigQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25133761326299603";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebACSServerProviderConfigQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebACSServerProviderConfigQuery";

    /**
     * The {@code project_name} GraphQL variable identifying the ACS project whose configuration is
     * requested, or {@code null} to omit it.
     */
    private final String projectName;

    /**
     * Constructs an ACS server-provider configuration request for the named project.
     *
     * <p>The {@code projectName} populates the {@code project_name} GraphQL variable; a {@code null}
     * value omits the variable from the serialized object.
     *
     * @param projectName the ACS project name to fetch the configuration for, or {@code null} to omit
     *                    the variable
     */
    public AcsServerProviderConfigWhatsAppWebGraphQlRequest(String projectName) {
        this.projectName = projectName;
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
     * @implNote This implementation emits {@code {"project_name": <projectName>}}, writing the field
     * only when its value is non-null and emitting {@code "{}"} when it is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebACSServerProvider", exports = "getPublicParameters",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (projectName != null) {
                writer.writeName("project_name");
                writer.writeColon();
                writer.writeString(projectName);
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
