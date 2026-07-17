package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudException;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudUnsupportedVersionException;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudApiException;
import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import com.github.auties00.cobalt.wire.cloud.template.CloudMessageTemplate;
import com.github.auties00.cobalt.wire.cloud.template.CloudOtpType;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateButton;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateButtonOtpBuilder;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateButtonQuickReplyBuilder;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateButtonUrlBuilder;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateCategory;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateComponent;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateComponentBodyBuilder;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateComponentFooterBuilder;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateComponentHeaderBuilder;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateHeaderFormat;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateStatus;
import com.github.auties00.cobalt.wire.cloud.template.library.CloudTemplateLibraryAdoptionBuilder;
import com.github.auties00.cobalt.wire.cloud.template.library.CloudTemplateLibraryButtonInputBuilder;
import com.github.auties00.cobalt.wire.cloud.template.library.CloudTemplateLibraryButtonType;
import com.github.auties00.cobalt.wire.cloud.template.library.CloudTemplateLibraryButtonValue;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises Cloud message-template management (single fetch, exhaustive pagination, deletion forms,
 * library adoption, and the typed-component serialize/parse round-trip) through a
 * {@link RecordingHttpClient}.
 */
@DisplayName("Cloud message-template management")
class CloudTemplateManagementTest {
    private static final String PHONE_ID = "1234567890";
    private static final String WABA_ID = "9988776655";

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
        return client(http, CloudApiVersion.V25_0);
    }

    @Nested
    @DisplayName("get and query")
    class GetAndQuery {
        @Test
        @DisplayName("queryMessageTemplateById fetches the template stanza by id")
        void getById() throws Exception {
            var http = http();
            http.respondWith("""
                    {"id":"1234567890","name":"order_confirmation","language":"en_US","category":"UTILITY",
                     "status":"APPROVED","components":[{"type":"BODY","text":"Your order {{1}} is confirmed."}]}""");
            var template = client(http).queryMessageTemplateById("1234567890").orElseThrow();
            assertEquals("1234567890", template.id().orElseThrow());
            assertEquals("order_confirmation", template.name());
            assertEquals("en_US", template.language());
            assertEquals(CloudTemplateCategory.UTILITY, template.category());
            assertEquals(CloudTemplateStatus.APPROVED, template.status().orElseThrow());
            assertEquals(1, template.components().size());
            var body = assertInstanceOf(CloudTemplateComponent.Body.class, template.components().getFirst());
            assertEquals("Your order {{1}} is confirmed.", body.text());
            assertTrue(http.lastUri().toString().contains("/1234567890"));
            assertFalse(http.lastUri().toString().contains("message_templates"));
        }

        @Test
        @DisplayName("queryMessageTemplateById returns empty when the response carries no id")
        void getByIdMissing() throws Exception {
            var http = http();
            http.respondWith("{}");
            assertTrue(client(http).queryMessageTemplateById("nope").isEmpty());
        }
    }

    @Nested
    @DisplayName("exhaustive pagination")
    class Pagination {
        @Test
        @DisplayName("queryAllMessageTemplates follows the cursor to the end")
        void followsCursor() throws Exception {
            var http = new PagingHttpClient(
                    "{\"data\":[{\"id\":\"1\",\"name\":\"a\",\"language\":\"en\",\"category\":\"UTILITY\"}],"
                            + "\"paging\":{\"cursors\":{\"after\":\"C1\"},\"next\":\"https://x?after=C1\"}}",
                    "{\"data\":[{\"id\":\"2\",\"name\":\"b\",\"language\":\"en\",\"category\":\"UTILITY\"}]}");
            var templates = clientFor(http).queryAllMessageTemplates();
            assertEquals(List.of("a", "b"), templates.stream().map(CloudMessageTemplate::name).toList());
        }

        @Test
        @DisplayName("queryAllMessageTemplates restarts once on a stale-cursor (#100) rejection")
        void restartsOnStaleCursor() throws Exception {
            var http = new PagingHttpClient(
                    "{\"data\":[{\"id\":\"1\",\"name\":\"a\",\"language\":\"en\",\"category\":\"UTILITY\"}],"
                            + "\"paging\":{\"cursors\":{\"after\":\"C1\"},\"next\":\"https://x?after=C1\"}}",
                    "__ERROR_100__",
                    "{\"data\":[{\"id\":\"1\",\"name\":\"a\",\"language\":\"en\",\"category\":\"UTILITY\"},"
                            + "{\"id\":\"2\",\"name\":\"b\",\"language\":\"en\",\"category\":\"UTILITY\"}]}");
            var templates = clientFor(http).queryAllMessageTemplates();
            assertEquals(List.of("a", "b"), templates.stream().map(CloudMessageTemplate::name).toList());
        }

        @Test
        @DisplayName("queryAllMessageTemplates rethrows a non-100 CloudApiException")
        void rethrowsAuthError() throws Exception {
            var http = new PagingHttpClient("__ERROR_131009__");
            assertThrows(WhatsAppCloudApiException.class,
                    () -> clientFor(http).queryAllMessageTemplates());
        }

        private static CloudWhatsAppClient clientFor(PagingHttpClient http) throws Exception {
            return CloudWhatsAppClient.builder(CloudWhatsAppStoreFactory.temporary())
                    .loadConnection("token", PHONE_ID)
                    .whatsappBusinessAccountId(WABA_ID)
                    .apiVersion(CloudApiVersion.V24_0)
                    .httpClient(http)
                    .build();
        }
    }

    @Nested
    @DisplayName("deletion")
    class Deletion {
        @Test
        @DisplayName("deleteMessageTemplateLanguage(name, id) sends both name and hsm_id")
        void deleteByNameAndId() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            client(http).deleteMessageTemplateLanguage("order_confirmation", "1627019861106475");
            var uri = http.lastUri().toString();
            assertEquals("DELETE", http.lastMethod());
            assertTrue(uri.contains(WABA_ID + "/message_templates"));
            assertTrue(uri.contains("name=order_confirmation"));
            assertTrue(uri.contains("hsm_id=1627019861106475"));
        }

        @Test
        @DisplayName("deleteMessageTemplates sends hsm_ids as a JSON array")
        void batchDelete() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            client(http).deleteMessageTemplates(List.of("111", "222"));
            var uri = java.net.URLDecoder.decode(http.lastUri().toString(), java.nio.charset.StandardCharsets.UTF_8);
            assertEquals("DELETE", http.lastMethod());
            assertTrue(uri.contains(WABA_ID + "/message_templates"));
            assertTrue(uri.contains("hsm_ids=[\"111\",\"222\"]"), uri);
        }

        @Test
        @DisplayName("deleteMessageTemplates on a v24 client throws CloudUnsupportedVersionException")
        void batchDeleteGuard() throws Exception {
            var http = http();
            var client = client(http, CloudApiVersion.V24_0);
            var exception = assertThrows(WhatsAppCloudUnsupportedVersionException.class,
                    () -> client.deleteMessageTemplates(List.of("111")));
            assertEquals("deleteMessageTemplates", exception.operation());
            assertEquals(CloudApiVersion.V25_0, exception.requiredVersion());
            assertEquals(CloudApiVersion.V24_0, exception.configuredVersion());
            assertNull(http.lastUri());
        }

        @Test
        @DisplayName("deleteMessageTemplates rejects an empty id list")
        void batchDeleteEmpty() throws Exception {
            var http = http();
            assertThrows(IllegalArgumentException.class,
                    () -> client(http).deleteMessageTemplates(List.of()));
        }
    }

    @Nested
    @DisplayName("template library")
    class Library {
        @Test
        @DisplayName("createTemplateFromLibrary forwards the library name and button inputs")
        void createFromLibrary() throws Exception {
            var http = http();
            http.respondWith("{\"id\":\"1071234567890123\",\"status\":\"PENDING\",\"category\":\"UTILITY\"}");
            var button = new CloudTemplateLibraryButtonInputBuilder()
                    .type(CloudTemplateLibraryButtonType.URL)
                    .url("https://www.example.com/{{1}}")
                    .build();
            var value = assertInstanceOf(CloudTemplateLibraryButtonValue.Url.class, button.value().orElseThrow());
            assertEquals("https://www.example.com/{{1}}", value.url());
            var adoption = new CloudTemplateLibraryAdoptionBuilder()
                    .name("order_confirmation_v1")
                    .language("en_US")
                    .category(CloudTemplateCategory.UTILITY)
                    .libraryTemplateName("order_confirmation")
                    .libraryButtons(List.of(button))
                    .build();
            var created = client(http).createTemplateFromLibrary(adoption);
            assertEquals("1071234567890123", created.id().orElseThrow());
            assertEquals(CloudTemplateStatus.PENDING, created.status().orElseThrow());
            assertEquals("order_confirmation_v1", created.name());
            var body = JSON.parseObject(http.lastBody());
            assertTrue(http.lastUri().toString().contains(WABA_ID + "/message_templates"));
            assertEquals("order_confirmation", body.getString("library_template_name"));
            var inputs = body.getJSONArray("library_template_button_inputs");
            assertEquals(1, inputs.size());
            assertEquals("URL", inputs.getJSONObject(0).getString("type"));
            assertEquals("https://www.example.com/{{1}}",
                    inputs.getJSONObject(0).getJSONObject("url").getString("base_url"));
        }
    }

    @Nested
    @DisplayName("typed components")
    class TypedComponents {
        @Test
        @DisplayName("createMessageTemplate serializes header, body, footer, and buttons to the wire shape")
        void serializesFullTemplate() throws Exception {
            var http = http();
            http.respondWith("{\"id\":\"1234567890\",\"status\":\"PENDING\",\"category\":\"MARKETING\"}");
            var header = new CloudTemplateComponentHeaderBuilder()
                    .format(CloudTemplateHeaderFormat.TEXT)
                    .text("Order update")
                    .build();
            var body = new CloudTemplateComponentBodyBuilder()
                    .text("Hi {{1}}, your order ships today.")
                    .example("Sam")
                    .build();
            var footer = new CloudTemplateComponentFooterBuilder()
                    .text("Reply STOP to opt out")
                    .build();
            var reply = new CloudTemplateButtonQuickReplyBuilder()
                    .text("Track order")
                    .build();
            var url = new CloudTemplateButtonUrlBuilder()
                    .text("Visit site")
                    .url("https://www.example.com/{{1}}")
                    .build();
            var buttons = new CloudTemplateComponent.Buttons(List.of(reply, url));
            var template = new CloudMessageTemplate(null, "order_update", "en_US", CloudTemplateCategory.MARKETING,
                    null, List.of(header, body, footer, buttons));
            var created = client(http).createMessageTemplate(template);
            assertEquals("1234567890", created.id().orElseThrow());

            var sent = JSON.parseObject(http.lastBody());
            assertEquals("order_update", sent.getString("name"));
            assertEquals("MARKETING", sent.getString("category"));
            var components = sent.getJSONArray("components");
            assertEquals(4, components.size());

            var headerNode = components.getJSONObject(0);
            assertEquals("HEADER", headerNode.getString("type"));
            assertEquals("TEXT", headerNode.getString("format"));
            assertEquals("Order update", headerNode.getString("text"));

            var bodyNode = components.getJSONObject(1);
            assertEquals("BODY", bodyNode.getString("type"));
            assertEquals("Hi {{1}}, your order ships today.", bodyNode.getString("text"));
            assertEquals("Sam", bodyNode.getString("example"));

            var footerNode = components.getJSONObject(2);
            assertEquals("FOOTER", footerNode.getString("type"));
            assertEquals("Reply STOP to opt out", footerNode.getString("text"));

            var buttonsNode = components.getJSONObject(3);
            assertEquals("BUTTONS", buttonsNode.getString("type"));
            var sentButtons = buttonsNode.getJSONArray("buttons");
            assertEquals("QUICK_REPLY", sentButtons.getJSONObject(0).getString("type"));
            assertEquals("Track order", sentButtons.getJSONObject(0).getString("text"));
            assertEquals("URL", sentButtons.getJSONObject(1).getString("type"));
            assertEquals("https://www.example.com/{{1}}", sentButtons.getJSONObject(1).getString("url"));
        }

        @Test
        @DisplayName("createMessageTemplate serializes a carousel of cards recursively")
        void serializesCarousel() throws Exception {
            var http = http();
            http.respondWith("{\"id\":\"1\",\"status\":\"PENDING\",\"category\":\"MARKETING\"}");
            var cardBody = new CloudTemplateComponentBodyBuilder().text("Card one").build();
            var cardButtons = new CloudTemplateComponent.Buttons(
                    List.of(new CloudTemplateButtonQuickReplyBuilder().text("Buy").build()));
            var card = new CloudTemplateComponent.Carousel.Card(List.of(cardBody, cardButtons));
            var carousel = new CloudTemplateComponent.Carousel(List.of(card));
            var template = new CloudMessageTemplate(null, "promo", "en_US", CloudTemplateCategory.MARKETING, null,
                    List.of(carousel));
            client(http).createMessageTemplate(template);

            var sent = JSON.parseObject(http.lastBody());
            var carouselNode = sent.getJSONArray("components").getJSONObject(0);
            assertEquals("CAROUSEL", carouselNode.getString("type"));
            var cards = carouselNode.getJSONArray("cards");
            assertEquals(1, cards.size());
            var cardComponents = cards.getJSONObject(0).getJSONArray("components");
            assertEquals("BODY", cardComponents.getJSONObject(0).getString("type"));
            assertEquals("Card one", cardComponents.getJSONObject(0).getString("text"));
            assertEquals("BUTTONS", cardComponents.getJSONObject(1).getString("type"));
        }

        @Test
        @DisplayName("queryMessageTemplateById parses a Graph components array into the typed tree")
        void parsesComponents() throws Exception {
            var http = http();
            http.respondWith("""
                    {"id":"1","name":"order_update","language":"en_US","category":"MARKETING","status":"APPROVED",
                     "components":[
                       {"type":"HEADER","format":"IMAGE"},
                       {"type":"BODY","text":"Hi {{1}}"},
                       {"type":"FOOTER","text":"Bye"},
                       {"type":"BUTTONS","buttons":[
                         {"type":"QUICK_REPLY","text":"Stop"},
                         {"type":"PHONE_NUMBER","text":"Call us","phone_number":"+15551234567"},
                         {"type":"COPY_CODE","example":"250126"}
                       ]}
                     ]}""");
            var template = client(http).queryMessageTemplateById("1").orElseThrow();
            var components = template.components();
            assertEquals(4, components.size());

            var header = assertInstanceOf(CloudTemplateComponent.Header.class, components.get(0));
            assertEquals(CloudTemplateHeaderFormat.IMAGE, header.format());
            assertTrue(header.text().isEmpty());

            var body = assertInstanceOf(CloudTemplateComponent.Body.class, components.get(1));
            assertEquals("Hi {{1}}", body.text());

            assertInstanceOf(CloudTemplateComponent.Footer.class, components.get(2));

            var buttons = assertInstanceOf(CloudTemplateComponent.Buttons.class, components.get(3));
            assertEquals(3, buttons.buttons().size());
            var reply = assertInstanceOf(CloudTemplateButton.QuickReply.class, buttons.buttons().get(0));
            assertEquals("Stop", reply.text());
            var phone = assertInstanceOf(CloudTemplateButton.PhoneNumber.class, buttons.buttons().get(1));
            assertEquals("+15551234567", phone.phoneNumber());
            var copyCode = assertInstanceOf(CloudTemplateButton.CopyCode.class, buttons.buttons().get(2));
            assertEquals("250126", copyCode.example().orElseThrow());
        }

        @Test
        @DisplayName("an OTP authentication template builds via the typed OTP button and flows through create")
        void otpAuthenticationTemplate() throws Exception {
            var http = http();
            http.respondWith("{\"id\":\"1234567890\",\"status\":\"PENDING\",\"category\":\"AUTHENTICATION\"}");
            var otp = new CloudTemplateButtonOtpBuilder()
                    .otpType(CloudOtpType.COPY_CODE)
                    .text("Copy code")
                    .build();
            var buttons = new CloudTemplateComponent.Buttons(List.of(otp));
            var body = new CloudTemplateComponentBodyBuilder()
                    .text("{{1}} is your verification code.")
                    .build();
            var template = new CloudMessageTemplate(null, "verify", "en_US", CloudTemplateCategory.AUTHENTICATION,
                    null, List.of(body, buttons));
            var created = client(http).createMessageTemplate(template);
            assertEquals(CloudTemplateStatus.PENDING, created.status().orElseThrow());

            var sent = JSON.parseObject(http.lastBody());
            assertEquals("AUTHENTICATION", sent.getString("category"));
            var otpNode = sent.getJSONArray("components").getJSONObject(1).getJSONArray("buttons").getJSONObject(0);
            assertEquals("OTP", otpNode.getString("type"));
            assertEquals("COPY_CODE", otpNode.getString("otp_type"));
            assertEquals("Copy code", otpNode.getString("text"));
        }

        @Test
        @DisplayName("a copy-code button without an example omits the example field")
        void copyCodeWithoutExample() throws Exception {
            var http = http();
            http.respondWith("{\"id\":\"1\",\"status\":\"PENDING\",\"category\":\"UTILITY\"}");
            var copyCode = new com.github.auties00.cobalt.wire.cloud.template.CloudTemplateButtonCopyCodeBuilder().build();
            var buttons = new CloudTemplateComponent.Buttons(List.of(copyCode));
            var template = new CloudMessageTemplate(null, "code", "en_US", CloudTemplateCategory.UTILITY, null,
                    List.of(buttons));
            client(http).createMessageTemplate(template);
            var otpNode = JSON.parseObject(http.lastBody())
                    .getJSONArray("components").getJSONObject(0).getJSONArray("buttons").getJSONObject(0);
            assertEquals("COPY_CODE", otpNode.getString("type"));
            assertFalse(otpNode.containsKey("example"));
        }
    }
}
