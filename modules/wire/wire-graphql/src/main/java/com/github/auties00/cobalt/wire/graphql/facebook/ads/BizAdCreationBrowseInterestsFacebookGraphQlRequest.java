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
 * Builds the Facebook GraphQL query that browses the WhatsApp Business ad-creation detailed-targeting tree at a
 * given path.
 *
 * <p>The query takes two GraphQL variables. {@code adAccountID} is the Facebook ad-account
 * identifier whose targeting catalog is browsed, and {@code audiencePath} is the path into the
 * detailed-targeting tree whose immediate children are listed (an empty path lists the tree roots).
 * The query returns the matching {@code DetailTargetingUnifiedNode} entries under
 * {@code detailed_targeting_browse}; the reply is consumed through
 * {@link BizAdCreationBrowseInterestsFacebookGraphQlResponse}.
 *
 * @see BizAdCreationBrowseInterestsFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationBrowseInterestsQuery")
public final class BizAdCreationBrowseInterestsFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationBrowseInterestsQuery.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25496886369942775";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationBrowseInterestsQuery.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizAdCreationBrowseInterestsQuery";

    /**
     * The {@code adAccountID} GraphQL variable carrying the Facebook ad-account identifier whose
     * targeting catalog is browsed, or {@code null} to omit it.
     */
    private final String adAccountId;

    /**
     * The {@code audiencePath} GraphQL variable carrying the path into the detailed-targeting tree
     * whose children are listed, or {@code null} to omit it.
     */
    private final String audiencePath;

    /**
     * Constructs a browse-interests query request.
     *
     * <p>Each value that is {@code null} omits its variable from the serialized object.
     *
     * @param adAccountId  the Facebook ad-account identifier whose targeting catalog is browsed, or
     *                     {@code null} to omit the variable
     * @param audiencePath the path into the detailed-targeting tree whose children are listed, or
     *                     {@code null} to omit the variable
     */
    public BizAdCreationBrowseInterestsFacebookGraphQlRequest(String adAccountId, String audiencePath) {
        this.adAccountId = adAccountId;
        this.audiencePath = audiencePath;
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
     * @implNote This implementation emits {@code {"adAccountID": <adAccountId>, "audiencePath":
     * <audiencePath>}}, writing each variable only when its value is non-null and emitting
     * {@code "{}"} when both are {@code null}. The variable names are the document's
     * {@code LocalArgument} names taken from the operation spec.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (adAccountId != null) {
                writer.writeName("adAccountID");
                writer.writeColon();
                writer.writeString(adAccountId);
            }

            if (audiencePath != null) {
                writer.writeName("audiencePath");
                writer.writeColon();
                writer.writeString(audiencePath);
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
