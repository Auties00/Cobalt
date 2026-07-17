package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import com.github.auties00.cobalt.wire.cloud.signup.CloudAppCredentialsBuilder;
import com.github.auties00.cobalt.wire.cloud.waba.CloudBusinessAccountUserTask;
import com.github.auties00.cobalt.wire.cloud.waba.CloudWabaOwnershipType;
import com.github.auties00.cobalt.wire.cloud.signup.CloudSignupCodeExchangeBuilder;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Currency;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises WhatsApp Business Account management, onboarding token exchange, and credit-sharing
 * operations through a {@link RecordingHttpClient}.
 */
@DisplayName("Cloud WABA management and onboarding")
class CloudWabaTest {
    private static final String PHONE_ID = "1234567890";
    private static final String WABA_ID = "123456789012345";
    private static final String BUSINESS_ID = "987654321";

    private static RecordingHttpClient http() {
        return new RecordingHttpClient();
    }

    private static CloudWhatsAppClient client(RecordingHttpClient http) throws Exception {
        return CloudWhatsAppClient.builder(CloudWhatsAppStoreFactory.temporary())
                .loadConnection("token", PHONE_ID)
                .apiVersion(CloudApiVersion.V23_0)
                .whatsappBusinessAccountId(WABA_ID)
                .businessId(BUSINESS_ID)
                .httpClient(http)
                .build();
    }

    private static CloudWhatsAppClient clientNoBusiness(RecordingHttpClient http) throws Exception {
        return CloudWhatsAppClient.builder(CloudWhatsAppStoreFactory.temporary())
                .loadConnection("token", PHONE_ID)
                .apiVersion(CloudApiVersion.V23_0)
                .whatsappBusinessAccountId(WABA_ID)
                .httpClient(http)
                .build();
    }

    private static JSONObject body(RecordingHttpClient http) {
        return JSON.parseObject(http.lastBody());
    }

    @Nested
    @DisplayName("account read")
    class AccountRead {
        @Test
        @DisplayName("queryBusinessAccount projects the account stanza fields")
        void getWaba() throws Exception {
            var http = http();
            http.respondWith("{\"id\":\"123456789012345\",\"name\":\"Test WhatsApp Business Account\","
                    + "\"timezone_id\":\"1\",\"currency\":\"USD\",\"message_template_namespace\":\"abcd1234_ns\","
                    + "\"country\":\"US\",\"business_verification_status\":\"verified\","
                    + "\"account_review_status\":\"APPROVED\",\"status\":\"ACTIVE\",\"ownership_type\":\"DIRECT\"}");
            var waba = client(http).queryBusinessAccount();
            assertEquals("123456789012345", waba.id());
            assertEquals("Test WhatsApp Business Account", waba.name().orElseThrow());
            assertEquals("USD", waba.currency().orElseThrow());
            assertEquals("abcd1234_ns", waba.messageTemplateNamespace().orElseThrow());
            assertEquals("verified", waba.businessVerificationStatus().orElseThrow());
            assertEquals(CloudWabaOwnershipType.DIRECT, waba.ownershipType().orElseThrow());
            assertTrue(http.lastUri().toString().contains(WABA_ID));
            assertTrue(http.lastUri().toString().contains("fields="));
        }

        @Test
        @DisplayName("queryBusinessAccount leaves absent fields empty")
        void getWabaPartial() throws Exception {
            var http = http();
            http.respondWith("{\"id\":\"123456789012345\",\"name\":\"Acme\"}");
            var waba = client(http).queryBusinessAccount();
            assertEquals("Acme", waba.name().orElseThrow());
            assertTrue(waba.currency().isEmpty());
            assertTrue(waba.ownershipType().isEmpty());
        }

        @Test
        @DisplayName("queryOwnedBusinessAccounts parses the data array in order")
        void owned() throws Exception {
            var http = http();
            http.respondWith("{\"data\":[{\"id\":\"1\",\"name\":\"First\"},{\"id\":\"2\",\"name\":\"Second\"}],"
                    + "\"paging\":{\"cursors\":{\"after\":\"A\"}}}");
            var accounts = client(http).queryOwnedBusinessAccounts();
            assertEquals(2, accounts.size());
            assertEquals("1", accounts.get(0).id());
            assertEquals("Second", accounts.get(1).name().orElseThrow());
            assertTrue(http.lastUri().toString().contains(BUSINESS_ID + "/owned_whatsapp_business_accounts"));
        }

        @Test
        @DisplayName("queryClientBusinessAccounts hits the client edge")
        void clientAccounts() throws Exception {
            var http = http();
            http.respondWith("{\"data\":[{\"id\":\"7\",\"name\":\"Shared\"}]}");
            var accounts = client(http).queryClientBusinessAccounts();
            assertEquals(1, accounts.size());
            assertEquals("7", accounts.get(0).id());
            assertTrue(http.lastUri().toString().contains(BUSINESS_ID + "/client_whatsapp_business_accounts"));
        }

        @Test
        @DisplayName("owned accounts require a configured business id")
        void ownedNoBusiness() throws Exception {
            var http = http();
            assertThrows(IllegalStateException.class,
                    () -> clientNoBusiness(http).queryOwnedBusinessAccounts());
        }
    }

    @Nested
    @DisplayName("assigned users")
    class AssignedUsers {
        @Test
        @DisplayName("addBusinessAccountUser posts the user and tasks as a JSON array string")
        void assign() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            client(http).addBusinessAccountUser("USER-1",
                    Set.of(CloudBusinessAccountUserTask.MANAGE, CloudBusinessAccountUserTask.DEVELOP));
            assertEquals("POST", http.lastMethod());
            assertTrue(http.lastUri().toString().contains(WABA_ID + "/assigned_users"));
            assertTrue(http.lastBody().contains("user=USER-1"));
            assertTrue(http.lastBody().contains("MANAGE"));
            assertTrue(http.lastBody().contains("DEVELOP"));
        }

