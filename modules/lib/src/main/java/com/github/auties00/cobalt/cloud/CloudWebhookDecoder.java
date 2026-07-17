package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.cloud.CloudAccountUpdate;
import com.github.auties00.cobalt.wire.cloud.CloudAppStateSyncAction;
import com.github.auties00.cobalt.wire.cloud.CloudAppStateSyncContact;
import com.github.auties00.cobalt.wire.cloud.CloudBusinessCapabilityUpdate;
import com.github.auties00.cobalt.wire.cloud.CloudCallDirection;
import com.github.auties00.cobalt.wire.cloud.CloudCallEvent;
import com.github.auties00.cobalt.wire.cloud.CloudCallHours;
import com.github.auties00.cobalt.wire.cloud.CloudCallPermissionResponse;
import com.github.auties00.cobalt.wire.cloud.CloudCallSettings;
import com.github.auties00.cobalt.wire.cloud.CloudCallStatus;
import com.github.auties00.cobalt.wire.cloud.CloudMarketingPreference;
import com.github.auties00.cobalt.wire.cloud.flow.CloudFlowStatusUpdate;
import com.github.auties00.cobalt.wire.cloud.CloudHistorySync;
import com.github.auties00.cobalt.wire.cloud.CloudMessagePricing;
import com.github.auties00.cobalt.wire.cloud.CloudCallSession;
import com.github.auties00.cobalt.wire.cloud.commerce.CloudPaymentConfiguration;
import com.github.auties00.cobalt.wire.cloud.phone.CloudMessagingLimitTier;
import com.github.auties00.cobalt.wire.cloud.phone.CloudPhoneNumberUpdate;
import com.github.auties00.cobalt.wire.cloud.CloudSecurityUpdate;
import com.github.auties00.cobalt.wire.cloud.CloudSystemUpdate;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateCategoryUpdate;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateComponentsUpdate;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplatePauseUpdate;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateQualityUpdate;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateStatusUpdate;
import com.github.auties00.cobalt.wire.cloud.CloudUserPreferenceUpdate;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.core.message.MessageStatus;
import com.github.auties00.cobalt.wire.core.message.MessageInfo;
import com.github.auties00.cobalt.wire.core.message.MediaType;
import com.github.auties00.cobalt.wire.core.message.ContactContent;
import com.github.auties00.cobalt.wire.cloud.CloudMessageContainer;
import com.github.auties00.cobalt.wire.cloud.CloudMessageInfo;
import com.github.auties00.cobalt.wire.cloud.content.CloudTextContent;
import com.github.auties00.cobalt.wire.cloud.content.CloudMediaContent;
import com.github.auties00.cobalt.wire.cloud.content.CloudLocationContent;
import com.github.auties00.cobalt.wire.cloud.content.CloudReactionContent;
import com.github.auties00.cobalt.wire.cloud.content.CloudContactContent;
import com.github.auties00.cobalt.wire.cloud.content.CloudContactsArrayContent;
import com.github.auties00.cobalt.wire.cloud.content.CloudButtonsResponseContent;
import com.github.auties00.cobalt.wire.cloud.content.CloudListResponseContent;
import com.github.auties00.cobalt.wire.cloud.content.CloudInteractiveResponseContent;
import com.github.auties00.cobalt.wire.cloud.content.CloudOrderContent;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates inbound WhatsApp Cloud API webhook change values into Cobalt's universal message model.
 *
 * <p>The decoder is the inverse of {@link CloudMessageEncoder}: it reads the {@code messages[]},
 * {@code statuses[]}, and {@code contacts[]} arrays of a webhook change {@code value} and produces
 * {@code ChatMessageInfo} instances for inbound messages and {@link StatusUpdate} records for outbound
 * delivery receipts, mapping the Cloud status strings onto the shared {@link MessageStatus} model.
 * Inbound media is referenced by its Cloud media id, stored in the media message's
 * url field so a later download resolves it through the media edge. Beyond text, media, location,
 * reaction and reply types it also decodes native-flow replies ({@code nfm_reply}), shared contact
 * cards and catalog orders; genuinely unmapped types fall back to {@code LinkedMessageContainer#empty()}.
 */
public final class CloudWebhookDecoder {
    /**
     * The logger for {@link CloudWebhookDecoder}.
     */
    private static final System.Logger LOGGER = Log.get(CloudWebhookDecoder.class);

    /**
     * Private constructor; the decoder exposes only static behaviour.
     */
    private CloudWebhookDecoder() {

    }

    /**
     * A single outbound message status transition decoded from a {@code statuses[]} entry.
     *
     * @param info    the message descriptor carrying the key and the mapped {@link MessageStatus}
     * @param deleted whether the entry reported a {@code deleted} status rather than a delivery
     *                transition
     * @param pricing the billing information carried by the entry, or {@code null} when the entry had
     *                no {@code pricing} object
     */
    public record StatusUpdate(MessageInfo info, boolean deleted, CloudMessagePricing pricing) {
    }

    /**
     * Decodes the inbound messages of a webhook change value.
     *
     * @param value the webhook change {@code value}
     * @return the decoded inbound messages, empty when the change carried none
     */
    public static List<MessageInfo> decodeMessages(JSONObject value) {
        return decodeMessageArray(value, "messages");
    }

    /**
     * Decodes the echoed messages of an {@code smb_message_echoes} change value.
     *
     * <p>WhatsApp Coexistence delivers a business's own outbound messages, including those sent from the
     * companion phone app, under the {@code message_echoes} array rather than the {@code messages} array
     * used by ordinary inbound deliveries; the per-message objects share the same shape, so the same
     * decode applies.
     *
     * @param value the webhook change {@code value}
     * @return the decoded echoed messages, empty when the change carried none
     */
    public static List<MessageInfo> decodeMessageEchoes(JSONObject value) {
        return decodeMessageArray(value, "message_echoes");
    }

    /**
     * Decodes the per-message objects of a named array within a webhook change value.
     *
     * @param value    the webhook change {@code value}
     * @param arrayKey the array member key, {@code messages} for inbound deliveries or
     *                 {@code message_echoes} for business message echoes
     * @return the decoded messages, empty when the array was absent or empty
     */
    private static List<MessageInfo> decodeMessageArray(JSONObject value, String arrayKey) {
        var messages = value.getJSONArray(arrayKey);
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        var pushNames = pushNamesByWaId(value);
        var result = new ArrayList<MessageInfo>();
        for (var index = 0; index < messages.size(); index++) {
            var message = messages.getJSONObject(index);
            if (isCallPermissionReply(message)) {
                continue;
            }
            result.add(decodeMessage(message, pushNames));
        }
        return result;
    }

    /**
     * Returns whether an inbound message is a call-permission reply, which is surfaced as a
     * {@link CloudCallEvent} through {@link #decodeCallPermissionReplies(JSONObject)} rather than as a
     * chat message.
     *
     * @param message the inbound message object
     * @return {@code true} when the message is an interactive {@code call_permission_reply}
     */
    private static boolean isCallPermissionReply(JSONObject message) {
        var interactive = message.getJSONObject("interactive");
        return interactive != null && "call_permission_reply".equals(interactive.getString("type"));
    }

    /**
     * Decodes the outbound status transitions of a webhook change value.
     *
     * <p>Each {@code statuses[]} entry carries the message id, the recipient, the status, and the
     * timestamp. The optional {@code pricing} member (present for {@code sent} and {@code delivered},
     * absent for {@code read} and {@code failed} and on some transitions in newer API versions) is
     * captured into {@link StatusUpdate#pricing()} when present, leaving it {@code null} otherwise.
     * The optional {@code conversation} member is not read, so its omission is decode-safe.
     *
     * @param value the webhook change {@code value}
     * @return the decoded status updates, empty when the change carried none
     */
    public static List<StatusUpdate> decodeStatuses(JSONObject value) {
        var statuses = value.getJSONArray("statuses");
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        var result = new ArrayList<StatusUpdate>();
        for (var index = 0; index < statuses.size(); index++) {
            var status = statuses.getJSONObject(index);
            var key = new MessageKeyBuilder()
                    .id(status.getString("id"))
                    .parentJid(userJid(status.getString("recipient_id")))
                    .fromMe(true)
                    .build();
            var statusValue = status.getString("status");
            var deleted = "deleted".equalsIgnoreCase(statusValue);
            var timestampValue = status.getLong("timestamp");
            var timestamp = timestampValue == null ? null : Instant.ofEpochSecond(timestampValue);
            var info = new CloudMessageInfo(key, CloudMessageContainer.empty(), deleted ? null : toMessageStatus(statusValue), timestamp, null, null);
            result.add(new StatusUpdate(info, deleted, decodeStatusPricing(status)));
        }
        return result;
    }

    /**
     * Decodes the {@code pricing} object of a {@code statuses[]} entry into a {@link CloudMessagePricing}.
     *
     * <p>The {@code pricing_model} and {@code category} members are required; an entry whose
     * {@code pricing} object lacks either is treated as carrying no pricing and yields {@code null}.
     * The {@code billable} member defaults to {@code true} when absent, matching the platform default
     * for the regular pricing type. The pricing type rides the {@code type} member, falling back to
     * the legacy {@code pricing_type} member.
     *
     * @param status the {@code statuses[]} entry object
     * @return the decoded pricing, or {@code null} when the entry carried no usable {@code pricing}
     *         object
     */
    private static CloudMessagePricing decodeStatusPricing(JSONObject status) {
        var pricing = status.getJSONObject("pricing");
        if (pricing == null) {
            return null;
        }
        var pricingModel = pricing.getString("pricing_model");
        var category = pricing.getString("category");
        if (pricingModel == null || category == null) {
            return null;
        }
        var billable = pricing.getBoolean("billable");
        var pricingType = pricing.getString("type");
        if (pricingType == null) {
            pricingType = pricing.getString("pricing_type");
        }
        return new CloudMessagePricing(billable == null || billable, pricingModel, pricingType, category);
    }

    /**
     * Maps a Cloud API {@code status} string onto the shared {@link MessageStatus} model.
     *
     * @param value the raw status string, for example {@code "delivered"}
     * @return the matching status, defaulting to {@link MessageStatus#SERVER_ACK} when the string is
     *         unrecognised
     */
    private static MessageStatus toMessageStatus(String value) {
        if (value == null) {
            return MessageStatus.SERVER_ACK;
        }
        return switch (value.toLowerCase()) {
            case "delivered" -> MessageStatus.DELIVERED;
            case "read" -> MessageStatus.READ;
            case "failed" -> MessageStatus.ERROR;
            default -> MessageStatus.SERVER_ACK;
        };
    }


    /**
     * Decodes a {@code message_template_status_update} change value.
     *
     * @param value the webhook change {@code value}
     * @return the decoded status transition
     */
    public static CloudTemplateStatusUpdate decodeTemplateStatus(JSONObject value) {
        Instant disableDate = null;
        var disableInfo = value.getJSONObject("disable_info");
        if (disableInfo != null) {
            disableDate = epochInstant(disableInfo.getLong("disable_date"));
        }
        return new CloudTemplateStatusUpdate(
                stringOrUnknown(value, "event"),
                idString(value, "message_template_id"),
                value.getString("message_template_name"),
                value.getString("message_template_language"),
                value.getString("reason"),
                disableDate);
    }

    /**
     * Decodes a {@code message_template_pause} or {@code message_template_unpause} change value.
     *
     * <p>The id, name, language, and reason members are required; the optional {@code pause_date} member
     * carries epoch seconds and is read only when present.
     *
     * @param value the webhook change {@code value}
     * @return the decoded pause or unpause notification
     */
    public static CloudTemplatePauseUpdate decodeTemplatePause(JSONObject value) {
        return new CloudTemplatePauseUpdate(
                idString(value, "message_template_id"),
                stringOrUnknown(value, "message_template_name"),
                stringOrUnknown(value, "message_template_language"),
                stringOrUnknown(value, "reason"),
                epochInstant(value.getLong("pause_date")));
    }

    /**
     * Decodes a {@code template_category_update} change value.
     *
     * @param value the webhook change {@code value}
     * @return the decoded category transition
     */
    public static CloudTemplateCategoryUpdate decodeTemplateCategory(JSONObject value) {
        return new CloudTemplateCategoryUpdate(
                idString(value, "message_template_id"),
                value.getString("message_template_name"),
                value.getString("message_template_language"),
                value.getString("previous_category"),
                stringOrUnknown(value, "new_category"),
                value.getString("correct_category"));
    }

    /**
     * Decodes a {@code message_template_quality_update} change value.
     *
     * @param value the webhook change {@code value}
     * @return the decoded quality transition
     */
    public static CloudTemplateQualityUpdate decodeTemplateQuality(JSONObject value) {
        return new CloudTemplateQualityUpdate(
                idString(value, "message_template_id"),
                value.getString("message_template_name"),
                value.getString("message_template_language"),
                value.getString("previous_quality_score"),
                stringOrUnknown(value, "new_quality_score"));
    }

    /**
     * Decodes a {@code phone_number_name_update} change value.
     *
     * @param value the webhook change {@code value}
     * @return the decoded display-name review outcome
     */
    public static CloudPhoneNumberUpdate decodePhoneNumberName(JSONObject value) {
        return new CloudPhoneNumberUpdate.Name(
                stringOrUnknown(value, "display_phone_number"),
                stringOrUnknown(value, "decision"),
                value.getString("requested_verified_name"),
                value.getString("rejection_reason"));
    }

    /**
     * Decodes a {@code phone_number_quality_update} change value.
     *
     * @param value the webhook change {@code value}
     * @return the decoded quality transition
     */
    public static CloudPhoneNumberUpdate decodePhoneNumberQuality(JSONObject value) {
        var currentLimit = value.getString("current_limit");
        return new CloudPhoneNumberUpdate.Quality(
                stringOrUnknown(value, "display_phone_number"),
                stringOrUnknown(value, "event"),
                currentLimit == null ? null : CloudMessagingLimitTier.of(currentLimit));
    }

    /**
     * Decodes an {@code account_update}, {@code account_alerts}, or {@code account_review_update}
     * change value.
     *
     * @param value the webhook change {@code value}
     * @return the decoded account update
     */
    public static CloudAccountUpdate decodeAccountUpdate(JSONObject value) {
        String banState = null;
        Instant banDate = null;
        var banInfo = value.getJSONObject("ban_info");
        if (banInfo != null) {
            banState = banInfo.getString("waba_ban_state");
            banDate = epochInstant(banInfo.getLong("waba_ban_date"));
        }
        var restrictions = new ArrayList<CloudAccountUpdate.Restriction>();
        var restrictionInfo = value.getJSONArray("restriction_info");
        if (restrictionInfo != null) {
            for (var index = 0; index < restrictionInfo.size(); index++) {
                var restriction = restrictionInfo.getJSONObject(index);
                var type = restriction.getString("restriction_type");
                if (type != null) {
                    restrictions.add(new CloudAccountUpdate.Restriction(type, epochInstant(restriction.getLong("expiration"))));
                }
            }
        }
        String violationType = null;
        var violationInfo = value.getJSONObject("violation_info");
        if (violationInfo != null) {
            violationType = violationInfo.getString("violation_type");
        }
        return new CloudAccountUpdate(
                value.getString("event"),
                value.getString("phone_number"),
                value.getString("decision"),
                banState,
                banDate,
                restrictions,
                violationType);
    }

    /**
     * Decodes a {@code business_capability_update} change value.
     *
     * @param value the webhook change {@code value}
     * @return the decoded capability update
     */
    public static CloudBusinessCapabilityUpdate decodeBusinessCapability(JSONObject value) {
        var maxDaily = value.getInteger("max_daily_conversation_per_phone");
        var maxNumbers = value.getInteger("max_phone_numbers_per_business");
        return new CloudBusinessCapabilityUpdate(
                maxDaily == null ? -1 : maxDaily,
                maxNumbers == null ? -1 : maxNumbers);
    }

    /**
     * Decodes the entries of a {@code user_preferences} change value.
     *
     * @param value the webhook change {@code value}
     * @return the decoded preference changes, empty when the change carried none
     */
    public static List<CloudUserPreferenceUpdate> decodeUserPreferences(JSONObject value) {
        var entries = value.getJSONArray("user_preferences");
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        var result = new ArrayList<CloudUserPreferenceUpdate>();
        for (var index = 0; index < entries.size(); index++) {
            var entry = entries.getJSONObject(index);
            var waId = entry.getString("wa_id");
            if (waId == null) {
                continue;
            }
            result.add(new CloudUserPreferenceUpdate(
                    waId,
                    entry.getString("detail"),
                    stringOrUnknown(entry, "category"),
                    CloudMarketingPreference.of(entry.getString("value")),
                    epochInstant(entry.getLong("timestamp"))));
        }
        return result;
    }

    /**
     * Decodes a {@code flows} change value.
     *
     * @param value the webhook change {@code value}
     * @return the decoded flow event
     */
    public static CloudFlowStatusUpdate decodeFlowStatus(JSONObject value) {
        var event = stringOrUnknown(value, "event");
        var flowId = idString(value, "flow_id");
        var message = value.getString("message");
        var oldStatus = value.getString("old_status");
        var newStatus = value.getString("new_status");
        if (oldStatus != null || newStatus != null) {
            return new CloudFlowStatusUpdate.StatusChange(event, flowId, message, oldStatus, newStatus);
        }
        return new CloudFlowStatusUpdate.EndpointHealth(event, flowId, message);
    }

    /**
     * Decodes a {@code message_template_components_update} change value.
     *
     * <p>The body text rides the {@code message_template_element} member; the optional text header
     * title, footer, and buttons are read only when the corresponding members are present. Each button
     * carries its type and label, and, depending on the type, a URL or a phone number.
     *
     * @param value the webhook change {@code value}
     * @return the decoded template components
     */
    public static CloudTemplateComponentsUpdate decodeTemplateComponents(JSONObject value) {
        var buttons = new ArrayList<CloudTemplateComponentsUpdate.Button>();
        var buttonArray = value.getJSONArray("message_template_buttons");
        if (buttonArray != null) {
            for (var index = 0; index < buttonArray.size(); index++) {
                var button = buttonArray.getJSONObject(index);
                buttons.add(CloudTemplateComponentsUpdate.Button.of(
                        stringOrUnknown(button, "message_template_button_type"),
                        stringOrUnknown(button, "message_template_button_text"),
                        button.getString("message_template_button_url"),
                        button.getString("message_template_button_phone_number")));
            }
        }
        return new CloudTemplateComponentsUpdate(
                idString(value, "message_template_id"),
                stringOrUnknown(value, "message_template_name"),
                stringOrUnknown(value, "message_template_language"),
                stringOrUnknown(value, "message_template_element"),
                value.getString("message_template_title"),
                value.getString("message_template_footer"),
                buttons);
    }

    /**
     * Decodes a {@code security} change value.
     *
     * @param value the webhook change {@code value}
     * @return the decoded security event
     */
    public static CloudSecurityUpdate decodeSecurity(JSONObject value) {
        return new CloudSecurityUpdate(
                stringOrUnknown(value, "display_phone_number"),
                stringOrUnknown(value, "event"),
                value.getString("requester"));
    }

    /**
     * Decodes a {@code payment_configuration_update} change value.
     *
     * <p>The {@code created_timestamp} and {@code updated_timestamp} members carry epoch seconds and
     * are projected onto {@link Instant} through {@link #epochInstant(Long)}.
     *
     * @param value the webhook change {@code value}
     * @return the decoded payment configuration change
     */
    public static CloudPaymentConfiguration decodePaymentConfiguration(JSONObject value) {
        return new CloudPaymentConfiguration(
                stringOrUnknown(value, "configuration_name"),
                value.getString("provider_name"),
                value.getString("provider_mid"),
                value.getString("status"),
                epochInstant(value.getLong("created_timestamp")),
                epochInstant(value.getLong("updated_timestamp")));
    }

    /**
     * Decodes the chunks of a {@code history} change value.
     *
     * @param value the webhook change {@code value}
     * @return the decoded history chunks, empty when the change carried none
     */
    public static List<CloudHistorySync> decodeHistory(JSONObject value) {
        var chunks = value.getJSONArray("history");
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        var result = new ArrayList<CloudHistorySync>();
        for (var index = 0; index < chunks.size(); index++) {
            var chunk = chunks.getJSONObject(index);
            var phase = -1;
            var chunkOrder = -1;
            var progress = -1;
            var metadata = chunk.getJSONObject("metadata");
            if (metadata != null) {
                var phaseValue = metadata.getInteger("phase");
                var chunkOrderValue = metadata.getInteger("chunk_order");
                var progressValue = metadata.getInteger("progress");
                phase = phaseValue == null ? -1 : phaseValue;
                chunkOrder = chunkOrderValue == null ? -1 : chunkOrderValue;
                progress = progressValue == null ? -1 : progressValue;
            }
            var messages = new ArrayList<MessageInfo>();
            var threads = chunk.getJSONArray("threads");
            if (threads != null) {
                for (var threadIndex = 0; threadIndex < threads.size(); threadIndex++) {
                    var threadMessages = threads.getJSONObject(threadIndex).getJSONArray("messages");
                    if (threadMessages == null) {
                        continue;
                    }
                    for (var messageIndex = 0; messageIndex < threadMessages.size(); messageIndex++) {
                        messages.add(decodeMessage(threadMessages.getJSONObject(messageIndex), Map.of()));
                    }
                }
            }
            result.add(new CloudHistorySync(phase, chunkOrder, progress, messages));
        }
        return result;
    }

    /**
     * Decodes the contact entries of an {@code smb_app_state_sync} change value.
     *
     * <p>WhatsApp Coexistence delivers the business app's current and changed contacts shortly after
     * onboarding succeeds. Each {@code state_sync[]} entry typed {@code contact} is projected onto a
     * {@link CloudAppStateSyncContact}.
     *
     * @param value the webhook change {@code value}
     * @return the decoded contact sync entries, empty when the change carried none
     */
    public static List<CloudAppStateSyncContact> decodeAppStateSync(JSONObject value) {
        var entries = value.getJSONArray("state_sync");
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        var result = new ArrayList<CloudAppStateSyncContact>();
        for (var index = 0; index < entries.size(); index++) {
            var entry = entries.getJSONObject(index);
            var contact = entry.getJSONObject("contact");
            var metadata = entry.getJSONObject("metadata");
            result.add(new CloudAppStateSyncContact(
                    stringOrUnknown(entry, "type"),
                    CloudAppStateSyncAction.of(entry.getString("action")),
                    contact == null ? null : contact.getString("full_name"),
                    contact == null ? null : contact.getString("first_name"),
                    contact == null ? null : contact.getString("phone_number"),
                    metadata == null ? null : epochInstant(callTimestamp(metadata))));
        }
        return result;
    }

    /**
     * Decodes an {@code account_settings_update} change value into the Calling configuration it carries.
     *
     * <p>The change reports the current phone-number settings under {@code phone_number_settings}; the
     * only modelled section is its {@code calling} object, which is projected onto a
     * {@link CloudCallSettings} carrying the master status, the in-app call-icon visibility, the
     * callback-permission status, the business-hours configuration, and the SIP bridge.
     *
     * @param value the webhook change {@code value}
     * @return the decoded Calling configuration, or {@code null} when the change carried no
     *         {@code calling} object
     */
    public static CloudCallSettings decodeAccountSettings(JSONObject value) {
        var settings = value.getJSONObject("phone_number_settings");
        var calling = settings == null ? null : settings.getJSONObject("calling");
        if (calling == null) {
            return null;
        }
        CloudCallSettings.CallIcons callIcons = null;
        var iconsNode = calling.getJSONObject("call_icons");
        if (iconsNode != null) {
            var countries = new ArrayList<String>();
            var countryArray = iconsNode.getJSONArray("restrict_to_user_countries");
            if (countryArray != null) {
                for (var index = 0; index < countryArray.size(); index++) {
                    countries.add(countryArray.getString(index));
                }
            }
            callIcons = new CloudCallSettings.CallIcons(countries);
        }
        return new CloudCallSettings(
                calling.getString("status"),
                calling.getString("call_icon_visibility"),
                callIcons,
                calling.getString("callback_permission_status"),
                calling.getString("srtp_key_exchange_protocol"),
                parseCallHours(calling.getJSONObject("call_hours")),
                parseSip(calling.getJSONObject("sip")));
    }

    /**
     * Parses a {@code call_hours} object into a {@link CloudCallHours}.
     *
     * @param hours the {@code call_hours} object, or {@code null}
     * @return the parsed business-hours configuration, or {@code null} when the input is {@code null}
     */
    private static CloudCallHours parseCallHours(JSONObject hours) {
        if (hours == null) {
            return null;
        }
        var weekly = new ArrayList<CloudCallHours.WeeklyOperatingHours>();
        var weeklyArray = hours.getJSONArray("weekly_operating_hours");
        if (weeklyArray != null) {
            for (var index = 0; index < weeklyArray.size(); index++) {
                var slot = weeklyArray.getJSONObject(index);
                weekly.add(new CloudCallHours.WeeklyOperatingHours(
                        slot.getString("day_of_week"),
                        slot.getString("open_time"),
                        slot.getString("close_time")));
            }
        }
        var holidays = new ArrayList<CloudCallHours.HolidaySchedule>();
        var holidayArray = hours.getJSONArray("holiday_schedule");
        if (holidayArray != null) {
            for (var index = 0; index < holidayArray.size(); index++) {
                var slot = holidayArray.getJSONObject(index);
                holidays.add(new CloudCallHours.HolidaySchedule(
                        slot.getString("date"),
                        slot.getString("start_time"),
                        slot.getString("end_time")));
            }
        }
        return new CloudCallHours(hours.getString("status"), hours.getString("timezone_id"), weekly, holidays);
    }

    /**
     * Parses a {@code sip} object into a {@link CloudCallSettings.Sip}.
     *
     * @param sip the {@code sip} object, or {@code null}
     * @return the parsed SIP configuration, or {@code null} when the input is {@code null}
     */
    private static CloudCallSettings.Sip parseSip(JSONObject sip) {
        if (sip == null) {
            return null;
        }
        var servers = new ArrayList<CloudCallSettings.SipServer>();
        var serverArray = sip.getJSONArray("servers");
        if (serverArray != null) {
            for (var index = 0; index < serverArray.size(); index++) {
                var server = serverArray.getJSONObject(index);
                var port = server.containsKey("port") ? server.getInteger("port") : null;
                var appId = server.containsKey("app_id") ? server.getInteger("app_id") : null;
                var params = new LinkedHashMap<String, String>();
                var paramsNode = server.getJSONObject("request_uri_user_params");
                if (paramsNode != null) {
                    for (var key : paramsNode.keySet()) {
                        params.put(key, paramsNode.getString(key));
                    }
                }
                servers.add(new CloudCallSettings.SipServer(
                        server.getString("hostname"), port, server.getString("sip_user_password"),
                        params, appId));
            }
        }
        return new CloudCallSettings.Sip(sip.getString("status"), servers);
    }

    /**
     * Decodes the system notifications carried as {@code system}-typed inbound messages in a
     * {@code messages} change value.
     *
     * <p>A consumer changing their phone number or their account identity arrives as an inbound message
     * whose {@code type} is {@code system}. Each matching message is projected onto the appropriate
     * {@link CloudSystemUpdate} variant: a {@code customer_changed_number} transition onto a
     * {@link CloudSystemUpdate.NumberChanged} (the new phone number is read from {@code system.new_wa_id}
     * or, in the legacy string form, from {@code system.wa_id}) and a {@code customer_identity_changed}
     * transition onto a {@link CloudSystemUpdate.IdentityChanged}. The polymorphic {@code system.type}
     * member is normalized to a single token through {@link #normalizeSystemType(JSONObject)}; any other
     * token is skipped to keep the variant set closed.
     *
     * @param value the webhook change {@code value}
     * @return the decoded system updates, empty when the change carried none
     */
    public static List<CloudSystemUpdate> decodeSystemUpdates(JSONObject value) {
        var messages = value.getJSONArray("messages");
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        var result = new ArrayList<CloudSystemUpdate>();
        for (var index = 0; index < messages.size(); index++) {
            var message = messages.getJSONObject(index);
            if (!"system".equals(message.getString("type"))) {
                continue;
            }
            var system = message.getJSONObject("system");
            if (system == null) {
                continue;
            }
            var from = message.getString("from");
            if (from == null) {
                continue;
            }
            var body = system.getString("body");
            var customer = system.getString("customer");
            var timestamp = epochInstant(message.getLong("timestamp"));
            var systemType = normalizeSystemType(system);
            switch (systemType) {
                case "customer_changed_number" -> {
                    var newWaId = system.getString("new_wa_id");
                    if (newWaId == null) {
                        newWaId = system.getString("wa_id");
                    }
                    if (newWaId != null) {
                        result.add(new CloudSystemUpdate.NumberChanged(from, body, customer, timestamp, newWaId));
                    }
                }
                case "customer_identity_changed" -> {
                    var identity = system.getString("identity");
                    if (identity != null) {
                        result.add(new CloudSystemUpdate.IdentityChanged(from, body, customer, timestamp, identity));
                    }
                }
                default -> {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "unmapped system update type {0}", systemType);
                }
            }
        }
        return result;
    }

    /**
     * Normalizes the polymorphic {@code system.type} member onto a single canonical string.
     *
     * <p>The member appears either as a string ({@code customer_changed_number},
     * {@code customer_identity_changed}, or the legacy {@code user_changed_number}) or as an object of
     * booleans across documentation versions; both forms are mapped to one of
     * {@code customer_changed_number} or {@code customer_identity_changed}, falling back to
     * {@code UNKNOWN}.
     *
     * @param system the {@code system} object
     * @return the canonical type string
     */
    private static String normalizeSystemType(JSONObject system) {
        var raw = system.get("type");
        if (raw instanceof String value) {
            return switch (value) {
                case "user_changed_number", "customer_changed_number" -> "customer_changed_number";
                case "user_identity_changed", "customer_identity_changed" -> "customer_identity_changed";
                default -> value;
            };
        }
        var object = system.getJSONObject("type");
        if (object != null) {
            if (Boolean.TRUE.equals(object.getBoolean("customer_changed_number"))) {
                return "customer_changed_number";
            }
            if (Boolean.TRUE.equals(object.getBoolean("customer_identity_changed"))) {
                return "customer_identity_changed";
            }
        }
        return "UNKNOWN";
    }

    /**
     * Decodes the inbound signaling events of a {@code calls} change value.
     *
     * <p>Each {@code calls[]} entry carries a {@code connect} (with an SDP offer in its {@code session}
     * object) or a {@code terminate} (with the final status, duration, and the start and end
     * timestamps). A {@code connect} is projected onto a {@link CloudCallEvent.Connect} and any other
     * entry is treated as a {@code terminate} and projected onto a {@link CloudCallEvent.Terminate}.
     *
     * @param value the webhook change {@code value}
     * @return the decoded signaling events, empty when the change carried none
     */
    public static List<CloudCallEvent.Signaling> decodeCalls(JSONObject value) {
        var calls = value.getJSONArray("calls");
        if (calls == null || calls.isEmpty()) {
            return List.of();
        }
        var result = new ArrayList<CloudCallEvent.Signaling>();
        for (var index = 0; index < calls.size(); index++) {
            var call = calls.getJSONObject(index);
            if ("connect".equals(call.getString("event"))) {
                var session = call.getJSONObject("session");
                var callSession = session == null ? null
                        : new CloudCallSession(CloudCallSession.Type.of(session.getString("sdp_type")), session.getString("sdp"));
                result.add(new CloudCallEvent.Connect(
                        call.getString("id"),
                        call.getString("from"),
                        call.getString("to"),
                        callDirection(call),
                        callSession,
                        epochInstant(callTimestamp(call))));
            } else {
                result.add(new CloudCallEvent.Terminate(
                        call.getString("id"),
                        call.getString("from"),
                        call.getString("to"),
                        callDirection(call),
                        call.getString("status"),
                        call.getInteger("duration"),
                        epochInstant(call.getLong("start_time")),
                        epochInstant(call.getLong("end_time")),
                        epochInstant(callTimestamp(call))));
            }
        }
        return result;
    }

    /**
     * Decodes the business-initiated call status transitions of a {@code calls} change value.
     *
     * <p>A business-initiated call reports its lifecycle ({@code RINGING}, {@code ACCEPTED},
     * {@code REJECTED}) through the {@code calls} field inside a {@code statuses[]} array, each entry
     * typed {@code call}. This is the status-update twin of {@link #decodeCalls(JSONObject)}, which reads
     * the {@code calls[]} array carrying the inbound SDP offer and the terminate disposition. Entries
     * whose {@code type} is not {@code call} (ordinary message statuses riding the same array) are
     * ignored. The callee phone number is the only party present and is mapped to
     * {@link CloudCallEvent#from()}.
     *
     * @param value the webhook change {@code value}
     * @return the decoded call status transitions, empty when the change carried none
     */
    public static List<CloudCallEvent.Status> decodeCallStatuses(JSONObject value) {
        var statuses = value.getJSONArray("statuses");
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        var result = new ArrayList<CloudCallEvent.Status>();
        for (var index = 0; index < statuses.size(); index++) {
            var status = statuses.getJSONObject(index);
            if (!"call".equals(status.getString("type"))) {
                continue;
            }
            result.add(new CloudCallEvent.Status(
                    status.getString("id"),
                    status.getString("recipient_id"),
                    CloudCallStatus.of(status.getString("status")),
                    CloudCallDirection.BUSINESS_INITIATED,
                    epochInstant(callTimestamp(status))));
        }
        return result;
    }

    /**
     * Decodes the call-permission replies carried as interactive messages in a {@code messages} change
     * value.
     *
     * <p>A consumer's accept or reject of a permission request arrives as an interactive message whose
     * {@code interactive.type} is {@code call_permission_reply}. Each matching message is projected onto
     * a {@link CloudCallEvent.PermissionReply} carrying the response, its source, and the granted
     * permission's expiration rather than an SDP description.
     *
     * @param value the webhook change {@code value}
     * @return the decoded permission replies, empty when the change carried none
     */
    public static List<CloudCallEvent.PermissionReply> decodeCallPermissionReplies(JSONObject value) {
        var messages = value.getJSONArray("messages");
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        var result = new ArrayList<CloudCallEvent.PermissionReply>();
        for (var index = 0; index < messages.size(); index++) {
            var message = messages.getJSONObject(index);
            var interactive = message.getJSONObject("interactive");
            if (interactive == null || !"call_permission_reply".equals(interactive.getString("type"))) {
                continue;
            }
            var reply = interactive.getJSONObject("call_permission_reply");
            if (reply == null) {
                continue;
            }
            result.add(new CloudCallEvent.PermissionReply(
                    message.getString("id"),
                    message.getString("from"),
                    CloudCallPermissionResponse.of(reply.getString("response")),
                    reply.getString("response_source"),
                    epochInstant(reply.getLong("expiration_timestamp")),
                    epochInstant(message.getLong("timestamp"))));
        }
        return result;
    }

    /**
     * Reads the {@code timestamp} member of a calling event, which the wire carries as a string of epoch
     * seconds.
     *
     * @param call the call object
     * @return the epoch seconds, or {@code null} when absent or unparseable
     */
    private static Long callTimestamp(JSONObject call) {
        var raw = call.get("timestamp");
        if (raw == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(raw));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * Reads a string member, substituting {@code UNKNOWN} when absent.
     *
     * @param value the change value
     * @param key   the member key
     * @return the member value, or {@code UNKNOWN} when absent
     */
    private static String stringOrUnknown(JSONObject value, String key) {
        var member = value.getString(key);
        return member == null ? "UNKNOWN" : member;
    }

    /**
     * Reads the {@code direction} member of a call entry into a {@link CloudCallDirection}, preserving
     * absence as {@code null} so the model exposes it as an empty optional.
     *
     * @param call the call entry
     * @return the decoded direction, or {@code null} when the entry carried none
     */
    private static CloudCallDirection callDirection(JSONObject call) {
        var direction = call.getString("direction");
        return direction == null ? null : CloudCallDirection.of(direction);
    }

    /**
     * Reads an id member that the wire may carry as a number or as a string.
     *
     * @param value the change value
     * @param key   the member key
     * @return the id as a string, or {@code null} when absent
     */
    private static String idString(JSONObject value, String key) {
        var member = value.get(key);
        return member == null ? null : String.valueOf(member);
    }

    /**
     * Converts an epoch-seconds value into an {@link Instant}.
     *
     * @param epochSeconds the epoch seconds, or {@code null}
     * @return the instant, or {@code null} when the input is {@code null}
     */
    private static Instant epochInstant(Long epochSeconds) {
        return epochSeconds == null ? null : Instant.ofEpochSecond(epochSeconds);
    }

    /**
     * Decodes a single inbound message into a {@code ChatMessageInfo}.
     *
     * <p>When the consumer hides their number, the phone-scoped {@code from} is omitted and only the
     * business-scoped user id ({@code from_user_id}, the BSUID) is present; in that case the BSUID is
     * used as the sender identity so the message key never carries a {@code null} JID. When neither is
     * present the key carries no sender JID at all.
     *
     * @param message   the inbound message object
     * @param pushNames the profile names indexed by sender wa_id, falling back to the BSUID key
     * @return the decoded message
     */
    private static CloudMessageInfo decodeMessage(JSONObject message, Map<String, String> pushNames) {
        var from = message.getString("from");
        var fromUserId = message.getString("from_user_id");
        var identity = from != null ? from : fromUserId;
        var senderJid = optionalUserJid(identity);
        var keyBuilder = new MessageKeyBuilder()
                .id(message.getString("id"))
                .fromMe(false);
        if (senderJid != null) {
            keyBuilder.parentJid(senderJid);
        }
        var container = decodeContent(message);
        var timestampValue = message.getLong("timestamp");
        var timestamp = timestampValue == null ? null : Instant.ofEpochSecond(timestampValue);
        return new CloudMessageInfo(keyBuilder.build(), container, MessageStatus.DELIVERED, timestamp, senderJid, pushNames.get(identity));
    }

    /**
     * Decodes the content of an inbound message into a {@code LinkedMessageContainer}.
     *
     * @param message the inbound message object
     * @return the decoded container
     */
    private static CloudMessageContainer decodeContent(JSONObject message) {
        var type = message.getString("type");
        if (type == null) {
            return CloudMessageContainer.empty();
        }
        return switch (type) {
            case "text" -> CloudMessageContainer.of(new CloudTextContent(textBody(message)));
            case "image" -> CloudMessageContainer.of(media(MediaType.IMAGE, message, "image", true));
            case "video" -> CloudMessageContainer.of(media(MediaType.VIDEO, message, "video", true));
            case "audio" -> CloudMessageContainer.of(media(MediaType.AUDIO, message, "audio", false));
            case "document" -> CloudMessageContainer.of(media(MediaType.DOCUMENT, message, "document", true));
            case "sticker" -> CloudMessageContainer.of(media(MediaType.STICKER, message, "sticker", false));
            case "location" -> decodeLocation(message);
            case "reaction" -> decodeReaction(message);
            case "interactive" -> decodeInteractiveReply(message);
            case "button" -> decodeButtonReply(message);
            case "contacts" -> decodeContacts(message);
            case "order" -> decodeOrder(message);
            // TODO: unmapped inbound content type; the message is delivered with an empty container so
            //       the dispatcher can route the type to the error listener for observability. Add the
            //       missing content decode (and its mapped type below) when the type is modelled.
            default -> {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "unmapped inbound content type {0}", type);
                yield CloudMessageContainer.empty();
            }
        };
    }

    /**
     * Builds a Cloud media content body from an inbound media message.
     *
     * @param mediaType   the kind of media
     * @param message     the inbound message object
     * @param key         the media type key (for example {@code "image"})
     * @param withCaption whether this media kind carries a caption
     * @return the decoded media content
     */
    private static CloudMediaContent media(MediaType mediaType, JSONObject message, String key, boolean withCaption) {
        return new CloudMediaContent(mediaType,
                mediaId(message, key),
                mediaMime(message, key),
                withCaption ? mediaCaption(message, key) : null);
    }

    /**
     * Returns whether an inbound message {@code type} has a content decode in {@link #decodeContent}.
     *
     * <p>The dispatcher consults this to detect a message whose content type fell through to the empty
     * fallback, so the unmapped type can be surfaced rather than silently delivered as an empty message.
     *
     * @param type the inbound message {@code type} member, or {@code null}
     * @return {@code true} when {@code type} is decoded, {@code false} when it falls through to the empty
     *         fallback
     */
    public static boolean isMappedContentType(String type) {
        if (type == null) {
            return false;
        }
        return switch (type) {
            case "text", "image", "video", "audio", "document", "sticker", "location", "reaction",
                 "interactive", "button", "contacts", "order", "system" -> true;
            default -> false;
        };
    }

    /**
     * Decodes an inbound location message.
     *
     * @param message the inbound message object
     * @return the decoded container
     */
    private static CloudMessageContainer decodeLocation(JSONObject message) {
        var location = message.getJSONObject("location");
        if (location == null) {
            return CloudMessageContainer.of(new CloudLocationContent(null, null, null, null));
        }
        return CloudMessageContainer.of(new CloudLocationContent(
                location.getDouble("latitude"),
                location.getDouble("longitude"),
                location.getString("name"),
                location.getString("address")));
    }

    /**
     * Decodes an inbound reaction message.
     *
     * @param message the inbound message object
     * @return the decoded container
     */
    private static CloudMessageContainer decodeReaction(JSONObject message) {
        var reaction = message.getJSONObject("reaction");
        if (reaction == null) {
            return CloudMessageContainer.of(new CloudReactionContent(null, null));
        }
        return CloudMessageContainer.of(new CloudReactionContent(
                reaction.getString("message_id"),
                reaction.getString("emoji")));
    }

    /**
     * Extracts the body of an inbound text message.
     *
     * @param message the inbound message object
     * @return the message body, or the empty string when absent
     */
    private static String textBody(JSONObject message) {
        var text = message.getJSONObject("text");
        return text == null ? "" : text.getString("body");
    }

    /**
     * Decodes an inbound interactive reply (a reply button or a list selection) into a typed response
     * message.
     *
     * @param message the inbound message object
     * @return the decoded container
     */
    private static CloudMessageContainer decodeInteractiveReply(JSONObject message) {
        var interactive = message.getJSONObject("interactive");
        if (interactive != null) {
            var buttonReply = interactive.getJSONObject("button_reply");
            if (buttonReply != null) {
                return CloudMessageContainer.of(new CloudButtonsResponseContent(
                        buttonReply.getString("id"),
                        buttonReply.getString("title")));
            }
            var listReply = interactive.getJSONObject("list_reply");
            if (listReply != null) {
                return CloudMessageContainer.of(new CloudListResponseContent(
                        listReply.getString("title"),
                        listReply.getString("description"),
                        listReply.getString("id")));
            }
            var nfmReply = interactive.getJSONObject("nfm_reply");
            if (nfmReply != null) {
                return CloudMessageContainer.of(new CloudInteractiveResponseContent(
                        nfmReply.getString("name"),
                        nfmReply.getString("response_json"),
                        nfmReply.getString("body")));
            }
        }
        return CloudMessageContainer.empty();
    }

    /**
     * Decodes an inbound contacts message into a contacts-array message.
     *
     * <p>Each structured Cloud contact is rebuilt into a vCard-backed {@code ContactMessage}, the
     * inverse of {@link CloudMessageEncoder} contact encoding. The array display name is the
     * comma-joined formatted names of the contained contacts.
     *
     * @param message the inbound message object
     * @return the decoded container
     */
    private static CloudMessageContainer decodeContacts(JSONObject message) {
        var contacts = message.getJSONArray("contacts");
        if (contacts == null || contacts.isEmpty()) {
            return CloudMessageContainer.empty();
        }
        var entries = new ArrayList<ContactContent>();
        var displayNames = new ArrayList<String>();
        for (var index = 0; index < contacts.size(); index++) {
            var contact = contacts.getJSONObject(index);
            var name = contact.getJSONObject("name");
            var formatted = name == null ? null : name.getString("formatted_name");
            if (formatted != null) {
                displayNames.add(formatted);
            }
            entries.add(new CloudContactContent(formatted, vcardFromCloudContact(contact, formatted)));
        }
        return CloudMessageContainer.of(new CloudContactsArrayContent(String.join(", ", displayNames), entries));
    }

    /**
     * Builds a vCard string from a Cloud structured contact object.
     *
     * <p>The formatted name becomes the {@code FN} line, each phone becomes a {@code TEL} line carrying
     * the contact type and the {@code wa_id} as parameters when present, and the company becomes an
     * {@code X-WA-BIZ-NAME} line.
     *
     * @param contact   the structured Cloud contact
     * @param formatted the formatted display name, or {@code null}
     * @return a vCard 3.0 string
     */
    private static String vcardFromCloudContact(JSONObject contact, String formatted) {
        var vcard = new StringBuilder("BEGIN:VCARD\r\nVERSION:3.0\r\n");
        if (formatted != null) {
            vcard.append("FN:").append(formatted).append("\r\n");
        }
        var phones = contact.getJSONArray("phones");
        if (phones != null) {
            for (var index = 0; index < phones.size(); index++) {
                var phone = phones.getJSONObject(index);
                var number = phone.getString("phone");
                if (number == null) {
                    continue;
                }
                var type = phone.getString("type");
                var waId = phone.getString("wa_id");
                vcard.append("TEL");
                if (type != null) {
                    vcard.append(";type=").append(type);
                }
                if (waId != null) {
                    vcard.append(";waid=").append(waId);
                }
                vcard.append(':').append(number).append("\r\n");
            }
        }
        var org = contact.getJSONObject("org");
        if (org != null) {
            var company = org.getString("company");
            if (company != null) {
                vcard.append("X-WA-BIZ-NAME:").append(company).append("\r\n");
            }
        }
        return vcard.append("END:VCARD\r\n").toString();
    }

    /**
     * Decodes an inbound catalog order message into an order message.
     *
     * <p>The mapping is partial: the order note, item count, currency and a derived total survive,
     * while the catalog id and per-line item details have no field on {@code OrderMessage} and are
     * dropped. The status defaults to {@code OrderMessage.OrderStatus#INQUIRY} and the surface to
     * {@code OrderMessage.OrderSurface#CATALOG}.
     *
     * @param message the inbound message object
     * @return the decoded container
     */
    private static CloudMessageContainer decodeOrder(JSONObject message) {
        var order = message.getJSONObject("order");
        String orderMessage = null;
        Integer itemCount = null;
        Long total1000 = null;
        String currency = null;
        if (order != null) {
            orderMessage = order.getString("text");
            var items = order.getJSONArray("product_items");
            if (items != null) {
                itemCount = items.size();
                long sum = 0;
                for (var index = 0; index < items.size(); index++) {
                    var item = items.getJSONObject(index);
                    var price = item.getDouble("item_price");
                    var quantity = item.getInteger("quantity");
                    if (price != null) {
                        // TODO: unverified - item_price is in major currency units; the *1000 scaling to
                        //       the thousandths contract could not be confirmed against a Meta source.
                        sum += Math.round(price * (quantity == null ? 1 : quantity) * 1000);
                    }
                    if (currency == null) {
                        currency = item.getString("currency");
                    }
                }
                total1000 = sum;
            }
            // TODO: order.catalog_id and per-item product_retailer_id/quantity have no field on the
            //       shared OrderContent contract; a Cloud-specific order model would be needed to carry them.
        }
        String sourceId = null;
        var context = message.getJSONObject("context");
        if (context != null) {
            sourceId = context.getString("id");
        }
        return CloudMessageContainer.of(new CloudOrderContent(
                orderMessage, itemCount, total1000, currency, sourceId));
    }

    /**
     * Decodes an inbound legacy quick-reply button tap into a typed response message.
     *
     * @param message the inbound message object
     * @return the decoded container
     */
    private static CloudMessageContainer decodeButtonReply(JSONObject message) {
        var button = message.getJSONObject("button");
        if (button == null) {
            return CloudMessageContainer.of(new CloudButtonsResponseContent(null, null));
        }
        return CloudMessageContainer.of(new CloudButtonsResponseContent(
                button.getString("payload"),
                button.getString("text")));
    }

    /**
     * Extracts the media id of an inbound media message.
     *
     * @param message the inbound message object
     * @param type    the media type key, for example {@code "image"}
     * @return the media id, or {@code null} when absent
     */
    private static String mediaId(JSONObject message, String type) {
        var media = message.getJSONObject(type);
        return media == null ? null : media.getString("id");
    }

    /**
     * Extracts the MIME type of an inbound media message.
     *
     * @param message the inbound message object
     * @param type    the media type key
     * @return the MIME type, or {@code null} when absent
     */
    private static String mediaMime(JSONObject message, String type) {
        var media = message.getJSONObject(type);
        return media == null ? null : media.getString("mime_type");
    }

    /**
     * Extracts the caption of an inbound media message.
     *
     * @param message the inbound message object
     * @param type    the media type key
     * @return the caption, or {@code null} when absent
     */
    private static String mediaCaption(JSONObject message, String type) {
        var media = message.getJSONObject(type);
        return media == null ? null : media.getString("caption");
    }

    /**
     * Indexes the inbound profile names by sender identity.
     *
     * <p>The profile name is keyed by {@code wa_id} when present, and additionally by the
     * business-scoped {@code user_id} (the BSUID) so a hidden-number message that carries only the
     * BSUID still resolves a push name.
     *
     * @param value the webhook change {@code value}
     * @return a map of sender identity to profile name
     */
    private static Map<String, String> pushNamesByWaId(JSONObject value) {
        var result = new HashMap<String, String>();
        var contacts = value.getJSONArray("contacts");
        if (contacts == null) {
            return result;
        }
        for (var index = 0; index < contacts.size(); index++) {
            var contact = contacts.getJSONObject(index);
            var profile = contact.getJSONObject("profile");
            if (profile == null) {
                continue;
            }
            var name = profile.getString("name");
            var waId = contact.getString("wa_id");
            if (waId != null) {
                result.put(waId, name);
            }
            var userId = contact.getString("user_id");
            if (userId != null) {
                result.put(userId, name);
            }
        }
        return result;
    }

    /**
     * Builds a user JID from an E.164 phone number.
     *
     * @param phoneNumber the phone number in E.164 form, without a leading plus
     * @return the user JID
     */
    private static Jid userJid(String phoneNumber) {
        return Jid.of(phoneNumber, JidServer.user());
    }

    /**
     * Builds a user JID from a sender identity, tolerating a non-parseable business-scoped user id.
     *
     * <p>A phone-scoped {@code wa_id}/{@code from} parses cleanly as a {@code user} JID. A hidden-number
     * sender carries only the business-scoped user id (BSUID), whose opaque form is not a phone number
     * and need not parse as a JID; in that case no JID is built so the key carries no sender JID rather
     * than crashing or fabricating a {@code null}-bearing one.
     *
     * @param identity the sender identity, or {@code null}
     * @return the user JID, or {@code null} when the identity is absent or not a parseable JID
     */
    private static Jid optionalUserJid(String identity) {
        if (identity == null) {
            return null;
        }
        try {
            return userJid(identity);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
