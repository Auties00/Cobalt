package it.auties.whatsapp.crypto;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.model.signal.keypair.ISignalKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalSignedKeyPair;
import it.auties.whatsapp.model.signal.message.SignalPreKeyMessage;
import it.auties.whatsapp.model.signal.session.*;
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.SignalConstants;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public record SessionBuilder(SessionAddress address, Keys keys) {
    public void createOutgoing(int id, byte[] identityKey, SignalSignedKeyPair signedPreKey, SignalSignedKeyPair preKey) {
        if (!keys.hasTrust(address, identityKey)) {
            throw new IllegalArgumentException("Untrusted key");
        }
        if (!Curve25519.verifySignature(ISignalKeyPair.toCurveKey(identityKey), signedPreKey.keyPair().signalPublicKey(), signedPreKey.signature())) {
            throw new IllegalArgumentException("Signature mismatch");
        }
        var baseKey = SignalKeyPair.random();
        var state = createState(true,
                baseKey,
                null,
                identityKey,
                preKey == null ? null : preKey.keyPair().signalPublicKey(),
                signedPreKey.keyPair().signalPublicKey(),
                id,
                SignalConstants.CURRENT_VERSION
        );
        var pendingPreKey = new SessionPreKey(
                preKey == null ? null : preKey.id(),
                baseKey.signalPublicKey(),
                signedPreKey.id()
        );
        state.pendingPreKey(pendingPreKey);
        keys.findSessionByAddress(address)
                .map(Session::closeCurrentState)
                .orElseGet(this::createSession)
                .addState(state);
    }

    public SessionState createState(boolean isInitiator, SignalKeyPair ourEphemeralKey, SignalKeyPair ourSignedKey, byte[] theirIdentityPubKey, byte[] theirEphemeralPubKey, byte[] theirSignedPubKey, int registrationId, int version) {
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
        var signedSecret = Curve25519.sharedKey(ISignalKeyPair.toCurveKey(theirSignedPubKey), keys.identityKeyPair()
                .privateKey());
        var identitySecret = Curve25519.sharedKey(ISignalKeyPair.toCurveKey(theirIdentityPubKey), ourSignedKey.privateKey());
        var signedIdentitySecret = Curve25519.sharedKey(ISignalKeyPair.toCurveKey(theirSignedPubKey), ourSignedKey.privateKey());
        var ephemeralSecret = theirEphemeralPubKey == null || ourEphemeralKey == null ? null : Curve25519.sharedKey(ISignalKeyPair.toCurveKey(theirEphemeralPubKey), ourEphemeralKey.privateKey());
        var sharedSecret = createStateSecret(isInitiator, signedSecret, identitySecret, signedIdentitySecret, ephemeralSecret);
        var masterKey = Hkdf.deriveSecrets(sharedSecret, "WhisperText".getBytes(StandardCharsets.UTF_8));
        var state = createState(isInitiator, ourEphemeralKey, ourSignedKey, theirIdentityPubKey, theirEphemeralPubKey, theirSignedPubKey, registrationId, version, masterKey);
        return isInitiator ? calculateSendingRatchet(state, theirSignedPubKey) : state;
    }

    private Session createSession() {
        var session = new Session();
        keys.putSession(address, session);
        return session;
    }

    private byte[] createStateSecret(boolean isInitiator, byte[] signedSecret, byte[] identitySecret, byte[] signedIdentitySecret, byte[] ephemeralSecret) {
        var header = new byte[32];
        Arrays.fill(header, (byte) 0xff);
        return Bytes.concat(
                header,
                isInitiator ? signedSecret : identitySecret,
                isInitiator ? identitySecret : signedSecret,
                signedIdentitySecret,
                ephemeralSecret
        );
    }

    private SessionState createState(boolean isInitiator, SignalKeyPair ourEphemeralKey, SignalKeyPair ourSignedKey, byte[] theirIdentityPubKey, byte[] theirEphemeralPubKey, byte[] theirSignedPubKey, int registrationId, int version, byte[][] masterKey) {
        return new SessionState(
                version,
                registrationId,
                isInitiator ? ourEphemeralKey.signalPublicKey() : theirEphemeralPubKey,
                theirIdentityPubKey,
                new ConcurrentHashMap<>(),
                masterKey[0],
                null,
                isInitiator ? SignalKeyPair.random() : ourSignedKey,
                Objects.requireNonNull(theirSignedPubKey),
                0,
                false
        );
    }

    private SessionState calculateSendingRatchet(SessionState state, byte[] theirSignedPubKey) {
        var initSecret = Curve25519.sharedKey(ISignalKeyPair.toCurveKey(theirSignedPubKey), state.ephemeralKeyPair()
                .privateKey());
        var initKey = Hkdf.deriveSecrets(initSecret, state.rootKey(), "WhisperRatchet".getBytes(StandardCharsets.UTF_8));
        var key = state.ephemeralKeyPair().signalPublicKey();
        var chain = new SessionChain(-1, initKey[1]);
        return state.addChain(key, chain).rootKey(initKey[0]);
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
                preKeyPair != null ? preKeyPair.toGenericKeyPair() : null,
                signedKeyPair.toGenericKeyPair(),
                message.identityKey(),
                message.baseKey(),
                null,
                message.registrationId(),
                message.version()
        );
        session.addState(nextState);
    }
}
