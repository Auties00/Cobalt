package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudException;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudUnsupportedVersionException;
import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import com.github.auties00.cobalt.wire.cloud.CloudCallHours;
import com.github.auties00.cobalt.wire.cloud.CloudCallSession;
import com.github.auties00.cobalt.wire.cloud.CloudCallSettings;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the Cloud Calling API request building and the version guard through a
 * {@link RecordingHttpClient}-backed {@link CloudWhatsAppClient}.
 */
@DisplayName("Cloud Calling API")
class CloudCallingTest {
    private static final String PHONE_ID = "1234567890";
    private static final Jid RECIPIENT = Jid.of("16505551234");

    private static RecordingHttpClient http() {
        return new RecordingHttpClient();
    }

    private static CloudWhatsAppClient client(RecordingHttpClient http, CloudApiVersion version) throws Exception {
        return CloudWhatsAppClient.builder(CloudWhatsAppStoreFactory.temporary())
                .loadConnection("token", PHONE_ID)
                .apiVersion(version)
                .httpClient(http)
                .build();
    }

    private static CloudWhatsAppClient client(RecordingHttpClient http) throws Exception {
        return client(http, CloudApiVersion.V23_0);
    }

    private static JSONObject body(RecordingHttpClient http) {
        return JSON.parseObject(http.lastBody());
    }

    @Nested
    @DisplayName("call control")
    class CallControl {
        @Test
        @DisplayName("connect posts a connect action with an SDP offer and returns the call id")
        void connect() throws Exception {
            var http = http();
            http.respondWith("{\"messaging_product\":\"whatsapp\",\"calls\":[{\"id\":\"wacid.X\"}]}");
            var id = client(http).startCall(RECIPIENT, new CloudCallSession(CloudCallSession.Type.OFFER, "v=0\r\n"), "order-4711");
            assertEquals("wacid.X", id.orElseThrow());
            assertTrue(http.lastUri().toString().endsWith(PHONE_ID + "/calls"));
            var body = body(http);
            assertEquals("whatsapp", body.getString("messaging_product"));
            assertEquals(RECIPIENT.user(), body.getString("to"));
            assertEquals("connect", body.getString("action"));
            assertEquals("offer", body.getJSONObject("session").getString("sdp_type"));
            assertEquals("v=0\r\n", body.getJSONObject("session").getString("sdp"));
            assertEquals("order-4711", body.getString("biz_opaque_callback_data"));
        }

        @Test
        @DisplayName("connect omits biz_opaque_callback_data when no callback data is given")
        void connectNoCallback() throws Exception {
            var http = http();
            http.respondWith("{\"calls\":[{\"id\":\"wacid.Y\"}]}");
            client(http).startCall(RECIPIENT, new CloudCallSession(CloudCallSession.Type.OFFER, "sdp"));
            assertFalse(body(http).containsKey("biz_opaque_callback_data"));
        }

        @Test
        @DisplayName("connect returns empty when the response carries no calls")
        void connectEmptyResponse() throws Exception {
            var http = http();
            http.respondWith("{\"messaging_product\":\"whatsapp\"}");
            assertTrue(client(http).startCall(RECIPIENT, new CloudCallSession(CloudCallSession.Type.OFFER, "sdp")).isEmpty());
        }

        @Test
        @DisplayName("pre_accept posts a pre_accept action with an SDP answer")
        void preAccept() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            client(http).preacceptCall("wacid.A", new CloudCallSession(CloudCallSession.Type.ANSWER, "ans"));
            var body = body(http);
            assertEquals("wacid.A", body.getString("call_id"));
            assertEquals("pre_accept", body.getString("action"));
            assertEquals("answer", body.getJSONObject("session").getString("sdp_type"));
        }

