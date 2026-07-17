package com.github.auties00.cobalt.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Cloud message-template migration")
class CloudTemplateMigrationTest {
    private static final String PHONE_ID = "1234567890";
    private static final String WABA_ID = "9988776655";

    private static CloudWhatsAppClient client(RecordingHttpClient http) throws Exception {
        return CloudWhatsAppClient.builder(CloudWhatsAppStoreFactory.temporary())
                .loadConnection("token", PHONE_ID)
                .whatsappBusinessAccountId(WABA_ID)
                .httpClient(http)
                .build();
    }

    @Test
    @DisplayName("posts source_waba_id to the migrate edge and parses the migrated templates")
    void migrates() throws Exception {
        var http = new RecordingHttpClient();
        http.respondWith("{\"data\":[{\"id\":\"1\",\"name\":\"welcome\",\"language\":\"en_US\",\"category\":\"UTILITY\",\"status\":\"APPROVED\"}]}");

        var migrated = client(http).migrateMessageTemplates("5566778899");

        assertTrue(http.lastUri().toString().endsWith(WABA_ID + "/migrate_message_templates"));
        assertEquals("POST", http.lastMethod());
        assertTrue(http.lastBody().contains("\"source_waba_id\":\"5566778899\""));
        assertEquals(1, migrated.size());
        assertEquals("welcome", migrated.getFirst().name());
    }
}
