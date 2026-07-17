package com.github.auties00.cobalt.wire.graphql.whatsappWeb.acs;

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
 * Builds the relay mutation that requests ACS (Anonymous Credential Service) credential issuance for
 * a batch of blinded tokens.
 *
 * <p>The mutation takes a single {@code input} GraphQL variable of type
 * {@code XWAWAACSCredsIssueRequest}. WhatsApp Web's {@code WAWebACSServerProvider.getCredentials}
 * builds it with the ACS project name, the configuration id returned by the configuration query, the
 * batch of blinded tokens (each base64url-encoded under {@code issue_data}), and the request proof.
 * The relay returns the issuance outcome under {@code xwa_wa_acs_issue_credentials}; the reply is
 * consumed through {@link AcsServerProviderIssuanceWhatsAppWebGraphQlResponse}.
 *
 * @see AcsServerProviderIssuanceWhatsAppWebGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebACSServerProviderIssuanceMutation")
public final class AcsServerProviderIssuanceWhatsAppWebGraphQlRequest implements WhatsAppWebGraphQlOperation.Request {
    /**
     * The persisted document identifier the relay maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the url-encoded request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebACSServerProviderIssuanceMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26039599689054760";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebACSServerProviderIssuanceMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebACSServerProviderIssuanceMutation";

    /**
     * The {@code project_name} field of the {@code input} object naming the ACS project, or
     * {@code null} to omit it.
     */
    private final String projectName;

    /**
     * The {@code config_id} field of the {@code input} object identifying the server-provider
     * configuration the tokens were blinded against, or {@code null} to omit it.
     */
    private final String configId;

    /**
     * The base64url-encoded blinded tokens emitted as the {@code issue_element} array, each wrapped in
     * an {@code {"issue_data": <token>}} object, or {@code null} to omit the array.
     */
    private final List<String> issueElements;

    /**
     * The {@code request_proof} field of the {@code input} object, or {@code null} to omit it.
     */
    private final String requestProof;

    /**
     * Constructs an ACS credential-issuance mutation request.
     *
     * <p>All four values populate the {@code input} GraphQL object. The {@code issueElements} are the
     * base64url-encoded blinded tokens; each becomes an {@code {"issue_data": <token>}} entry of the
     * {@code issue_element} array. Each value that is {@code null} omits its field from the serialized
     * object.
     *
     * @param projectName   the ACS project name, or {@code null} to omit the field
     * @param configId      the server-provider configuration id, or {@code null} to omit the field
     * @param issueElements the base64url-encoded blinded tokens, or {@code null} to omit the array
     * @param requestProof  the request proof, or {@code null} to omit the field
     */
    public AcsServerProviderIssuanceWhatsAppWebGraphQlRequest(String projectName, String configId, List<String> issueElements, String requestProof) {
        this.projectName = projectName;
        this.configId = configId;
        this.issueElements = issueElements;
        this.requestProof = requestProof;
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
     * @implNote This implementation emits {@code {"input": {"project_name": <projectName>,
     * "config_id": <configId>, "issue_element": [{"issue_data": <token>}, ...], "request_proof":
     * <requestProof>}}}, writing each field only when its value is non-null and emitting
     * {@code {"input": {}}} when every value is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebACSServerProvider", exports = "getCredentials",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (projectName != null) {
                writer.writeName("project_name");
                writer.writeColon();
                writer.writeString(projectName);
            }

            if (configId != null) {
                writer.writeName("config_id");
                writer.writeColon();
                writer.writeString(configId);
            }

            if (issueElements != null) {
                writer.writeName("issue_element");
                writer.writeColon();
                writer.startArray();
                for (var index = 0; index < issueElements.size(); index++) {
                    if (index > 0) {
                        writer.writeComma();
                    }
                    writer.startObject();
                    writer.writeName("issue_data");
                    writer.writeColon();
                    writer.writeString(issueElements.get(index));
                    writer.endObject();
                }
                writer.endArray();
            }

            if (requestProof != null) {
                writer.writeName("request_proof");
                writer.writeColon();
                writer.writeString(requestProof);
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
