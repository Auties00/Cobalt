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
 * Builds the Facebook GraphQL query that fetches the caller's WhatsApp subscriptions and their feature flags.
 *
 * <p>The single {@code data} GraphQL variable is the {@code XWAGetSubscriptionsRequest} input object.
 * WhatsApp Web's {@code WAWebFetchSubscriptions.fetchSubscriptions} fills it with a single
 * {@code platform} field naming the requesting platform; the only value the bundle emits is the
 * literal {@code "UNKNOWN"}. The Meta graph endpoint returns the subscription list and the feature-flag list under
 * {@code xwa_get_subscriptions}; the reply is consumed through {@link FetchSubscriptionsFacebookGraphQlResponse}.
 *
 * @see FetchSubscriptionsFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebFetchSubscriptionsQuery")
public final class FetchSubscriptionsFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebFetchSubscriptionsQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "35324254123840149";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebFetchSubscriptionsQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebFetchSubscriptionsQuery";

    /**
     * The {@code platform} field of the {@code data} input object naming the requesting platform, or
     * {@code null} to omit it.
     *
     * <p>WhatsApp Web emits the literal {@code "UNKNOWN"}; the value is kept as a {@link String}
     * because the full {@code XWAPlatform} value set is not confirmable from the JS bundle.
     */
    private final String platform;

    /**
     * Constructs a fetch-subscriptions request carrying the requesting platform.
     *
     * <p>The value populates the {@code data} input object; a {@code null} value is omitted from the
     * serialized object.
     *
     * @param platform the requesting platform, or {@code null} to omit the field
     */
    public FetchSubscriptionsFacebookGraphQlRequest(String platform) {
        this.platform = platform;
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
     * @implNote This implementation emits {@code {"data": {"platform": <platform>}}}, writing the
     * {@code platform} field only when its value is non-null and emitting {@code {"data": {}}} when it
     * is {@code null}. The {@code data} variable maps to the GraphQL {@code request} argument server
     * side.
     */
    @WhatsAppWebExport(moduleName = "WAWebFetchSubscriptions", exports = "fetchSubscriptions",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("data");
            writer.writeColon();
            writer.startObject();
            if (platform != null) {
                writer.writeName("platform");
                writer.writeColon();
                writer.writeString(platform);
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
