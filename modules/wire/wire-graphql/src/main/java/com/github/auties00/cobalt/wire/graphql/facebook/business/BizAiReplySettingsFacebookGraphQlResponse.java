package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiReplySettings;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiReplySettingsBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the fetch-reply-settings query built by
 * {@link BizAiReplySettingsFacebookGraphQlRequest} into a {@link BusinessAiReplySettings}.
 *
 * <p>Projects the linked {@code xfb_meta_ai_biz_agent_wa_reply_chat_trigger} field, which carries the
 * chat-scope marker and the nested daily active-window settings, onto the {@link BusinessAiReplySettings}
 * model.
 *
 * @see BizAiReplySettingsFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiReplySettingsQuery")
public final class BizAiReplySettingsFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected reply settings, or {@code null} when the Meta graph endpoint omitted the field.
     */
    private final BusinessAiReplySettings replySettings;

    /**
     * Constructs a response wrapping the projected reply settings.
     *
     * <p>Reserved for the static parser.
     *
     * @param replySettings the projected reply settings, or {@code null} when the Meta graph endpoint omitted the
     *                      field
     */
    private BizAiReplySettingsFacebookGraphQlResponse(BusinessAiReplySettings replySettings) {
        this.replySettings = replySettings;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * chat-scope marker and daily active-window settings onto a {@link BusinessAiReplySettings}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAiReplySettingsFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var trigger = data.getJSONObject("xfb_meta_ai_biz_agent_wa_reply_chat_trigger");
        if (trigger == null) {
            return Optional.of(new BizAiReplySettingsFacebookGraphQlResponse(null));
        }

        var window = trigger.getJSONObject("bot_enabled_time");
        var enabled = window != null && Boolean.TRUE.equals(window.getBoolean("enabled_time"));
        var replySettings = new BusinessAiReplySettingsBuilder()
                .triggerChatType(trigger.getString("trigger_chat_type"))
                .enabled(enabled)
                .fromSecondOfDay(window != null ? window.getLong("from_sec_in_day") : null)
                .toSecondOfDay(window != null ? window.getLong("to_sec_in_day") : null)
                .timeZone(window != null ? window.getString("time_zone") : null)
                .build();
        return Optional.of(new BizAiReplySettingsFacebookGraphQlResponse(replySettings));
    }

    /**
     * Returns the projected automatic-reply settings.
     *
     * @return the projected {@link BusinessAiReplySettings}, or empty when the Meta graph endpoint omitted the field
     */
    public Optional<BusinessAiReplySettings> replySettings() {
        return Optional.ofNullable(replySettings);
    }
}
