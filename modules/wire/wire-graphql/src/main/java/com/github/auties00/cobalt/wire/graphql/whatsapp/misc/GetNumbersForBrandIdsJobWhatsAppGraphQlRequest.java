package com.github.auties00.cobalt.wire.graphql.whatsapp.misc;

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
import java.util.List;

/**
 * Builds the graph.whatsapp.com GraphQL query that resolves the WhatsApp phone numbers and LIDs backing a set of
 * marketing brand identifiers.
 *
 * <p>The single {@code input} GraphQL variable, forwarded to the server-side field argument named
 * {@code request}, carries the list of {@code brand_ids} to resolve and a {@code lid_based_response}
 * flag selecting whether the graph.whatsapp.com endpoint returns LIDs rather than phone numbers. WhatsApp Web's
 * {@code WAWebGetNumbersForBrandIdsJob.getNumbersForBrandIdsJob(brandIds)} fills the list from the
 * caller's opt-out brand identifiers and the flag from the marketing-messages LID gating check. The
 * graph.whatsapp.com endpoint returns one record per brand id under {@code xwa_get_numbers_for_brand_ids}; the reply is
 * consumed through {@link GetNumbersForBrandIdsJobWhatsAppGraphQlResponse}.
 *
 * @see GetNumbersForBrandIdsJobWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebGetNumbersForBrandIdsJobQuery")
public final class GetNumbersForBrandIdsJobWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebGetNumbersForBrandIdsJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "33391034967211217";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebGetNumbersForBrandIdsJobQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebGetNumbersForBrandIdsJobQuery";

    /**
     * The {@code brand_ids} field of the {@code input} object listing the marketing brand identifiers
     * to resolve, or {@code null} to omit it.
     */
    private final List<String> brandIds;

    /**
     * The {@code lid_based_response} field of the {@code input} object selecting whether the graph.whatsapp.com endpoint
     * returns LIDs instead of phone numbers, or {@code null} to omit it.
     */
    private final Boolean lidBasedResponse;

    /**
     * Constructs a get-numbers-for-brand-ids query request carrying the brand identifiers to resolve
     * and the LID-based response flag.
     *
     * <p>Both values populate the {@code input} GraphQL object; each value that is {@code null} is
     * omitted from the serialized object.
     *
     * @param brandIds         the marketing brand identifiers to resolve, or {@code null} to omit the
     *                         field
     * @param lidBasedResponse whether the graph.whatsapp.com endpoint returns LIDs instead of phone numbers, or
     *                         {@code null} to omit the field
     */
    public GetNumbersForBrandIdsJobWhatsAppGraphQlRequest(List<String> brandIds, Boolean lidBasedResponse) {
        this.brandIds = brandIds;
        this.lidBasedResponse = lidBasedResponse;
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
     * @implNote This implementation emits {@code {"input": {"brand_ids": [<brandIds>...],
     * "lid_based_response": <lidBasedResponse>}}}, writing each field only when its value is non-null
     * and emitting {@code {"input": {}}} when both are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebGetNumbersForBrandIdsJob", exports = "getNumbersForBrandIdsJob",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (brandIds != null) {
                writer.writeName("brand_ids");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < brandIds.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    writer.writeString(brandIds.get(i));
                }
                writer.endArray();
            }

            if (lidBasedResponse != null) {
                writer.writeName("lid_based_response");
                writer.writeColon();
                writer.writeBool(lidBasedResponse);
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
        return WhatsAppGraphQlEnvironment.WWW;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> accessToken() {
        return Optional.empty();
    }
}
