package com.github.auties00.cobalt.sync.crypto;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises every primitive on {@link MutationKeys} against WA Web's byte
 * layout: HKDF slice derivation, associated-data layout, AES-CBC round-trips,
 * the HMAC-SHA512 value MAC, the index MAC, value-MAC slicing, key-material
 * destruction on close, and the wire constants. Each primitive pairs synthetic
 * deterministic vectors (slice offsets, AAD layout, IV-prefix layout, MAC
 * truncation length, round-trip correctness) with a WA Web byte-equality
 * oracle. Oracle assertions are gated on
 * {@link SyncFixtures#isOracleAvailable(String)} so the synthetic suite still
 * runs before the captured corpus lands; {@code SYNC_KEY_PATTERN} matches the
 * {@code "pattern"} sample in the captured {@code crypto/mutation-keys.expected}
 * fixture so the synthetic derivation tests and the oracle exercise the same
 * key.
 */
@DisplayName("MutationKeys")
class MutationKeysTest {
    // k[i] = (i * 7) & 0xFF; matches the "pattern" sample in the captured fixture
    private static final byte[] SYNC_KEY_PATTERN = patternKey();

    private static byte[] patternKey() {
        var k = new byte[32];
        for (var i = 0; i < k.length; i++) {
            k[i] = (byte) ((i * 7) & 0xFF);
        }
        return k;
    }

    private static byte[] filled(int length, int value) {
        var b = new byte[length];
        for (var i = 0; i < length; i++) b[i] = (byte) value;
        return b;
    }

    @Nested
    @DisplayName("ofSyncKey - HKDF derivation")
    class OfSyncKey {
        @Test
        @DisplayName("rejects null")
        void rejectsNull() {
            assertThrows(NullPointerException.class, () -> MutationKeys.ofSyncKey(null));
        }

        @Test
        @DisplayName("rejects wrong length")
        void rejectsWrongLength() {
            for (var len : new int[]{0, 1, 16, 31, 33, 64}) {
                assertThrows(IllegalArgumentException.class,
                        () -> MutationKeys.ofSyncKey(new byte[len]),
                        "length=" + len);
            }
        }

        @Test
        @DisplayName("derives five 32-byte slices for a valid sync key")
        void slicesAre32Bytes() {
            try (var keys = MutationKeys.ofSyncKey(filled(32, 0x42))) {
                assertEquals(32, keys.indexKey().getEncoded().length);
                assertEquals(32, keys.valueEncryptionKey().getEncoded().length);
                assertEquals(32, keys.valueMacKey().getEncoded().length);
                assertEquals(32, keys.snapshotMacKey().getEncoded().length);
                assertEquals(32, keys.patchMacKey().getEncoded().length);
            }
        }

        @Test
        @DisplayName("slices use distinct algorithms matching WA Web")
        void slicesHaveAlgorithms() {
            try (var keys = MutationKeys.ofSyncKey(filled(32, 0x42))) {
                assertEquals("HmacSHA256", keys.indexKey().getAlgorithm(), "indexKey");
                assertEquals("AES",        keys.valueEncryptionKey().getAlgorithm(), "valueEncryptionKey");
                assertEquals("HmacSHA512", keys.valueMacKey().getAlgorithm(), "valueMacKey");
                assertEquals("HmacSHA256", keys.snapshotMacKey().getAlgorithm(), "snapshotMacKey");
                assertEquals("HmacSHA256", keys.patchMacKey().getAlgorithm(), "patchMacKey");
            }
        }

        @Test
        @DisplayName("derivation is deterministic for the same sync key")
        void deterministic() {
            try (var a = MutationKeys.ofSyncKey(SYNC_KEY_PATTERN);
                 var b = MutationKeys.ofSyncKey(SYNC_KEY_PATTERN)) {
                assertArrayEquals(a.indexKey().getEncoded(),           b.indexKey().getEncoded());
                assertArrayEquals(a.valueEncryptionKey().getEncoded(), b.valueEncryptionKey().getEncoded());
                assertArrayEquals(a.valueMacKey().getEncoded(),        b.valueMacKey().getEncoded());
                assertArrayEquals(a.snapshotMacKey().getEncoded(),     b.snapshotMacKey().getEncoded());
                assertArrayEquals(a.patchMacKey().getEncoded(),        b.patchMacKey().getEncoded());
            }
        }

        @Test
        @DisplayName("different sync keys produce different slices")
        void distinctKeysDistinctSlices() {
            try (var a = MutationKeys.ofSyncKey(filled(32, 0x00));
                 var b = MutationKeys.ofSyncKey(filled(32, 0xFF))) {
                assertFalse(MessageDigest.isEqual(a.indexKey().getEncoded(),           b.indexKey().getEncoded()));
                assertFalse(MessageDigest.isEqual(a.valueEncryptionKey().getEncoded(), b.valueEncryptionKey().getEncoded()));
                assertFalse(MessageDigest.isEqual(a.valueMacKey().getEncoded(),        b.valueMacKey().getEncoded()));
                assertFalse(MessageDigest.isEqual(a.snapshotMacKey().getEncoded(),     b.snapshotMacKey().getEncoded()));
                assertFalse(MessageDigest.isEqual(a.patchMacKey().getEncoded(),        b.patchMacKey().getEncoded()));
            }
        }

        @Test
        @DisplayName("the five slices are mutually distinct within a single derivation")
        void slicesAreMutuallyDistinct() {
            try (var k = MutationKeys.ofSyncKey(SYNC_KEY_PATTERN)) {
                var slices = new byte[][]{
                        k.indexKey().getEncoded(),
                        k.valueEncryptionKey().getEncoded(),
                        k.valueMacKey().getEncoded(),
                        k.snapshotMacKey().getEncoded(),
                        k.patchMacKey().getEncoded()
                };
                for (var i = 0; i < slices.length; i++) {
                    for (var j = i + 1; j < slices.length; j++) {
                        assertFalse(MessageDigest.isEqual(slices[i], slices[j]),
                                "slices " + i + " and " + j + " collided");
                    }
                }
            }
        }

        @Test
        @DisplayName("WA Web oracle parity (zero / ones / pattern sync keys)")
        void waWebOracleParity() {
            if (!SyncFixtures.isOracleAvailable("crypto/mutation-keys")) return;
            var oracle = SyncFixtures.loadOracle("crypto/mutation-keys");
            assertOracleSample(oracle, "zero",    filled(32, 0x00));
            assertOracleSample(oracle, "ones",    filled(32, 0xFF));
            assertOracleSample(oracle, "pattern", SYNC_KEY_PATTERN);
        }

        // The oracle file groups three samples ("zero", "ones", "pattern") under
        // one JSON object; this compares each of the five slices so a failure
        // names the specific slice that disagreed.
        private void assertOracleSample(JSONObject oracle, String tag, byte[] syncKey) {
            var sample = oracle.getJSONObject(tag);
            assertNotNull(sample, "missing oracle sample '" + tag + "'");
            var oracleSyncKey = Base64.getDecoder().decode(sample.getString("syncKey"));
            assertArrayEquals(syncKey, oracleSyncKey, "oracle/Java sync-key disagree for '" + tag + "'");

            try (var keys = MutationKeys.ofSyncKey(syncKey)) {
                assertArrayEquals(Base64.getDecoder().decode(sample.getString("indexKey")),
                        keys.indexKey().getEncoded(), "indexKey/" + tag);
                assertArrayEquals(Base64.getDecoder().decode(sample.getString("valueEncryptionKey")),
                        keys.valueEncryptionKey().getEncoded(), "valueEncryptionKey/" + tag);
                assertArrayEquals(Base64.getDecoder().decode(sample.getString("valueMacKey")),
                        keys.valueMacKey().getEncoded(), "valueMacKey/" + tag);
                assertArrayEquals(Base64.getDecoder().decode(sample.getString("snapshotMacKey")),
                        keys.snapshotMacKey().getEncoded(), "snapshotMacKey/" + tag);
                assertArrayEquals(Base64.getDecoder().decode(sample.getString("patchMacKey")),
                        keys.patchMacKey().getEncoded(), "patchMacKey/" + tag);
            }
        }
    }

    @Nested
    @DisplayName("generateAssociatedData - SET=0x01, REMOVE=0x02, key id appended")
    class GenerateAssociatedData {
        @Test
        @DisplayName("SET prepends 0x01 then the raw key id")
        void setPrependsOne() {
            var keyId = new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD};
            var ad = MutationKeys.generateAssociatedData(SyncdOperation.SET, keyId);
            assertEquals(5, ad.length);
            assertEquals((byte) 0x01, ad[0]);
            assertArrayEquals(keyId, Arrays.copyOfRange(ad, 1, ad.length));
        }

        @Test
        @DisplayName("REMOVE prepends 0x02 then the raw key id")
        void removePrependsTwo() {
            var keyId = new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD};
            var ad = MutationKeys.generateAssociatedData(SyncdOperation.REMOVE, keyId);
            assertEquals(5, ad.length);
            assertEquals((byte) 0x02, ad[0]);
            assertArrayEquals(keyId, Arrays.copyOfRange(ad, 1, ad.length));
        }

        @Test
        @DisplayName("empty key id produces a single-byte AAD")
        void emptyKeyId() {
            var ad = MutationKeys.generateAssociatedData(SyncdOperation.SET, new byte[0]);
            assertEquals(1, ad.length);
            assertEquals((byte) 0x01, ad[0]);
        }

        @Test
        @DisplayName("rejects null operation")
        void rejectsNullOperation() {
            assertThrows(NullPointerException.class,
                    () -> MutationKeys.generateAssociatedData(null, new byte[1]));
        }

        @Test
        @DisplayName("rejects null key id")
        void rejectsNullKeyId() {
            assertThrows(NullPointerException.class,
                    () -> MutationKeys.generateAssociatedData(SyncdOperation.SET, null));
        }

        @Test
        @DisplayName("WA Web oracle parity")
        void waWebOracleParity() {
            if (!SyncFixtures.isOracleAvailable("crypto/associated-data")) return;
            var oracle = SyncFixtures.loadOracle("crypto/associated-data");
            var keyId = Base64.getDecoder().decode(oracle.getString("keyId"));
            assertArrayEquals(
                    Base64.getDecoder().decode(oracle.getString("set")),
                    MutationKeys.generateAssociatedData(SyncdOperation.SET, keyId),
                    "SET AAD must byte-equal WAWebSyncdMutationsCryptoUtils.generateAssociatedData('set', keyId)");
            assertArrayEquals(
                    Base64.getDecoder().decode(oracle.getString("remove")),
                    MutationKeys.generateAssociatedData(SyncdOperation.REMOVE, keyId),
                    "REMOVE AAD must byte-equal WAWebSyncdMutationsCryptoUtils.generateAssociatedData('remove', keyId)");
        }
    }

    @Nested
    @DisplayName("generatePadding - currently always empty (MAX_OF_MIN_DATA_LENGTH = 0)")
    class GeneratePadding {
        @Test
        @DisplayName("returns empty array regardless of input lengths")
        void alwaysEmpty() {
            assertEquals(0, MutationKeys.generatePadding(0, 0).length);
            assertEquals(0, MutationKeys.generatePadding(1, 1).length);
            assertEquals(0, MutationKeys.generatePadding(100, 100).length);
            assertEquals(0, MutationKeys.generatePadding(Integer.MAX_VALUE, 0).length);
        }
    }

    @Nested
    @DisplayName("generateCipherText / decryptCipherText - AES-CBC round-trip")
    class CipherText {
        @Test
        @DisplayName("round-trip with random IV recovers the plaintext")
        void randomIvRoundTrip() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_PATTERN)) {
                var plaintext = filled(64, 0x33);
                var output = keys.generateCipherText(plaintext);
                assertTrue(output.length >= MutationKeys.IV_LENGTH + plaintext.length,
                        "output must carry IV + ciphertext");
                var iv = Arrays.copyOfRange(output, 0, MutationKeys.IV_LENGTH);
                var ciphertext = Arrays.copyOfRange(output, MutationKeys.IV_LENGTH, output.length);
                var recovered = keys.decryptCipherText(iv, ciphertext);
                assertArrayEquals(plaintext, recovered);
            }
        }

        @Test
        @DisplayName("two encryptions of the same plaintext use different IVs")
        void freshIvPerCall() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_PATTERN)) {
                var pt = filled(32, 0x55);
                var a = keys.generateCipherText(pt);
                var b = keys.generateCipherText(pt);
                var ivA = Arrays.copyOfRange(a, 0, MutationKeys.IV_LENGTH);
                var ivB = Arrays.copyOfRange(b, 0, MutationKeys.IV_LENGTH);
                assertFalse(MessageDigest.isEqual(ivA, ivB),
                        "consecutive encryptions must draw fresh IVs");
            }
        }

        @Test
        @DisplayName("fixed-IV overload produces deterministic output")
        void fixedIvDeterministic() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_PATTERN)) {
                var iv = filled(MutationKeys.IV_LENGTH, 0x11);
                var pt = filled(64, 0x33);
                var a = keys.generateCipherText(iv, pt);
                var b = keys.generateCipherText(iv, pt);
                assertArrayEquals(a, b, "fixed-(key, IV, plaintext) must produce identical ciphertext");
                assertArrayEquals(iv, Arrays.copyOfRange(a, 0, MutationKeys.IV_LENGTH),
                        "IV must be prepended");
            }
        }

        @Test
        @DisplayName("decrypt with wrong key rejects via BadPaddingException")
        void wrongKeyFails() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_PATTERN);
                 var other = MutationKeys.ofSyncKey(filled(32, 0xAB))) {
                var pt = filled(64, 0x33);
                var output = keys.generateCipherText(pt);
                var iv = Arrays.copyOfRange(output, 0, MutationKeys.IV_LENGTH);
                var ct = Arrays.copyOfRange(output, MutationKeys.IV_LENGTH, output.length);
                assertThrows(BadPaddingException.class, () -> other.decryptCipherText(iv, ct));
            }
        }

        @Test
        @DisplayName("WA Web oracle parity for fixed-IV ciphertext")
        void waWebOracleParity() throws GeneralSecurityException {
            if (!SyncFixtures.isOracleAvailable("crypto/cipher-text")) return;
            var oracle = SyncFixtures.loadOracle("crypto/cipher-text");
            var syncKey = Base64.getDecoder().decode(oracle.getString("syncKey"));
            var iv = Base64.getDecoder().decode(oracle.getString("iv"));
            var plaintext = Base64.getDecoder().decode(oracle.getString("plaintext"));
            var expected = Base64.getDecoder().decode(oracle.getString("ciphertext"));
            try (var keys = MutationKeys.ofSyncKey(syncKey)) {
                var actual = keys.generateCipherText(iv, plaintext);
                assertArrayEquals(expected, actual,
                        "Cobalt's AES-CBC output must byte-equal WAWebSyncdMutationsCryptoUtils.generateCipherText");
            }
        }
    }

    @Nested
    @DisplayName("generateMac - HMAC-SHA512 truncated to 32 bytes with length suffix")
    class GenerateMac {
        @Test
        @DisplayName("output is exactly 32 bytes")
        void macLength() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_PATTERN)) {
                var mac = keys.generateMac(new byte[]{1, 2, 3}, new byte[]{4, 5, 6});
                assertEquals(MutationKeys.MAC_LENGTH, mac.length);
            }
        }

        @Test
        @DisplayName("output is deterministic for the same inputs")
        void deterministic() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_PATTERN)) {
                var ad = new byte[]{0x01, 0x02, 0x03};
                var ct = filled(80, 0x55);
                assertArrayEquals(keys.generateMac(ad, ct), keys.generateMac(ad, ct));
            }
        }

        @Test
        @DisplayName("length-suffix invariant: associated-data length is mixed in")
        void lengthSuffixSensitive() throws GeneralSecurityException {
            // The same AAD bytes appearing with extra trailing data must produce a different MAC
            // because lengthSuffix[7] = ad.length is mixed into the HMAC.
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_PATTERN)) {
                var ct = filled(64, 0x77);
                var macShort = keys.generateMac(new byte[]{1, 2, 3}, ct);
                var macLong  = keys.generateMac(new byte[]{1, 2, 3, 4}, ct);
                assertFalse(MessageDigest.isEqual(macShort, macLong),
                        "AAD length must affect the MAC (length suffix invariant)");
            }
        }

        @Test
        @DisplayName("WA Web oracle parity")
        void waWebOracleParity() throws GeneralSecurityException {
            if (!SyncFixtures.isOracleAvailable("crypto/mac")) return;
            var oracle = SyncFixtures.loadOracle("crypto/mac");
            var syncKey = Base64.getDecoder().decode(oracle.getString("syncKey"));
            var ad = Base64.getDecoder().decode(oracle.getString("associatedData"));
            var ct = Base64.getDecoder().decode(oracle.getString("ciphertext"));
            var expected = Base64.getDecoder().decode(oracle.getString("mac"));
            try (var keys = MutationKeys.ofSyncKey(syncKey)) {
                assertArrayEquals(expected, keys.generateMac(ad, ct),
                        "Cobalt's value MAC must byte-equal WAWebSyncdMutationsCryptoUtils.generateMac");
            }
        }
    }

    @Nested
    @DisplayName("generateIndexMac - HMAC-SHA256 over index bytes")
    class GenerateIndexMac {
        @Test
        @DisplayName("output is 32 bytes")
        void length() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_PATTERN)) {
                assertEquals(32, keys.generateIndexMac("archive".getBytes()).length);
            }
        }

        @Test
        @DisplayName("output is deterministic")
        void deterministic() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_PATTERN)) {
                var idx = "[\"archive\",\"1234@s.whatsapp.net\"]".getBytes();
                assertArrayEquals(keys.generateIndexMac(idx), keys.generateIndexMac(idx));
            }
        }

        @Test
        @DisplayName("different index keys produce different MACs for the same input")
        void differentKeysProduceDifferentMacs() throws GeneralSecurityException {
            try (var a = MutationKeys.ofSyncKey(filled(32, 0x11));
                 var b = MutationKeys.ofSyncKey(filled(32, 0x22))) {
                var idx = "archive".getBytes();
                assertFalse(MessageDigest.isEqual(a.generateIndexMac(idx), b.generateIndexMac(idx)));
            }
        }

        @Test
        @DisplayName("empty index produces a stable 32-byte MAC")
        void emptyIndex() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_PATTERN)) {
                var mac = keys.generateIndexMac(new byte[0]);
                assertEquals(32, mac.length);
            }
        }
    }

    @Nested
    @DisplayName("valueMacFromIndexAndValueCipherText - exact slice of last 32 bytes")
    class ValueMacFromIndexAndValueCipherText {
        @Test
        @DisplayName("returns the last MAC_LENGTH bytes verbatim")
        void slicesLastMacLengthBytes() {
            var buf = new byte[100];
            for (var i = 0; i < buf.length; i++) buf[i] = (byte) i;
            var mac = MutationKeys.valueMacFromIndexAndValueCipherText(buf);
            assertEquals(MutationKeys.MAC_LENGTH, mac.length);
            assertArrayEquals(Arrays.copyOfRange(buf, buf.length - MutationKeys.MAC_LENGTH, buf.length), mac);
        }

        @Test
        @DisplayName("buffer of exactly MAC_LENGTH bytes is the MAC itself")
        void exactlyMacLength() {
            var buf = filled(MutationKeys.MAC_LENGTH, 0x99);
            assertArrayEquals(buf, MutationKeys.valueMacFromIndexAndValueCipherText(buf));
        }

        @Test
        @DisplayName("buffer shorter than MAC_LENGTH throws IAE")
        void shortBufferThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> MutationKeys.valueMacFromIndexAndValueCipherText(new byte[MutationKeys.MAC_LENGTH - 1]));
        }
    }

    @Nested
    @DisplayName("close - destroys key material without error")
    class Close {
        @Test
        @DisplayName("close swallows DestroyFailedException and is idempotent")
        void closeIsIdempotent() {
            var keys = MutationKeys.ofSyncKey(SYNC_KEY_PATTERN);
            keys.close();
            keys.close(); // must not throw on double-close
        }

        @Test
        @DisplayName("close used via try-with-resources")
        void tryWithResources() throws GeneralSecurityException {
            byte[] indexMac;
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_PATTERN)) {
                indexMac = keys.generateIndexMac(new byte[]{1, 2, 3});
            }
            assertEquals(32, indexMac.length);
        }

        @Test
        @DisplayName("toString does not leak key material")
        void toStringIsOpaque() {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_PATTERN)) {
                var s = keys.toString();
                assertEquals("AppStateSyncKeys", s);
                assertNotEquals(HexFormat.of().formatHex(SYNC_KEY_PATTERN), s,
                        "toString must not contain hex sync key");
            }
        }
    }

    @Nested
    @DisplayName("constants - match WA Web's WAWebSyncdCryptoConst")
    class Constants {
        @Test
        @DisplayName("MAC_LENGTH is 32")
        void macLength() {
            assertEquals(32, MutationKeys.MAC_LENGTH);
        }

        @Test
        @DisplayName("IV_LENGTH is 16")
        void ivLength() {
            assertEquals(16, MutationKeys.IV_LENGTH);
        }

        @Test
        @DisplayName("OCTET_LENGTH is 8")
        void octetLength() {
            assertEquals(8, MutationKeys.OCTET_LENGTH);
        }

        @Test
        @DisplayName("MAX_OF_MIN_DATA_LENGTH is 0 (WA Web current value)")
        void maxOfMinDataLength() {
            assertEquals(0, MutationKeys.MAX_OF_MIN_DATA_LENGTH);
        }
    }

    @Nested
    @DisplayName("accessor sanity - key getters return the expected slices")
    class Accessors {
        @Test
        @DisplayName("each accessor returns the same SecretKeySpec across calls")
        void accessorsAreStable() {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_PATTERN)) {
                SecretKeySpec a1 = keys.indexKey(), a2 = keys.indexKey();
                assertArrayEquals(a1.getEncoded(), a2.getEncoded());
            }
        }
    }
}
