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
 * Builds the Facebook GraphQL query that resolves an uploaded image hash to a displayable URL during the
 * WhatsApp Business ad-creation flow.
 *
 * <p>The query takes two GraphQL variables: {@code legacyAdAccountID}, the Facebook ad account the
 * image was uploaded under, and {@code imageHash}, the hash of the previously uploaded image. The
 * relay returns the resolved URL under the linked {@code lwi} root's scalar
 * {@code image_url_from_hash}; the reply is consumed through
 * {@link BizAdCreationResolveImageUrlFacebookGraphQlResponse}.
 *
 * @implNote This implementation models {@code legacyAdAccountID} as a Facebook ad-account identifier
 * (a numeric string), not a WhatsApp address, so it is a {@code String} rather than a
 * {@link com.github.auties00.cobalt.wire.core.jid.Jid}. The two GraphQL variables are named
 * {@code legacyAdAccountID} and {@code imageHash}; the compiled document forwards them to the
 * {@code image_url_from_hash} field's {@code legacy_ad_account_id} and {@code image_hash} arguments,
 * but the request body carries the variable names, not the field-argument names.
 *
 * @see BizAdCreationResolveImageUrlFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationResolveImageURLQuery")
public final class BizAdCreationResolveImageUrlFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationResolveImageURLQuery.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26359678263651380";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationResolveImageURLQuery.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAdCreationResolveImageURLQuery";

    /**
     * The {@code legacyAdAccountID} GraphQL variable naming the Facebook ad account the image was
     * uploaded under, or {@code null} to omit it.
     *
     * <p>A Facebook ad-account identifier (a numeric string), not a WhatsApp address.
     */
    private final String legacyAdAccountId;

    /**
     * The {@code imageHash} GraphQL variable carrying the hash of the previously uploaded image, or
     * {@code null} to omit it.
     */
    private final String imageHash;

    /**
     * Constructs a resolve-image-URL query request.
     *
     * <p>The {@code legacyAdAccountId} populates the {@code legacyAdAccountID} GraphQL variable and
     * the {@code imageHash} populates the {@code imageHash} GraphQL variable; each value that is
     * {@code null} omits its variable from the serialized object.
     *
     * @param legacyAdAccountId the Facebook ad-account identifier, or {@code null} to omit the
     *                          variable
     * @param imageHash         the uploaded image hash, or {@code null} to omit the variable
     */
    public BizAdCreationResolveImageUrlFacebookGraphQlRequest(String legacyAdAccountId, String imageHash) {
        this.legacyAdAccountId = legacyAdAccountId;
        this.imageHash = imageHash;
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
     * "imageHash": <imageHash>}}, writing each variable only when its value is non-null and emitting
     * {@code "{}"} when both are {@code null}.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (legacyAdAccountId != null) {
                writer.writeName("legacyAdAccountID");
                writer.writeColon();
                writer.writeString(legacyAdAccountId);
            }

            if (imageHash != null) {
                writer.writeName("imageHash");
                writer.writeColon();
                writer.writeString(imageHash);
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
