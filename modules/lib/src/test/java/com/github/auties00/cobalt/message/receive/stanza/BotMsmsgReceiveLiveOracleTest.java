package com.github.auties00.cobalt.message.receive.stanza;

import com.github.auties00.cobalt.message.MessageFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wire-shape oracle for inbound MSMSG bot reply streams, anchored to the
 * captured {@code fixtures/message/receive/template-or-buttons-from-biz.jsonl}
 * corpus modelling a single user question answered by a multi-chunk Meta AI
 * bot response. The tests pin down the structural invariants the receive
 * pipeline relies on (per-chunk {@code <bot>} streaming marker,
 * {@code <meta target_id="...">} pointer back at the user's question, and an
 * {@code <enc type="msmsg">} ciphertext) that {@link MessageReceiveStanzaParser}
 * must accept before {@link MessageReceiveBotInfo} and
 * {@link MessageReceiveEncryptedPayload} reach the MSMSG decryption path. The
 * fixture is read through {@link MessageFixtures} and every test is guarded on
 * {@link MessageFixtures#isAvailable} so the suite stays green when the corpus
 * is not checked out locally.
 */
@DisplayName("Bot MSMSG receive live wire oracle")
class BotMsmsgReceiveLiveOracleTest {

    private static final String TOPIC = "receive/template-or-buttons-from-biz";

    // Captured from the live business session; this literal is part of the fixture and cannot be derived at runtime.
    private static final String BOT_JID = "867051314767696@bot";

    @Test
    @DisplayName("captured bot stream contains at least one first + one last chunk")
    void streamHasFirstAndLastMarkers() {
        if (!MessageFixtures.isAvailable(TOPIC)) return;

        var chunks = MessageFixtures.loadEvents(TOPIC).stream()
                .filter(e -> "in".equals(e.getString("direction")))
                .filter(e -> "message".equals(e.getString("tag")))
                .map(MessageFixtures::buildNodeFromEvent)
                .filter(n -> BOT_JID.equals(n.getAttributeAsString("from").orElse(null)))
                .toList();
        assertFalse(chunks.isEmpty(), "bot stream must contain at least one chunk");

        var editValues = chunks.stream()
                .map(c -> c.getChild("bot").orElseThrow().getAttributeAsString("edit").orElse(null))
                .toList();
        assertTrue(editValues.contains("first"),
                "bot stream must contain an edit=\"first\" chunk to mark the stream start, got " + editValues);
        assertTrue(editValues.contains("last"),
                "bot stream must contain an edit=\"last\" chunk to mark the stream end, got " + editValues);
    }

    @Test
    @DisplayName("every bot chunk carries <bot> + <meta target_id=...> + <enc type=msmsg>")
    void everyChunkShape() {
        if (!MessageFixtures.isAvailable(TOPIC)) return;

        var chunks = MessageFixtures.loadEvents(TOPIC).stream()
                .filter(e -> "in".equals(e.getString("direction")))
                .filter(e -> "message".equals(e.getString("tag")))
                .map(MessageFixtures::buildNodeFromEvent)
                .filter(n -> BOT_JID.equals(n.getAttributeAsString("from").orElse(null)))
                .toList();
        assertFalse(chunks.isEmpty());

        for (var chunk : chunks) {
            assertEquals("text", chunk.getAttributeAsString("type").orElseThrow(),
                    "all bot chunks must carry type=text");
            assertNotNull(chunk.getAttributeAsString("notify").orElse(null),
                    "every bot chunk has a notify= attribute (display name)");

            var bot = chunk.getChild("bot").orElseThrow(
                    () -> new AssertionError("every bot chunk has a <bot> marker"));
            var edit = bot.getAttributeAsString("edit").orElseThrow();
            assertTrue(List.of("first", "inner", "last").contains(edit),
                    "<bot edit=...> must be first/inner/last, got " + edit);
            bot.getAttributeAsString("edit_target_id").orElseThrow();

            var meta = chunk.getChild("meta").orElseThrow(
                    () -> new AssertionError("every bot chunk has a <meta> child"));
            assertTrue(meta.getAttributeAsString("target_id").isPresent(),
                    "<meta target_id=...> must bind chunk to user's question");

            var enc = chunk.getChild("enc").orElseThrow();
            assertEquals("2", enc.getAttributeAsString("v").orElseThrow(),
                    "<enc> must be v=2");
            assertEquals("msmsg", enc.getAttributeAsString("type").orElseThrow(),
                    "<enc type=...> must be msmsg for bot replies");
            assertTrue(enc.hasContent(),
                    "<enc> must carry MSMSG ciphertext bytes");
        }
    }

    @Test
    @DisplayName("every chunk's <meta target_id=...> points back at the same user question")
    void everyChunkBindsToSameUserQuestion() {
        if (!MessageFixtures.isAvailable(TOPIC)) return;

        var chunks = MessageFixtures.loadEvents(TOPIC).stream()
                .filter(e -> "in".equals(e.getString("direction")))
                .filter(e -> "message".equals(e.getString("tag")))
                .map(MessageFixtures::buildNodeFromEvent)
                .filter(n -> BOT_JID.equals(n.getAttributeAsString("from").orElse(null)))
                .toList();
        assertFalse(chunks.isEmpty());

        var targetIds = chunks.stream()
                .map(c -> c.getChild("meta").orElseThrow().getAttributeAsString("target_id").orElseThrow())
                .distinct()
                .toList();
        assertEquals(1, targetIds.size(),
                "every chunk in a single bot reply must bind to the same target_id; got " + targetIds);
    }

    @Test
    @DisplayName("inner/last chunks carry edit_target_id pointing at the first chunk's id")
    void innerAndLastReferenceFirstChunk() {
        if (!MessageFixtures.isAvailable(TOPIC)) return;

        var chunks = MessageFixtures.loadEvents(TOPIC).stream()
                .filter(e -> "in".equals(e.getString("direction")))
                .filter(e -> "message".equals(e.getString("tag")))
                .map(MessageFixtures::buildNodeFromEvent)
                .filter(n -> BOT_JID.equals(n.getAttributeAsString("from").orElse(null)))
                .toList();

        var firstChunk = chunks.stream()
                .filter(c -> "first".equals(c.getChild("bot").orElseThrow().getAttributeAsString("edit").orElse(null)))
                .findFirst()
                .orElseThrow();
        var firstId = firstChunk.getAttributeAsString("id").orElseThrow();

        for (var chunk : chunks) {
            var edit = chunk.getChild("bot").orElseThrow().getAttributeAsString("edit").orElseThrow();
            var editTargetId = chunk.getChild("bot").orElseThrow()
                    .getAttributeAsString("edit_target_id").orElseThrow();
            if ("first".equals(edit)) {
                // The empty edit_target_id on the first chunk is the asymmetry the stitcher uses to detect the head of the reply stream.
                assertEquals("", editTargetId,
                        "<bot edit=\"first\"> must have empty edit_target_id");
            } else {
                assertEquals(firstId, editTargetId,
                        "<bot edit=\"" + edit + "\"> edit_target_id must reference the first chunk's id");
            }
        }
    }
}
