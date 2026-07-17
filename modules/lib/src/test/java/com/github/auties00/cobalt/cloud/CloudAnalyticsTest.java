package com.github.auties00.cobalt.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import com.github.auties00.cobalt.wire.cloud.analytics.CloudConversationAnalytics;
import com.github.auties00.cobalt.wire.cloud.analytics.CloudConversationAnalyticsQueryBuilder;
import com.github.auties00.cobalt.wire.cloud.analytics.CloudMessagingAnalytics;
import com.github.auties00.cobalt.wire.cloud.analytics.CloudMessagingAnalyticsQueryBuilder;
import com.github.auties00.cobalt.wire.cloud.analytics.CloudPricingAnalytics;
import com.github.auties00.cobalt.wire.cloud.analytics.CloudPricingAnalyticsQueryBuilder;
import com.github.auties00.cobalt.wire.cloud.analytics.CloudTemplateAnalytics;
import com.github.auties00.cobalt.wire.cloud.analytics.CloudTemplateAnalyticsQueryBuilder;
import com.github.auties00.cobalt.wire.cloud.analytics.CloudTemplateGroupAnalyticsQueryBuilder;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises Cloud analytics (messaging, conversation, pricing, and template) through a
 * {@link RecordingHttpClient}, asserting the emitted {@code fields=} field expansion and the parsed
 * data points. The harness decodes the recorded request URI so the field expansion can be matched
 * verbatim.
 */
@DisplayName("Cloud analytics")
class CloudAnalyticsTest {
    private static final String PHONE_ID = "1234567890";
    private static final String WABA_ID = "102290129340398";

    private static CloudWhatsAppClient client(RecordingHttpClient http) throws Exception {
        return CloudWhatsAppClient.builder(CloudWhatsAppStoreFactory.temporary())
                .loadConnection("token", PHONE_ID)
                .whatsappBusinessAccountId(WABA_ID)
                .apiVersion(CloudApiVersion.V25_0)
                .httpClient(http)
                .build();
    }

    private static String fields(RecordingHttpClient http) {
        var query = http.lastUri().getRawQuery();
        for (var pair : query.split("&")) {
            var split = pair.split("=", 2);
            if (split[0].equals("fields")) {
                return URLDecoder.decode(split[1], StandardCharsets.UTF_8);
            }
        }
        throw new IllegalStateException("no fields parameter in " + query);
    }

    @Nested
    @DisplayName("queryMessagingAnalytics")
    class Messaging {
        @Test
        @DisplayName("emits the analytics field expansion and parses sent/delivered data points")
        void parsesDataPoints() throws Exception {
            var http = new RecordingHttpClient();
            http.respondWith("""
                    {"analytics":{"phone_numbers":["19035551234"],"country_codes":[],"granularity":"DAILY",
                     "data_points":[{"start":1685602800,"end":1685689200,"sent":9,"delivered":9}]},
                     "id":"102290129340398"}""");
            var analytics = client(http).queryMessagingAnalytics(new CloudMessagingAnalyticsQueryBuilder()
                    .start(Instant.ofEpochSecond(1685602800))
                    .end(Instant.ofEpochSecond(1685689200))
                    .granularity(CloudMessagingAnalytics.Granularity.DAY)
                    .phoneNumbers(List.of("19035551234"))
                    .build());
            assertEquals("DAILY", analytics.granularity());
            assertEquals(List.of("19035551234"), analytics.phoneNumbers());
            assertEquals(1, analytics.dataPoints().size());
            var point = analytics.dataPoints().getFirst();
            assertEquals(9, point.sent());
            assertEquals(9, point.delivered());
            assertEquals(Instant.ofEpochSecond(1685602800), point.start());
            assertEquals(Instant.ofEpochSecond(1685689200), point.end());
            var fields = fields(http);
            assertTrue(fields.startsWith("analytics.start(1685602800).end(1685689200).granularity(DAY)"), fields);
            assertTrue(fields.contains(".phone_numbers([\"19035551234\"])"), fields);
            assertTrue(http.lastUri().getPath().endsWith("/" + WABA_ID));
        }

