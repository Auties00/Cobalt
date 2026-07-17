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
 * Builds the comet mutation that edits a saved WhatsApp Business ad-creation audience.
 *
 * <p>The mutation takes three GraphQL variables. {@code name} is the new audience display name,
 * {@code savedAudienceID} is the server-assigned id of the saved audience to edit, and
 * {@code targetingSpecString} is the JSON-encoded targeting spec to store. WhatsApp Web's
 * {@code WAWebBizAdCreationAudienceUtils.editAudience} stringifies the targeting spec before passing
 * it and remaps the variables onto the document fields {@code name}, {@code saved_audience_id} and
 * {@code targeting_spec_string} (with {@code skip_normalization} fixed to {@code true}). The mutation
 * returns the edited audience under {@code saved_audience_edit}; the reply is consumed through
 * {@link BizAdCreationAudienceUtils_EditAudienceFacebookGraphQlResponse}.
 *
 * @see BizAdCreationAudienceUtils_EditAudienceFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationAudienceUtils_EditAudienceMutation")
public final class BizAdCreationAudienceUtils_EditAudienceFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationAudienceUtils_EditAudienceMutation.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25726350063668723";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationAudienceUtils_EditAudienceMutation.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAdCreationAudienceUtils_EditAudienceMutation";

    /**
     * The {@code name} GraphQL variable carrying the new audience display name, or {@code null} to
     * omit it.
     */
    private final String name;

    /**
     * The {@code savedAudienceID} GraphQL variable carrying the server-assigned id of the saved
     * audience to edit, or {@code null} to omit it.
     */
    private final String savedAudienceId;

    /**
     * The {@code targetingSpecString} GraphQL variable carrying the JSON-encoded targeting spec to
     * store, or {@code null} to omit it.
     */
    private final String targetingSpecString;

    /**
     * Constructs an edit-saved-audience mutation request.
     *
     * <p>Each value that is {@code null} omits its variable from the serialized object.
     *
     * @param name                the new audience display name, or {@code null} to omit the variable
     * @param savedAudienceId     the server-assigned id of the saved audience to edit, or
     *                            {@code null} to omit the variable
     * @param targetingSpecString the JSON-encoded targeting spec to store, or {@code null} to omit
     *                            the variable
     */
    public BizAdCreationAudienceUtils_EditAudienceFacebookGraphQlRequest(String name, String savedAudienceId, String targetingSpecString) {
        this.name = name;
        this.savedAudienceId = savedAudienceId;
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
     * @implNote This implementation emits {@code {"name": <name>, "savedAudienceID":
     * <savedAudienceId>, "targetingSpecString": <targetingSpecString>}}, writing each variable only
     * when its value is non-null and emitting {@code "{}"} when all are {@code null}. The variable
     * names are the document's {@code LocalArgument} names; the document remaps them onto the
     * {@code saved_audience_edit} field arguments {@code name}, {@code saved_audience_id} and
     * {@code targeting_spec_string}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationAudienceUtils", exports = "editAudience",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (name != null) {
                writer.writeName("name");
                writer.writeColon();
                writer.writeString(name);
            }

            if (savedAudienceId != null) {
                writer.writeName("savedAudienceID");
                writer.writeColon();
                writer.writeString(savedAudienceId);
            }

            if (targetingSpecString != null) {
                writer.writeName("targetingSpecString");
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
