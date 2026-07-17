package com.github.auties00.cobalt.wire.graphql.whatsappWeb.promotion;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.time.Instant;

/**
 * Builds the relay mutation that logs a consumer quick-promotion interaction event.
 *
 * <p>The mutation takes a single {@code input} GraphQL variable, forwarded to the server-side field
 * argument {@code data}. WhatsApp Web's
 * {@code WAWebConsumerQuickPromotionActionMutation.executeConsumerQuickPromotionActionMutation()}
 * fills it from the impression, dismiss, and primary-click banner jobs with the {@code event} kind,
 * an optional {@code action} discriminator (set only when {@code event} is {@code ACTION}), the
 * {@code promotion_id} and {@code surface_nux_id} the event applies to, an opaque
 * {@code promotion_logging_data} blob (empty on the consumer build), and the {@code client_time} the
 * event occurred. The relay acknowledges the log under {@code wa_consumer_quick_promotion_log_event};
 * the reply is consumed through {@link ConsumerQuickPromotionActionGraphQlWhatsAppWebGraphQlResponse}.
 *
 * @implNote This implementation keeps {@code event} and {@code action} as plain strings rather than
 * enums: the WhatsApp Web jobs only ever emit {@code event} values {@code VIEW} and {@code ACTION}
 * and {@code action} values {@code DISMISS} and {@code PRIMARY}, but the server-side
 * {@code WaConsumerQuickPromotionLogEventInput} enum is not present in the JS bundle of snapshot
 * {@code 1040120866}, so the full accepted value set cannot be confirmed (the fetch response models a
 * secondary-click counter, implying a {@code SECONDARY} action the web jobs never send).
 *
 * @see ConsumerQuickPromotionActionGraphQlWhatsAppWebGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebConsumerQuickPromotionActionGraphQLMutation")
public final class ConsumerQuickPromotionActionGraphQlWhatsAppWebGraphQlRequest implements WhatsAppWebGraphQlOperation.Request {
    /**
     * The persisted document identifier the relay maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the url-encoded request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebConsumerQuickPromotionActionGraphQLMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25690382143972563";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebConsumerQuickPromotionActionGraphQLMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebConsumerQuickPromotionActionGraphQLMutation";

    /**
     * The {@code event} field of the {@code input} object naming the kind of interaction, or
     * {@code null} to omit it.
     *
     * <p>WhatsApp Web emits {@code VIEW} for impressions and {@code ACTION} for dismiss and
     * primary-click events.
     */
    private final String event;

    /**
     * The {@code action} field of the {@code input} object discriminating an {@code ACTION} event, or
     * {@code null} to omit it.
     *
     * <p>WhatsApp Web emits {@code DISMISS} for dismissals and {@code PRIMARY} for primary clicks; it
     * is omitted for {@code VIEW} events.
     */
    private final String action;

    /**
     * The {@code promotion_id} field of the {@code input} object naming the promotion the event
     * applies to, or {@code null} to omit it.
     */
    private final String promotionId;

    /**
     * The {@code surface_nux_id} field of the {@code input} object naming the surface the event
     * applies to, or {@code null} to omit it.
     */
    private final String surfaceNuxId;

    /**
     * The {@code promotion_logging_data} field of the {@code input} object carrying the opaque
     * logging blob, or {@code null} to omit it.
     */
    private final String promotionLoggingData;

    /**
     * The {@code client_time} field of the {@code input} object timestamping the event, or
     * {@code null} to omit it.
     *
     * <p>Serialized as seconds-since-epoch.
     */
    private final Instant clientTime;

    /**
     * Constructs a consumer quick-promotion action mutation request.
     *
     * <p>The values populate the {@code input} GraphQL object; each value that is {@code null} is
     * omitted from the serialized object.
     *
     * @param event                the interaction kind ({@code VIEW} or {@code ACTION}), or
     *                             {@code null} to omit the field
     * @param action               the action discriminator ({@code DISMISS} or {@code PRIMARY}) for an
     *                             {@code ACTION} event, or {@code null} to omit the field
     * @param promotionId          the promotion the event applies to, or {@code null} to omit the
     *                             field
     * @param surfaceNuxId         the surface the event applies to, or {@code null} to omit the field
     * @param promotionLoggingData the opaque logging blob, or {@code null} to omit the field
     * @param clientTime           the instant the event occurred, or {@code null} to omit the field
     */
    public ConsumerQuickPromotionActionGraphQlWhatsAppWebGraphQlRequest(String event, String action, String promotionId, String surfaceNuxId, String promotionLoggingData, Instant clientTime) {
        this.event = event;
        this.action = action;
        this.promotionId = promotionId;
        this.surfaceNuxId = surfaceNuxId;
        this.promotionLoggingData = promotionLoggingData;
        this.clientTime = clientTime;
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
     * @implNote This implementation emits {@code {"input": {"event": <event>, "action": <action>,
     * "promotion_id": <promotionId>, "surface_nux_id": <surfaceNuxId>, "promotion_logging_data":
     * <promotionLoggingData>, "client_time": <clientTime>}}}, writing each field only when its value
     * is non-null, rendering {@code clientTime} as its seconds-since-epoch, and emitting
     * {@code {"input": {}}} when every value is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebConsumerQuickPromotionActionGraphQL", exports = "executeConsumerQuickPromotionActionGraphQL",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (event != null) {
                writer.writeName("event");
                writer.writeColon();
                writer.writeString(event);
            }

            if (action != null) {
                writer.writeName("action");
                writer.writeColon();
                writer.writeString(action);
            }

            if (promotionId != null) {
                writer.writeName("promotion_id");
                writer.writeColon();
                writer.writeString(promotionId);
            }

            if (surfaceNuxId != null) {
                writer.writeName("surface_nux_id");
                writer.writeColon();
                writer.writeString(surfaceNuxId);
            }

            if (promotionLoggingData != null) {
                writer.writeName("promotion_logging_data");
                writer.writeColon();
                writer.writeString(promotionLoggingData);
            }

            if (clientTime != null) {
                writer.writeName("client_time");
                writer.writeColon();
                writer.writeInt64(clientTime.getEpochSecond());
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
