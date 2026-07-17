package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import com.github.auties00.cobalt.wire.cloud.CloudConversationalAutomation;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the conversational automation read and write edges through a {@link RecordingHttpClient}.
 */
@DisplayName("Cloud conversational automation")
class CloudConversationalAutomationTest {
    private static final String PHONE_ID = "1234567890";

    private static RecordingHttpClient http() {
        return new RecordingHttpClient();
    }

    private static CloudWhatsAppClient client(RecordingHttpClient http) throws Exception {
        return CloudWhatsAppClient.builder(CloudWhatsAppStoreFactory.temporary())
                .loadConnection("token", PHONE_ID)
                .apiVersion(CloudApiVersion.V23_0)
                .httpClient(http)
                .build();
    }

    private static JSONObject body(RecordingHttpClient http) {
        return JSON.parseObject(http.lastBody());
    }

    @Test
    @DisplayName("set posts enable_welcome_message, prompts and commands to the conversational_automation edge")
    void setFull() throws Exception {
        var http = http();
        http.respondWith("{\"success\":true}");
        var automation = new CloudConversationalAutomation(true, List.of("How can we help?"),
                List.of(new CloudConversationalAutomation.Command("help", "Show help")));
        client(http).editConversationalAutomation(automation);
        assertTrue(http.lastUri().toString().endsWith(PHONE_ID + "/conversational_automation"));
        var body = body(http);
        assertTrue(body.getBooleanValue("enable_welcome_message"));
        assertEquals("How can we help?", body.getJSONArray("prompts").getString(0));
        var command = body.getJSONArray("commands").getJSONObject(0);
        assertEquals("help", command.getString("command_name"));
        assertEquals("Show help", command.getString("command_description"));
    }

    @Test
    @DisplayName("set omits prompts and commands when empty and welcome flag when unspecified")
    void setMinimal() throws Exception {
        var http = http();
        http.respondWith("{\"success\":true}");
        client(http).editConversationalAutomation(new CloudConversationalAutomation(null, List.of(), List.of()));
        var body = body(http);
        assertFalse(body.containsKey("enable_welcome_message"));
        assertFalse(body.containsKey("prompts"));
        assertFalse(body.containsKey("commands"));
    }

    @Test
    @DisplayName("get parses the conversational_automation wrapper")
    void getPresent() throws Exception {
        var http = http();
        http.respondWith("{\"conversational_automation\":{\"id\":\"123\",\"enable_welcome_message\":true,"
                + "\"prompts\":[\"Hi!\"],\"commands\":[{\"command_name\":\"help\","
                + "\"command_description\":\"Show help\"}]},\"id\":\"123\"}");
        var automation = client(http).queryConversationalAutomation().orElseThrow();
        assertEquals(true, automation.enableWelcomeMessage().orElseThrow());
        assertEquals(List.of("Hi!"), automation.prompts());
        assertEquals(1, automation.commands().size());
        assertEquals("help", automation.commands().getFirst().name());
        assertEquals("Show help", automation.commands().getFirst().description());
    }

    @Test
    @DisplayName("get returns empty when no conversational_automation key is present")
    void getEmpty() throws Exception {
        var http = http();
        http.respondWith("{\"id\":\"123\"}");
        assertTrue(client(http).queryConversationalAutomation().isEmpty());
    }
}
