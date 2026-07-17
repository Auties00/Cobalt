package com.github.auties00.cobalt.message.send.bot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Structural coverage for {@link BotMessageSecret#derive(byte[])}: 32-byte output length,
 * determinism, input-sensitivity, input non-mutation, and arbitrary input-keying-material
 * length. These tests assert structure rather than a fixed output vector; the byte-equal
 * known-answer vector against WhatsApp Web is captured separately.
 */
@DisplayName("BotMessageSecret")
class BotMessageSecretTest {

    @Test
    @DisplayName("derive produces exactly 32 bytes")
    void derivedLength() throws GeneralSecurityException {
        var secret = new byte[32];
        var derived = BotMessageSecret.derive(secret);
        assertEquals(32, derived.length, "HKDF expand length is the bot-secret slot size");
    }

    @Test
    @DisplayName("derive is deterministic: same input yields same output")
    void deterministic() throws GeneralSecurityException {
        var secret = new byte[]{
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32
        };
        var first = BotMessageSecret.derive(secret);
        var second = BotMessageSecret.derive(secret);
        assertArrayEquals(first, second,
                "HKDF must produce identical output for the same input (derivation is stateless)");
    }

    @Test
    @DisplayName("derive is input-sensitive: different inputs yield different outputs")
    void inputSensitive() throws GeneralSecurityException {
        var secretA = new byte[32];
        var secretB = new byte[32];
        secretB[0] = 1;
        var derivedA = BotMessageSecret.derive(secretA);
        var derivedB = BotMessageSecret.derive(secretB);
        assertFalse(Arrays.equals(derivedA, derivedB),
                "HKDF-SHA256 must propagate input differences through the full 32-byte output");
    }

    @Test
    @DisplayName("derive does not mutate the input secret")
    void inputIsNotMutated() throws GeneralSecurityException {
        var secret = new byte[]{
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32
        };
        var snapshot = secret.clone();
        BotMessageSecret.derive(secret);
        assertArrayEquals(snapshot, secret, "derive must not modify the caller's secret buffer");
    }

    // WhatsApp Web always passes a 32-byte secret; HKDF-Extract collapses any non-null length to a fixed PRK
    @Test
    @DisplayName("derive accepts non-32-byte secrets (HKDF is length-agnostic)")
    void acceptsArbitraryLengthSecret() throws GeneralSecurityException {
        var shortSecret = new byte[]{1, 2, 3};
        var longSecret = new byte[256];
        for (var i = 0; i < longSecret.length; i++) {
            longSecret[i] = (byte) i;
        }
        assertEquals(32, BotMessageSecret.derive(shortSecret).length);
        assertEquals(32, BotMessageSecret.derive(longSecret).length);
    }

    @Test
    @DisplayName("null secret throws NullPointerException")
    void nullSecretThrows() {
        assertThrows(NullPointerException.class, () -> BotMessageSecret.derive(null));
    }
}
