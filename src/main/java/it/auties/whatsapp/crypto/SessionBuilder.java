package it.auties.whatsapp.crypto;

import it.auties.bytes.Bytes;
import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.controller.WhatsappKeys;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalSignedKeyPair;
import it.auties.whatsapp.model.signal.message.SignalPreKeyMessage;
import it.auties.whatsapp.model.signal.session.*;
import it.auties.whatsapp.util.CipherScheduler;
import it.auties.whatsapp.util.KeyHelper;
import it.auties.whatsapp.util.SignalSpecification;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

@Log // FIXME: 04/06/2022 Remove once bug is fixed
public record SessionBuilder(@NonNull SessionAddress address, @NonNull WhatsappKeys keys) implements SignalSpecification {
    @SneakyThrows
    public void createOutgoing(int id, byte[] identityKey, SignalSignedKeyPair signedPreKey, SignalSignedKeyPair preKey){
        CipherScheduler.run(() -> {
            Validate.isTrue(keys.hasTrust(address, identityKey),
                    "Untrusted key", SecurityException.class);
            Validate.isTrue(Curve25519.verifySignature(KeyHelper.withoutHeader(identityKey), signedPreKey.keyPair().encodedPublicKey(), signedPreKey.signature()),
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
            keys.findSessionByAddress(address)
                    .map(Session::closeCurrentState)
                    .orElseGet(this::createSession)
                    .addState(state);
        });
    }

    private Session createSession() {
        var session = new Session();
        keys.addSession(address, session);
        return session;
    }

    public void createIncoming(Session session, SignalPreKeyMessage message){
        Validate.isTrue(keys.hasTrust(address, message.identityKey()),
                "Untrusted key", SecurityException.class);

        if(session.hasState(message.version(), message.baseKey())){
            return;
        }

        // FIXME: 04/06/2022 message#preKeyId is sometimes null
        var preKeyPair = Optional.ofNullable(message.preKeyId())
                .map(keys::findPreKeyById)
                .orElseGet(() -> {
                    log.warning("Message pre key was null(%s), defaulting to first pre key".formatted(message));
                    return keys.preKeys().getFirst();
                });
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

        var signedSecret = Curve25519.sharedKey(KeyHelper.withoutHeader(theirSignedPubKey),
                keys.identityKeyPair().privateKey());
        var identitySecret = Curve25519.sharedKey(KeyHelper.withoutHeader(theirIdentityPubKey),
                ourSignedKey.privateKey());
        var signedIdentitySecret = Curve25519.sharedKey(KeyHelper.withoutHeader(theirSignedPubKey),
                ourSignedKey.privateKey());
        var ephemeralSecret =  theirEphemeralPubKey == null || ourEphemeralKey == null ? null :
                Curve25519.sharedKey(KeyHelper.withoutHeader(theirEphemeralPubKey), ourEphemeralKey.privateKey());
        var sharedSecret = createStateSecret(isInitiator, signedSecret, identitySecret,
                signedIdentitySecret, ephemeralSecret);
        var masterKey = Hkdf.deriveSecrets(sharedSecret.toByteArray(),
                "WhisperText".getBytes(StandardCharsets.UTF_8));
        var state = createState(isInitiator, ourEphemeralKey, ourSignedKey, theirIdentityPubKey,
                theirEphemeralPubKey, theirSignedPubKey, registrationId, version, masterKey);
        return isInitiator ? calculateSendingRatchet(state, theirSignedPubKey) : state;
    }

    private SessionState calculateSendingRatchet(SessionState state, byte[] theirSignedPubKey) {
        var initSecret = Curve25519.sharedKey(KeyHelper.withoutHeader(theirSignedPubKey),
                state.ephemeralKeyPair().privateKey());
        var initKey = Hkdf.deriveSecrets(initSecret, state.rootKey(),
                "WhisperRatchet".getBytes(StandardCharsets.UTF_8));
        var key = state.ephemeralKeyPair().encodedPublicKey();
        var chain = new SessionChain(-1, initKey[1]);
        return state.addChain(key, chain)
                .rootKey(initKey[0]);
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

    private Bytes createStateSecret(boolean isInitiator, byte[] signedSecret, byte[] identitySecret,
                                    byte[] signedIdentitySecret, byte[] ephemeralSecret) {
        return Bytes.newBuffer(32)
                .fill(0xff)
                .append(isInitiator ? signedSecret : identitySecret)
                .append(isInitiator ? identitySecret : signedSecret)
                .append(signedIdentitySecret)
                .appendNullable(ephemeralSecret);
    }
}
