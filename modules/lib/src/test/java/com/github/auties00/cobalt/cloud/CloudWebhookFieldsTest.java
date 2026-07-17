package com.github.auties00.cobalt.cloud;
import com.github.auties00.cobalt.wire.core.message.EmptyContent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.cloud.CloudAppStateSyncAction;
import com.github.auties00.cobalt.wire.cloud.CloudCallDirection;
import com.github.auties00.cobalt.wire.cloud.CloudCallStatus;
import com.github.auties00.cobalt.wire.cloud.CloudSystemUpdate;
import com.github.auties00.cobalt.wire.cloud.flow.CloudFlowStatusUpdate;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateComponentsUpdate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Decode cells for the webhook fields folded into existing or new listeners: business-initiated
 * call statuses on the {@code calls} field, the {@code smb_app_state_sync} contact sync, the
 * {@code system}-typed inbound message, the {@code smb_message_echoes} echo array, the
 * {@code account_settings_update} Calling configuration, the {@code message_template_components_update}
 * rendered components, the {@code security} event, and the {@code payment_configuration_update}
 * configuration lifecycle.
 */
@DisplayName("Cloud webhook field decode")
class CloudWebhookFieldsTest {
    private static JSONObject value(String json) {
        return JSON.parseObject(json);
    }

    @Test
    @DisplayName("business-initiated call status rides statuses[] typed call")
    void callStatusRinging() {
        var value = value("""
                {"messaging_product":"whatsapp","metadata":{"display_phone_number":"16315553601","phone_number_id":"123"},
                "statuses":[{"id":"wacid.AB","timestamp":"1671644824","type":"call","status":"RINGING",
                "recipient_id":"163155536021","biz_opaque_callback_data":"trace-1"}]}""");
        var events = CloudWebhookDecoder.decodeCallStatuses(value);
        assertEquals(1, events.size());
        var event = events.getFirst();
        assertEquals("wacid.AB", event.callId());
        assertEquals("163155536021", event.from());
        assertEquals(CloudCallStatus.RINGING, event.status());
        assertEquals(CloudCallDirection.BUSINESS_INITIATED, event.direction());
        assertEquals(Instant.ofEpochSecond(1671644824L), event.timestamp().orElseThrow());
    }

    @Test
    @DisplayName("call status ignores message statuses on the same statuses[] shape")
    void callStatusSkipsMessageStatus() {
        var value = value("""
                {"statuses":[{"id":"wamid.X","timestamp":"1671644824","status":"delivered","recipient_id":"163155536021"},
                {"id":"wacid.AB","timestamp":"1671644824","type":"call","status":"ACCEPTED","recipient_id":"163155536021"}]}""");
        var events = CloudWebhookDecoder.decodeCallStatuses(value);
        assertEquals(1, events.size());
        assertEquals(CloudCallStatus.ACCEPTED, events.getFirst().status());
    }

    @Test
    @DisplayName("smb_app_state_sync decodes a contact entry")
    void appStateSyncContact() {
        var value = value("""
                {"messaging_product":"whatsapp","metadata":{"display_phone_number":"16315553601","phone_number_id":"123"},
                "state_sync":[{"type":"contact","contact":{"full_name":"Jane Doe","first_name":"Jane","phone_number":"+16315551234"},
                "action":"add","metadata":{"timestamp":"1671644824"}}]}""");
        var contacts = CloudWebhookDecoder.decodeAppStateSync(value);
        assertEquals(1, contacts.size());
        var contact = contacts.getFirst();
        assertEquals("contact", contact.type());
        assertEquals(CloudAppStateSyncAction.ADD, contact.action());
        assertEquals("Jane Doe", contact.fullName().orElseThrow());
        assertEquals("Jane", contact.firstName().orElseThrow());
        assertEquals("+16315551234", contact.phoneNumber().orElseThrow());
        assertEquals(Instant.ofEpochSecond(1671644824L), contact.timestamp().orElseThrow());
    }

    @Test
    @DisplayName("system update string form decodes a number change")
    void systemUpdateStringForm() {
        var value = value("""
                {"messages":[{"from":"573000000001","id":"wamid.sys1","timestamp":"1687834799","type":"system",
                "system":{"body":"User A changed from 573000000001 to 573000000002","wa_id":"573000000002",
                "type":"user_changed_number"}}]}""");
        var updates = CloudWebhookDecoder.decodeSystemUpdates(value);
        assertEquals(1, updates.size());
        var update = assertInstanceOf(CloudSystemUpdate.NumberChanged.class, updates.getFirst());
        assertEquals("573000000001", update.from());
        assertEquals("573000000002", update.newWaId());
        assertEquals(Instant.ofEpochSecond(1687834799L), update.timestamp().orElseThrow());
    }

