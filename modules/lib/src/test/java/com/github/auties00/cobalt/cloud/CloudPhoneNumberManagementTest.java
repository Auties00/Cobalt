package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudException;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudUnsupportedVersionException;
import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import com.github.auties00.cobalt.wire.cloud.phone.CloudLocalStorageSettings;
import com.github.auties00.cobalt.wire.cloud.phone.CloudLocalStorageStatus;
import com.github.auties00.cobalt.wire.cloud.phone.CloudMessagingLimitTier;
import com.github.auties00.cobalt.wire.cloud.phone.CloudPhoneNumberAccountMode;
import com.github.auties00.cobalt.wire.cloud.phone.CloudPhoneNumberAddBuilder;
import com.github.auties00.cobalt.wire.cloud.phone.CloudPhoneNumberNameStatus;
import com.github.auties00.cobalt.wire.cloud.phone.CloudPhoneNumberPlatformType;
import com.github.auties00.cobalt.wire.cloud.phone.CloudPhoneNumberQualityRating;
import com.github.auties00.cobalt.wire.cloud.phone.CloudRegistrationBackupBuilder;
import com.github.auties00.cobalt.wire.cloud.phone.CloudThroughputLevel;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises Cloud phone-number and registration management request building, the enriched phone-number
 * projection, and the local-storage version guard through a {@link RecordingHttpClient}-backed
 * {@link CloudWhatsAppClient}.
 */
@DisplayName("Cloud phone-number management")
class CloudPhoneNumberManagementTest {
    private static final String PHONE_ID = "1234567890";
    private static final String WABA_ID = "555000111";

    private static RecordingHttpClient http() {
        return new RecordingHttpClient();
    }

    private static CloudWhatsAppClient client(RecordingHttpClient http, CloudApiVersion version) throws Exception {
        return CloudWhatsAppClient.builder(CloudWhatsAppStoreFactory.temporary())
                .loadConnection("token", PHONE_ID)
                .whatsappBusinessAccountId(WABA_ID)
                .apiVersion(version)
                .httpClient(http)
                .build();
    }

    private static CloudWhatsAppClient client(RecordingHttpClient http) throws Exception {
        return client(http, CloudApiVersion.V21_0);
    }

    private static JSONObject body(RecordingHttpClient http) {
        return JSON.parseObject(http.lastBody());
    }

    @Nested
    @DisplayName("registration")
    class Registration {
        @Test
        @DisplayName("register posts messaging_product and pin and returns success")
        void register() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            var result = client(http).registerPhoneNumber("123456");
            assertTrue(result.success());
            assertTrue(result.message().isEmpty());
            assertTrue(http.lastUri().toString().endsWith(PHONE_ID + "/register"));
            var body = body(http);
            assertEquals("whatsapp", body.getString("messaging_product"));
            assertEquals("123456", body.getString("pin"));
            assertFalse(body.containsKey("backup"));
        }

