package it.auties.whatsapp.crypto;

import it.auties.bytes.Bytes;
import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.controller.WhatsappKeys;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalSignedKeyPair;
import it.auties.whatsapp.model.signal.message.SignalPreKeyMessage;
import it.auties.whatsapp.model.signal.session.*;
import it.auties.whatsapp.util.Keys;
import it.auties.whatsapp.util.SignalSpecification;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Semaphore;

public record SessionBuilder(@NonNull SessionAddress address, @NonNull WhatsappKeys keys) implements SignalSpecification {
    private static final Semaphore ENCRYPTION_SEMAPHORE = new Semaphore(1);

    public void createOutgoing(int id, byte[] identityKey, SignalSignedKeyPair signedPreKey, SignalSignedKeyPair preKey){
        try {
            ENCRYPTION_SEMAPHORE.acquire();
            Validate.isTrue(keys.hasTrust(address, identityKey),
                    "Untrusted key", SecurityException.class);
            Validate.isTrue(Curve25519.verifySignature(Keys.withoutHeader(identityKey), signedPreKey.keyPair().encodedPublicKey(), signedPreKey.signature()),
                    "Signature mismatch", SecurityException.class);

            var baseKey = SignalKeyPair.random();
            var state = createState(
                    true,
                    baseKey,
                    null,
                    identityKey,
                    preKey == null ? null : preKey.keyPair().encodedPublicKey(),
                    signedPreKey.keyPair().encodedPublicKey(),
                    id,
                    CURRENT_VERSION
            );

            var pendingPreKey = new SessionPreKey(preKey == null ? 0 : preKey.id(), baseKey.encodedPublicKey(), signedPreKey.id());
            state.pendingPreKey(pendingPreKey);

            var session = keys.findSessionByAddress(address)
                    .map(Session::closeCurrentState)
                    .orElseGet(Session::new);

            session.addState(state);
            keys.addSession(address, session);
        }catch (Throwable throwable){
            throw new RuntimeException("Cannot create outgoing: an exception occured", throwable);
        }finally {
            ENCRYPTION_SEMAPHORE.release();
        }
    }

    public void createIncoming(Session session, SignalPreKeyMessage message){
        Validate.isTrue(keys.hasTrust(address, message.identityKey()),
                "Untrusted key", SecurityException.class);

        if(session.hasState(message.version(), message.baseKey())){
            return;
        }

        var preKeyPair = keys.findPreKeyById(message.preKeyId())
                .orElse(null);
        Validate.isTrue(message.preKeyId() == 0 || preKeyPair != null,
                "Invalid pre key id: %s", SecurityException.class, message.preKeyId());

        var signedPreKeyPair = keys.findSignedKeyPairById(message.signedPreKeyId());
        session.closeCurrentState();
        var nextState = createState(
                false,
                preKeyPair != null ? preKeyPair.toGenericKeyPair() : null,
                signedPreKeyPair.toGenericKeyPair(),
                message.identityKey(),
                message.baseKey(),
                null,
                message.registrationId(),
                message.version()
        );

        session.addState(nextState);
    }

    @SneakyThrows
    public SessionState createState(boolean isInitiator, SignalKeyPair ourEphemeralKey, SignalKeyPair ourSignedKey, byte[] theirIdentityPubKey,
                                    byte[] theirEphemeralPubKey, byte[] theirSignedPubKey, int registrationId, int version){
        if(isInitiator){
            Validate.isTrue(ourSignedKey == null, "Our signed key should be null");
            ourSignedKey = ourEphemeralKey;
        }else {
            Validate.isTrue(theirSignedPubKey == null, "Their signed public key should be null");
            theirSignedPubKey = theirEphemeralPubKey;
        }

        var signedSecret = Curve25519.sharedKey(Keys.withoutHeader(theirSignedPubKey),
                keys.identityKeyPair().privateKey());
        var identitySecret = Curve25519.sharedKey(Keys.withoutHeader(theirIdentityPubKey),
                ourSignedKey.privateKey());
        var sharedSecret = createStateSecret(isInitiator, ourEphemeralKey, ourSignedKey,
                theirEphemeralPubKey, theirSignedPubKey, signedSecret, identitySecret);
        var masterKey = Hkdf.deriveSecrets(sharedSecret.toByteArray(),
                "WhisperText".getBytes(StandardCharsets.UTF_8));
        var state = createState(isInitiator, ourEphemeralKey, ourSignedKey, theirIdentityPubKey,
                theirEphemeralPubKey, theirSignedPubKey, registrationId, version, masterKey);
        return isInitiator ? calculateSendingRatchet(state, theirSignedPubKey) : state;
    }

    private SessionState calculateSendingRatchet(SessionState state, byte[] theirSignedPubKey) {
        var initSecret = Curve25519.sharedKey(Keys.withoutHeader(theirSignedPubKey),
                state.ephemeralKeyPair().privateKey());
        var initKey = Hkdf.deriveSecrets(initSecret, state.rootKey(),
                "WhisperRatchet".getBytes(StandardCharsets.UTF_8));
        var chain = new SessionChain(-1, initKey[1]);
        return state.addChain(state.ephemeralKeyPair().encodedPublicKey(), chain).rootKey(initKey[0]);
    }

    private SessionState createState(boolean isInitiator, SignalKeyPair ourEphemeralKey, SignalKeyPair ourSignedKey,
                                     byte[] theirIdentityPubKey, byte[] theirEphemeralPubKey, byte[] theirSignedPubKey,
                                     int registrationId, int version, byte[][] masterKey) {
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

    private Bytes createStateSecret(boolean isInitiator, SignalKeyPair ourEphemeralKey, SignalKeyPair ourSignedKey,
                                    byte[] theirEphemeralPubKey, byte[] theirSignedPubKey, byte[] signedSecret, byte[] identitySecret) {
        var sharedSecret = Bytes.newBuffer(32)
                .fill(0xff)
                .append(isInitiator ? signedSecret : identitySecret)
                .append(isInitiator ? identitySecret : signedSecret)
                .append(Curve25519.sharedKey(Keys.withoutHeader(theirSignedPubKey), ourSignedKey.privateKey()));
        if (ourEphemeralKey == null || theirEphemeralPubKey == null) {
            return sharedSecret;
        }

        var additional = Curve25519.sharedKey(Keys.withoutHeader(theirEphemeralPubKey),
                ourEphemeralKey.privateKey());
        return sharedSecret.append(additional);
    }
}
