package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL mutation that updates the daily time window during which a WhatsApp Business AI
 * agent's auto-reply bot is enabled.
 *
 * <p>The mutation takes a single {@code input} GraphQL variable. WhatsApp Web's
 * {@code WAWebBizAiReplyBotEnabledTimeUpdateMutation.updateBotEnabledTime(window)} builds it as
 * {@code {input: {enabled_time, time_zone, from_sec_in_day, to_sec_in_day}}}, the same window shape
 * the agent's reply settings expose through {@code bot_enabled_time}. The Meta graph endpoint returns the update
 * outcome under the linked field {@code xfb_meta_ai_biz_agent_wa_update_bot_enabled_time}; the reply
 * is consumed through {@link BizAiReplyBotEnabledTimeUpdateFacebookGraphQlResponse}.
 *
 * <p>The {@code from_sec_in_day} and {@code to_sec_in_day} fields are the window bounds expressed as
 * seconds elapsed since the start of the day; they are modelled as {@link Integer}. The
 * {@code enabled_time} field toggles the timed window and is modelled as {@link Boolean}; the
 * {@code time_zone} field carries the window's time zone and is modelled as a {@link String}.
 *
 * @see BizAiReplyBotEnabledTimeUpdateFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiReplyBotEnabledTimeUpdateMutation")
public final class BizAiReplyBotEnabledTimeUpdateFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiReplyBotEnabledTimeUpdateMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26104383745927952";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiReplyBotEnabledTimeUpdateMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiReplyBotEnabledTimeUpdateMutation";

    /**
     * The {@code enabled_time} field of the {@code input} object toggling the timed window, or
     * {@code null} to omit it.
     */
    private final Boolean enabledTime;

    /**
     * The {@code time_zone} field of the {@code input} object carrying the window's time zone, or
     * {@code null} to omit it.
     */
    private final String timeZone;

    /**
     * The {@code from_sec_in_day} field of the {@code input} object holding the window start as
     * seconds since the start of the day, or {@code null} to omit it.
     */
    private final Integer fromSecInDay;

    /**
     * The {@code to_sec_in_day} field of the {@code input} object holding the window end as seconds
     * since the start of the day, or {@code null} to omit it.
     */
    private final Integer toSecInDay;

    /**
     * Constructs a reply-bot-enabled-time-update mutation request.
     *
     * <p>The {@code enabledTime} flag, {@code timeZone}, and the two seconds-in-day bounds populate
     * the {@code input} object. Each value that is {@code null} omits its field from the serialized
     * object.
     *
     * @param enabledTime  whether the timed window is enabled, or {@code null} to omit the field
     * @param timeZone     the window's time zone, or {@code null} to omit the field
     * @param fromSecInDay the window start as seconds since the start of the day, or {@code null} to
     *                     omit the field
     * @param toSecInDay   the window end as seconds since the start of the day, or {@code null} to
     *                     omit the field
     */
    public BizAiReplyBotEnabledTimeUpdateFacebookGraphQlRequest(Boolean enabledTime, String timeZone, Integer fromSecInDay, Integer toSecInDay) {
        this.enabledTime = enabledTime;
        this.timeZone = timeZone;
        this.fromSecInDay = fromSecInDay;
        this.toSecInDay = toSecInDay;
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
     * @implNote This implementation emits {@code {"input": {"enabled_time": <enabledTime>,
     * "time_zone": <timeZone>, "from_sec_in_day": <fromSecInDay>, "to_sec_in_day": <toSecInDay>}}},
     * writing each inner field only when its value is non-null and emitting {@code {"input": {}}} when
     * all are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiReplyBotEnabledTimeUpdateMutation", exports = "updateBotEnabledTime",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (enabledTime != null) {
                writer.writeName("enabled_time");
                writer.writeColon();
                writer.writeBool(enabledTime);
            }

            if (timeZone != null) {
                writer.writeName("time_zone");
                writer.writeColon();
                writer.writeString(timeZone);
            }

            if (fromSecInDay != null) {
                writer.writeName("from_sec_in_day");
                writer.writeColon();
                writer.writeInt32(fromSecInDay);
            }

            if (toSecInDay != null) {
                writer.writeName("to_sec_in_day");
                writer.writeColon();
                writer.writeInt32(toSecInDay);
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
