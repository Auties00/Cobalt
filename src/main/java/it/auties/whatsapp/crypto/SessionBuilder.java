package it.auties.whatsapp.crypto;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalSignedKeyPair;
import it.auties.whatsapp.model.signal.message.SignalPreKeyMessage;
import it.auties.whatsapp.model.signal.session.*;
import it.auties.whatsapp.util.SignalConstants;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import static it.auties.whatsapp.util.SignalConstants.KEY_LENGTH;

public record SessionBuilder(SessionAddress address, Keys keys) {
    public void createOutgoing(int id, byte[] identityKey, SignalSignedKeyPair signedPreKey, SignalSignedKeyPair preKey) {
        if (!keys.hasTrust(address, identityKey)) {
            throw new IllegalArgumentException("Untrusted key");
        }
        if (!Curve25519.verifySignature(SignalConstants.createCurveKey(identityKey), signedPreKey.signalPublicKey(), signedPreKey.signature())) {
            throw new IllegalArgumentException("Signature mismatch");
        }
        var baseKey = SignalKeyPair.random();
        var state = createState(
                true,
                baseKey,
                null,
                identityKey,
                preKey == null ? null : preKey.signalPublicKey(),
                signedPreKey.signalPublicKey(),
                id,
                SignalConstants.CURRENT_VERSION
        );
        var pendingPreKey = new SessionPreKey(
                preKey == null ? null : preKey.id(),
                baseKey.signalPublicKey(),
                signedPreKey.id()
        );
        state.pendingPreKey(pendingPreKey);
        var session = keys.findSessionByAddress(address)
                .orElse(null);
        if (session != null) {
            session.closeCurrentState();
            session.addState(state);
        }else {
            var newSession = new Session();
            keys.addSession(address, newSession);
            newSession.addState(state);
        }
    }

    private byte[][] computeMasterKey(boolean isInitiator, byte[] signedSecret, byte[] identitySecret, byte[] signedIdentitySecret, byte[] ephemeralSecret) {
        var result = new byte[KEY_LENGTH + signedSecret.length + identitySecret.length + signedIdentitySecret.length + (ephemeralSecret == null ? 0 : ephemeralSecret.length)];
        Arrays.fill(result, 0, KEY_LENGTH, (byte) 0xff);
        if(isInitiator) {
            System.arraycopy(signedSecret, 0, result, KEY_LENGTH, signedSecret.length);
            System.arraycopy(identitySecret, 0, result, KEY_LENGTH + signedSecret.length, identitySecret.length);
        } else {
            System.arraycopy(identitySecret, 0, result, KEY_LENGTH, identitySecret.length);
            System.arraycopy(signedSecret, 0, result, KEY_LENGTH + identitySecret.length, signedSecret.length);
        }
        System.arraycopy(signedIdentitySecret, 0, result, KEY_LENGTH + signedSecret.length + identitySecret.length, signedIdentitySecret.length);
        if(ephemeralSecret != null) {
            System.arraycopy(ephemeralSecret, 0, result, KEY_LENGTH + signedSecret.length + identitySecret.length + signedIdentitySecret.length, ephemeralSecret.length);
        }
        return Hkdf.deriveSecrets(result, "WhisperText".getBytes(StandardCharsets.UTF_8));
    }

    private SessionState calculateSendingRatchet(SessionState state, byte[] theirSignedPubKey) {
        var initSecret = Curve25519.sharedKey(SignalConstants.createCurveKey(theirSignedPubKey), state.ephemeralKeyPair().privateKey());
        var initKey = Hkdf.deriveSecrets(initSecret, state.rootKey(), "WhisperRatchet".getBytes(StandardCharsets.UTF_8));
        var key = state.ephemeralKeyPair().signalPublicKey();
        var chain = new SessionChain(-1, initKey[1]);
        return state.addChain(key, chain)
                .setRootKey(initKey[0]);
    }

    public void createIncoming(Session session, SignalPreKeyMessage message) {
        if (!keys.hasTrust(address, message.identityKey())) {
            throw new IllegalArgumentException("Untrusted key");
        }
        if (session.hasState(message.version(), message.baseKey())) {
            return;
        }
        var preKeyPair = keys.findPreKeyById(message.preKeyId())
                .orElse(null);
        var signedKeyPair = keys.findSignedKeyPairById(message.signedPreKeyId())
                .orElseThrow(() -> new NoSuchElementException("Cannot find signed key with id %s".formatted(message.signedPreKeyId())));
        session.closeCurrentState();
        var nextState = createState(
                false,
                preKeyPair == null ? null : preKeyPair.toGenericKeyPair(),
                signedKeyPair.toGenericKeyPair(),
                message.identityKey(),
                message.baseKey(),
                null,
                message.registrationId(),
                message.version()
        );
        session.addState(nextState);
    }

    private SessionState createState(
            boolean isInitiator,
            SignalKeyPair ourEphemeralKey,
            SignalKeyPair ourSignedKey,
            byte[] theirIdentityPubKey, byte[] theirEphemeralPubKey, byte[] theirSignedPubKey,
            int registrationId, int version
    ) {
        if (isInitiator) {
            if (ourSignedKey != null) {
                throw new IllegalArgumentException("Our signed key should be null");
            }
            ourSignedKey = ourEphemeralKey;
        } else {
            if (theirSignedPubKey != null) {
                throw new IllegalArgumentException("Their signed public key should be null");
            }
            theirSignedPubKey = theirEphemeralPubKey;
        }
        var signedSecret = Curve25519.sharedKey(SignalConstants.createCurveKey(theirSignedPubKey), keys.identityKeyPair().privateKey());
        var identitySecret = Curve25519.sharedKey(SignalConstants.createCurveKey(theirIdentityPubKey), ourSignedKey.privateKey());
        var signedIdentitySecret = Curve25519.sharedKey(SignalConstants.createCurveKey(theirSignedPubKey), ourSignedKey.privateKey());
        var ephemeralSecret = theirEphemeralPubKey == null || ourEphemeralKey == null ? null : Curve25519.sharedKey(SignalConstants.createCurveKey(theirEphemeralPubKey), ourEphemeralKey.privateKey());
        var masterKey = computeMasterKey(isInitiator, signedSecret, identitySecret, signedIdentitySecret, ephemeralSecret);
        var state = new SessionState(
                version,
                registrationId,
                isInitiator ? ourEphemeralKey.signalPublicKey() : theirEphemeralPubKey,
                theirIdentityPubKey,
                new ConcurrentHashMap<>(),
                masterKey[0],
                null,
                isInitiator ? SignalKeyPair.random() : ourSignedKey,
                theirSignedPubKey,
                0,
                false
        );
        if (isInitiator) {
            return calculateSendingRatchet(state, theirSignedPubKey);
        }else {
            return state;
        }
    }
}
