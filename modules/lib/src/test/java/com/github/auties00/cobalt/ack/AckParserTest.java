package com.github.auties00.cobalt.ack;

import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Synthetic-input cells for {@link AckParser}: each cell builds an {@code <ack ... />} stanza with a
 * specific attribute combination and asserts that the resulting {@link AckResult} subtype extracts
 * the matching slots, covering success, the notable nack codes (421/478/479), {@code phash}
 * presence, {@code refresh_lid="true"}, the {@code sync}, {@code count}, and
 * {@code addressing_mode} attributes, and the failure cases (wrong tag, null stanza).
 *
 * <p>Pairs with {@link AckParserLiveOracleTest}, which feeds captured live wire acks through the
 * same parser to assert parity on the success shape.
 */
@DisplayName("AckParser")
class AckParserTest {

    private static MessageAck parseMessage(Stanza ack) {
        return assertInstanceOf(MessageAck.class, AckParser.parse(ack));
    }

    @Test
    @DisplayName("success ack: only timestamp; error is empty")
    void successAck() {
        var ack = new StanzaBuilder()
                .description("ack")
                .attribute("t", "1700000000")
                .build();

        var result = parseMessage(ack);
        assertEquals(Instant.ofEpochSecond(1700000000L), result.timestamp().orElseThrow());
        assertTrue(result.error().isEmpty(), "no error attribute means no error in result");
        assertTrue(result.isSuccess(), "no error code means success");
        assertFalse(result.hasPhashMismatch());
        assertFalse(result.refreshLid());
    }

