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
 * Builds the graph.whatsapp.com GraphQL query that fetches the localised commerce price tiers available to a WhatsApp
 * Business catalog.
 *
 * <p>The single {@code request} GraphQL variable is the
 * {@code XWAWhatsAppGetPricingTiersRequest} input object. WhatsApp Web's
 * {@code WAWebBizGetPriceTiersQuery} fills it with a single {@code locale} field naming the locale to
 * localise the tier descriptions and currency symbols. The graph.whatsapp.com endpoint returns the tier catalog under
 * {@code xwa_whatsapp_get_pricing_tiers}; the reply is consumed through
 * {@link BizGetPriceTiersWhatsAppGraphQlResponse}.
 *
 * @see BizGetPriceTiersWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizGetPriceTiersQuery")
public final class BizGetPriceTiersWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizGetPriceTiersQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "6190826684377935";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizGetPriceTiersQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizGetPriceTiersQuery";

    /**
     * The {@code locale} field of the {@code request} object naming the locale used to localise the
     * tier descriptions and currency symbols, or {@code null} to omit it.
     */
    private final String locale;

    /**
     * Constructs a price-tiers query request localised to the given locale.
     *
     * <p>The {@code locale} populates the {@code locale} field of the {@code request} GraphQL object;
     * a {@code null} value omits the field.
     *
     * @param locale the locale used to localise the tier descriptions and currency symbols, or
     *               {@code null} to omit the field
     */
    public BizGetPriceTiersWhatsAppGraphQlRequest(String locale) {
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
     * @implNote This implementation emits {@code {"request": {"locale": <locale>}}}, writing the
     * {@code locale} field only when {@link #locale} is non-null and emitting {@code {"request": {}}}
     * otherwise.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizGetPriceTiersQuery", exports = "getPriceTiers",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("request");
            writer.writeColon();
            writer.startObject();
            if (locale != null) {
                writer.writeName("locale");
                writer.writeColon();
                writer.writeString(locale);
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
