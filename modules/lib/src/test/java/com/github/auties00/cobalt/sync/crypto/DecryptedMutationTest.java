package com.github.auties00.cobalt.sync.crypto;

import com.github.auties00.cobalt.exception.linked.web.WhatsAppWebAppStateSyncException;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValue;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncFixtures;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the decryption-side failure matrix of {@link DecryptedMutation}:
 * the value-MAC and index-MAC failure modes, the input-shape guards, the
 * {@code Untrusted}/{@code Trusted} record contracts, and a WA Web oracle that
 * decrypts a captured patch to its captured contents. Round-trip happy paths
 * live in {@code EncryptedMutationTest}. The {@code freshEncryptedArchive}
 * helper produces a fresh wire blob per test so tampering does not pollute the
 * next case.
 */
@DisplayName("DecryptedMutation")
class DecryptedMutationTest {
    // matches the "pattern" expectations baked into the crypto fixtures
    private static final byte[] SYNC_KEY = filled(32, 0x42);

    private static final byte[] KEY_ID = new byte[]{0x10, 0x20, 0x30, 0x40};

    private static byte[] filled(int length, int value) {
        var out = new byte[length];
        for (var i = 0; i < length; i++) out[i] = (byte) value;
        return out;
    }

    private static EncryptedMutation freshEncryptedArchive(MutationKeys keys) throws GeneralSecurityException {
        var action = new ArchiveChatActionBuilder().archived(true).build();
        var value = new SyncActionValueBuilder()
                .timestamp(Instant.ofEpochSecond(1700000000L))
                .archiveChatAction(action)
                .build();
        var index = "[\"archive\",\"1234@s.whatsapp.net\"]";
        var trusted = new DecryptedMutation.Trusted(
                index, value, SyncdOperation.SET, Instant.ofEpochSecond(1700000000L), 3);
        return EncryptedMutation.of(new SyncPendingMutation(trusted, 0), keys, KEY_ID);
    }

    @Nested
    @DisplayName("happy path - Untrusted exposes the decrypted fields")
    class HappyPath {
        @Test
        @DisplayName("Untrusted carries index / operation / timestamp / actionVersion / keyId")
        void untrustedFieldsPopulated() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var enc = freshEncryptedArchive(keys);
                var dec = DecryptedMutation.Untrusted.of(
                        enc.encryptedValue(), enc.indexMac(), keys,
                        SyncdOperation.SET, KEY_ID);