        @Test
        @DisplayName("reads start/end as epoch seconds and the sent/delivered counts confirmed against wapi.go")
        void parsesConfirmedFieldNames() throws Exception {
            var http = new RecordingHttpClient();
            http.respondWith("""
                    {"analytics":{"phone_numbers":[],"granularity":"DAY","data_points":[
                     {"start":1685602800,"end":1685689200,"sent":42,"delivered":40}
                     ]},"id":"102290129340398"}""");
            var analytics = client(http).queryMessagingAnalytics(new CloudMessagingAnalyticsQueryBuilder()
                    .start(Instant.ofEpochSecond(1685602800))
                    .end(Instant.ofEpochSecond(1685689200))
                    .granularity(CloudMessagingAnalytics.Granularity.DAY)
                    .build());
            assertEquals("DAY", analytics.granularity());
            assertEquals(1, analytics.dataPoints().size());
            var point = analytics.dataPoints().getFirst();
            assertEquals(Instant.ofEpochSecond(1685602800), point.start());
            assertEquals(Instant.ofEpochSecond(1685689200), point.end());
            assertEquals(42, point.sent());
            assertEquals(40, point.delivered());
        }

        @Test
        @DisplayName("reads an absent data_points array as an empty data-point list")
        void absentDataPoints() throws Exception {
            var http = new RecordingHttpClient();
            http.respondWith("""
                    {"analytics":{"phone_numbers":["19035551234"],"granularity":"DAY"},
                     "id":"102290129340398"}""");
            var analytics = client(http).queryMessagingAnalytics(new CloudMessagingAnalyticsQueryBuilder()
                    .start(Instant.ofEpochSecond(1685602800))
                    .end(Instant.ofEpochSecond(1685689200))
                    .granularity(CloudMessagingAnalytics.Granularity.DAY)
                    .build());
            assertTrue(analytics.dataPoints().isEmpty());
        }
    }

    @Nested
    @DisplayName("queryConversationAnalytics")
    class Conversation {
        @Test
        @DisplayName("flattens data[].data_points[] and emits dimension/metric filters")
        void parsesDataPoints() throws Exception {
            var http = new RecordingHttpClient();
            http.respondWith("""
                    {"conversation_analytics":{"data":[{"data_points":[
                     {"start":1672675200,"end":1672761600,"conversation":1,"phone_number":"14132521446",
                      "country":"HK","conversation_type":"FREE_TIER","conversation_direction":"USER_INITIATED","cost":0}
                     ]}]},"id":"102290129340398"}""");
            var analytics = client(http).queryConversationAnalytics(new CloudConversationAnalyticsQueryBuilder()
                    .start(Instant.ofEpochSecond(1672675200))
                    .end(Instant.ofEpochSecond(1672761600))
                    .granularity(CloudConversationAnalytics.Granularity.DAILY)
                    .metricTypes(List.of(CloudConversationAnalytics.MetricType.CONVERSATION, CloudConversationAnalytics.MetricType.COST))
                    .build());
            assertEquals(1, analytics.dataPoints().size());
            var point = analytics.dataPoints().getFirst();
            assertEquals(1, point.conversation());
            assertEquals(0.0, point.cost());
            assertEquals("HK", point.country().orElseThrow());
            assertEquals("FREE_TIER", point.conversationType().orElseThrow());
            assertEquals("USER_INITIATED", point.conversationDirection().orElseThrow());
            assertTrue(point.conversationCategory().isEmpty());
            var fields = fields(http);
            assertTrue(fields.startsWith("conversation_analytics.start(1672675200).end(1672761600).granularity(DAILY)"), fields);
            assertTrue(fields.contains(".metric_types([CONVERSATION,COST])"), fields);
        }

