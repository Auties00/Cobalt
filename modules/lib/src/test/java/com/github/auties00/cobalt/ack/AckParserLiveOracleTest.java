package com.github.auties00.cobalt.ack;

import com.github.auties00.cobalt.message.MessageFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live wire oracle for {@link AckParser}, feeding captured inbound {@code <ack>} stanzas through the
 * parser and asserting that the success-side attribute slots round-trip identically.
 *
 * <p>Fixtures are loaded by topic via {@link MessageFixtures}: each {@code <topic>.ack.jsonl} capture
 * is a successful server ack of an outgoing send, so it carries no {@code error} attribute and no
 * {@code phash}; the error and phash branches are exercised by the synthetic {@link AckParserTest}.
 * Topics whose capture is missing are filtered out via {@link MessageFixtures#isAvailable}, so cells
 * skip cleanly rather than fail.
 */
@DisplayName("AckParser live wire oracle")
class AckParserLiveOracleTest {

    static Stream<String> ackTopics() {
        return Stream.of(
                "send/peer-text",
                "send/group-small-text",
                "send/group-large-text",
                "send/cag-text",
                "send/peer-edit-revoke",
                "send/bot-text",
                "send/hosted-biz-text",
                "send/newsletter-text"
        ).filter(t -> MessageFixtures.isAvailable(t + ".ack"));
    }

    @ParameterizedTest(name = "ack for {0}")
    @MethodSource("ackTopics")
    @DisplayName("captured server ack parses with the success shape")
    void successAckParses(String topic) {
        var events = MessageFixtures.loadEvents(topic + ".ack");
        assertFalse(events.isEmpty(), "ack fixture must have at least one event for " + topic);

        for (var event : events) {
            assertEquals("ack", event.getString("tag"), "fixture only contains <ack> events");
            assertEquals("in", event.getString("direction"), "all captured acks are inbound");

            var ack = MessageFixtures.buildNodeFromEvent(event);
            var result = (MessageAck) AckParser.parse(ack);

            assertTrue(result.timestamp().isPresent(),
                    "server ack must carry t= timestamp for topic " + topic);
            assertTrue(result.timestamp().orElseThrow().getEpochSecond() > 0,
                    "ack timestamp must be a positive epoch second");
            assertTrue(result.isSuccess(),
                    "captured fixture only contains success acks; got error " + (result.error().isPresent() ? result.error().getAsInt() : null));
            assertTrue(result.error().isEmpty(),
                    "success ack must not have an error code");
            assertFalse(result.refreshLid(),
                    "success ack does not signal refresh_lid");
            assertTrue(result.phash().isEmpty(),
                    "success ack does not carry phash");
        }
    }

    @ParameterizedTest(name = "group ack count for {0}")
    @MethodSource("groupAckTopics")
    @DisplayName("group acks carry count=<int> attribute")
    void groupAckHasCount(String topic) {
        var events = MessageFixtures.loadEvents(topic + ".ack");
        for (var event : events) {
            var ack = MessageFixtures.buildNodeFromEvent(event);
            var result = (MessageAck) AckParser.parse(ack);
            assertTrue(result.count().isPresent(),
                    "group ack must carry count= attribute for topic " + topic);
            assertTrue(result.count().getAsInt() > 0,
                    "count must be positive, got " + result.count().getAsInt());
        }
    }

    static Stream<String> groupAckTopics() {
        return Stream.of("send/group-small-text", "send/group-large-text", "send/cag-text")
                .filter(t -> MessageFixtures.isAvailable(t + ".ack"));
    }

    @Test
    @DisplayName("newsletter ack carries server_id (not a parsed attribute; preserved on the stanza)")
    void newsletterAckCarriesServerId() {
        var topic = "send/newsletter-text";
        if (!MessageFixtures.isAvailable(topic + ".ack")) return;
        var events = MessageFixtures.loadEvents(topic + ".ack");
        assertFalse(events.isEmpty(), "newsletter ack fixture must have an event");
        for (var event : events) {
            var ack = MessageFixtures.buildNodeFromEvent(event);
            // server_id is the newsletter publish receipt id; AckParser does not surface it but it
            // must be on the captured stanza so higher-level newsletter handlers can read it.
            assertTrue(ack.getAttributeAsString("server_id").isPresent(),
                    "newsletter ack must carry server_id attribute");
        }
    }

    @Test
    @DisplayName("every captured ack uses class=\"message\"")
    void allAcksAreMessageClass() {
        var topics = ackTopics().toList();
        for (var topic : topics) {
            var events = MessageFixtures.loadEvents(topic + ".ack");
            for (var event : events) {
                var ack = MessageFixtures.buildNodeFromEvent(event);
                assertEquals("message",
                        ack.getAttributeAsString("class").orElse(null),
                        topic + ": ack class must be \"message\"");
            }
        }
        assertFalse(topics.isEmpty());
    }
}
