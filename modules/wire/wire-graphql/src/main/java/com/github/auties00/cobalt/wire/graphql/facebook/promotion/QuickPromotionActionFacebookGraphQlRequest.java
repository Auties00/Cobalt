package com.github.auties00.cobalt.wire.graphql.facebook.promotion;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.time.Instant;

/**
 * Builds the Facebook GraphQL mutation that logs a WhatsApp Business quick-promotion (in-app comms nux) action
 * to the in-app-comms backend.
 *
 * <p>The mutation takes one {@code input} GraphQL variable of type
 * {@code WaQuickPromotionLogEventData}. WhatsApp Web's
 * {@code WAWebQuickPromotionActionMutation.executeQuickPromotionActionMutation(input)} forwards the
 * object built by the quick-promotion jobs straight to the Meta graph endpoint; the recovered fields are the event
 * discriminator ({@code event}, for example {@code "VIEW"} or {@code "ACTION"}), the optional
 * action discriminator ({@code action}, for example {@code "DISMISS"} or {@code "PRIMARY"}, present
 * only for {@code "ACTION"} events), the promotion identifier ({@code promotion_id}), the surface nux
 * identifier ({@code surface_nux_id}), the opaque logging blob ({@code promotion_logging_data}), and
 * the client timestamp ({@code client_time}). The Meta graph endpoint returns the logged-event acknowledgement
 * under {@code wa_quick_promotion_log_event}; the reply is consumed through
 * {@link QuickPromotionActionFacebookGraphQlResponse}.
 *
 * @implNote This implementation keeps {@code event} and {@code action} as {@code String} rather than
 * Java enums: the WhatsApp Web jobs ({@code WAWebJobImpressionOnQuickPromotion},
 * {@code WAWebJobDismissQuickPromotion}, {@code WAWebJobPrimaryActionClickInQuickPromotion}) emit only
 * the literals {@code "VIEW"}/{@code "ACTION"} and {@code "DISMISS"}/{@code "PRIMARY"}, but the full
 * server-side value set of {@code WaQuickPromotionLogEventData} is not declared in the bundle of
 * snapshot {@code 1040120866}, so the closed sets cannot be confirmed. The {@code client_time} field
 * is the job's {@code ts} value, sent to the in-app-comms event RPC as
 * {@code eventTimestampSec}; it is therefore modelled as an {@link Instant} serialized as
 * epoch-seconds.
 *
 * @see QuickPromotionActionFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebQuickPromotionActionMutation")
public final class QuickPromotionActionFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebQuickPromotionActionMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9741612265875562";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebQuickPromotionActionMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebQuickPromotionActionMutation";

    /**
     * The {@code event} field of the {@code input} object naming the logged event kind (for example
     * {@code "VIEW"} or {@code "ACTION"}), or {@code null} to omit it.
     */
    private final String event;

    /**
     * The {@code action} field of the {@code input} object naming the action kind (for example
     * {@code "DISMISS"} or {@code "PRIMARY"}) for an {@code "ACTION"} event, or {@code null} to omit
     * it.
     */
    private final String action;

    /**
     * The {@code promotion_id} field of the {@code input} object identifying the quick promotion, or
     * {@code null} to omit it.
     */
    private final String promotionId;

    /**
     * The {@code surface_nux_id} field of the {@code input} object identifying the surface nux, or
     * {@code null} to omit it.
     */
    private final String surfaceNuxId;

    /**
     * The {@code promotion_logging_data} field of the {@code input} object carrying the opaque logging
     * blob, or {@code null} to omit it.
     */
    private final String promotionLoggingData;

    /**
     * The {@code client_time} field of the {@code input} object recording when the client logged the
     * event, or {@code null} to omit it.
     *
     * <p>WhatsApp Web sets this to the originating job's timestamp; it is serialized as
     * seconds-since-epoch.
     */
    private final Instant clientTime;

    /**
     * Constructs a quick-promotion-action mutation request carrying the logged event.
     *
     * <p>The values populate the {@code input} GraphQL object; each value that is {@code null} is
     * omitted from the serialized object.
     *
     * @param event                the logged event kind, or {@code null} to omit the field
     * @param action               the action kind for an action event, or {@code null} to omit the
     *                             field
     * @param promotionId          the quick-promotion identifier, or {@code null} to omit the field
     * @param surfaceNuxId         the surface nux identifier, or {@code null} to omit the field
     * @param promotionLoggingData the opaque logging blob, or {@code null} to omit the field
     * @param clientTime           the client timestamp, or {@code null} to omit the field
     */
    public QuickPromotionActionFacebookGraphQlRequest(String event, String action, String promotionId, String surfaceNuxId, String promotionLoggingData, Instant clientTime) {
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
     * is non-null, rendering {@code clientTime} as its seconds-since-epoch numeric value, and emitting
     * {@code {"input": {}}} when every field is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebQuickPromotionActionMutation", exports = "executeQuickPromotionActionMutation",
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
