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
 * Builds the comet mutation that deletes a saved audience from the WhatsApp Business ad-creation
 * targeting modal's delete-confirmation dialog.
 *
 * <p>The mutation takes a single GraphQL variable, {@code savedAudienceID}, the Facebook saved-audience
 * id to delete; it is a numeric Facebook id rather than a WhatsApp address, so it is kept as a
 * {@link String}. The compiled document forwards it to the server as the {@code saved_audience_id}
 * argument of the scalar {@code delete_saved_audience} field. The mutation returns the deletion
 * outcome under the scalar {@code delete_saved_audience}; the reply is consumed through
 * {@link BizAdCreationTargetingModalDeleteAudienceConfirmationModal_SavedAudienceDeleteFacebookGraphQlResponse}.
 *
 * @see BizAdCreationTargetingModalDeleteAudienceConfirmationModal_SavedAudienceDeleteFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationTargetingModalDeleteAudienceConfirmationModal_SavedAudienceDeleteMutation")
public final class BizAdCreationTargetingModalDeleteAudienceConfirmationModal_SavedAudienceDeleteFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationTargetingModalDeleteAudienceConfirmationModal_SavedAudienceDeleteMutation.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25755919607334972";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationTargetingModalDeleteAudienceConfirmationModal_SavedAudienceDeleteMutation.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAdCreationTargetingModalDeleteAudienceConfirmationModal_SavedAudienceDeleteMutation";

    /**
     * The {@code savedAudienceID} GraphQL variable carrying the Facebook saved-audience id to delete,
     * or {@code null} to omit it.
     */
    private final String savedAudienceId;

    /**
     * Constructs a delete-saved-audience mutation request.
     *
     * <p>The {@code savedAudienceId} is the Facebook saved-audience id to delete. A {@code null} value
     * omits the variable from the serialized object.
     *
     * @param savedAudienceId the Facebook saved-audience id to delete, or {@code null} to omit the
     *                        variable
     */
    public BizAdCreationTargetingModalDeleteAudienceConfirmationModal_SavedAudienceDeleteFacebookGraphQlRequest(String savedAudienceId) {
        this.savedAudienceId = savedAudienceId;
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
     * @implNote This implementation emits {@code {"savedAudienceID": <savedAudienceId>}}, writing the
     * variable only when its value is non-null and emitting {@code "{}"} when it is {@code null}. The
     * GraphQL variable name is {@code savedAudienceID}; the server-side {@code saved_audience_id}
     * argument mapping is performed by the persisted document, not by this request.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (savedAudienceId != null) {
                writer.writeName("savedAudienceID");
                writer.writeColon();
                writer.writeString(savedAudienceId);
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
