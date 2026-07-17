package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlEnvironment;
import java.util.Optional;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the graph.whatsapp.com GraphQL query that fetches the details of a single WhatsApp Business order.
 *
 * <p>The single {@code request} GraphQL variable wraps an {@code order} object naming the order to
 * fetch. WhatsApp Web's {@code WAWebBizQueryOrderJob.queryOrder} fills it from the calling user's
 * personal-number {@link Jid}, the order's server id, the secrecy {@code token} fronting the order,
 * the requested thumbnail {@code image_dimensions}, and an optional direct-connection encrypted info
 * blob. The graph.whatsapp.com endpoint returns the full order projection under {@code xwa_checkout_get_order_info}; the
 * reply is consumed through {@link BizQueryOrderJobWhatsAppGraphQlResponse}.
 *
 * @see BizQueryOrderJobWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizQueryOrderJobQuery")
public final class BizQueryOrderJobWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizQueryOrderJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26593811266898374";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizQueryOrderJobQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizQueryOrderJobQuery";

    /**
     * The {@code order.jid} field naming the calling user whose order is being fetched, or
     * {@code null} to omit it.
     */
    private final Jid jid;

    /**
     * The {@code order.id} field naming the server-assigned order id, or {@code null} to omit it.
     */
    private final String orderId;

    /**
     * The {@code order.token.sensitive_string_value} field carrying the order secrecy token, or
     * {@code null} to omit it.
     */
    private final String token;

    /**
     * The {@code order.image_dimensions.height} field requesting the thumbnail height, or
     * {@code null} to omit it.
     */
    private final Integer height;

    /**
     * The {@code order.image_dimensions.width} field requesting the thumbnail width, or {@code null}
     * to omit it.
     */
    private final Integer width;

    /**
     * The {@code order.direct_connection_encrypted_info} field carrying an optional direct-connection
     * encrypted info blob, or {@code null} to omit it.
     */
    private final String directConnectionEncryptedInfo;

    /**
     * Constructs a query-order request.
     *
     * <p>Each value populates the nested {@code request.order} object; each value that is
     * {@code null} is omitted from the serialized object. The {@code token} populates the
     * {@code order.token.sensitive_string_value} field, and {@code height}/{@code width} populate the
     * {@code order.image_dimensions} object.
     *
     * @param jid                           the calling user {@link Jid}, or {@code null} to omit it
     * @param orderId                       the server-assigned order id, or {@code null} to omit it
     * @param token                         the order secrecy token, or {@code null} to omit it
     * @param height                        the requested thumbnail height, or {@code null} to omit it
     * @param width                         the requested thumbnail width, or {@code null} to omit it
     * @param directConnectionEncryptedInfo the optional direct-connection encrypted info blob, or
     *                                      {@code null} to omit it
     */
    public BizQueryOrderJobWhatsAppGraphQlRequest(Jid jid, String orderId, String token, Integer height, Integer width, String directConnectionEncryptedInfo) {
        this.jid = jid;
        this.orderId = orderId;
        this.token = token;
        this.height = height;
        this.width = width;
        this.directConnectionEncryptedInfo = directConnectionEncryptedInfo;
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
     * @implNote This implementation emits {@code {"request": {"order": {"jid": <jid>, "token":
     * {"sensitive_string_value": <token>}, "id": <orderId>, "image_dimensions": {"height": <height>,
     * "width": <width>}, "direct_connection_encrypted_info": <info>}}}}, writing each scalar only when
     * its value is non-null and emitting the nested {@code token} and {@code image_dimensions} objects
     * only when at least one of their fields is present.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizQueryOrderJob", exports = "queryOrder",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("request");
            writer.writeColon();
            writer.startObject();
            writer.writeName("order");
            writer.writeColon();
            writer.startObject();
            if (jid != null) {
                writer.writeName("jid");
                writer.writeColon();
                writer.writeString(jid.toString());
            }

            if (token != null) {
                writer.writeName("token");
                writer.writeColon();
                writer.startObject();
                writer.writeName("sensitive_string_value");
                writer.writeColon();
                writer.writeString(token);
                writer.endObject();
            }

            if (orderId != null) {
                writer.writeName("id");
                writer.writeColon();
                writer.writeString(orderId);
            }

            if (height != null || width != null) {
                writer.writeName("image_dimensions");
                writer.writeColon();
                writer.startObject();
                if (height != null) {
                    writer.writeName("height");
                    writer.writeColon();
                    writer.writeInt32(height);
                }

                if (width != null) {
                    writer.writeName("width");
                    writer.writeColon();
                    writer.writeInt32(width);
                }
                writer.endObject();
            }

            if (directConnectionEncryptedInfo != null) {
                writer.writeName("direct_connection_encrypted_info");
                writer.writeColon();
                writer.writeString(directConnectionEncryptedInfo);
            }
            writer.endObject();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public WhatsAppGraphQlEnvironment environment() {
        return WhatsAppGraphQlEnvironment.CATALOG;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> accessToken() {
        return Optional.empty();
    }
}