                assertEquals("[\"archive\",\"1234@s.whatsapp.net\"]", dec.index());
                assertEquals(SyncdOperation.SET, dec.operation());
                assertEquals(Instant.ofEpochSecond(1700000000L), dec.timestamp());
                assertEquals(3, dec.actionVersion());
                assertArrayEquals(KEY_ID, dec.keyId());
                assertTrue(dec.value().isPresent());
                assertEquals(32, dec.indexMac().length);
                assertEquals(32, dec.valueMac().length);
            }
        }

        @Test
        @DisplayName("Untrusted is a DecryptedMutation (sealed sub-type)")
        void untrustedIsDecryptedMutation() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var enc = freshEncryptedArchive(keys);
                var dec = DecryptedMutation.Untrusted.of(
                        enc.encryptedValue(), enc.indexMac(), keys,
                        SyncdOperation.SET, KEY_ID);
                assertInstanceOf(DecryptedMutation.class, dec);
            }
        }
    }

    @Nested
    @DisplayName("failure modes - value MAC")
    class ValueMacFailures {
        @Test
        @DisplayName("tampering the last byte of encryptedValue (MAC) fails ValueMacMismatch")
        void tamperedMacByte() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var enc = freshEncryptedArchive(keys);
                var tampered = enc.encryptedValue().clone();
                tampered[tampered.length - 1] ^= (byte) 0x01;
                assertThrows(WhatsAppWebAppStateSyncException.ValueMacMismatch.class,
                        () -> DecryptedMutation.Untrusted.of(
                                tampered, enc.indexMac(), keys,
                                SyncdOperation.SET, KEY_ID));
            }
        }

        @Test
        @DisplayName("tampering the IV (front bytes) fails ValueMacMismatch")
        void tamperedIv() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var enc = freshEncryptedArchive(keys);
                var tampered = enc.encryptedValue().clone();
                tampered[0] ^= (byte) 0x01;
                assertThrows(WhatsAppWebAppStateSyncException.ValueMacMismatch.class,
                        () -> DecryptedMutation.Untrusted.of(
                                tampered, enc.indexMac(), keys,
                                SyncdOperation.SET, KEY_ID));
            }
        }

        @Test
        @DisplayName("tampering a ciphertext byte (middle) fails ValueMacMismatch")
        void tamperedCiphertext() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var enc = freshEncryptedArchive(keys);
                var tampered = enc.encryptedValue().clone();
                var midPoint = MutationKeys.IV_LENGTH + 4; // inside ciphertext, before trailing MAC
                tampered[midPoint] ^= (byte) 0x01;
                assertThrows(WhatsAppWebAppStateSyncException.ValueMacMismatch.class,
                        () -> DecryptedMutation.Untrusted.of(
                                tampered, enc.indexMac(), keys,
                                SyncdOperation.SET, KEY_ID));
            }
        }

        @Test
        @DisplayName("decrypting with the wrong sync key fails ValueMacMismatch")
        void wrongKey() throws GeneralSecurityException {
            try (var encKeys = MutationKeys.ofSyncKey(SYNC_KEY);
                 var decKeys = MutationKeys.ofSyncKey(filled(32, 0x77))) {
                var enc = freshEncryptedArchive(encKeys);
                assertThrows(WhatsAppWebAppStateSyncException.ValueMacMismatch.class,
                        () -> DecryptedMutation.Untrusted.of(
                                enc.encryptedValue(), enc.indexMac(), decKeys,
                                SyncdOperation.SET, KEY_ID));
            }
        }

        @Test
        @DisplayName("decrypting with the wrong operation fails ValueMacMismatch (AAD prefix differs)")
        void wrongOperation() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var enc = freshEncryptedArchive(keys);
                assertThrows(WhatsAppWebAppStateSyncException.ValueMacMismatch.class,
                        () -> DecryptedMutation.Untrusted.of(
                                enc.encryptedValue(), enc.indexMac(), keys,
                                SyncdOperation.REMOVE, KEY_ID));
            }
        }

        @Test
        @DisplayName("decrypting with the wrong key id fails ValueMacMismatch (AAD includes key id)")
        void wrongKeyId() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var enc = freshEncryptedArchive(keys);
                var wrongKeyId = new byte[]{(byte) 0xAA};
                assertThrows(WhatsAppWebAppStateSyncException.ValueMacMismatch.class,
                        () -> DecryptedMutation.Untrusted.of(
                                enc.encryptedValue(), enc.indexMac(), keys,
                                SyncdOperation.SET, wrongKeyId));
            }
        }
    }

    @Nested
    @DisplayName("failure modes - index MAC")
    class IndexMacFailures {
        @Test
        @DisplayName("tampering the wire index MAC fails IndexMacMismatch")
        void tamperedIndexMac() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var enc = freshEncryptedArchive(keys);
                var tampered = enc.indexMac().clone();
                tampered[0] ^= (byte) 0x01;
                assertThrows(WhatsAppWebAppStateSyncException.IndexMacMismatch.class,
                        () -> DecryptedMutation.Untrusted.of(
                                enc.encryptedValue(), tampered, keys,
                                SyncdOperation.SET, KEY_ID));
            }
        }

        @Test
        @DisplayName("a totally bogus index MAC fails IndexMacMismatch")
        void bogusIndexMac() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var enc = freshEncryptedArchive(keys);
                assertThrows(WhatsAppWebAppStateSyncException.IndexMacMismatch.class,
                        () -> DecryptedMutation.Untrusted.of(
                                enc.encryptedValue(), filled(32, 0xFF), keys,
                                SyncdOperation.SET, KEY_ID));
            }
        }
    }

    @Nested
    @DisplayName("failure modes - input shape")
    class InputShape {
        @Test
        @DisplayName("encryptedValue shorter than IV + MAC throws IllegalArgumentException")
        void shortEncryptedValue() {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var tooShort = new byte[MutationKeys.IV_LENGTH + MutationKeys.MAC_LENGTH - 1];
                assertThrows(IllegalArgumentException.class,
                        () -> DecryptedMutation.Untrusted.of(
                                tooShort, new byte[32], keys,
                                SyncdOperation.SET, KEY_ID));
            }
        }

        @Test
        @DisplayName("encryptedValue exactly IV + MAC long (empty ciphertext) fails ValueMacMismatch")
        void minimumEncryptedValue() {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var minimal = new byte[MutationKeys.IV_LENGTH + MutationKeys.MAC_LENGTH];
                // With a random key the MAC will not validate (vanishingly unlikely otherwise)
                assertThrows(WhatsAppWebAppStateSyncException.ValueMacMismatch.class,
                        () -> DecryptedMutation.Untrusted.of(
                                minimal, new byte[32], keys,
                                SyncdOperation.SET, KEY_ID));
            }
        }
    }

    @Nested
    @DisplayName("Trusted - record contract")
    class TrustedRecord {
        @Test
        @DisplayName("Trusted carries the same observable fields as Untrusted (minus MAC metadata)")
        void trustedRecordFields() {
            var value = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1700000000L))
                    .archiveChatAction(new ArchiveChatActionBuilder().archived(true).build())
                    .build();
            var trusted = new DecryptedMutation.Trusted(
                    "[\"archive\",\"42@s.whatsapp.net\"]", value,
                    SyncdOperation.SET, Instant.ofEpochSecond(1700000000L), 3);

            assertEquals("[\"archive\",\"42@s.whatsapp.net\"]", trusted.index());
            assertEquals(SyncdOperation.SET, trusted.operation());
            assertEquals(Instant.ofEpochSecond(1700000000L), trusted.timestamp());
            assertEquals(3, trusted.actionVersion());
            assertEquals(value, trusted.value().orElseThrow());
        }

        @Test
        @DisplayName("Trusted is a DecryptedMutation (sealed sub-type)")
        void trustedIsDecryptedMutation() {
            var trusted = new DecryptedMutation.Trusted(
                    "idx", emptyValue(), SyncdOperation.SET, Instant.now(), 1);
            assertInstanceOf(DecryptedMutation.class, trusted);
        }

        private SyncActionValue emptyValue() {
            return new SyncActionValueBuilder().timestamp(Instant.now()).build();
        }
    }

    @Nested
    @DisplayName("WA Web oracle parity")
    class OracleParity {
        @Test
        @DisplayName("captured encrypted patch decrypts to the captured Trusted contents")
        void waWebOracle() throws GeneralSecurityException {
            if (!SyncFixtures.isOracleAvailable("crypto/decrypt-mutation")) return;
            var oracle = SyncFixtures.loadOracle("crypto/decrypt-mutation");
            var syncKey = SyncFixtures.decodeOracleBytes(oracle, "syncKey");
            var keyId   = SyncFixtures.decodeOracleBytes(oracle, "keyId");
            var indexMac        = SyncFixtures.decodeOracleBytes(oracle, "indexMac");
            var encryptedValue  = SyncFixtures.decodeOracleBytes(oracle, "encryptedValue");
            var operation = SyncdOperation.valueOf(oracle.getString("operation").toUpperCase());
            var expectedIndex = oracle.getString("expectedIndex");

            try (var keys = MutationKeys.ofSyncKey(syncKey)) {
                var dec = DecryptedMutation.Untrusted.of(
                        encryptedValue, indexMac, keys, operation, keyId);
                assertEquals(expectedIndex, dec.index(),
                        "decoded index must byte-equal the WA Web oracle");
                assertTrue(dec.actionVersion() >= 0);
            }
        }
    }
}
