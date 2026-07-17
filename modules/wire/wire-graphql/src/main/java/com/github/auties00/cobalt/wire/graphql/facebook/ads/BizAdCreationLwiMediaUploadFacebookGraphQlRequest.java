package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the Facebook GraphQL mutation that uploads click-to-WhatsApp ad media into Meta's Lightweight
 * Instagram (LWI) media store.
 *
 * <p>The mutation takes four GraphQL variables. WhatsApp Web's {@code WAWebBizAdCreationLWIMediaUpload}
 * supplies the Facebook ad-account id, the page id, the list of Facebook media ids ({@code fbid}) to
 * upload, and an optional Facebook access token; the token is wrapped server-side as
 * {@code {sensitive_string_value: <token>}}. The Meta graph endpoint returns the uploaded media descriptors under
 * {@code wa_ad_creation_lwi_media_upload}; the reply is consumed through
 * {@link BizAdCreationLwiMediaUploadFacebookGraphQlResponse}.
 *
 * @see BizAdCreationLwiMediaUploadFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationLWIMediaUploadMutation")
public final class BizAdCreationLwiMediaUploadFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationLWIMediaUploadMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26635433729479722";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationLWIMediaUploadMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAdCreationLWIMediaUploadMutation";

    /**
     * The {@code ad_account_id} GraphQL variable naming the Facebook ad account, or {@code null} to
     * omit it.
     */
    private final String adAccountId;

    /**
     * The {@code page_id} GraphQL variable naming the Facebook page, or {@code null} to omit it.
     */
    private final String pageId;

    /**
     * The {@code media_ids} GraphQL variable listing the Facebook media ids to upload, or
     * {@code null} to omit the array.
     */
    private final List<String> mediaIds;

    /**
     * The Facebook access token emitted under {@code fb_access_token} as
     * {@code {sensitive_string_value: <token>}}, or {@code null} to emit {@code null}.
     */
    private final String fbAccessToken;

    /**
     * Constructs an LWI media-upload mutation request.
     *
     * <p>The four values populate the GraphQL variables. The {@code fbAccessToken} is wrapped as
     * {@code {sensitive_string_value: <token>}} when present and serialized as {@code null} when
     * absent; the other values are omitted when {@code null}.
     *
     * @param adAccountId   the Facebook ad-account id, or {@code null} to omit the variable
     * @param pageId        the Facebook page id, or {@code null} to omit the variable
     * @param mediaIds      the Facebook media ids to upload, or {@code null} to omit the array
     * @param fbAccessToken the Facebook access token, or {@code null} to serialize {@code null}
     */
    public BizAdCreationLwiMediaUploadFacebookGraphQlRequest(String adAccountId, String pageId, List<String> mediaIds, String fbAccessToken) {
        this.adAccountId = adAccountId;
        this.pageId = pageId;
        this.mediaIds = mediaIds;
        this.fbAccessToken = fbAccessToken;
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
     * @implNote This implementation emits {@code {"ad_account_id": <adAccountId>, "page_id": <pageId>,
     * "media_ids": [<id>, ...], "fb_access_token": {"sensitive_string_value": <token>}}}, writing each
     * scalar and the {@code media_ids} array only when non-null and emitting an explicit {@code null}
     * for {@code fb_access_token} when no token was supplied, matching
     * {@code WAWebBizAdCreationLWIMediaUpload}'s {@code fb_access_token: token != null ?
     * {sensitive_string_value: token} : null} binding.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationLWIMediaUpload", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (adAccountId != null) {
                writer.writeName("ad_account_id");
                writer.writeColon();
                writer.writeString(adAccountId);
            }

            if (pageId != null) {
                writer.writeName("page_id");
                writer.writeColon();
                writer.writeString(pageId);
            }

            if (mediaIds != null) {
                writer.writeName("media_ids");
                writer.writeColon();
                writer.startArray();
                for (var index = 0; index < mediaIds.size(); index++) {
                    if (index > 0) {
                        writer.writeComma();
                    }
                    writer.writeString(mediaIds.get(index));
                }
                writer.endArray();
            }

            writer.writeName("fb_access_token");
            writer.writeColon();
            if (fbAccessToken != null) {
                writer.startObject();
                writer.writeName("sensitive_string_value");
                writer.writeColon();
                writer.writeString(fbAccessToken);
                writer.endObject();
            } else {
                writer.writeNull();
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