        @Test
        @DisplayName("reads start/end, conversation, cost, and all five dimension fields from data_points")
        void parsesAllDimensions() throws Exception {
            var http = new RecordingHttpClient();
            http.respondWith("""
                    {"conversation_analytics":{"data":[{"data_points":[
                     {"start":1672675200,"end":1672761600,"conversation":7,"cost":1.25,
                      "phone_number":"14132521446","country":"US","conversation_type":"REGULAR",
                      "conversation_direction":"BUSINESS_INITIATED","conversation_category":"MARKETING"}
                     ]}],"paging":{}},"id":"102290129340398"}""");
            var analytics = client(http).queryConversationAnalytics(new CloudConversationAnalyticsQueryBuilder()
                    .start(Instant.ofEpochSecond(1672675200))
                    .end(Instant.ofEpochSecond(1672761600))
                    .granularity(CloudConversationAnalytics.Granularity.DAILY)
                    .dimensions(List.of(CloudConversationAnalytics.Dimension.PHONE,
                            CloudConversationAnalytics.Dimension.COUNTRY,
                            CloudConversationAnalytics.Dimension.CONVERSATION_TYPE,
                            CloudConversationAnalytics.Dimension.CONVERSATION_DIRECTION,
                            CloudConversationAnalytics.Dimension.CONVERSATION_CATEGORY))
                    .build());
            assertEquals(1, analytics.dataPoints().size());
            var point = analytics.dataPoints().getFirst();
            assertEquals(Instant.ofEpochSecond(1672675200), point.start());
            assertEquals(Instant.ofEpochSecond(1672761600), point.end());
            assertEquals(7, point.conversation());
            assertEquals(1.25, point.cost());
            assertEquals("14132521446", point.phoneNumber().orElseThrow());
            assertEquals("US", point.country().orElseThrow());
            assertEquals("REGULAR", point.conversationType().orElseThrow());
            assertEquals("BUSINESS_INITIATED", point.conversationDirection().orElseThrow());
            assertEquals("MARKETING", point.conversationCategory().orElseThrow());
            var fields = fields(http);
            assertTrue(fields.contains(".dimensions([PHONE,COUNTRY,CONVERSATION_TYPE,CONVERSATION_DIRECTION,CONVERSATION_CATEGORY])"), fields);
        }

        @Test
        @DisplayName("flattens data_points across multiple data[] entries")
        void flattensMultipleDataEntries() throws Exception {
            var http = new RecordingHttpClient();
            http.respondWith("""
                    {"conversation_analytics":{"data":[
                     {"data_points":[{"start":1672675200,"end":1672761600,"conversation":1,"cost":0}]},
                     {"data_points":[{"start":1672761600,"end":1672848000,"conversation":2,"cost":0.5}]}
                     ]},"id":"102290129340398"}""");
            var analytics = client(http).queryConversationAnalytics(new CloudConversationAnalyticsQueryBuilder()
                    .start(Instant.ofEpochSecond(1672675200))
                    .end(Instant.ofEpochSecond(1672848000))
                    .granularity(CloudConversationAnalytics.Granularity.DAILY)
                    .build());
            assertEquals(2, analytics.dataPoints().size());
            assertEquals(1, analytics.dataPoints().get(0).conversation());
            assertEquals(2, analytics.dataPoints().get(1).conversation());
            assertEquals(0.5, analytics.dataPoints().get(1).cost());
        }
    }

    @Nested
    @DisplayName("queryPricingAnalytics")
    class Pricing {
        @Test
        @DisplayName("emits metric/dimension filters and flattens data points")
        void parsesDataPoints() throws Exception {
            var http = new RecordingHttpClient();
            http.respondWith("""
                    {"pricing_analytics":{"data":[{"data_points":[
                     {"start":1685602800,"end":1685689200,"volume":12,"cost":0.05,
                      "pricing_type":"REGULAR","pricing_category":"MARKETING"}
                     ]}],"paging":{}},"id":"102290129340398"}""");
            var analytics = client(http).queryPricingAnalytics(new CloudPricingAnalyticsQueryBuilder()
                    .start(Instant.ofEpochSecond(1685602800))
                    .end(Instant.ofEpochSecond(1685689200))
                    .granularity(CloudPricingAnalytics.Granularity.DAILY)
                    .metricTypes(List.of(CloudPricingAnalytics.MetricType.VOLUME, CloudPricingAnalytics.MetricType.COST))
                    .dimensions(List.of(CloudPricingAnalytics.Dimension.PRICING_CATEGORY))
                    .build());
            assertEquals(1, analytics.dataPoints().size());
            var point = analytics.dataPoints().getFirst();
            assertEquals(12, point.volume());
            assertEquals(0.05, point.cost());
            assertEquals("REGULAR", point.pricingType().orElseThrow());
            assertEquals("MARKETING", point.pricingCategory().orElseThrow());
            var fields = fields(http);
            assertEquals("pricing_analytics.start(1685602800).end(1685689200).granularity(DAILY)"
                    + ".metric_types([VOLUME,COST]).dimensions([PRICING_CATEGORY])", fields);
        }

