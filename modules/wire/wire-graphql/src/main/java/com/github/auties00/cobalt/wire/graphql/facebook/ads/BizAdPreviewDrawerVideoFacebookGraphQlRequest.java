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
 * Builds the Facebook GraphQL query that resolves the playable URL for a WhatsApp Business ad preview-drawer
 * video.
 *
 * <p>The single {@code videoId} GraphQL variable is the Facebook video object identifier whose
 * playable URL is being resolved; the compiled document maps it to the {@code id} argument of the
 * {@code fetch__Video} root field. The {@code videoId} is a numeric Facebook stanza id rather than a
 * WhatsApp address, so it is kept as a {@link String}. The query returns the video's playable URL and
 * identifier; the reply is consumed through {@link BizAdPreviewDrawerVideoFacebookGraphQlResponse}.
 *
 * @see BizAdPreviewDrawerVideoFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdPreviewDrawerVideoQuery")
public final class BizAdPreviewDrawerVideoFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdPreviewDrawerVideoQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25044237358582412";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdPreviewDrawerVideoQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAdPreviewDrawerVideoQuery";

    /**
     * The {@code videoId} GraphQL variable identifying the Facebook video whose playable URL is being
     * resolved, or {@code null} to omit it.
     */
    private final String videoId;

    /**
     * Constructs a preview-drawer-video query request.
     *
     * <p>The {@code videoId} is the Facebook video object identifier whose playable URL is being
     * resolved. A {@code null} value omits the variable from the serialized object.
     *
     * @param videoId the Facebook video object identifier, or {@code null} to omit the variable
     */
    public BizAdPreviewDrawerVideoFacebookGraphQlRequest(String videoId) {
        this.videoId = videoId;
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
     * @implNote This implementation emits {@code {"videoId": <videoId>}}, writing the variable only
     * when its value is non-null and emitting {@code "{}"} when it is {@code null}. The compiled
     * document remaps this {@code videoId} {@code LocalArgument} onto the {@code id} argument of the
     * {@code fetch__Video} root field.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (videoId != null) {
                writer.writeName("videoId");
                writer.writeColon();
                writer.writeString(videoId);
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