        @Test
        @DisplayName("removeBusinessAccountUser deletes by user query parameter")
        void remove() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            client(http).removeBusinessAccountUser("USER-1");
            assertEquals("DELETE", http.lastMethod());
            assertTrue(http.lastUri().toString().contains(WABA_ID + "/assigned_users"));
            assertTrue(http.lastUri().toString().contains("user=USER-1"));
        }

        @Test
        @DisplayName("queryBusinessAccountUsers parses the entries and their tasks")
        void query() throws Exception {
            var http = http();
            http.respondWith("{\"data\":[{\"id\":\"ASSIGNED-1\",\"name\":\"Jane\",\"tasks\":[\"MANAGE\"]}],"
                    + "\"paging\":{\"cursors\":{\"after\":\"A\"}}}");
            var users = client(http).queryBusinessAccountUsers();
            assertEquals(1, users.size());
            assertEquals("ASSIGNED-1", users.get(0).id());
            assertEquals("Jane", users.get(0).name().orElseThrow());
            assertEquals(List.of(CloudBusinessAccountUserTask.MANAGE), users.get(0).tasks());
            assertTrue(http.lastUri().toString().contains("business=" + BUSINESS_ID));
        }
    }

    @Nested
    @DisplayName("onboarding tokens")
    class OnboardingTokens {
        @Test
        @DisplayName("exchangeSignupCode hits oauth/access_token with the code grant")
        void exchangeCode() throws Exception {
            var http = http();
            http.respondWith("{\"access_token\":\"TOKEN\",\"token_type\":\"bearer\",\"expires_in\":5183999}");
            var token = client(http).exchangeSignupCode(new CloudSignupCodeExchangeBuilder()
                    .credentials(new CloudAppCredentialsBuilder().appId("APP").appSecret("SECRET").build())
                    .redirectUri("https://app/redirect")
                    .code("CODE")
                    .build());
            assertEquals("TOKEN", token.accessToken());
            assertEquals("bearer", token.tokenType().orElseThrow());
            assertEquals(5183999L, token.expiresIn().orElseThrow().toSeconds());
            var uri = http.lastUri().toString();
            assertTrue(uri.contains("oauth/access_token"));
            assertTrue(uri.contains("client_id=APP"));
            assertTrue(uri.contains("code=CODE"));
            assertFalse(uri.contains("grant_type"));
        }

        @Test
        @DisplayName("exchangeLongLivedToken uses the fb_exchange_token grant")
        void exchangeLongLived() throws Exception {
            var http = http();
            http.respondWith("{\"access_token\":\"LONG\",\"token_type\":\"bearer\"}");
            var token = client(http).exchangeLongLivedToken(
                    new CloudAppCredentialsBuilder().appId("APP").appSecret("SECRET").build(), "SHORT");
            assertEquals("LONG", token.accessToken());
            assertTrue(token.expiresIn().isEmpty());
            var uri = http.lastUri().toString();
            assertTrue(uri.contains("grant_type=fb_exchange_token"));
            assertTrue(uri.contains("fb_exchange_token=SHORT"));
        }

        @Test
        @DisplayName("inspectToken parses scopes, validity, and the expiry instant")
        void debug() throws Exception {
            var http = http();
            http.respondWith("{\"data\":{\"app_id\":\"APP\",\"type\":\"USER\",\"application\":\"My App\","
                    + "\"is_valid\":true,\"issued_at\":1347235328,\"expires_at\":1352419328,"
                    + "\"scopes\":[\"whatsapp_business_management\",\"whatsapp_business_messaging\"],"
                    + "\"user_id\":\"USER-9\"}}");
            var info = client(http).inspectToken("INPUT", "APP-TOKEN");
            assertEquals("APP", info.appId());
            assertEquals("USER", info.type().orElseThrow());
            assertTrue(info.valid());
            assertEquals(2, info.scopes().size());
            assertEquals(1352419328L, info.expiresAt().orElseThrow().getEpochSecond());
            assertEquals("USER-9", info.userId().orElseThrow());
            assertTrue(http.lastUri().toString().contains("debug_token"));
            assertTrue(http.lastUri().toString().contains("input_token=INPUT"));
        }

        @Test
        @DisplayName("inspectToken treats a missing expiry as a non-expiring token")
        void debugNoExpiry() throws Exception {
            var http = http();
            http.respondWith("{\"data\":{\"app_id\":\"APP\",\"is_valid\":false,\"expires_at\":0,\"scopes\":[]}}");
            var info = client(http).inspectToken("INPUT", "APP-TOKEN");
            assertFalse(info.valid());
            assertTrue(info.expiresAt().isEmpty());
            assertTrue(info.scopes().isEmpty());
        }
    }

    @Nested
    @DisplayName("credit sharing")
    class CreditSharing {
        @Test
        @DisplayName("shareCreditLine posts the waba and currency and parses the allocation")
        void share() throws Exception {
            var http = http();
            http.respondWith("{\"allocation_config_id\":\"5550001\",\"waba_id\":\"123456789012345\"}");
            var allocation = client(http).shareCreditLine("CREDIT-1", WABA_ID, Currency.getInstance("USD"));
            assertEquals("5550001", allocation.allocationConfigId());
            assertEquals(WABA_ID, allocation.wabaId());
            assertTrue(http.lastUri().toString().contains("CREDIT-1/whatsapp_credit_sharing_and_attach"));
            var posted = body(http);
            assertEquals(WABA_ID, posted.getString("waba_id"));
            assertEquals("USD", posted.getString("waba_currency"));
        }
    }
}
