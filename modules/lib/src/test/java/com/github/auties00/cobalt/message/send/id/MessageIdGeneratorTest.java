package com.github.auties00.cobalt.message.send.id;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the externally visible structural contract of {@link MessageIdGenerator#generate} for both
 * {@link MessageIdVersion} variants: prefix, suffix length, uppercase-hex alphabet, uniqueness across consecutive
 * calls, and {@code null}-argument rejection. {@link MessageIdGenerator#generate} draws its randomness and wall
 * clock internally, so a byte-equal known-answer comparison is not possible; uniqueness is checked statistically
 * over 100 calls per version.
 */
@DisplayName("MessageIdGenerator")
class MessageIdGeneratorTest {
    private static final Jid SENDER = Jid.of("393495089819@s.whatsapp.net");

    @Test
    @DisplayName("V1 id is \"3EB0\" + 16 uppercase hex characters")
    void v1IdStructure() {
        var id = MessageIdGenerator.generate(MessageIdVersion.V1, SENDER);
        assertEquals(20, id.length(), "V1 ids are exactly 20 characters long");
        assertTrue(id.startsWith(MessageIdGenerator.PREFIX), "V1 id must start with the shared prefix");
        var suffix = id.substring(MessageIdGenerator.PREFIX.length());
        assertEquals(16, suffix.length(), "V1 suffix is 16 hex chars (8 random bytes)");
        assertTrue(suffix.chars().allMatch(MessageIdGeneratorTest::isUpperHex),
                "V1 suffix must be uppercase hex: " + suffix);
    }

    @Test
    @DisplayName("V2 id is \"3EB0\" + 18 uppercase hex characters")
    void v2IdStructure() {
        var id = MessageIdGenerator.generate(MessageIdVersion.V2, SENDER);
        assertEquals(22, id.length(), "V2 ids are exactly 22 characters long");
        assertTrue(id.startsWith(MessageIdGenerator.PREFIX), "V2 id must start with the shared prefix");
        var suffix = id.substring(MessageIdGenerator.PREFIX.length());
        assertEquals(18, suffix.length(), "V2 suffix is 18 hex chars (first 9 bytes of SHA-256)");
        assertTrue(suffix.chars().allMatch(MessageIdGeneratorTest::isUpperHex),
                "V2 suffix must be uppercase hex: " + suffix);
    }

    @Test
    @DisplayName("Consecutive V1 ids do not collide")
    void v1IdsAreUnique() {
        var ids = new HashSet<String>();
        for (var i = 0; i < 100; i++) {
            ids.add(MessageIdGenerator.generate(MessageIdVersion.V1, SENDER));
        }
        assertEquals(100, ids.size(), "100 consecutive V1 ids must all be distinct (RNG collision is astronomically unlikely)");
    }

    @Test
    @DisplayName("Consecutive V2 ids do not collide")
    void v2IdsAreUnique() {
        var ids = new HashSet<String>();
        for (var i = 0; i < 100; i++) {
            ids.add(MessageIdGenerator.generate(MessageIdVersion.V2, SENDER));
        }
        assertEquals(100, ids.size(), "100 consecutive V2 ids must all be distinct");
    }

    @Test
    @DisplayName("V1 and V2 produce different-length ids for the same sender")
    void versionsProduceDifferentLengths() {
        var v1 = MessageIdGenerator.generate(MessageIdVersion.V1, SENDER);
        var v2 = MessageIdGenerator.generate(MessageIdVersion.V2, SENDER);
        assertNotEquals(v1.length(), v2.length(),
                "the two versions encode different amounts of entropy and must produce different-length suffixes");
    }

    // The random component alone would also make these differ, so this asserts a necessary but not sufficient
    // condition for the sender feeding the SHA-256 pre-image; v1IdsAreUnique covers the random-only path.
    @Test
    @DisplayName("V2 id differs across sender JIDs (sender feeds the SHA-256 pre-image)")
    void v2DiffersAcrossSenders() {
        var senderA = Jid.of("393495089819@s.whatsapp.net");
        var senderB = Jid.of("12025550100@s.whatsapp.net");
        var idA = MessageIdGenerator.generate(MessageIdVersion.V2, senderA);
        var idB = MessageIdGenerator.generate(MessageIdVersion.V2, senderB);
        assertNotEquals(idA, idB);
    }

    @Test
    @DisplayName("Null version throws NullPointerException")
    void nullVersionThrows() {
        assertThrows(NullPointerException.class,
                () -> MessageIdGenerator.generate(null, SENDER));
    }

    @Test
    @DisplayName("Null sender throws NullPointerException")
    void nullSenderThrows() {
        assertThrows(NullPointerException.class,
                () -> MessageIdGenerator.generate(MessageIdVersion.V2, null));
    }

    private static boolean isUpperHex(int c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F');
    }
}