        @Test
        @DisplayName("accept posts an accept action with an SDP answer and callback data")
        void accept() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            client(http).acceptCall("wacid.B", new CloudCallSession(CloudCallSession.Type.ANSWER, "ans"), "cb");
            var body = body(http);
            assertEquals("wacid.B", body.getString("call_id"));
            assertEquals("accept", body.getString("action"));
            assertEquals("answer", body.getJSONObject("session").getString("sdp_type"));
            assertEquals("cb", body.getString("biz_opaque_callback_data"));
        }

        @Test
        @DisplayName("reject posts a reject action with no session")
        void reject() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            client(http).rejectCall("wacid.C");
            var body = body(http);
            assertEquals("wacid.C", body.getString("call_id"));
            assertEquals("reject", body.getString("action"));
            assertFalse(body.containsKey("session"));
        }

        @Test
        @DisplayName("terminate posts a terminate action with no session")
        void terminate() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            client(http).terminateCall("wacid.D");
            var body = body(http);
            assertEquals("wacid.D", body.getString("call_id"));
            assertEquals("terminate", body.getString("action"));
            assertFalse(body.containsKey("session"));
        }
    }

    @Nested
    @DisplayName("call permissions")
    class CallPermissions {
        @Test
        @DisplayName("sendCallPermissionRequest posts the interactive permission request")
        void sendRequest() throws Exception {
            var http = http();
            http.respondWith("{\"messages\":[{\"id\":\"wamid.TEST\"}]}");
            var key = client(http).sendCallPermissionRequest(RECIPIENT, "May we call you?");
            assertEquals("wamid.TEST", key.id().orElseThrow());
            assertTrue(key.fromMe());
            assertEquals(RECIPIENT, key.parentJid().orElseThrow());
            var body = body(http);
            assertEquals("interactive", body.getString("type"));
            var interactive = body.getJSONObject("interactive");
            assertEquals("call_permission_request", interactive.getString("type"));
            assertEquals("call_permission_request", interactive.getJSONObject("action").getString("name"));
            assertEquals("May we call you?", interactive.getJSONObject("body").getString("text"));
        }

        @Test
        @DisplayName("queryCallPermission parses permission and actions and sends user_wa_id")
        void getPermissions() throws Exception {
            var http = http();
            http.respondWith("""
                    {"messaging_product":"whatsapp",
                     "permission":{"status":"temporary","expiration_time":1700000000},
                     "actions":[{"action_name":"start_call","can_perform_action":true,
                       "limits":[{"time_period":"PT24H","max_allowed":5,"current_usage":1,
                                  "limit_expiration_time":1700003600}]}]}""");
            var permission = client(http).queryCallPermission(RECIPIENT);
            assertEquals("temporary", permission.status());
            assertEquals(1700000000L, permission.expirationTime().orElseThrow().getEpochSecond());
            assertEquals(1, permission.actions().size());
            var action = permission.actions().getFirst();
            assertEquals("start_call", action.actionName());
            assertTrue(action.canPerformAction());
            var limit = action.limits().getFirst();
            assertEquals("PT24H", limit.timePeriod());
            assertEquals(5, limit.maxAllowed());
            assertEquals(1, limit.currentUsage());
            assertEquals(1700003600L, limit.limitExpirationTime().orElseThrow().getEpochSecond());
            var uri = http.lastUri().toString();
            assertTrue(uri.contains(PHONE_ID + "/call_permissions"));
            assertTrue(uri.contains("user_wa_id="), uri);
        }

        @Test
        @DisplayName("queryCallPermission on a v19 client throws CloudUnsupportedVersionException")
        void guardRejectsOldVersion() throws Exception {
            var http = http();
            var client = client(http, CloudApiVersion.V19_0);
            var exception = assertThrows(WhatsAppCloudUnsupportedVersionException.class,
                    () -> client.queryCallPermission(RECIPIENT));
            assertEquals("queryCallPermission", exception.operation());
            assertEquals(CloudApiVersion.V23_0, exception.requiredVersion());
            assertEquals(CloudApiVersion.V19_0, exception.configuredVersion());
            assertNull(http.lastUri());
        }
    }

    @Nested
    @DisplayName("call settings")
    class CallSettings {
        @Test
        @DisplayName("queryCallSettings maps the full calling object")
        void getSettings() throws Exception {
            var http = http();
            http.respondWith("""
                    {"calling":{"status":"ENABLED","call_icon_visibility":"DEFAULT",
                    "callback_permission_status":"ENABLED",
                    "call_hours":{"status":"ENABLED","timezone_id":"America/Manaus",
                    "weekly_operating_hours":[{"day_of_week":"MONDAY","open_time":"0400","close_time":"1020"}],
                    "holiday_schedule":[{"date":"2026-01-01","start_time":"0000","end_time":"2359"}]},
                    "sip":{"status":"ENABLED","servers":[{"hostname":"sip.pbx.com","port":5061,"sip_user_password":"secret"}]}}}""");
            var settings = client(http).queryCallSettings();
            assertEquals("ENABLED", settings.status().orElseThrow());
            assertEquals("DEFAULT", settings.callIconVisibility().orElseThrow());
            var hours = settings.callHours().orElseThrow();
            assertEquals("America/Manaus", hours.timezoneId().orElseThrow());
            assertEquals(1, hours.weeklyOperatingHours().size());
            assertEquals("MONDAY", hours.weeklyOperatingHours().getFirst().dayOfWeek());
            assertEquals(1, hours.holidaySchedule().size());
            var server = settings.sip().orElseThrow().servers().getFirst();
            assertEquals("sip.pbx.com", server.hostname().orElseThrow());
            assertEquals(5061, server.port().orElseThrow());
            assertEquals("secret", server.sipUserPassword().orElseThrow());
        }

        @Test
        @DisplayName("queryCallSettings returns an empty configuration when calling is absent")
        void getSettingsEmpty() throws Exception {
            var http = http();
            http.respondWith("{}");
            var settings = client(http).queryCallSettings();
            assertTrue(settings.status().isEmpty());
            assertTrue(settings.callHours().isEmpty());
            assertTrue(settings.sip().isEmpty());
        }

        @Test
        @DisplayName("updateCallSettings posts the full calling object")
        void updateSettings() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            var weekly = List.of(new CloudCallHours.WeeklyOperatingHours("MONDAY", "0400", "1020"));
            var holidays = List.of(new CloudCallHours.HolidaySchedule("2026-01-01", "0000", "2359"));
            var hours = new CloudCallHours("ENABLED", "America/Manaus", weekly, holidays);
            var sip = new CloudCallSettings.Sip("ENABLED",
                    List.of(new CloudCallSettings.SipServer("sip.pbx.com", 5061, null, null, null)));
            client(http).updateCallSettings(new CloudCallSettings("ENABLED", "DEFAULT", null, "ENABLED",
                    null, hours, sip));
            var body = body(http);
            assertEquals("whatsapp", body.getString("messaging_product"));
            var calling = body.getJSONObject("calling");
            assertEquals("ENABLED", calling.getString("status"));
            var callHours = calling.getJSONObject("call_hours");
            assertEquals("America/Manaus", callHours.getString("timezone_id"));
            assertEquals("MONDAY", callHours.getJSONArray("weekly_operating_hours").getJSONObject(0).getString("day_of_week"));
            assertEquals("2026-01-01", callHours.getJSONArray("holiday_schedule").getJSONObject(0).getString("date"));
            assertEquals(5061, calling.getJSONObject("sip").getJSONArray("servers").getJSONObject(0).getInteger("port"));
        }

        @Test
        @DisplayName("updateCallSettings with only status omits call_hours and sip")
        void updateSettingsMinimal() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            client(http).updateCallSettings(new CloudCallSettings("ENABLED", null, null, null, null, null, null));
            var calling = body(http).getJSONObject("calling");
            assertEquals("ENABLED", calling.getString("status"));
            assertFalse(calling.containsKey("call_hours"));
            assertFalse(calling.containsKey("sip"));
        }

        @Test
        @DisplayName("updateCallSettings omits holiday_schedule when empty")
        void updateSettingsNoHolidays() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            var hours = new CloudCallHours("ENABLED", "America/Manaus",
                    List.of(new CloudCallHours.WeeklyOperatingHours("MONDAY", "0400", "1020")), List.of());
            client(http).updateCallSettings(new CloudCallSettings(null, null, null, null, null, hours, null));
            var callHours = body(http).getJSONObject("calling").getJSONObject("call_hours");
            assertFalse(callHours.containsKey("holiday_schedule"));
        }
    }
}