    @Test
    @DisplayName("system update object form decodes an identity change")
    void systemUpdateObjectForm() {
        var value = value("""
                {"messages":[{"from":"573000000001","id":"wamid.sys2","timestamp":"1687834800","type":"system",
                "system":{"body":"identity changed","identity":"hashXYZ","customer":"573000000001",
                "type":{"customer_changed_number":false,"customer_identity_changed":true}}}]}""");
        var update = assertInstanceOf(CloudSystemUpdate.IdentityChanged.class,
                CloudWebhookDecoder.decodeSystemUpdates(value).getFirst());
        assertEquals("hashXYZ", update.identity());
        assertEquals("573000000001", update.customer().orElseThrow());
    }

    @Test
    @DisplayName("a non-system message does not produce a system update")
    void nonSystemMessageNoUpdate() {
        var value = value("""
                {"messages":[{"from":"573000000001","id":"wamid.t","timestamp":"1687834800","type":"text",
                "text":{"body":"hi"}}]}""");
        assertTrue(CloudWebhookDecoder.decodeSystemUpdates(value).isEmpty());
        assertFalse(CloudWebhookDecoder.decodeMessages(value).isEmpty());
    }

    @Test
    @DisplayName("a system message is also surfaced as an empty chat message for backward compatibility")
    void systemMessageStillEmptyContainer() {
        var value = value("""
                {"messages":[{"from":"573000000001","id":"wamid.sys3","timestamp":"1687834800","type":"system",
                "system":{"body":"changed","type":"customer_changed_number","new_wa_id":"573000000002"}}]}""");
        var messages = CloudWebhookDecoder.decodeMessages(value);
        assertEquals(1, messages.size());
        assertSame(EmptyContent.of(), messages.getFirst().message().content());
    }

    @Test
    @DisplayName("smb_message_echoes decodes its message_echoes array, not the messages key")
    void messageEchoesDecodesNonEmpty() {
        // smb_message_echoes carries the echoed messages under "message_echoes", not "messages";
        // decodeMessages reading "messages" must come back empty for the same value.
        var value = value("""
                {"messaging_product":"whatsapp","metadata":{"display_phone_number":"16315553601","phone_number_id":"123"},
                "message_echoes":[{"from":"16315553601","to":"16505551234","id":"wamid.echo1","timestamp":"1700000000",
                "type":"text","text":{"body":"sent from the phone"}}]}""");
        var echoes = CloudWebhookDecoder.decodeMessageEchoes(value);
        assertEquals(1, echoes.size());
        assertEquals("wamid.echo1", echoes.getFirst().key().id().orElseThrow());
        assertTrue(CloudWebhookDecoder.decodeMessages(value).isEmpty());
    }

    @Test
    @DisplayName("account_settings_update decodes the calling configuration")
    void accountSettingsCalling() {
        var value = value("""
                {"messaging_product":"whatsapp","metadata":{"display_phone_number":"16315553601","phone_number_id":"123"},
                "phone_number_settings":{"calling":{"status":"ENABLED","call_icon_visibility":"DEFAULT",
                "callback_permission_status":"ENABLED","call_hours":{"status":"ENABLED","timezone_id":"America/Manaus",
                "weekly_operating_hours":[{"day_of_week":"MONDAY","open_time":"0400","close_time":"1020"}]},
                "sip":{"status":"ENABLED","servers":[{"hostname":"sip.example.com","port":5061}]}}}}""");
        var settings = CloudWebhookDecoder.decodeAccountSettings(value);
        assertEquals("ENABLED", settings.status().orElseThrow());
        assertEquals("DEFAULT", settings.callIconVisibility().orElseThrow());
        assertEquals("ENABLED", settings.callbackPermissionStatus().orElseThrow());
        var hours = settings.callHours().orElseThrow();
        assertEquals("America/Manaus", hours.timezoneId().orElseThrow());
        assertEquals("MONDAY", hours.weeklyOperatingHours().getFirst().dayOfWeek());
        var sip = settings.sip().orElseThrow();
        assertEquals("sip.example.com", sip.servers().getFirst().hostname().orElseThrow());
        assertEquals(5061, sip.servers().getFirst().port().orElseThrow());
    }

    @Test
    @DisplayName("account_settings_update without a calling object decodes to null")
    void accountSettingsNoCalling() {
        var value = value("""
                {"messaging_product":"whatsapp","metadata":{"display_phone_number":"16315553601","phone_number_id":"123"},
                "phone_number_settings":{}}""");
        assertNull(CloudWebhookDecoder.decodeAccountSettings(value));
    }

