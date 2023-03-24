package it.auties.whatsapp.crypto;

import it.auties.bytes.Bytes;
import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalSignedKeyPair;
import it.auties.whatsapp.model.signal.message.SignalPreKeyMessage;
import it.auties.whatsapp.model.signal.session.*;
import it.auties.whatsapp.util.KeyHelper;
import it.auties.whatsapp.util.Spec;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Objects;

public record SessionBuilder(@NonNull SessionAddress address, @NonNull Keys keys) {
    public void createOutgoing(int id, byte[] identityKey, SignalSignedKeyPair signedPreKey, SignalSignedKeyPair preKey) {
        Validate.isTrue(keys.hasTrust(address, identityKey), "Untrusted key", SecurityException.class);
        Validate.isTrue(Curve25519.verifySignature(KeyHelper.withoutHeader(identityKey), signedPreKey.keyPair()
                .encodedPublicKey(), signedPreKey.signature()), "Signature mismatch", SecurityException.class);
        var baseKey = SignalKeyPair.random();
        var state = createState(true, baseKey, null, identityKey, preKey == null ? null : preKey.keyPair()
                .encodedPublicKey(), signedPreKey.keyPair()
                .encodedPublicKey(), id, Spec.Signal.CURRENT_VERSION);
        var pendingPreKey = new SessionPreKey(preKey == null ? 0 : preKey.id(), baseKey.encodedPublicKey(), signedPreKey.id());
        state.pendingPreKey(pendingPreKey);
        keys.findSessionByAddress(address)
                .map(Session::closeCurrentState)
                .orElseGet(this::createSession)
                .addState(state);
    }

    public SessionState createState(boolean isInitiator, SignalKeyPair ourEphemeralKey, SignalKeyPair ourSignedKey, byte[] theirIdentityPubKey, byte[] theirEphemeralPubKey, byte[] theirSignedPubKey, int registrationId, int version) {
        if (isInitiator) {
            Validate.isTrue(ourSignedKey == null, "Our signed key should be null");
            ourSignedKey = ourEphemeralKey;
        } else {
            Validate.isTrue(theirSignedPubKey == null, "Their signed public key should be null");
            theirSignedPubKey = theirEphemeralPubKey;
        }
        var signedSecret = Curve25519.sharedKey(KeyHelper.withoutHeader(theirSignedPubKey), keys.identityKeyPair()
                .privateKey());
        var identitySecret = Curve25519.sharedKey(KeyHelper.withoutHeader(theirIdentityPubKey), ourSignedKey.privateKey());
        var signedIdentitySecret = Curve25519.sharedKey(KeyHelper.withoutHeader(theirSignedPubKey), ourSignedKey.privateKey());
        var ephemeralSecret = theirEphemeralPubKey == null || ourEphemeralKey == null ? null : Curve25519.sharedKey(KeyHelper.withoutHeader(theirEphemeralPubKey), ourEphemeralKey.privateKey());
        var sharedSecret = createStateSecret(isInitiator, signedSecret, identitySecret, signedIdentitySecret, ephemeralSecret);
        var masterKey = Hkdf.deriveSecrets(sharedSecret.toByteArray(), "WhisperText".getBytes(StandardCharsets.UTF_8));
        var state = createState(isInitiator, ourEphemeralKey, ourSignedKey, theirIdentityPubKey, theirEphemeralPubKey, theirSignedPubKey, registrationId, version, masterKey);
        return isInitiator ? calculateSendingRatchet(state, theirSignedPubKey) : state;
    }

    private Session createSession() {
        var session = new Session();
        keys.putSession(address, session);
        return session;
    }

    private Bytes createStateSecret(boolean isInitiator, byte[] signedSecret, byte[] identitySecret, byte[] signedIdentitySecret, byte[] ephemeralSecret) {
        return Bytes.newBuffer(32)
                .fill(0xff)
                .append(isInitiator ? signedSecret : identitySecret)
                .append(isInitiator ? identitySecret : signedSecret)
                .append(signedIdentitySecret)
                .appendNullable(ephemeralSecret);
    }

    private SessionState createState(boolean isInitiator, SignalKeyPair ourEphemeralKey, SignalKeyPair ourSignedKey, byte[] theirIdentityPubKey, byte[] theirEphemeralPubKey, byte[] theirSignedPubKey, int registrationId, int version, byte[][] masterKey) {
        return SessionState.builder()
                .version(version)
                .registrationId(registrationId)
                .rootKey(masterKey[0])
                .ephemeralKeyPair(isInitiator ? SignalKeyPair.random() : ourSignedKey)
                .lastRemoteEphemeralKey(Objects.requireNonNull(theirSignedPubKey))
                .previousCounter(0)
                .remoteIdentityKey(theirIdentityPubKey)
                .baseKey(isInitiator ? ourEphemeralKey.encodedPublicKey() : theirEphemeralPubKey)
                .closed(false)
                .build();
    }

    private SessionState calculateSendingRatchet(SessionState state, byte[] theirSignedPubKey) {
        var initSecret = Curve25519.sharedKey(KeyHelper.withoutHeader(theirSignedPubKey), state.ephemeralKeyPair()
                .privateKey());
        var initKey = Hkdf.deriveSecrets(initSecret, state.rootKey(), "WhisperRatchet".getBytes(StandardCharsets.UTF_8));
        var key = state.ephemeralKeyPair().encodedPublicKey();
        var chain = new SessionChain(-1, initKey[1]);
        return state.addChain(key, chain).rootKey(initKey[0]);
    }

    public void createIncoming(Session session, SignalPreKeyMessage message) {
        Validate.isTrue(keys.hasTrust(address, message.identityKey()), "Untrusted key", SecurityException.class);
        if (session.hasState(message.version(), message.baseKey())) {
            return;
        }
        var preKeyPair = keys.findPreKeyById(message.preKeyId()).orElse(null);
        Validate.isTrue(message.preKeyId() == null || preKeyPair != null, "Invalid pre key id: %s", SecurityException.class, message.preKeyId());
        var signedPreKeyPair = keys.findSignedKeyPairById(message.signedPreKeyId())
                .orElseThrow(() -> new NoSuchElementException("Cannot find signed pre key with id %s".formatted(message.signedPreKeyId())));
        session.closeCurrentState();
        var nextState = createState(false, preKeyPair != null ? preKeyPair.toGenericKeyPair() : null, signedPreKeyPair.toGenericKeyPair(), message.identityKey(), message.baseKey(), null, message.registrationId(), message.version());
        session.addState(nextState);
    }
}
