package com.github.auties00.cobalt.wire.graphql.whatsapp.misc;

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
 * Builds the graph.whatsapp.com GraphQL query that verifies a postcode against a WhatsApp Business direct-connection
 * catalogue.
 *
 * <p>The single {@code request} GraphQL variable nests a {@code verify_postcode} object carrying the
 * business account {@code biz_jid} {@link Jid} and the {@code direct_connection_encrypted_info}
 * cypher token tying the postcode to the catalogue's direct-connection session. WhatsApp Web's
 * {@code WAWebGraphQLVerifyPostcodeJob.verifyPostcode(wid, encryptedInfo)} fills both fields. The
 * graph.whatsapp.com endpoint returns the verification outcome under {@code xwa_product_catalog_get_verify_postcode}; the
 * reply is consumed through {@link GraphQlVerifyPostcodeJobWhatsAppGraphQlResponse}.
 *
 * @see GraphQlVerifyPostcodeJobWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebGraphQLVerifyPostcodeJobQuery")
public final class GraphQlVerifyPostcodeJobWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     *
     * @implNote This implementation ships the live {@code WAWebGraphQLPersistedQueries} numeric id
     * rather than the compiled {@code params.id} literal, which for this operation is the document
     * name {@code "WAWebGraphQLVerifyPostcodeJobQuery"}; the persisted-query map overrides that
     * literal with the numeric id at dispatch time on WhatsApp Web.
     */
    @WhatsAppWebExport(moduleName = "WAWebGraphQLVerifyPostcodeJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "7573183149457062";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebGraphQLVerifyPostcodeJobQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebGraphQLVerifyPostcodeJobQuery";

    /**
     * The {@code biz_jid} field of the nested {@code verify_postcode} object naming the business
     * account whose catalogue is being checked, or {@code null} to omit it.
     */
    private final Jid bizJid;

    /**
     * The {@code direct_connection_encrypted_info} field of the nested {@code verify_postcode} object
     * carrying the direct-connection cypher token, or {@code null} to omit it.
     */
    private final String directConnectionEncryptedInfo;

    /**
     * Constructs a verify-postcode query request carrying the business account and the encrypted
     * direct-connection token.
     *
     * <p>Both values populate the nested {@code verify_postcode} object; each value that is
     * {@code null} is omitted from the serialized object.
     *
     * @param bizJid                        the business account {@link Jid}, or {@code null} to omit
     *                                      the field
     * @param directConnectionEncryptedInfo the direct-connection cypher token, or {@code null} to
     *                                      omit the field
     */
    public GraphQlVerifyPostcodeJobWhatsAppGraphQlRequest(Jid bizJid, String directConnectionEncryptedInfo) {
        this.bizJid = bizJid;
        this.directConnectionEncryptedInfo = directConnectionEncryptedInfo;
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
     * @implNote This implementation emits {@code {"request": {"verify_postcode": {"biz_jid":
     * <bizJid>, "direct_connection_encrypted_info": <directConnectionEncryptedInfo>}}}}, writing each
     * inner field only when its value is non-null and emitting {@code {"request":
     * {"verify_postcode": {}}}} when both are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebGraphQLVerifyPostcodeJob", exports = "verifyPostcode",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("request");
            writer.writeColon();
            writer.startObject();
            writer.writeName("verify_postcode");
            writer.writeColon();
            writer.startObject();
            if (bizJid != null) {
                writer.writeName("biz_jid");
                writer.writeColon();
                writer.writeString(bizJid.toString());
            }

            if (directConnectionEncryptedInfo != null) {
                writer.writeName("direct_connection_encrypted_info");
                writer.writeColon();
                writer.writeString(directConnectionEncryptedInfo);
            }
            writer.endObject();
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
