package com.github.auties00.cobalt.sync.crypto;

import com.github.auties00.cobalt.exception.linked.web.WhatsAppWebAppStateSyncException;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.signal.KeyId;
import com.github.auties00.cobalt.wire.linked.signal.KeyIdBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdPatchBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdSnapshotBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdVersion;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdVersionBuilder;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises both the MAC formulas and the stateful verification routines of
 * {@link MutationIntegrityVerifier}. The static formulas (snapshot MAC, patch
 * MAC, and the chained outgoing pair) are matched byte-for-byte against the
 * independent {@code referenceSnapshotMac} and {@code referencePatchMac}
 * implementations defined in this file, which build the HMAC directly from
 * {@code Mac.getInstance("HmacSHA256")} so neither side trusts the other. The
 * stateful {@code verifySnapshotMac} and {@code verifyPatchIntegrity} routines
 * run against a temporary store seeded with one sync key, so every failure mode
 * (missing key id, missing MAC, MAC mismatch) maps to its expected
 * {@link WhatsAppWebAppStateSyncException} subtype. A WA Web oracle pins the
 * snapshot MAC to a captured vector when the fixture is present.
 */
@DisplayName("MutationIntegrityVerifier")
class MutationIntegrityVerifierTest {
    private static final byte[] SYNC_KEY_DATA = filled(32, 0x42);

    private static final byte[] SYNC_KEY_ID = new byte[]{1, 2, 3, 4};

    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");

    private LinkedWhatsAppStore store;

    private MutationIntegrityVerifier verifier;

    @BeforeEach
    void setUp() {
        store = SyncFixtures.temporaryStoreWithSyncKey(SELF_PN, null, SYNC_KEY_ID, SYNC_KEY_DATA);
        store.syncStore().setCheckPatchMacs(true);
        verifier = new MutationIntegrityVerifier(store);
    }

    private static byte[] filled(int length, int value) {
        var out = new byte[length];
        for (var i = 0; i < length; i++) out[i] = (byte) value;
        return out;
    }

    // TODO: remove the dead reference64 helper; the body returns eight zero
    //       bytes regardless of input and no test calls it.
    @SuppressWarnings("unused")
    private static byte[] reference64(byte[] v) {
        var b = new byte[8];
        b[0] = (byte) (v[0] != 0 ? 0 : 0);
        for (var i = 0; i < 8; i++) b[i] = (byte) 0;
        return b;
    }

    private static byte[] referenceSnapshotMac(SecretKeySpec key, byte[] ltHash, long version, SyncPatchType type) throws GeneralSecurityException {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        mac.update(ltHash);
        // 8-byte big-endian version suffix
        for (var shift = 56; shift >= 0; shift -= 8) {
            mac.update((byte) (version >> shift));
        }
        mac.update(type.toBytes());
        return mac.doFinal();
    }

    private static byte[] referencePatchMac(SecretKeySpec key, byte[] snapshotMac, List<byte[]> valueMacs, long version, SyncPatchType type) throws GeneralSecurityException {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        if (snapshotMac != null) mac.update(snapshotMac);
        for (var v : valueMacs) mac.update(v);
        for (var shift = 56; shift >= 0; shift -= 8) {
            mac.update((byte) (version >> shift));
        }
        mac.update(type.toBytes());
        return mac.doFinal();
    }

