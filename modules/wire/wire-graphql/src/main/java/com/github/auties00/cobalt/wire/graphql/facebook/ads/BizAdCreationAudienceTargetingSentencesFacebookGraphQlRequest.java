package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL query that renders the human-readable targeting sentences for a WhatsApp Business
 * ad-creation audience.
 *
 * <p>The query takes four scalar GraphQL variables passed straight through to
 * {@code lwi.targeting_sentences(...)}: {@code ad_account_id} is the Facebook ad-account legacy id,
 * {@code audience_option} is the audience option discriminator, {@code location_only} restricts the
 * rendered sentences to the location facet, and {@code targeting_spec_string} is the JSON-encoded
 * targeting spec to describe. The query returns the rendered sentences keyed by category; the reply
 * is consumed through {@link BizAdCreationAudienceTargetingSentencesFacebookGraphQlResponse}.
 *
 * @see BizAdCreationAudienceTargetingSentencesFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationAudienceTargetingSentencesQuery")
public final class BizAdCreationAudienceTargetingSentencesFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationAudienceTargetingSentencesQuery.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "27057381273953167";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationAudienceTargetingSentencesQuery.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAdCreationAudienceTargetingSentencesQuery";

    /**
     * The {@code ad_account_id} GraphQL variable carrying the Facebook ad-account legacy id, or
     * {@code null} to omit it.
     */
    private final String adAccountId;

    /**
     * The {@code audience_option} GraphQL variable naming the audience option discriminator, or
     * {@code null} to omit it.
     */
    private final String audienceOption;

    /**
     * The {@code location_only} GraphQL variable restricting the rendered sentences to the location
     * facet, or {@code null} to omit it.
     */
    private final Boolean locationOnly;

    /**
     * The {@code targeting_spec_string} GraphQL variable carrying the JSON-encoded targeting spec to
     * describe, or {@code null} to omit it.
     */
    private final String targetingSpecString;

    /**
     * Constructs a targeting-sentences query request.
     *
     * <p>Each value that is {@code null} omits its variable from the serialized object.
     *
     * @param adAccountId         the Facebook ad-account legacy id, or {@code null} to omit the
     *                            variable
     * @param audienceOption      the audience option discriminator, or {@code null} to omit the
     *                            variable
     * @param locationOnly        whether to restrict the sentences to the location facet, or
     *                            {@code null} to omit the variable
     * @param targetingSpecString the JSON-encoded targeting spec to describe, or {@code null} to omit
     *                            the variable
     */
    public BizAdCreationAudienceTargetingSentencesFacebookGraphQlRequest(String adAccountId, String audienceOption, Boolean locationOnly, String targetingSpecString) {
        this.adAccountId = adAccountId;
        this.audienceOption = audienceOption;
        this.locationOnly = locationOnly;
        this.targetingSpecString = targetingSpecString;
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
     * @implNote This implementation emits {@code {"ad_account_id": <adAccountId>, "audience_option":
     * <audienceOption>, "location_only": <locationOnly>, "targeting_spec_string":
     * <targetingSpecString>}}, writing each variable only when its value is non-null and emitting
     * {@code "{}"} when all are {@code null}.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (adAccountId != null) {
                writer.writeName("ad_account_id");
                writer.writeColon();
                writer.writeString(adAccountId);
            }

            if (audienceOption != null) {
                writer.writeName("audience_option");
                writer.writeColon();
                writer.writeString(audienceOption);
            }

            if (locationOnly != null) {
                writer.writeName("location_only");
                writer.writeColon();
                writer.writeBool(locationOnly);
            }

            if (targetingSpecString != null) {
                writer.writeName("targeting_spec_string");
                writer.writeColon();
                writer.writeString(targetingSpecString);
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
