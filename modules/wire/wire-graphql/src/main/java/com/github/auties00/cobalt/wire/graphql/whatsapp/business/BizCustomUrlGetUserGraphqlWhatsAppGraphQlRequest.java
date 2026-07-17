package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

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

/**
 * Builds the graph.whatsapp.com GraphQL query that resolves a WhatsApp Business short-link custom-url path to its owning
 * account.
 *
 * <p>The single {@code data} GraphQL variable is an object whose only field is the nested
 * {@code custom_url} object carrying the {@code path} slug to resolve. WhatsApp Web's
 * {@code WAWebBizCustomUrlGetUserGraphql.queryCustomUrlGetUser(path)} fills it as
 * {@code {custom_url: {path}}}. The graph.whatsapp.com endpoint returns the resolution outcome under the linked
 * {@code xwa_custom_url_get_user}; the reply is consumed through
 * {@link BizCustomUrlGetUserGraphqlWhatsAppGraphQlResponse}.
 *
 * @see BizCustomUrlGetUserGraphqlWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizCustomUrlGetUserGraphqlQuery")
public final class BizCustomUrlGetUserGraphqlWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCustomUrlGetUserGraphqlQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26867176859566677";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCustomUrlGetUserGraphqlQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizCustomUrlGetUserGraphqlQuery";

    /**
     * The {@code path} slug of the nested {@code custom_url} object naming the short-link to resolve,
     * or {@code null} to omit the {@code custom_url} object.
     */
    private final String path;

    /**
     * Constructs a custom-url resolution request carrying the short-link path slug.
     *
     * <p>The {@code path} populates the nested {@code custom_url} object of the {@code data} GraphQL
     * variable. A {@code null} {@code path} omits the {@code custom_url} object from the serialized
     * variable.
     *
     * @param path the short-link path slug to resolve, or {@code null} to omit the field
     */
    public BizCustomUrlGetUserGraphqlWhatsAppGraphQlRequest(String path) {
        this.path = path;
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
     * @implNote This implementation emits {@code {"data": {"custom_url": {"path": <path>}}}}, writing
     * the {@code custom_url} object only when {@code path} is non-null and emitting
     * {@code {"data": {}}} when it is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCustomUrlGetUserGraphql", exports = "queryCustomUrlGetUser",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("data");
            writer.writeColon();
            writer.startObject();
            if (path != null) {
                writer.writeName("custom_url");
                writer.writeColon();
                writer.startObject();
                writer.writeName("path");
                writer.writeColon();
                writer.writeString(path);
                writer.endObject();
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