    @Test
    @DisplayName("error 421 (no-route) surfaces on result.error()")
    void error421() {
        var ack = new StanzaBuilder()
                .description("ack")
                .attribute("t", "1700000000")
                .attribute("error", "421")
                .build();

        var result = parseMessage(ack);
        assertEquals(421, result.error().orElseThrow());
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("error 478 (phash mismatch) parses error and signals resend")
    void error478() {
        var ack = new StanzaBuilder()
                .description("ack")
                .attribute("t", "1700000000")
                .attribute("error", "478")
                .attribute("phash", "2:abcDEF")
                .build();

        var result = parseMessage(ack);
        assertEquals(478, result.error().orElseThrow());
        assertEquals("2:abcDEF", result.phash().orElseThrow());
        assertTrue(result.hasPhashMismatch(), "phash present implies resend to delta devices");
    }

    // 479 is the server's signal that the participant fanout used a wrong addressing form (PN in a
    // LID-addressed fanout or vice versa); UserMessageSenderTest's 479 invariant exists specifically
    // to prevent this nack from firing.
    @Test
    @DisplayName("error 479 (recipient_addressing_mismatch) surfaces on result.error()")
    void error479() {
        var ack = new StanzaBuilder()
                .description("ack")
                .attribute("t", "1700000000")
                .attribute("error", "479")
                .build();

        var result = parseMessage(ack);
        assertEquals(479, result.error().orElseThrow());
        assertFalse(result.isSuccess(), "error 479 is a server-side rejection, not a success");
    }

    @Test
    @DisplayName("refresh_lid=\"true\" flips the boolean")
    void refreshLidTrue() {
        var ack = new StanzaBuilder()
                .description("ack")
                .attribute("t", "1700000000")
                .attribute("refresh_lid", "true")
                .build();

        var result = parseMessage(ack);
        assertTrue(result.refreshLid(), "refresh_lid=true triggers a LID refresh on the receiver side");
    }

    @Test
    @DisplayName("refresh_lid attribute absent defaults to false")
    void refreshLidAbsent() {
        var ack = new StanzaBuilder()
                .description("ack")
                .attribute("t", "1700000000")
                .build();

        var result = parseMessage(ack);
        assertFalse(result.refreshLid(), "missing attribute must default to false, not throw");
    }

    @Test
    @DisplayName("sync attribute is passed through verbatim")
    void syncAttribute() {
        var ack = new StanzaBuilder()
                .description("ack")
                .attribute("t", "1700000000")
                .attribute("sync", "regular_low")
                .build();

        var result = parseMessage(ack);
        assertEquals("regular_low", result.sync().orElseThrow());
    }

    @Test
    @DisplayName("addressing_mode is passed through verbatim")
    void addressingMode() {
        var ack = new StanzaBuilder()
                .description("ack")
                .attribute("t", "1700000000")
                .attribute("addressing_mode", "lid")
                .build();

        var result = parseMessage(ack);
        assertEquals("lid", result.addressingMode().orElseThrow());
    }

    @Test
    @DisplayName("count attribute parses as int")
    void countAttribute() {
        var ack = new StanzaBuilder()
                .description("ack")
                .attribute("t", "1700000000")
                .attribute("count", "42")
                .build();

        var result = parseMessage(ack);
        assertEquals(42, result.count().orElseThrow());
    }

    @Test
    @DisplayName("all-fields ack populates every result slot")
    void allFieldsPresent() {
        var ack = new StanzaBuilder()
                .description("ack")
                .attribute("t", "1700000000")
                .attribute("sync", "regular")
                .attribute("phash", "2:hash")
                .attribute("refresh_lid", "true")
                .attribute("addressing_mode", "lid")
                .attribute("count", "5")
                .attribute("error", "478")
                .build();

        var result = parseMessage(ack);
        assertEquals(Instant.ofEpochSecond(1700000000L), result.timestamp().orElseThrow());
        assertEquals("regular", result.sync().orElseThrow());
        assertEquals("2:hash", result.phash().orElseThrow());
        assertTrue(result.refreshLid());
        assertEquals("lid", result.addressingMode().orElseThrow());
        assertEquals(5, result.count().orElseThrow());
        assertEquals(478, result.error().orElseThrow());
    }

    @Test
    @DisplayName("missing timestamp leaves result.timestamp() empty")
    void missingTimestamp() {
        var ack = new StanzaBuilder()
                .description("ack")
                .build();

        var result = AckParser.parse(ack);
        assertTrue(result.timestamp().isEmpty(),
                "missing t attribute must not throw; produces an empty Optional");
    }

    @Test
    @DisplayName("class=\"message\" parses as MessageAck")
    void messageClassDispatch() {
        var ack = new StanzaBuilder()
                .description("ack")
                .attribute("class", "message")
                .attribute("t", "1700000000")
                .build();
        assertInstanceOf(MessageAck.class, AckParser.parse(ack));
    }

    @Test
    @DisplayName("class=\"receipt\" parses as ReceiptAck")
    void receiptClassDispatch() {
        var ack = new StanzaBuilder()
                .description("ack")
                .attribute("class", "receipt")
                .attribute("t", "1700000000")
                .build();
        assertInstanceOf(ReceiptAck.class, AckParser.parse(ack));
    }

    @Test
    @DisplayName("class=\"notification\" parses as NotificationAck")
    void notificationClassDispatch() {
        var ack = new StanzaBuilder()
                .description("ack")
                .attribute("class", "notification")
                .attribute("t", "1700000000")
                .build();
        assertInstanceOf(NotificationAck.class, AckParser.parse(ack));
    }

    @Test
    @DisplayName("class=\"call\" without <relay> parses as CallAck with empty relay")
    void callClassDispatchNoRelay() {
        var ack = new StanzaBuilder()
                .description("ack")
                .attribute("class", "call")
                .attribute("t", "1700000000")
                .build();
        var result = assertInstanceOf(CallAck.class, AckParser.parse(ack));
        assertTrue(result.relay().isEmpty());
    }

    @Test
    @DisplayName("non-<ack> tag throws IllegalArgumentException")
    void wrongTagThrows() {
        var bogus = new StanzaBuilder()
                .description("message")
                .attribute("t", "1700000000")
                .build();

        assertThrows(IllegalArgumentException.class, () -> AckParser.parse(bogus));
    }

    @Test
    @DisplayName("null stanza throws NullPointerException")
    void nullNodeThrows() {
        assertThrows(NullPointerException.class, () -> AckParser.parse(null));
    }
}