        @Test
        @DisplayName("emits the pricing_categories, pricing_types, and tiers filter segments")
        void emitsFilters() throws Exception {
            var http = new RecordingHttpClient();
            http.respondWith("{\"pricing_analytics\":{\"data\":[]},\"id\":\"102290129340398\"}");
            client(http).queryPricingAnalytics(new CloudPricingAnalyticsQueryBuilder()
                    .start(Instant.ofEpochSecond(1685602800))
                    .end(Instant.ofEpochSecond(1685689200))
                    .granularity(CloudPricingAnalytics.Granularity.DAILY)
                    .phoneNumbers(List.of("19035551234"))
                    .countryCodes(List.of("US"))
                    .metricTypes(List.of(CloudPricingAnalytics.MetricType.VOLUME))
                    .dimensions(List.of(CloudPricingAnalytics.Dimension.PRICING_CATEGORY, CloudPricingAnalytics.Dimension.TIER))
                    .pricingTypes(List.of(CloudPricingAnalytics.PricingType.REGULAR, CloudPricingAnalytics.PricingType.FREE_ENTRY_POINT))
                    .pricingCategories(List.of(CloudPricingAnalytics.PricingCategory.MARKETING, CloudPricingAnalytics.PricingCategory.AUTHENTICATION))
                    .tiers(List.of("tier-1k", "tier-10k"))
                    .build());
            var fields = fields(http);
            assertTrue(fields.contains(".phone_numbers([\"19035551234\"])"), fields);
            assertTrue(fields.contains(".country_codes([\"US\"])"), fields);
            assertTrue(fields.contains(".pricing_types([REGULAR,FREE_ENTRY_POINT])"), fields);
            assertTrue(fields.contains(".pricing_categories([MARKETING,AUTHENTICATION])"), fields);
            assertTrue(fields.contains(".tiers([\"tier-1k\",\"tier-10k\"])"), fields);
        }
    }

    @Nested
    @DisplayName("queryTemplateAnalytics")
    class Template {
        @Test
        @DisplayName("emits template ids and default metrics, parses clicked breakdown")
        void parsesDataPoints() throws Exception {
            var http = new RecordingHttpClient();
            http.respondWith("""
                    {"template_analytics":{"data":[{"granularity":"DAILY","data_points":[
                     {"template_id":"111","start":1707264000,"end":1707350400,"sent":1000,"delivered":950,"read":700,
                      "clicked":[{"type":"quick_reply_button","button_content":"Yes","count":120},
                                 {"type":"url_button","button_content":"Shop now","count":80}]},
                     {"template_id":"222","start":1707264000,"end":1707350400,"sent":5,"delivered":4,"read":3}
                     ]}],"paging":{}},"id":"102290129340398"}""");
            var analytics = client(http).queryTemplateAnalytics(new CloudTemplateAnalyticsQueryBuilder()
                    .start(Instant.ofEpochSecond(1707264000))
                    .end(Instant.ofEpochSecond(1707350400))
                    .templateIds(List.of("111", "222"))
                    .build());
            assertEquals(2, analytics.size());
            var first = analytics.getFirst();
            assertEquals("111", first.templateId());
            assertEquals(1000L, first.sent().orElseThrow());
            assertEquals(950L, first.delivered().orElseThrow());
            assertEquals(700L, first.read().orElseThrow());
            assertEquals(Instant.ofEpochSecond(1707264000), first.start());
            assertEquals(2, first.clicked().size());
            assertEquals("quick_reply_button", first.clicked().getFirst().type());
            assertEquals("Yes", first.clicked().getFirst().buttonContent());
            assertEquals(120L, first.clicked().getFirst().count());
            assertTrue(analytics.get(1).clicked().isEmpty());
            var fields = fields(http);
            assertTrue(fields.contains("template_analytics.start(1707264000).end(1707350400).granularity(DAILY)"), fields);
            assertTrue(fields.contains(".template_ids([111,222])"), fields);
            assertTrue(fields.contains(".metric_types([SENT,DELIVERED,READ,CLICKED])"), fields);
        }

