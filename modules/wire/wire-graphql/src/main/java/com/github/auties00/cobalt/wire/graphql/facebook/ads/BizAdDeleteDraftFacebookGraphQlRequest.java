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
 * Builds the comet mutation that deletes a click-to-WhatsApp (CTWA) ad draft.
 *
 * <p>The mutation takes a single {@code input} GraphQL object identifying the draft to delete by its
 * {@code draft_id}. The relay returns the deletion outcome under the scalar
 * {@code delete_ads_ctwa_draft}; the reply is consumed through
 * {@link BizAdDeleteDraftFacebookGraphQlResponse}.
 *
 * @see BizAdDeleteDraftFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdDeleteDraftMutation")
public final class BizAdDeleteDraftFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdDeleteDraftMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26580263294949276";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdDeleteDraftMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizAdDeleteDraftMutation";

    /**
     * The {@code draft_id} field of the {@code input} object identifying the draft to delete, or
     * {@code null} to omit it.
     */
    private final String draftId;

    /**
     * Constructs a delete-CTWA-ad-draft mutation request.
     *
     * <p>The {@code draftId} identifies the draft to delete. A {@code null} value omits the field from
     * the serialized {@code input} object.
     *
     * @param draftId the ad-draft identifier, or {@code null} to omit the field
     */
    public BizAdDeleteDraftFacebookGraphQlRequest(String draftId) {
        this.draftId = draftId;
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
     * @implNote This implementation emits {@code {"input": {"draft_id": <draftId>}}}, writing
     * {@code draft_id} only when {@code draftId} is non-null and emitting {@code {"input": {}}}
     * otherwise.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (draftId != null) {
                writer.writeName("draft_id");
                writer.writeColon();
                writer.writeString(draftId);
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
