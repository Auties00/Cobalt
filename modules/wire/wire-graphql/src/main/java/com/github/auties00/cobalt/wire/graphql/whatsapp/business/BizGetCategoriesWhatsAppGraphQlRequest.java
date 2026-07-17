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
 * Builds the graph.whatsapp.com GraphQL query that fetches the flat WhatsApp Business profile-category typeahead list.
 *
 * <p>The single {@code query_params} GraphQL variable is an object carrying the typeahead
 * {@code query} prefix, the request {@code locale}, and two fixed discriminators: an
 * {@code operation} of {@code "PROFILE_TYPEAHEAD"} and a {@code version} of {@code "V_1"}. WhatsApp
 * Web's {@code WAWebBizGetCategoriesQuery.getBusinessCategories(query, locale)} fills it from those
 * two caller inputs. The graph.whatsapp.com endpoint returns the matching categories under the linked
 * {@code whatsapp_catkit_typeahead_proxy}; the reply is consumed through
 * {@link BizGetCategoriesWhatsAppGraphQlResponse}.
 *
 * @see BizGetCategoriesWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizGetCategoriesQuery")
public final class BizGetCategoriesWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizGetCategoriesQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26266473919627648";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizGetCategoriesQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizGetCategoriesQuery";

    /**
     * The fixed {@code operation} discriminator of the {@code query_params} object selecting the
     * profile-typeahead lookup.
     */
    private static final String OPERATION = "PROFILE_TYPEAHEAD";

    /**
     * The fixed {@code version} discriminator of the {@code query_params} object selecting the flat
     * (version one) category list shape.
     */
    private static final String VERSION = "V_1";

    /**
     * The {@code query} prefix of the {@code query_params} object the typeahead matches against, or
     * {@code null} to omit it.
     */
    private final String query;

    /**
     * The {@code locale} of the {@code query_params} object selecting the localised display names, or
     * {@code null} to omit it.
     */
    private final String locale;

    /**
     * Constructs a category-typeahead request carrying the query prefix and locale.
     *
     * <p>The {@code query} and {@code locale} populate the {@code query_params} object alongside the
     * fixed {@code operation} and {@code version} discriminators. Each value that is {@code null} is
     * omitted from the serialized object.
     *
     * @param query  the typeahead query prefix, or {@code null} to omit the field
     * @param locale the request locale, or {@code null} to omit the field
     */
    public BizGetCategoriesWhatsAppGraphQlRequest(String query, String locale) {
        this.query = query;
        this.locale = locale;
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
     * @implNote This implementation emits {@code {"query_params": {"query": <query>, "locale":
     * <locale>, "operation": "PROFILE_TYPEAHEAD", "version": "V_1"}}}, writing {@code query} and
     * {@code locale} only when non-null and always emitting the fixed {@code operation} and
     * {@code version} discriminators.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizGetCategoriesQuery", exports = "getBusinessCategories",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("query_params");
            writer.writeColon();
            writer.startObject();
            if (query != null) {
                writer.writeName("query");
                writer.writeColon();
                writer.writeString(query);
            }

            if (locale != null) {
                writer.writeName("locale");
                writer.writeColon();
                writer.writeString(locale);
            }

            writer.writeName("operation");
            writer.writeColon();
            writer.writeString(OPERATION);
            writer.writeName("version");
            writer.writeColon();
            writer.writeString(VERSION);
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