    @Test
    @DisplayName("message_template_components_update decodes body, header, footer and buttons")
    void templateComponents() {
        var value = value("""
                {"message_template_id":1234567890,"message_template_name":"order_update",
                "message_template_language":"en_US","message_template_element":"Your order {{1}} shipped.",
                "message_template_title":"Order shipped","message_template_footer":"Reply STOP to opt out",
                "message_template_buttons":[{"message_template_button_type":"URL","message_template_button_text":"Track",
                "message_template_button_url":"https://example.com/track"},
                {"message_template_button_type":"PHONE_NUMBER","message_template_button_text":"Call us",
                "message_template_button_phone_number":"+16315551234"}]}""");
        var update = CloudWebhookDecoder.decodeTemplateComponents(value);
        assertEquals("1234567890", update.templateId());
        assertEquals("order_update", update.name());
        assertEquals("en_US", update.language());
        assertEquals("Your order {{1}} shipped.", update.body());
        assertEquals("Order shipped", update.title().orElseThrow());
        assertEquals("Reply STOP to opt out", update.footer().orElseThrow());
        assertEquals(2, update.buttons().size());
        var urlButton = assertInstanceOf(CloudTemplateComponentsUpdate.Button.Url.class, update.buttons().getFirst());
        assertEquals("Track", urlButton.text());
        assertEquals("https://example.com/track", urlButton.url());
        var phoneButton = assertInstanceOf(CloudTemplateComponentsUpdate.Button.PhoneNumber.class,
                update.buttons().get(1));
        assertEquals("+16315551234", phoneButton.phoneNumber());
    }

    @Test
    @DisplayName("message_template_components_update without optional members omits them")
    void templateComponentsMinimal() {
        var value = value("""
                {"message_template_id":"42","message_template_name":"welcome","message_template_language":"en",
                "message_template_element":"Hello!"}""");
        var update = CloudWebhookDecoder.decodeTemplateComponents(value);
        assertEquals("42", update.templateId());
        assertEquals("Hello!", update.body());
        assertTrue(update.title().isEmpty());
        assertTrue(update.footer().isEmpty());
        assertTrue(update.buttons().isEmpty());
    }

    @Test
    @DisplayName("security decodes phone number, event and requester")
    void security() {
        var value = value("""
                {"display_phone_number":"16315553601","event":"pin_changed","requester":"admin-123"}""");
        var update = CloudWebhookDecoder.decodeSecurity(value);
        assertEquals("16315553601", update.displayPhoneNumber());
        assertEquals("pin_changed", update.event());
        assertEquals("admin-123", update.requester().orElseThrow());
    }

    @Test
    @DisplayName("security without a requester leaves it empty")
    void securityNoRequester() {
        var value = value("""
                {"display_phone_number":"16315553601","event":"number_disabled"}""");
        var update = CloudWebhookDecoder.decodeSecurity(value);
        assertEquals("number_disabled", update.event());
        assertTrue(update.requester().isEmpty());
    }

    @Test
    @DisplayName("payment_configuration_update decodes fields and epoch-seconds timestamps to Instant")
    void paymentConfiguration() {
        var value = value("""
                {"configuration_name":"my_gateway","provider_name":"Stripe","provider_mid":"acct_999",
                "status":"ENABLED","created_timestamp":1671644824,"updated_timestamp":1671648424}""");
        var update = CloudWebhookDecoder.decodePaymentConfiguration(value);
        assertEquals("my_gateway", update.configurationName());
        assertEquals("Stripe", update.providerName().orElseThrow());
        assertEquals("acct_999", update.providerMerchantId().orElseThrow());
        assertEquals("ENABLED", update.status().orElseThrow());
        assertEquals(Instant.ofEpochSecond(1671644824L), update.createdTimestamp().orElseThrow());
        assertEquals(Instant.ofEpochSecond(1671648424L), update.updatedTimestamp().orElseThrow());
    }

    @Test
    @DisplayName("payment_configuration_update with only a name omits the optional fields")
    void paymentConfigurationMinimal() {
        var value = value("""
                {"configuration_name":"bare_gateway"}""");
        var update = CloudWebhookDecoder.decodePaymentConfiguration(value);
        assertEquals("bare_gateway", update.configurationName());
        assertTrue(update.providerName().isEmpty());
        assertTrue(update.status().isEmpty());
        assertTrue(update.createdTimestamp().isEmpty());
        assertTrue(update.updatedTimestamp().isEmpty());
    }

    @Test
    @DisplayName("flows status transition decodes a StatusChange variant")
    void flowStatusChange() {
        var value = value("""
                {"event":"FLOW_STATUS_CHANGE","flow_id":1234567890,"old_status":"DRAFT","new_status":"PUBLISHED"}""");
        var update = assertInstanceOf(CloudFlowStatusUpdate.StatusChange.class,
                CloudWebhookDecoder.decodeFlowStatus(value));
        assertEquals("FLOW_STATUS_CHANGE", update.event());
        assertEquals("1234567890", update.flowId().orElseThrow());
        assertEquals("DRAFT", update.oldStatus().orElseThrow());
        assertEquals("PUBLISHED", update.newStatus().orElseThrow());
    }

    @Test
    @DisplayName("flows endpoint alert decodes an EndpointHealth variant")
    void flowEndpointHealth() {
        var value = value("""
                {"event":"ENDPOINT_ERROR_RATE","flow_id":"42","message":"error rate exceeded 10%"}""");
        var update = assertInstanceOf(CloudFlowStatusUpdate.EndpointHealth.class,
                CloudWebhookDecoder.decodeFlowStatus(value));
        assertEquals("ENDPOINT_ERROR_RATE", update.event());
        assertEquals("error rate exceeded 10%", update.message().orElseThrow());
    }
}
