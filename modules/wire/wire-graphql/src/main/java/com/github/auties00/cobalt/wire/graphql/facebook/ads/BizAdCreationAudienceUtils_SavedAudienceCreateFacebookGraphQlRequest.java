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
 * Builds the comet mutation that creates a saved WhatsApp Business ad-creation audience.
 *
 * <p>The mutation takes three GraphQL variables. {@code legacyAdAccountID} is the Facebook
 * ad-account identifier the audience is saved under, {@code targetingSpecString} is the JSON-encoded
 * targeting spec to store, and {@code name} is the audience display name. WhatsApp Web's
 * {@code WAWebBizAdCreationAudienceUtils.createSavedAudience} stringifies the targeting spec before
 * passing it and remaps the variables onto the {@code saved_audience_create} field arguments
 * {@code legacy_ad_account_id}, {@code name} and {@code targeting_spec_string} (with
 * {@code skip_normalization} fixed to {@code true}). The mutation returns the created audience under
 * {@code saved_audience_create}; the reply is consumed through
 * {@link BizAdCreationAudienceUtils_SavedAudienceCreateFacebookGraphQlResponse}.
 *
 * @see BizAdCreationAudienceUtils_SavedAudienceCreateFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationAudienceUtils_SavedAudienceCreateMutation")
public final class BizAdCreationAudienceUtils_SavedAudienceCreateFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationAudienceUtils_SavedAudienceCreateMutation.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25962668106650365";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationAudienceUtils_SavedAudienceCreateMutation.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAdCreationAudienceUtils_SavedAudienceCreateMutation";

    /**
     * The {@code legacyAdAccountID} GraphQL variable carrying the Facebook ad-account identifier the
     * audience is saved under, or {@code null} to omit it.
     */
    private final String legacyAdAccountId;

    /**
     * The {@code targetingSpecString} GraphQL variable carrying the JSON-encoded targeting spec to
     * store, or {@code null} to omit it.
     */
    private final String targetingSpecString;

    /**
     * The {@code name} GraphQL variable carrying the audience display name, or {@code null} to omit
     * it.
     */
    private final String name;

    /**
     * Constructs a create-saved-audience mutation request.
     *
     * <p>Each value that is {@code null} omits its variable from the serialized object.
     *
     * @param legacyAdAccountId   the Facebook ad-account identifier the audience is saved under, or
     *                            {@code null} to omit the variable
     * @param targetingSpecString the JSON-encoded targeting spec to store, or {@code null} to omit
     *                            the variable
     * @param name                the audience display name, or {@code null} to omit the variable
     */
    public BizAdCreationAudienceUtils_SavedAudienceCreateFacebookGraphQlRequest(String legacyAdAccountId, String targetingSpecString, String name) {
        this.legacyAdAccountId = legacyAdAccountId;
        this.targetingSpecString = targetingSpecString;
        this.name = name;
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
     * @implNote This implementation emits {@code {"legacyAdAccountID": <legacyAdAccountId>,
     * "targetingSpecString": <targetingSpecString>, "name": <name>}}, writing each variable only when
     * its value is non-null and emitting {@code "{}"} when all are {@code null}. The variable names
     * are the document's {@code LocalArgument} names; the document remaps them onto the
     * {@code saved_audience_create} field arguments {@code legacy_ad_account_id}, {@code name} and
     * {@code targeting_spec_string}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationAudienceUtils", exports = "createSavedAudience",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (legacyAdAccountId != null) {
                writer.writeName("legacyAdAccountID");
                writer.writeColon();
                writer.writeString(legacyAdAccountId);
            }

            if (targetingSpecString != null) {
                writer.writeName("targetingSpecString");
                writer.writeColon();
                writer.writeString(targetingSpecString);
            }

            if (name != null) {
                writer.writeName("name");
                writer.writeColon();
                writer.writeString(name);
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
