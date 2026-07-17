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
 * Builds the Facebook GraphQL query that resolves the preferred-thumbnail image URL for a WhatsApp Business
 * ad-creation video.
 *
 * <p>The single {@code videoID} GraphQL variable is the Facebook video object identifier whose
 * thumbnail is being resolved; the compiled document maps it to the {@code id} argument of the
 * {@code fetch__Video} root field. The {@code videoID} is a numeric Facebook stanza id rather than a
 * WhatsApp address, so it is kept as a {@link String}. The query returns the video's preferred
 * thumbnail image URI; the reply is consumed through
 * {@link BizAdCreationResolveVideoThumbnailUrlFacebookGraphQlResponse}.
 *
 * @see BizAdCreationResolveVideoThumbnailUrlFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationResolveVideoThumbnailURLQuery")
public final class BizAdCreationResolveVideoThumbnailUrlFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationResolveVideoThumbnailURLQuery.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "27339656232288736";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationResolveVideoThumbnailURLQuery.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAdCreationResolveVideoThumbnailURLQuery";

    /**
     * The {@code videoID} GraphQL variable identifying the Facebook video whose thumbnail is being
     * resolved, or {@code null} to omit it.
     */
    private final String videoId;

    /**
     * Constructs a resolve-video-thumbnail-URL query request.
     *
     * <p>The {@code videoId} is the Facebook video object identifier whose preferred thumbnail is
     * being resolved. A {@code null} value omits the variable from the serialized object.
     *
     * @param videoId the Facebook video object identifier, or {@code null} to omit the variable
     */
    public BizAdCreationResolveVideoThumbnailUrlFacebookGraphQlRequest(String videoId) {
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
     * @implNote This implementation emits {@code {"videoID": <videoId>}}, writing the variable only
     * when its value is non-null and emitting {@code "{}"} when it is {@code null}.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (videoId != null) {
                writer.writeName("videoID");
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
