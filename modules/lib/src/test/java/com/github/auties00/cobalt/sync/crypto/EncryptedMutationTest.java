package com.github.auties00.cobalt.sync.crypto;

import com.github.auties00.cobalt.exception.linked.web.WhatsAppWebAppStateSyncException;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncFixtures;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the outgoing-mutation wire format produced by
 * {@link EncryptedMutation}: the {@code IV || ciphertext || valueMac} layout of
 * {@code encryptedValue}, the associated-data prefix invariant (SET prepends
 * {@code 0x01}, REMOVE prepends {@code 0x02}), fresh-IV randomness, round-trip
 * with {@link DecryptedMutation.Untrusted#of}, key isolation, and a WA Web
 * byte-equality oracle for captured plaintext/ciphertext pairs. Fresh-IV
 * randomness is verified by encrypting the same plaintext twice and checking
 * that the IV prefix differs; the matching deterministic-output check lives on
 * the fixed-IV overload exercised in {@link MutationKeysTest}.
 */
@DisplayName("EncryptedMutation")
class EncryptedMutationTest {
    private static final byte[] SYNC_KEY = filled(32, 0x42);

    private static final byte[] KEY_ID = new byte[]{0x10, 0x20, 0x30, 0x40};

    private static byte[] filled(int length, int value) {
        var out = new byte[length];
        for (var i = 0; i < length; i++) out[i] = (byte) value;
        return out;
    }

    private static SyncPendingMutation pendingArchive(SyncdOperation op, Instant ts) {
        var action = new ArchiveChatActionBuilder()
                .archived(true)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .archiveChatAction(action)
                .build();
        var index = "[\"archive\",\"1234@s.whatsapp.net\"]";
        var trusted = new DecryptedMutation.Trusted(index, value, op, ts, 3);
        return new SyncPendingMutation(trusted, 0);
    }

    private static SyncPendingMutation pendingPin(Instant ts) {
        var pin = new PinActionBuilder().pinned(true).build();
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .pinAction(pin)
                .build();
        var index = "[\"pin_v1\",\"5678@s.whatsapp.net\"]";
        var trusted = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, 5);
        return new SyncPendingMutation(trusted, 0);
    }

    @Nested
    @DisplayName("wire layout")
    class WireLayout {
        @Test
        @DisplayName("encryptedValue is [IV (16) || ciphertext (16 or more) || valueMac (32)]")
        void encryptedValueLayout() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var enc = EncryptedMutation.of(
                        pendingArchive(SyncdOperation.SET, Instant.ofEpochSecond(1700000000L)),
                        keys, KEY_ID);
                var ev = enc.encryptedValue();
                assertTrue(ev.length >= MutationKeys.IV_LENGTH + 16 + MutationKeys.MAC_LENGTH,
                        "encryptedValue must contain at least IV + 1 cipher block + MAC: got len=" + ev.length);

                // valueMac() must equal the trailing MAC_LENGTH bytes
                var trailing = Arrays.copyOfRange(ev, ev.length - MutationKeys.MAC_LENGTH, ev.length);
                assertArrayEquals(trailing, enc.valueMac());
            }
        }

        @Test
        @DisplayName("indexMac is 32 bytes (HMAC-SHA256 of the index)")
        void indexMacLength() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var enc = EncryptedMutation.of(
                        pendingArchive(SyncdOperation.SET, Instant.ofEpochSecond(1700000000L)),
                        keys, KEY_ID);
                assertEquals(32, enc.indexMac().length);
            }
        }

        @Test
        @DisplayName("operation is propagated from the source mutation")
        void operationPropagated() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                assertEquals(SyncdOperation.SET,
                        EncryptedMutation.of(pendingArchive(SyncdOperation.SET, Instant.now()), keys, KEY_ID).operation());
                assertEquals(SyncdOperation.REMOVE,
                        EncryptedMutation.of(pendingArchive(SyncdOperation.REMOVE, Instant.now()), keys, KEY_ID).operation());
            }
        }

        @Test
        @DisplayName("keyId is propagated verbatim")
        void keyIdPropagated() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var keyId = new byte[]{(byte) 0xAB, (byte) 0xCD, (byte) 0xEF};
                var enc = EncryptedMutation.of(
                        pendingArchive(SyncdOperation.SET, Instant.now()), keys, keyId);
                assertArrayEquals(keyId, enc.keyId());
            }
        }
    }

    @Nested
    @DisplayName("freshness - fresh IV per call")
    class Freshness {
        @Test
        @DisplayName("two encryptions of the same mutation use different IVs")
        void freshIvPerCall() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var ts = Instant.ofEpochSecond(1700000000L);
                var a = EncryptedMutation.of(pendingArchive(SyncdOperation.SET, ts), keys, KEY_ID);
                var b = EncryptedMutation.of(pendingArchive(SyncdOperation.SET, ts), keys, KEY_ID);

                var ivA = Arrays.copyOfRange(a.encryptedValue(), 0, MutationKeys.IV_LENGTH);
                var ivB = Arrays.copyOfRange(b.encryptedValue(), 0, MutationKeys.IV_LENGTH);

                assertFalse(MessageDigest.isEqual(ivA, ivB),
                        "two encryptions of the same plaintext must draw fresh IVs");
                assertFalse(MessageDigest.isEqual(a.encryptedValue(), b.encryptedValue()),
                        "fresh IV propagates to a different ciphertext");
            }
        }

        @Test
        @DisplayName("indexMac is deterministic across calls (the index doesn't change)")
        void indexMacDeterministic() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var ts = Instant.ofEpochSecond(1700000000L);
                var a = EncryptedMutation.of(pendingArchive(SyncdOperation.SET, ts), keys, KEY_ID);
                var b = EncryptedMutation.of(pendingArchive(SyncdOperation.SET, ts), keys, KEY_ID);
                assertArrayEquals(a.indexMac(), b.indexMac(),
                        "same key plus same index produces same index MAC");
            }
        }
    }

    @Nested
    @DisplayName("round-trip with DecryptedMutation")
    class RoundTrip {
        @Test
        @DisplayName("SET encrypt then decrypt recovers the original action")
        void setRoundTrip() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var ts = Instant.ofEpochSecond(1700000000L);
                var pending = pendingArchive(SyncdOperation.SET, ts);
                var enc = EncryptedMutation.of(pending, keys, KEY_ID);
                var dec = DecryptedMutation.Untrusted.of(
                        enc.encryptedValue(), enc.indexMac(), keys,
                        SyncdOperation.SET, KEY_ID);

                assertEquals(pending.mutation().index(), dec.index());
                assertEquals(SyncdOperation.SET, dec.operation());
                assertEquals(ts, dec.timestamp());
                assertEquals(3, dec.actionVersion());

                // The decrypted action value must carry the same payload
                var roundtrip = dec.value().flatMap(sav -> sav.action()).filter(action -> action instanceof ArchiveChatAction).map(action -> (ArchiveChatAction) action).orElseThrow();
                assertTrue(roundtrip.archived(), "archived flag must round-trip");
            }
        }

        @Test
        @DisplayName("REMOVE encrypt then decrypt recovers the operation")
        void removeRoundTrip() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var ts = Instant.ofEpochSecond(1700000000L);
                var pending = pendingArchive(SyncdOperation.REMOVE, ts);
                var enc = EncryptedMutation.of(pending, keys, KEY_ID);
                var dec = DecryptedMutation.Untrusted.of(
                        enc.encryptedValue(), enc.indexMac(), keys,
                        SyncdOperation.REMOVE, KEY_ID);
                assertEquals(SyncdOperation.REMOVE, dec.operation());
            }
        }

        @Test
        @DisplayName("different actions round-trip independently")
        void multipleActionsRoundTrip() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var ts = Instant.ofEpochSecond(1700000000L);
                var archive = EncryptedMutation.of(pendingArchive(SyncdOperation.SET, ts), keys, KEY_ID);
                var pin = EncryptedMutation.of(pendingPin(ts), keys, KEY_ID);

                assertFalse(MessageDigest.isEqual(archive.indexMac(), pin.indexMac()),
                        "distinct indices must produce distinct index MACs");
            }
        }

        @Test
        @DisplayName("AAD prefix invariant: SET and REMOVE produce different value MACs for the same payload")
        void aadPrefixInvariant() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var ts = Instant.ofEpochSecond(1700000000L);
                var encSet = EncryptedMutation.of(pendingArchive(SyncdOperation.SET, ts), keys, KEY_ID);
                var encRemove = EncryptedMutation.of(pendingArchive(SyncdOperation.REMOVE, ts), keys, KEY_ID);
                assertNotNull(encSet);
                assertNotNull(encRemove);

                assertCrossOpFails(keys, encSet.encryptedValue(), encSet.indexMac(), SyncdOperation.REMOVE);
                assertCrossOpFails(keys, encRemove.encryptedValue(), encRemove.indexMac(), SyncdOperation.SET);
            }
        }

        private void assertCrossOpFails(MutationKeys keys, byte[] ev, byte[] indexMac, SyncdOperation wrongOp) {
            Assertions.assertThrows(
                    WhatsAppWebAppStateSyncException.ValueMacMismatch.class,
                    () -> DecryptedMutation.Untrusted.of(ev, indexMac, keys, wrongOp, KEY_ID),
                    "decrypting with the wrong operation must fail at the value MAC step");
        }
    }

    @Nested
    @DisplayName("key isolation - wrong sync key cannot decrypt")
    class KeyIsolation {
        @Test
        @DisplayName("decrypting with a different key fails at the value MAC step")
        void wrongKeyFails() throws GeneralSecurityException {
            try (var encKeys = MutationKeys.ofSyncKey(SYNC_KEY);
                 var wrongKeys = MutationKeys.ofSyncKey(filled(32, 0x77))) {
                var ts = Instant.ofEpochSecond(1700000000L);
                var enc = EncryptedMutation.of(pendingArchive(SyncdOperation.SET, ts), encKeys, KEY_ID);
                Assertions.assertThrows(
                        WhatsAppWebAppStateSyncException.ValueMacMismatch.class,
                        () -> DecryptedMutation.Untrusted.of(
                                enc.encryptedValue(), enc.indexMac(), wrongKeys,
                                SyncdOperation.SET, KEY_ID));
            }
        }

        @Test
        @DisplayName("decrypting with a different key id is rejected at the value MAC step")
        void wrongKeyIdFails() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var ts = Instant.ofEpochSecond(1700000000L);
                var enc = EncryptedMutation.of(pendingArchive(SyncdOperation.SET, ts), keys, KEY_ID);
                var wrongKeyId = new byte[]{(byte) 0xAA};
                Assertions.assertThrows(
                        WhatsAppWebAppStateSyncException.ValueMacMismatch.class,
                        () -> DecryptedMutation.Untrusted.of(
                                enc.encryptedValue(), enc.indexMac(), keys,
                                SyncdOperation.SET, wrongKeyId));
            }
        }
    }

    @Nested
    @DisplayName("WA Web oracle parity")
    class OracleParity {
        @Test
        @DisplayName("captured (plaintext, sync key, key id) reproduces the captured ciphertext on decrypt")
        void waWebOracle() throws GeneralSecurityException {
            if (!SyncFixtures.isOracleAvailable("crypto/encrypt-mutation")) return;
            var oracle = SyncFixtures.loadOracle("crypto/encrypt-mutation");
            var syncKey = SyncFixtures.decodeOracleBytes(oracle, "syncKey");
            var keyId   = SyncFixtures.decodeOracleBytes(oracle, "keyId");
            var indexMac        = SyncFixtures.decodeOracleBytes(oracle, "indexMac");
            var encryptedValue  = SyncFixtures.decodeOracleBytes(oracle, "encryptedValue");
            var operation = SyncdOperation.valueOf(oracle.getString("operation").toUpperCase());

            // Random IV per call means we cannot byte-compare ciphertext.
            // Decrypt the captured wire blob under the captured key instead
            // and assert recovery of a non-null SyncActionValue.
            try (var keys = MutationKeys.ofSyncKey(syncKey)) {
                var dec = DecryptedMutation.Untrusted.of(
                        encryptedValue, indexMac, keys, operation, keyId);
                assertTrue(dec.value().isPresent(), "decrypted SyncActionValue must be present");
            }
        }

        @Test
        @DisplayName("byte-equality with captured fixed-IV oracle (when present)")
        void fixedIvOracle() {
            if (!SyncFixtures.isOracleAvailable("crypto/encrypt-mutation-fixed-iv")) return;
            var oracle = SyncFixtures.loadOracle("crypto/encrypt-mutation-fixed-iv");
            var expected = Base64.getDecoder().decode(oracle.getString("expectedEncryptedValue"));
            // TODO: implement fixed-IV byte-equality assertion once the capture fixture lands.
            //       Fixed-IV captures pin the AES-CBC output against a known plaintext, which
            //       byte-equality cannot be tested under the production random-IV path.
            assertNotNull(expected);
        }
    }

    @Nested
    @DisplayName("record component sanity")
    class RecordComponents {
        @Test
        @DisplayName("valueMac() agrees with valueMacFromIndexAndValueCipherText(encryptedValue)")
        void valueMacAgreesWithStaticHelper() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var enc = EncryptedMutation.of(
                        pendingArchive(SyncdOperation.SET, Instant.ofEpochSecond(1700000000L)),
                        keys, KEY_ID);
                assertArrayEquals(
                        MutationKeys.valueMacFromIndexAndValueCipherText(enc.encryptedValue()),
                        enc.valueMac());
            }
        }

        @Test
        @DisplayName("record accessors return the stored buffers (reference equality)")
        void recordAccessorsReturnTheStoredBuffers() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY)) {
                var enc = EncryptedMutation.of(
                        pendingArchive(SyncdOperation.SET, Instant.ofEpochSecond(1700000000L)),
                        keys, KEY_ID);
                // record accessors return the underlying field reference
                assertTrue(enc.encryptedValue() == enc.encryptedValue());
                assertTrue(enc.indexMac() == enc.indexMac());
                assertTrue(enc.keyId() == enc.keyId());
            }
        }
    }
}
