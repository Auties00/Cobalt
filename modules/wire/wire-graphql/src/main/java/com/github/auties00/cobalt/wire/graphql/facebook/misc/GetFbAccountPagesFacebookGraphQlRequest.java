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
 * Builds the Facebook GraphQL query that fetches the Facebook pages promotable from a business account.
 *
 * <p>The single {@code userId} GraphQL variable is the Facebook user id of the business account whose
 * pages are listed; WhatsApp Web's {@code WAWebGetFBAccountPages} fills it with the business profile
 * id ({@code bp_id}). It is a Facebook numeric id rather than a WhatsApp address, so it is modelled as
 * a plain {@link String}. The variable maps to the GraphQL {@code id} argument of the {@code user}
 * field server side. The Meta graph endpoint returns the user's promotable pages under {@code user.facebook_pages};
 * the reply is consumed through {@link GetFbAccountPagesFacebookGraphQlResponse}.
 *
 * @see GetFbAccountPagesFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebGetFBAccountPagesQuery")
public final class GetFbAccountPagesFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebGetFBAccountPagesQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "24564518546541529";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebGetFBAccountPagesQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebGetFBAccountPagesQuery";

    /**
     * The {@code userId} variable carrying the Facebook user id of the business account, or
     * {@code null} to omit it.
     *
     * <p>A Facebook numeric id (the business profile id), not a WhatsApp address.
     */
    private final String userId;

    /**
     * Constructs a get-Facebook-account-pages request carrying the Facebook user id.
     *
     * <p>The value populates the {@code userId} variable; a {@code null} value is omitted from the
     * serialized object.
     *
     * @param userId the Facebook user id of the business account, or {@code null} to omit the variable
     */
    public GetFbAccountPagesFacebookGraphQlRequest(String userId) {
        this.userId = userId;
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
     * @implNote This implementation emits {@code {"userId": <userId>}}, writing the {@code userId}
     * field only when its value is non-null and emitting {@code "{}"} when it is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebGetFBAccountPages", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (userId != null) {
                writer.writeName("userId");
                writer.writeColon();
                writer.writeString(userId);
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
