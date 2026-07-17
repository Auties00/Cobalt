package com.github.auties00.cobalt.device.adv;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.linked.device.identity.ADVEncryptionType;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.libsignal.SignalProtocolAddress;
import com.github.auties00.libsignal.key.SignalIdentityPublicKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Byte-equality known-answer tests for {@link DeviceADVValidator} against WA Web's
 * {@code WAWebHandleAdvDeviceNotificationUtils.decodeSignedKeyIndexBytes}. The captured triple
 * (primary identity key, signed-key-index bytes, and the decoded ValidatedKeyIndexListResult) lives
 * in {@code fixtures/device/adv-decode-self-oracle.expected.json}; each test plants the primary
 * identity into a temporary store and asserts Cobalt's
 * {@link DeviceADVValidator#decodeSignedKeyIndexBytes(Jid, byte[])} reproduces the same {@code rawId},
 * {@code timestamp}, {@code validIndexes}, {@code currentIndex}, and {@code accountType} the WA Web JS
 * bundle produced on the same wire bytes. Re-capture the fixture if WA Web changes the ADV protobuf
 * schema or the signature-verification algorithm.
 */
@DisplayName("DeviceADVValidator")
class DeviceADVValidatorTest {
    private static final Jid SELF_PN = Jid.of("393495089819@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("258252122116273@lid");

    @Test
    @DisplayName("decodeSignedKeyIndexBytes byte-equality against WA Web oracle")
    void decodeMatchesWaWebOracle() {
        var oracleDoc = DeviceFixtures.loadExpected("adv-decode-self-oracle");
        var inner = JSON.parseObject(
                oracleDoc.getJSONObject("result").getString("value"));

        var primaryKeyB64 = inner.getString("primaryIdentityKeyBase64");
        var signedB64 = inner.getString("signedKeyIndexBytesBase64");
        var expected = inner.getJSONObject("decoded");

        var primaryKey = Base64.getDecoder().decode(primaryKeyB64);
        var signedBytes = Base64.getDecoder().decode(signedB64);
        assertEquals(32, primaryKey.length, "primary identity key is exactly 32 bytes");

        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        // Companion-device JID form so findIdentityByAddress at (user, 0) falls through to the
        // remoteIdentities map instead of short-circuiting to the store's own identity key pair.
        store.accountStore().setJid(Jid.of("393495089819:75@s.whatsapp.net"));

        var address = new SignalProtocolAddress(SELF_PN.user(), 0);
        store.signalStore().saveIdentity(address, SignalIdentityPublicKey.ofDirect(primaryKey));

        var retrieved = store.signalStore().findIdentityByAddress(address);
        assertTrue(retrieved.isPresent(), "planted identity should be retrievable");
        Assertions.assertArrayEquals(primaryKey, retrieved.get().toEncodedPoint(),
                "retrieved identity bytes should match planted bytes");

        var validator = new DeviceADVValidator(store, TestABPropsService.builder().build());
        var actual = validator.decodeSignedKeyIndexBytes(SELF_PN, signedBytes)
                .orElseThrow(() -> new AssertionError(
                        "Cobalt's decodeSignedKeyIndexBytes should succeed for the captured triple"));

        assertEquals(expected.getLongValue("rawId"), actual.rawId(),
                "rawId must match WA Web's oracle byte-for-byte");
        assertEquals(Instant.ofEpochSecond(expected.getLongValue("timestamp")), actual.timestamp(),
                "timestamp must match");
        assertEquals(expected.getIntValue("currentIndex"), actual.currentIndex(),
                "currentIndex must match");
        assertEquals(ADVEncryptionType.E2EE, actual.accountType(),
                "captured account is E2EE (oracle reported accountType=0)");

        var oracleIndexes = expected.getJSONArray("validIndexes");
        assertEquals(oracleIndexes.size(), actual.validIndexes().size(),
                "validIndexes size must match");
        for (var i = 0; i < oracleIndexes.size(); i++) {
            assertTrue(actual.validIndexes().contains(oracleIndexes.getIntValue(i)),
                    "validIndex " + oracleIndexes.getIntValue(i) + " missing from Cobalt result");
        }
    }

    @Test
    @DisplayName("verifySKeyIndexWithAccSigKey: hosted-path verification using the embedded key")
    void hostedPathUsesEmbeddedKey() {
        var oracleDoc = DeviceFixtures.loadExpected("adv-decode-self-oracle");
        var inner = JSON.parseObject(
                oracleDoc.getJSONObject("result").getString("value"));
        var signedBytes = Base64.getDecoder().decode(inner.getString("signedKeyIndexBytesBase64"));

        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        var validator = new DeviceADVValidator(store, TestABPropsService.builder().build());

        var result = validator.verifySKeyIndexWithAccSigKey(signedBytes);
        assertTrue(result.isEmpty(),
                "non-hosted signed-key-index has no embedded accountSignatureKey to verify against");
    }

    @Test
    @DisplayName("decodeSignedKeyIndexBytes returns empty when no identity is stored")
    void decodeMissingIdentityReturnsEmpty() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        store.accountStore().setJid(Jid.of("393495089819:75@s.whatsapp.net"));
        var validator = new DeviceADVValidator(store, TestABPropsService.builder().build());

        var oracleDoc = DeviceFixtures.loadExpected("adv-decode-self-oracle");
        var inner = JSON.parseObject(
                oracleDoc.getJSONObject("result").getString("value"));
        var signedBytes = Base64.getDecoder().decode(inner.getString("signedKeyIndexBytesBase64"));

        var result = validator.decodeSignedKeyIndexBytes(SELF_PN, signedBytes);
        assertTrue(result.isEmpty(),
                "without a stored primary identity, decode must return Optional.empty() (signature can't be verified)");
    }

    @Test
    @DisplayName("decodeSignedKeyIndexBytes returns empty when the bytes are tampered")
    void decodeTamperedBytesReturnEmpty() {
        var oracleDoc = DeviceFixtures.loadExpected("adv-decode-self-oracle");
        var inner = JSON.parseObject(
                oracleDoc.getJSONObject("result").getString("value"));
        var primaryKey = Base64.getDecoder().decode(inner.getString("primaryIdentityKeyBase64"));
        var signedBytes = Base64.getDecoder().decode(inner.getString("signedKeyIndexBytesBase64"));

        // Flip one byte mid-buffer to corrupt the signature area so verifySignature rejects it.
        signedBytes[signedBytes.length / 2] ^= 0x42;

        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        store.accountStore().setJid(Jid.of("393495089819:75@s.whatsapp.net"));
        store.signalStore().saveIdentity(
                new SignalProtocolAddress(SELF_PN.user(), 0),
                SignalIdentityPublicKey.ofDirect(primaryKey));

        var validator = new DeviceADVValidator(store, TestABPropsService.builder().build());
        var result = validator.decodeSignedKeyIndexBytes(SELF_PN, signedBytes);
        assertTrue(result.isEmpty(),
                "tampered signed-key-index bytes must fail verification and return empty");
    }
}