        @Test
        @DisplayName("returns empty list when template_analytics is absent")
        void absentNode() throws Exception {
            var http = new RecordingHttpClient();
            http.respondWith("{\"id\":\"102290129340398\"}");
            var analytics = client(http).queryTemplateAnalytics(new CloudTemplateAnalyticsQueryBuilder()
                    .start(Instant.ofEpochSecond(1707264000))
                    .end(Instant.ofEpochSecond(1707350400))
                    .templateIds(List.of("111"))
                    .build());
            assertTrue(analytics.isEmpty());
        }

        @Test
        @DisplayName("defaults product_type to CLOUD_API and omits use_waba_timezone")
        void defaultProductType() throws Exception {
            var http = new RecordingHttpClient();
            http.respondWith("{\"id\":\"102290129340398\"}");
            client(http).queryTemplateAnalytics(new CloudTemplateAnalyticsQueryBuilder()
                    .start(Instant.ofEpochSecond(1707264000))
                    .end(Instant.ofEpochSecond(1707350400))
                    .templateIds(List.of("111"))
                    .build());
            var fields = fields(http);
            assertTrue(fields.contains(".product_type(CLOUD_API)"), fields);
            assertTrue(!fields.contains("use_waba_timezone"), fields);
        }

        @Test
        @DisplayName("emits the requested product_type and use_waba_timezone segments")
        void productTypeAndTimezone() throws Exception {
            var http = new RecordingHttpClient();
            http.respondWith("{\"id\":\"102290129340398\"}");
            client(http).queryTemplateAnalytics(new CloudTemplateAnalyticsQueryBuilder()
                    .start(Instant.ofEpochSecond(1707264000))
                    .end(Instant.ofEpochSecond(1707350400))
                    .templateIds(List.of("111"))
                    .productType(CloudTemplateAnalytics.ProductType.MARKETING_MESSAGES_LITE_API)
                    .useBusinessAccountTimezone(true)
                    .build());
            var fields = fields(http);
            assertTrue(fields.contains(".product_type(MARKETING_MESSAGES_LITE_API)"), fields);
            assertTrue(fields.contains(".use_waba_timezone(true)"), fields);
        }
    }

    @Nested
    @DisplayName("queryTemplateGroupAnalytics")
    class TemplateGroup {
        @Test
        @DisplayName("hits template_group_analytics with template_group_ids and reuses the data-point model")
        void parsesDataPoints() throws Exception {
            var http = new RecordingHttpClient();
            http.respondWith("""
                    {"template_group_analytics":{"data":[{"granularity":"DAILY","data_points":[
                     {"template_id":"grp-1","start":1707264000,"end":1707350400,"sent":1000,"delivered":950,"read":700}
                     ]}],"paging":{}},"id":"102290129340398"}""");
            var analytics = client(http).queryTemplateGroupAnalytics(new CloudTemplateGroupAnalyticsQueryBuilder()
                    .start(Instant.ofEpochSecond(1707264000))
                    .end(Instant.ofEpochSecond(1707350400))
                    .templateGroupIds(List.of("grp-1", "grp-2"))
                    .build());
            assertEquals(1, analytics.size());
            var first = analytics.getFirst();
            assertEquals("grp-1", first.templateId());
            assertEquals(1000L, first.sent().orElseThrow());
            assertEquals(950L, first.delivered().orElseThrow());
            assertEquals(700L, first.read().orElseThrow());
            var fields = fields(http);
            assertTrue(fields.startsWith("template_group_analytics.start(1707264000).end(1707350400).granularity(DAILY)"), fields);
            assertTrue(fields.contains(".template_group_ids([grp-1,grp-2])"), fields);
            assertTrue(fields.contains(".metric_types([SENT,DELIVERED,READ,CLICKED])"), fields);
            assertTrue(fields.contains(".product_type(CLOUD_API)"), fields);
        }

        @Test
        @DisplayName("returns empty list when template_group_analytics is absent")
        void absentNode() throws Exception {
            var http = new RecordingHttpClient();
            http.respondWith("{\"id\":\"102290129340398\"}");
            var analytics = client(http).queryTemplateGroupAnalytics(new CloudTemplateGroupAnalyticsQueryBuilder()
                    .start(Instant.ofEpochSecond(1707264000))
                    .end(Instant.ofEpochSecond(1707350400))
                    .templateGroupIds(List.of("grp-1"))
                    .build());
            assertTrue(analytics.isEmpty());
        }
    }
}
