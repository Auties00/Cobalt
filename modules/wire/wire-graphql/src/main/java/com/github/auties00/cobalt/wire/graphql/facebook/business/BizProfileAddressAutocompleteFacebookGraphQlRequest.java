package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessGeoPoint;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL query that resolves a postal-address typeahead for a WhatsApp Business profile.
 *
 * <p>The single {@code input} GraphQL variable is the Maps typeahead query parameters object; the
 * compiled document remaps it onto the server-side {@code query_params} argument of the
 * {@code whatsapp_maps_typeahead} field, carrying the map {@code center} {@link BusinessGeoPoint} to
 * bias suggestions around, the free-text {@code query}, and the {@code use_case_id} scoping the lookup.
 * The Meta graph endpoint returns the ranked typeahead matches under {@code whatsapp_maps_typeahead};
 * the reply is consumed through {@link BizProfileAddressAutocompleteFacebookGraphQlResponse}.
 *
 * @see BizProfileAddressAutocompleteFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizProfileAddressAutocompleteQuery")
public final class BizProfileAddressAutocompleteFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizProfileAddressAutocompleteQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "34963438739971331";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizProfileAddressAutocompleteQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizProfileAddressAutocompleteQuery";

    /**
     * The {@code center} field of the {@code input} object biasing suggestions around a map center, or
     * {@code null} to omit it.
     */
    private final BusinessGeoPoint center;

    /**
     * The {@code query} field of the {@code input} object carrying the free-text address query, or
     * {@code null} to omit it.
     */
    private final String query;

    /**
     * The {@code use_case_id} field of the {@code input} object scoping the lookup, or {@code null} to
     * omit it.
     */
    private final String useCaseId;

    /**
     * Constructs an address-autocomplete query request.
     *
     * <p>Each value that is {@code null} omits its field from the serialized {@code input} object.
     *
     * @param center    the map center to bias suggestions around, or {@code null} to omit the field
     * @param query     the free-text address query, or {@code null} to omit the field
     * @param useCaseId the use-case identifier scoping the lookup, or {@code null} to omit the field
     */
    public BizProfileAddressAutocompleteFacebookGraphQlRequest(BusinessGeoPoint center, String query, String useCaseId) {
        this.center = center;
        this.query = query;
        this.useCaseId = useCaseId;
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
     * @implNote This implementation emits {@code {"input": {"center": {"latitude": ..., "longitude":
     * ...}, "query": <query>, "use_case_id": <useCaseId>}}}, writing each field only when its value is
     * non-null and emitting {@code {"input": {}}} when every field is {@code null}.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (center != null) {
                writer.writeName("center");
                writer.writeColon();
                writer.startObject();
                center.latitude().ifPresent(value -> {
                    writer.writeName("latitude");
                    writer.writeColon();
                    writer.writeDouble(value);
                });
                center.longitude().ifPresent(value -> {
                    writer.writeName("longitude");
                    writer.writeColon();
                    writer.writeDouble(value);
                });
                writer.endObject();
            }

            if (query != null) {
                writer.writeName("query");
                writer.writeColon();
                writer.writeString(query);
            }

            if (useCaseId != null) {
                writer.writeName("use_case_id");
                writer.writeColon();
                writer.writeString(useCaseId);
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