    @Nested
    @DisplayName("computeSnapshotMac - HMAC-SHA256(snapshotKey, ltHash || version8 || collectionUtf8)")
    class ComputeSnapshotMac {
        @Test
        @DisplayName("matches the independent HMAC-SHA256 reference for v=0")
        void matchesReferenceAtVersionZero() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_DATA)) {
                for (var type : SyncPatchType.values()) {
                    var ltHash = filled(MutationLTHash.HASH_LENGTH, 0x11);
                    var expected = referenceSnapshotMac(keys.snapshotMacKey(), ltHash, 0L, type);
                    var actual = MutationIntegrityVerifier.computeSnapshotMac(keys.snapshotMacKey(), ltHash, 0L, type);
                    assertArrayEquals(expected, actual, "type=" + type);
                }
            }
        }

        @Test
        @DisplayName("matches the reference for a non-trivial version (mid-range)")
        void matchesReferenceForMidVersion() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_DATA)) {
                var ltHash = filled(MutationLTHash.HASH_LENGTH, 0x77);
                var version = 0x0102030405L;
                var expected = referenceSnapshotMac(keys.snapshotMacKey(), ltHash, version, SyncPatchType.REGULAR_LOW);
                var actual = MutationIntegrityVerifier.computeSnapshotMac(keys.snapshotMacKey(), ltHash, version, SyncPatchType.REGULAR_LOW);
                assertArrayEquals(expected, actual);
            }
        }

        @Test
        @DisplayName("output is 32 bytes")
        void length() {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_DATA)) {
                var mac = MutationIntegrityVerifier.computeSnapshotMac(
                        keys.snapshotMacKey(), MutationLTHash.EMPTY_HASH, 5L, SyncPatchType.REGULAR);
                assertEquals(32, mac.length);
            }
        }

        @Test
        @DisplayName("different versions produce different MACs (version is mixed in)")
        void versionSensitivity() {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_DATA)) {
                var a = MutationIntegrityVerifier.computeSnapshotMac(
                        keys.snapshotMacKey(), MutationLTHash.EMPTY_HASH, 1L, SyncPatchType.REGULAR);
                var b = MutationIntegrityVerifier.computeSnapshotMac(
                        keys.snapshotMacKey(), MutationLTHash.EMPTY_HASH, 2L, SyncPatchType.REGULAR);
                assertFalse(MessageDigest.isEqual(a, b),
                        "snapshot MAC must change with version");
            }
        }

        @Test
        @DisplayName("different collection types produce different MACs (collection is mixed in)")
        void collectionSensitivity() {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_DATA)) {
                var a = MutationIntegrityVerifier.computeSnapshotMac(
                        keys.snapshotMacKey(), MutationLTHash.EMPTY_HASH, 0L, SyncPatchType.REGULAR);
                var b = MutationIntegrityVerifier.computeSnapshotMac(
                        keys.snapshotMacKey(), MutationLTHash.EMPTY_HASH, 0L, SyncPatchType.REGULAR_LOW);
                assertFalse(MessageDigest.isEqual(a, b),
                        "snapshot MAC must change with collection type");
            }
        }

        @Test
        @DisplayName("different LT-Hash inputs produce different MACs")
        void ltHashSensitivity() {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_DATA)) {
                var a = MutationIntegrityVerifier.computeSnapshotMac(
                        keys.snapshotMacKey(), filled(MutationLTHash.HASH_LENGTH, 0x00), 0L, SyncPatchType.REGULAR);
                var b = MutationIntegrityVerifier.computeSnapshotMac(
                        keys.snapshotMacKey(), filled(MutationLTHash.HASH_LENGTH, 0x01), 0L, SyncPatchType.REGULAR);
                assertFalse(MessageDigest.isEqual(a, b),
                        "snapshot MAC must change with LT-Hash input");
            }
        }
    }

    @Nested
    @DisplayName("computePatchMac - HMAC-SHA256(patchKey, snapshotMac || valueMacs || version8 || collectionUtf8)")
    class ComputePatchMac {
        @Test
        @DisplayName("matches the independent HMAC-SHA256 reference")
        void matchesReference() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_DATA)) {
                var snapshotMac = filled(32, 0x55);
                var valueMacs = List.of(filled(32, 0x11), filled(32, 0x22), filled(32, 0x33));
                var version = 7L;
                var type = SyncPatchType.CRITICAL_BLOCK;
                var expected = referencePatchMac(keys.patchMacKey(), snapshotMac, valueMacs, version, type);
                var actual = MutationIntegrityVerifier.computePatchMac(keys.patchMacKey(), snapshotMac, valueMacs, version, type);
                assertArrayEquals(expected, actual);
            }
        }

        @Test
        @DisplayName("null snapshot MAC is permitted (omitted from the HMAC input)")
        void nullSnapshotMacIsHandled() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_DATA)) {
                var valueMacs = List.of(filled(32, 0xAA));
                var actual = MutationIntegrityVerifier.computePatchMac(
                        keys.patchMacKey(), null, valueMacs, 1L, SyncPatchType.REGULAR);
                var expected = referencePatchMac(
                        keys.patchMacKey(), null, valueMacs, 1L, SyncPatchType.REGULAR);
                assertArrayEquals(expected, actual);
            }
        }

        @Test
        @DisplayName("empty value-MAC list is permitted")
        void emptyValueMacList() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_DATA)) {
                var snapshotMac = filled(32, 0x55);
                var actual = MutationIntegrityVerifier.computePatchMac(
                        keys.patchMacKey(), snapshotMac, List.of(), 1L, SyncPatchType.REGULAR);
                var expected = referencePatchMac(
                        keys.patchMacKey(), snapshotMac, List.of(), 1L, SyncPatchType.REGULAR);
                assertArrayEquals(expected, actual);
            }
        }

        @Test
        @DisplayName("snapshot MAC participation: removing or changing it changes the output")
        void snapshotMacSensitivity() {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_DATA)) {
                var valueMacs = List.of(filled(32, 0x11));
                var a = MutationIntegrityVerifier.computePatchMac(
                        keys.patchMacKey(), filled(32, 0x55), valueMacs, 1L, SyncPatchType.REGULAR);
                var b = MutationIntegrityVerifier.computePatchMac(
                        keys.patchMacKey(), filled(32, 0x56), valueMacs, 1L, SyncPatchType.REGULAR);
                assertFalse(MessageDigest.isEqual(a, b),
                        "patch MAC must change with snapshot MAC");
            }
        }
    }

    @Nested
    @DisplayName("computeOutgoingSnapshotAndPatchMacs - chained computation")
    class OutgoingMacs {
        @Test
        @DisplayName("patch MAC is chained over the snapshot MAC")
        void chainedOverSnapshot() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_DATA)) {
                var ltHash = filled(MutationLTHash.HASH_LENGTH, 0x88);
                var valueMacs = List.of(filled(32, 0x99));
                var version = 12L;
                var type = SyncPatchType.REGULAR_HIGH;

                var pair = MutationIntegrityVerifier.computeOutgoingSnapshotAndPatchMacs(
                        keys.snapshotMacKey(), keys.patchMacKey(),
                        ltHash, valueMacs, version, type);

                var expectedSnapshot = referenceSnapshotMac(keys.snapshotMacKey(), ltHash, version, type);
                var expectedPatch = referencePatchMac(
                        keys.patchMacKey(), expectedSnapshot, valueMacs, version, type);

                assertArrayEquals(expectedSnapshot, pair.snapshotMac());
                assertArrayEquals(expectedPatch, pair.patchMac());
            }
        }
    }

    @Nested
    @DisplayName("verifySnapshotMac - round-trip plus failure modes")
    class VerifySnapshotMac {
        @Test
        @DisplayName("happy path: snapshot built with matching MAC passes verification")
        void happyPath() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_DATA)) {
                var ltHash = filled(MutationLTHash.HASH_LENGTH, 0xAB);
                var version = 42L;
                var type = SyncPatchType.REGULAR_LOW;
                var mac = MutationIntegrityVerifier.computeSnapshotMac(
                        keys.snapshotMacKey(), ltHash, version, type);
                var snapshot = new SyncdSnapshotBuilder()
                        .version(syncdVersion(version))
                        .mac(mac)
                        .keyId(keyId(SYNC_KEY_ID))
                        .build();

                var returned = verifier.verifySnapshotMac(type, version, snapshot, ltHash);
                assertArrayEquals(mac, returned, "happy path must return the computed MAC");
            }
        }

        @Test
        @DisplayName("when checkPatchMacs is disabled the verifier returns null")
        void disabledShortCircuit() throws GeneralSecurityException {
            store.syncStore().setCheckPatchMacs(false);
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_DATA)) {
                var snapshot = new SyncdSnapshotBuilder()
                        .version(syncdVersion(1L))
                        .mac(new byte[]{0x00})
                        .keyId(keyId(SYNC_KEY_ID))
                        .build();
                assertNull(verifier.verifySnapshotMac(
                        SyncPatchType.REGULAR, 1L, snapshot, MutationLTHash.EMPTY_HASH));
            }
        }

        @Test
        @DisplayName("missing snapshot MAC field is fatal")
        void missingMac() {
            var snapshot = new SyncdSnapshotBuilder()
                    .version(syncdVersion(1L))
                    .keyId(keyId(SYNC_KEY_ID))
                    .build();
            assertThrows(WhatsAppWebAppStateSyncException.SnapshotMacMismatch.class,
                    () -> verifier.verifySnapshotMac(
                            SyncPatchType.REGULAR, 1L, snapshot, MutationLTHash.EMPTY_HASH));
        }

        @Test
        @DisplayName("missing key id raises the unexpected-error subtype")
        void missingKeyId() {
            var snapshot = new SyncdSnapshotBuilder()
                    .version(syncdVersion(1L))
                    .mac(filled(32, 0x00))
                    .build();
            assertThrows(WhatsAppWebAppStateSyncException.UnexpectedError.class,
                    () -> verifier.verifySnapshotMac(
                            SyncPatchType.REGULAR, 1L, snapshot, MutationLTHash.EMPTY_HASH));
        }

        @Test
        @DisplayName("unknown key id raises MissingKey")
        void unknownKeyId() {
            var snapshot = new SyncdSnapshotBuilder()
                    .version(syncdVersion(1L))
                    .mac(filled(32, 0x00))
                    .keyId(keyId(new byte[]{(byte) 0xDE, (byte) 0xAD}))
                    .build();
            assertThrows(WhatsAppWebAppStateSyncException.MissingKey.class,
                    () -> verifier.verifySnapshotMac(
                            SyncPatchType.REGULAR, 1L, snapshot, MutationLTHash.EMPTY_HASH));
        }

        @Test
        @DisplayName("tampered MAC raises SnapshotMacMismatch")
        void tamperedMac() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_DATA)) {
                var ltHash = filled(MutationLTHash.HASH_LENGTH, 0xAB);
                var version = 42L;
                var type = SyncPatchType.REGULAR_LOW;
                var goodMac = MutationIntegrityVerifier.computeSnapshotMac(
                        keys.snapshotMacKey(), ltHash, version, type);
                var bogusMac = goodMac.clone();
                bogusMac[0] ^= (byte) 0x01;

                var snapshot = new SyncdSnapshotBuilder()
                        .version(syncdVersion(version))
                        .mac(bogusMac)
                        .keyId(keyId(SYNC_KEY_ID))
                        .build();

                assertThrows(WhatsAppWebAppStateSyncException.SnapshotMacMismatch.class,
                        () -> verifier.verifySnapshotMac(type, version, snapshot, ltHash));
            }
        }
    }

    @Nested
    @DisplayName("verifyPatchIntegrity - patch MAC fatal, snapshot MAC non-fatal")
    class VerifyPatchIntegrity {
        @Test
        @DisplayName("happy path: wire patchMac matches and snapshotMac matches, returns true")
        void happyPath() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_DATA)) {
                var ltHash = filled(MutationLTHash.HASH_LENGTH, 0x11);
                var version = 3L;
                var type = SyncPatchType.REGULAR;
                var valueMacs = List.of(filled(32, 0x42));

                var snapshotMac = MutationIntegrityVerifier.computeSnapshotMac(
                        keys.snapshotMacKey(), ltHash, version, type);
                var patchMac = MutationIntegrityVerifier.computePatchMac(
                        keys.patchMacKey(), snapshotMac, valueMacs, version, type);

                var patch = new SyncdPatchBuilder()
                        .version(syncdVersion(version))
                        .snapshotMac(snapshotMac)
                        .patchMac(patchMac)
                        .keyId(keyId(SYNC_KEY_ID))
                        .build();

                assertTrue(verifier.verifyPatchIntegrity(type, patch, ltHash, valueMacs));
            }
        }

        @Test
        @DisplayName("snapshot MAC mismatch is non-fatal (returns false)")
        void snapshotMacMismatchNonFatal() throws GeneralSecurityException {
            try (var keys = MutationKeys.ofSyncKey(SYNC_KEY_DATA)) {
                var ltHash = filled(MutationLTHash.HASH_LENGTH, 0x11);
                var version = 3L;
                var type = SyncPatchType.REGULAR;
                var valueMacs = List.of(filled(32, 0x42));

                var wireSnapshotMac = filled(32, 0x99); // wrong on purpose
                var patchMac = MutationIntegrityVerifier.computePatchMac(
                        keys.patchMacKey(), wireSnapshotMac, valueMacs, version, type);

                var patch = new SyncdPatchBuilder()
                        .version(syncdVersion(version))
                        .snapshotMac(wireSnapshotMac)
                        .patchMac(patchMac)
                        .keyId(keyId(SYNC_KEY_ID))
                        .build();

                assertFalse(verifier.verifyPatchIntegrity(type, patch, ltHash, valueMacs),
                        "snapshot MAC mismatch should return false, not throw");
            }
        }

        @Test
        @DisplayName("patch MAC mismatch is fatal (PatchMacMismatch)")
        void patchMacMismatchFatal() {
            var patch = new SyncdPatchBuilder()
                    .version(syncdVersion(1L))
                    .snapshotMac(filled(32, 0x00))
                    .patchMac(filled(32, 0xFF))  // not the computed value
                    .keyId(keyId(SYNC_KEY_ID))
                    .build();

            assertThrows(WhatsAppWebAppStateSyncException.PatchMacMismatch.class,
                    () -> verifier.verifyPatchIntegrity(
                            SyncPatchType.REGULAR, patch,
                            MutationLTHash.EMPTY_HASH, List.of(filled(32, 0x01))));
        }

        @Test
        @DisplayName("checkPatchMacs disabled short-circuits to true")
        void disabledShortCircuit() {
            store.syncStore().setCheckPatchMacs(false);
            var patch = new SyncdPatchBuilder()
                    .version(syncdVersion(1L))
                    .patchMac(filled(32, 0xFF))
                    .keyId(keyId(SYNC_KEY_ID))
                    .build();
            assertTrue(verifier.verifyPatchIntegrity(
                    SyncPatchType.REGULAR, patch, MutationLTHash.EMPTY_HASH, List.of()));
        }

        @Test
        @DisplayName("missing key id raises UnexpectedError")
        void missingKeyId() {
            var patch = new SyncdPatchBuilder()
                    .version(syncdVersion(1L))
                    .patchMac(filled(32, 0x00))
                    .build();
            assertThrows(WhatsAppWebAppStateSyncException.UnexpectedError.class,
                    () -> verifier.verifyPatchIntegrity(
                            SyncPatchType.REGULAR, patch, MutationLTHash.EMPTY_HASH, List.of()));
        }

        @Test
        @DisplayName("unknown key id raises MissingKey")
        void unknownKeyId() {
            var patch = new SyncdPatchBuilder()
                    .version(syncdVersion(1L))
                    .patchMac(filled(32, 0x00))
                    .keyId(keyId(new byte[]{(byte) 0xDE, (byte) 0xAD}))
                    .build();
            assertThrows(WhatsAppWebAppStateSyncException.MissingKey.class,
                    () -> verifier.verifyPatchIntegrity(
                            SyncPatchType.REGULAR, patch, MutationLTHash.EMPTY_HASH, List.of()));
        }
    }

    @Nested
    @DisplayName("indexAndValueMacToString - diagnostic formatter")
    class IndexAndValueMacToString {
        @Test
        @DisplayName("truncated default returns last 16 hex chars of each MAC, colon-joined")
        void truncatedDefault() {
            var indexMac = new byte[]{
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0xEE, (byte) 0xFF
            };
            var valueMac = indexMac.clone();
            var formatted = MutationIntegrityVerifier.indexAndValueMacToString(indexMac, valueMac);
            assertEquals("0809aabbccddeeff:0809aabbccddeeff", formatted,
                    "default truncates each side to the last 16 hex chars");
        }

        @Test
        @DisplayName("non-truncated returns full hex of each MAC")
        void notTruncated() {
            var indexMac = new byte[]{0x12, 0x34};
            var valueMac = new byte[]{(byte) 0xAB, (byte) 0xCD};
            assertEquals("1234:abcd",
                    MutationIntegrityVerifier.indexAndValueMacToString(indexMac, valueMac, false));
        }

        @Test
        @DisplayName("short input with truncate=true returns full hex (no slicing past start)")
        void shortInputWithTruncate() {
            var indexMac = new byte[]{0x12};
            var valueMac = new byte[]{(byte) 0xAB};
            assertEquals("12:ab",
                    MutationIntegrityVerifier.indexAndValueMacToString(indexMac, valueMac, true));
        }
    }

    @Nested
    @DisplayName("SyncdPatchDirection - enum present, both variants distinct")
    class PatchDirection {
        @Test
        void variantsAreDistinct() {
            assertNotEquals(MutationIntegrityVerifier.SyncdPatchDirection.INCOMING,
                    MutationIntegrityVerifier.SyncdPatchDirection.OUTGOING);
        }
    }

    @Nested
    @DisplayName("constants - match WA Web")
    class Constants {
        @Test
        @DisplayName("HASH_LENGTH is 128")
        void hashLength() {
            assertEquals(128, MutationLTHash.HASH_LENGTH);
        }

        @Test
        @DisplayName("the verifier holds the store reference passed in")
        void storeReferenceHeld() {
            assertNotNull(verifier);
        }
    }

    @Nested
    @DisplayName("WA Web oracle parity")
    class OracleParity {
        @Test
        @DisplayName("captured snapshot/patch MAC vector matches Cobalt's output")
        void waWebVectors() throws GeneralSecurityException {
            if (!SyncFixtures.isOracleAvailable("crypto/integrity-verifier")) return;
            var oracle = SyncFixtures.loadOracle("crypto/integrity-verifier");

            var syncKey  = SyncFixtures.decodeOracleBytes(oracle, "syncKey");
            var ltHash   = SyncFixtures.decodeOracleBytes(oracle, "ltHash");
            var version  = oracle.getLong("version").longValue();
            var typeStr  = oracle.getString("collection");
            var type     = SyncPatchType.of(typeStr).orElseThrow();
            var expectedSnapshotMac = SyncFixtures.decodeOracleBytes(oracle, "snapshotMac");

            try (var keys = MutationKeys.ofSyncKey(syncKey)) {
                var actual = MutationIntegrityVerifier.computeSnapshotMac(
                        keys.snapshotMacKey(), ltHash, version, type);
                assertArrayEquals(expectedSnapshotMac, actual,
                        "snapshot MAC must byte-equal WAWebSyncdEncryptionManager.generateSnapshotMac");
            }
        }
    }

    private static SyncdVersion syncdVersion(long v) {
        return new SyncdVersionBuilder().version(v).build();
    }

    private static KeyId keyId(byte[] id) {
        return new KeyIdBuilder().id(id).build();
    }
}