        @Test
        @DisplayName("register with a backup attaches the backup data and password")
        void registerWithBackup() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            client(http).registerPhoneNumber("123456", new CloudRegistrationBackupBuilder()
                    .data("YmxvYg==")
                    .password("secret")
                    .build());
            var backup = body(http).getJSONObject("backup");
            assertEquals("YmxvYg==", backup.getString("data"));
            assertEquals("secret", backup.getString("password"));
        }

        @Test
        @DisplayName("register with backup data but no password omits the password key")
        void registerBackupNoPassword() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            client(http).registerPhoneNumber("123456", new CloudRegistrationBackupBuilder()
                    .data("YmxvYg==")
                    .build());
            var backup = body(http).getJSONObject("backup");
            assertEquals("YmxvYg==", backup.getString("data"));
            assertFalse(backup.containsKey("password"));
        }

        @Test
        @DisplayName("register with a null backup rejects the call")
        void registerNullBackup() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            var client = client(http);
            assertThrows(NullPointerException.class, () -> client.registerPhoneNumber("123456", null));
        }
    }

    @Nested
    @DisplayName("phone-number creation")
    class Creation {
        @Test
        @DisplayName("addPhoneNumber posts cc, phone_number, verified_name and returns the new id")
        void add() throws Exception {
            var http = http();
            http.respondWith("{\"id\":\"1906385232743451\"}");
            var id = client(http).addPhoneNumber(new CloudPhoneNumberAddBuilder()
                    .countryCode("1")
                    .phoneNumber("6505551234")
                    .verifiedName("Jasper's Market")
                    .build());
            assertEquals("1906385232743451", id);
            assertTrue(http.lastUri().toString().endsWith(WABA_ID + "/phone_numbers"));
            var body = body(http);
            assertEquals("1", body.getString("cc"));
            assertEquals("6505551234", body.getString("phone_number"));
            assertEquals("Jasper's Market", body.getString("verified_name"));
        }

        @Test
        @DisplayName("addPhoneNumber rejects a null request")
        void addNullArgs() throws Exception {
            var http = http();
            var client = client(http);
            assertThrows(NullPointerException.class, () -> client.addPhoneNumber(null));
        }

        @Test
        @DisplayName("addPhoneNumber without a configured WABA id throws IllegalStateException")
        void addNoWaba() throws Exception {
            var http = http();
            var client = CloudWhatsAppClient.builder(CloudWhatsAppStoreFactory.temporary())
                    .loadConnection("token", PHONE_ID)
                    .apiVersion(CloudApiVersion.V21_0)
                    .httpClient(http)
                    .build();
            assertThrows(IllegalStateException.class,
                    () -> client.addPhoneNumber(new CloudPhoneNumberAddBuilder()
                            .countryCode("1")
                            .phoneNumber("6505551234")
                            .verifiedName("Jasper's Market")
                            .build()));
        }
    }

    @Nested
    @DisplayName("phone-number query")
    class Query {
        @Test
        @DisplayName("queryPhoneNumber maps the full enriched field set")
        void full() throws Exception {
            var http = http();
            http.respondWith("""
                    {"id":"123456789","display_phone_number":"+1 555-012-3456","verified_name":"Jasper's Market",
                    "quality_rating":"GREEN","code_verification_status":"VERIFIED","status":"CONNECTED",
                    "name_status":"APPROVED","new_name_status":"NONE","messaging_limit_tier":"TIER_1K",
                    "throughput":{"level":"STANDARD"},"platform_type":"CLOUD_API","certificate":"Y2VydA==",
                    "is_official_business_account":true,"account_mode":"LIVE"}""");
            var number = client(http).queryPhoneNumber();
            assertEquals("123456789", number.id());
            assertEquals(CloudPhoneNumberQualityRating.GREEN, number.qualityRating().orElseThrow());
            assertEquals(CloudPhoneNumberNameStatus.APPROVED, number.nameStatus().orElseThrow());
            assertEquals(CloudPhoneNumberNameStatus.NONE, number.newNameStatus().orElseThrow());
            assertEquals(CloudMessagingLimitTier.TIER_1K, number.messagingLimitTier().orElseThrow());
            assertEquals(CloudThroughputLevel.STANDARD, number.throughputLevel().orElseThrow());
            assertEquals(CloudPhoneNumberPlatformType.CLOUD_API, number.platformType().orElseThrow());
            assertEquals("Y2VydA==", number.certificate().orElseThrow());
            assertTrue(number.officialBusinessAccount().orElseThrow());
            assertEquals(CloudPhoneNumberAccountMode.LIVE, number.accountMode().orElseThrow());
        }

        @Test
        @DisplayName("queryPhoneNumber leaves extras empty and falls back to the configured id")
        void minimal() throws Exception {
            var http = http();
            http.respondWith("{}");
            var number = client(http).queryPhoneNumber();
            assertEquals(PHONE_ID, number.id());
            assertTrue(number.nameStatus().isEmpty());
            assertTrue(number.throughputLevel().isEmpty());
            assertTrue(number.platformType().isEmpty());
            assertTrue(number.certificate().isEmpty());
            assertTrue(number.officialBusinessAccount().isEmpty());
            assertTrue(number.accountMode().isEmpty());
        }
    }

    @Nested
    @DisplayName("local storage")
    class LocalStorage {
        @Test
        @DisplayName("queryLocalStorageSettings maps the storage_configuration object")
        void get() throws Exception {
            var http = http();
            http.respondWith("{\"storage_configuration\":{\"status\":\"IN_COUNTRY_STORAGE_ENABLED\","
                    + "\"data_localization_region\":\"DE\"}}");
            var settings = client(http).queryLocalStorageSettings();
            assertEquals(CloudLocalStorageStatus.IN_COUNTRY_STORAGE_ENABLED, settings.status().orElseThrow());
            assertTrue(settings.enabled());
            assertEquals("DE", settings.dataLocalizationRegion().orElseThrow());
        }

        @Test
        @DisplayName("queryLocalStorageSettings maps the no-storage retention window")
        void getNoStorage() throws Exception {
            var http = http();
            http.respondWith("{\"storage_configuration\":{\"status\":\"NO_STORAGE_ENABLED\","
                    + "\"retention_minutes\":1440}}");
            var settings = client(http).queryLocalStorageSettings();
            assertEquals(CloudLocalStorageStatus.NO_STORAGE_ENABLED, settings.status().orElseThrow());
            assertFalse(settings.enabled());
            assertEquals(1440, settings.retentionMinutes().orElseThrow());
        }

        @Test
        @DisplayName("queryLocalStorageSettings returns an absent configuration when missing")
        void getEmpty() throws Exception {
            var http = http();
            http.respondWith("{}");
            var settings = client(http).queryLocalStorageSettings();
            assertFalse(settings.enabled());
            assertTrue(settings.status().isEmpty());
            assertTrue(settings.dataLocalizationRegion().isEmpty());
        }

        @Test
        @DisplayName("updateLocalStorageSettings posts the storage_configuration with the region")
        void set() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            client(http).updateLocalStorageSettings(
                    new CloudLocalStorageSettings(CloudLocalStorageStatus.IN_COUNTRY_STORAGE_ENABLED, "DE", null));
            var body = body(http);
            assertEquals("whatsapp", body.getString("messaging_product"));
            var storage = body.getJSONObject("storage_configuration");
            assertEquals("IN_COUNTRY_STORAGE_ENABLED", storage.getString("status"));
            assertEquals("DE", storage.getString("data_localization_region"));
        }

        @Test
        @DisplayName("updateLocalStorageSettings no-storage sends the retention window and omits the region")
        void setNoStorage() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            client(http).updateLocalStorageSettings(
                    new CloudLocalStorageSettings(CloudLocalStorageStatus.NO_STORAGE_ENABLED, null, 1440));
            var storage = body(http).getJSONObject("storage_configuration");
            assertEquals("NO_STORAGE_ENABLED", storage.getString("status"));
            assertEquals(1440, storage.getIntValue("retention_minutes"));
            assertFalse(storage.containsKey("data_localization_region"));
        }

        @Test
        @DisplayName("updateLocalStorageSettings rejects a null configuration")
        void setNull() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            var client = client(http);
            assertThrows(NullPointerException.class, () -> client.updateLocalStorageSettings(null));
        }

        @Test
        @DisplayName("queryLocalStorageSettings on a v19 client throws CloudUnsupportedVersionException")
        void guardGet() throws Exception {
            var http = http();
            var client = client(http, CloudApiVersion.V19_0);
            var exception = assertThrows(WhatsAppCloudUnsupportedVersionException.class,
                    client::queryLocalStorageSettings);
            assertEquals("queryLocalStorageSettings", exception.operation());
            assertEquals(CloudApiVersion.V21_0, exception.requiredVersion());
            assertEquals(CloudApiVersion.V19_0, exception.configuredVersion());
            assertNull(http.lastUri());
        }

        @Test
        @DisplayName("updateLocalStorageSettings on a v19 client throws before sending")
        void guardSet() throws Exception {
            var http = http();
            var client = client(http, CloudApiVersion.V19_0);
            var exception = assertThrows(WhatsAppCloudUnsupportedVersionException.class,
                    () -> client.updateLocalStorageSettings(
                            new CloudLocalStorageSettings(CloudLocalStorageStatus.IN_COUNTRY_STORAGE_ENABLED, "DE", null)));
            assertEquals("updateLocalStorageSettings", exception.operation());
            assertNull(http.lastUri());
        }
    }
}
