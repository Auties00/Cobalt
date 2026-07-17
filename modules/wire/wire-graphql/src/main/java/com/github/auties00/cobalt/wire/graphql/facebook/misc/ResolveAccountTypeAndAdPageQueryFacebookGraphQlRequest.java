package com.github.auties00.cobalt.wire.graphql.facebook.misc;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL query that resolves a Facebook page's account type and ad-creation eligibility for
 * a WhatsApp Business advertising flow.
 *
 * <p>The single {@code pageId} GraphQL variable names the Facebook page to look up; the Meta graph endpoint binds it
 * to the {@code id} argument of the {@code page} field and returns whether the viewer may create ads
 * for that page. The reply is consumed through {@link ResolveAccountTypeAndAdPageQueryFacebookGraphQlResponse}.
 *
 * @see ResolveAccountTypeAndAdPageQueryFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebResolveAccountTypeAndAdPageQuery")
public final class ResolveAccountTypeAndAdPageQueryFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebResolveAccountTypeAndAdPageQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "24856134350695832";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebResolveAccountTypeAndAdPageQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebResolveAccountTypeAndAdPageQuery";

    /**
     * The {@code pageId} GraphQL variable naming the Facebook page to resolve, or {@code null} to omit
     * it.
     *
     * <p>A Facebook page identifier rather than a WhatsApp address, so it is carried as a plain
     * {@link String}.
     */
    private final String pageId;

    /**
     * Constructs a resolve-account-type-and-ad-page query request.
     *
     * <p>The {@code pageId} names the Facebook page to resolve; a {@code null} value omits the
     * variable from the serialized object.
     *
     * @param pageId the Facebook page identifier to resolve, or {@code null} to omit the variable
     */
    public ResolveAccountTypeAndAdPageQueryFacebookGraphQlRequest(String pageId) {
        this.pageId = pageId;
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
     * @implNote This implementation emits {@code {"pageId": <pageId>}}, writing the field only when
     * {@code pageId} is non-null and emitting {@code "{}"} otherwise.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (pageId != null) {
                writer.writeName("pageId");
                writer.writeColon();
                writer.writeString(pageId);
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
