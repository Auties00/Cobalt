package com.github.auties00.cobalt.message.send.token;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural and binding tests for {@link ReportingToken#generate(byte[], String, Jid, Jid, byte[], int)}:
 * token length and version echo, the empty/null content sentinels, per-input
 * determinism, and that each binding input (secret, stanza id, sender JID,
 * remote JID) is load-bearing so flipping any one changes the token bytes.
 * The inputs are synthetic 32-byte secrets and a hand-rolled stanza id rather
 * than captured live data, since these structural assertions do not depend on
 * matching a specific WA Web vector; the byte-equal known-answer test is
 * captured separately.
 */
@DisplayName("ReportingToken")
class ReportingTokenTest {

    private static final byte[] SECRET = repeatedByte(32, (byte) 0x42);
    private static final byte[] OTHER_SECRET = repeatedByte(32, (byte) 0x55);
    private static final Jid SENDER = Jid.of("12025550100@s.whatsapp.net");
    private static final Jid REMOTE = Jid.of("19254863482@s.whatsapp.net");
    private static final String STANZA_ID = "3EB0CAFEBABE0123456789";
    private static final byte[] CONTENT = "message body".getBytes();
    private static final int VERSION = 2;

    @Test
    @DisplayName("generate: returns a 16-byte token wrapped in ReportingTokenResult")
    void generateBasic() throws GeneralSecurityException {
        var result = ReportingToken.generate(SECRET, STANZA_ID, SENDER, REMOTE, CONTENT, VERSION)
                .orElseThrow();
        assertEquals(VERSION, result.version(), "result.version() must echo the input version");
        assertEquals(16, result.token().length, "token must be 16 bytes (TOKEN_LENGTH)");
    }

    @Test
    @DisplayName("generate: empty content returns empty Optional (storage-size sentinel)")
    void emptyContentReturnsEmpty() throws GeneralSecurityException {
        assertTrue(ReportingToken.generate(SECRET, STANZA_ID, SENDER, REMOTE, new byte[0], VERSION).isEmpty(),
                "empty content must produce empty Optional");
    }

    @Test
    @DisplayName("generate: null content returns empty Optional")
    void nullContentReturnsEmpty() throws GeneralSecurityException {
        assertTrue(ReportingToken.generate(SECRET, STANZA_ID, SENDER, REMOTE, null, VERSION).isEmpty(),
                "null content must produce empty Optional");
    }

    @Test
    @DisplayName("generate: same inputs yield deterministic token")
    void deterministicForSameInputs() throws GeneralSecurityException {
        var first = ReportingToken.generate(SECRET, STANZA_ID, SENDER, REMOTE, CONTENT, VERSION).orElseThrow();
        var second = ReportingToken.generate(SECRET, STANZA_ID, SENDER, REMOTE, CONTENT, VERSION).orElseThrow();
        Assertions.assertArrayEquals(first.token(), second.token(),
                "token is a deterministic HMAC; same inputs yield same bytes");
    }

    @Test
    @DisplayName("generate: different secret yields different token")
    void secretBindsToken() throws GeneralSecurityException {
        var first = ReportingToken.generate(SECRET, STANZA_ID, SENDER, REMOTE, CONTENT, VERSION).orElseThrow();
        var second = ReportingToken.generate(OTHER_SECRET, STANZA_ID, SENDER, REMOTE, CONTENT, VERSION).orElseThrow();
        assertNotEquals(toHex(first.token()), toHex(second.token()),
                "different secret must produce different token (HKDF key isolation)");
    }

    @Test
    @DisplayName("generate: different stanza id yields different token")
    void stanzaIdBindsToken() throws GeneralSecurityException {
        var first = ReportingToken.generate(SECRET, STANZA_ID, SENDER, REMOTE, CONTENT, VERSION).orElseThrow();
        var second = ReportingToken.generate(SECRET, STANZA_ID + "-tamper", SENDER, REMOTE, CONTENT, VERSION).orElseThrow();
        assertNotEquals(toHex(first.token()), toHex(second.token()));
    }

    @Test
    @DisplayName("generate: different sender / remote JID yields different token")
    void jidsBindToken() throws GeneralSecurityException {
        var baseline = ReportingToken.generate(SECRET, STANZA_ID, SENDER, REMOTE, CONTENT, VERSION).orElseThrow();
        var otherSender = ReportingToken.generate(SECRET, STANZA_ID,
                Jid.of("12025550999@s.whatsapp.net"), REMOTE, CONTENT, VERSION).orElseThrow();
        var otherRemote = ReportingToken.generate(SECRET, STANZA_ID, SENDER,
                Jid.of("12025550888@s.whatsapp.net"), CONTENT, VERSION).orElseThrow();
        assertNotEquals(toHex(baseline.token()), toHex(otherSender.token()),
                "sender change must alter token");
        assertNotEquals(toHex(baseline.token()), toHex(otherRemote.token()),
                "remote change must alter token");
    }

    @Test
    @DisplayName("generate: null required arg throws NullPointerException")
    void nullArgsThrow() {
        assertThrows(NullPointerException.class, () -> ReportingToken.generate(
                null, STANZA_ID, SENDER, REMOTE, CONTENT, VERSION));
        assertThrows(NullPointerException.class, () -> ReportingToken.generate(
                SECRET, null, SENDER, REMOTE, CONTENT, VERSION));
        assertThrows(NullPointerException.class, () -> ReportingToken.generate(
                SECRET, STANZA_ID, null, REMOTE, CONTENT, VERSION));
        assertThrows(NullPointerException.class, () -> ReportingToken.generate(
                SECRET, STANZA_ID, SENDER, null, CONTENT, VERSION));
    }

    private static byte[] repeatedByte(int len, byte b) {
        var out = new byte[len];
        Arrays.fill(out, b);
        return out;
    }

    private static String toHex(byte[] bytes) {
        var sb = new StringBuilder(bytes.length * 2);
        for (var b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
