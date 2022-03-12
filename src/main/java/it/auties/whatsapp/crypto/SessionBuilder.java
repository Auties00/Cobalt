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

import static it.auties.curve25519.Curve25519.calculateAgreement;

public record SessionBuilder(@NonNull SessionAddress address, @NonNull WhatsappKeys keys) implements SignalSpecification {
    public void createOutgoing(int id, byte[] identityKey, SignalSignedKeyPair signedPreKey, SignalSignedKeyPair preKey){
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
                .orElseGet(Session::new)
                .addState(state);
        keys.addSession(address, session);
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
                "Invalid pre key id: %s", message.preKeyId());

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

        var signedSecret = calculateAgreement(Keys.withoutHeader(theirSignedPubKey), keys.identityKeyPair().privateKey());
        var identitySecret = calculateAgreement(Keys.withoutHeader(theirIdentityPubKey), ourSignedKey.privateKey());
        var sharedSecret = Bytes.newBuffer(32)
                .fill(0xff)
                .append(isInitiator ? signedSecret : identitySecret)
                .append(isInitiator ? identitySecret : signedSecret)
                .append(calculateAgreement(Keys.withoutHeader(theirSignedPubKey), ourSignedKey.privateKey()));
        if (ourEphemeralKey != null && theirEphemeralPubKey != null) {
            var ephemeralSecret = calculateAgreement(Keys.withoutHeader(theirEphemeralPubKey), ourEphemeralKey.privateKey());
            sharedSecret = sharedSecret.append(ephemeralSecret);
        }

        var masterKeyInput = sharedSecret
                .assertSize(ourEphemeralKey == null || theirEphemeralPubKey == null ? 128 : 160)
                .toByteArray();
        var masterKey = Hkdf.deriveSecrets(masterKeyInput,
                "WhisperText".getBytes(StandardCharsets.UTF_8));
        var state = SessionState.builder()
                .version(version)
                .rootKey(masterKey[0])
                .ephemeralKeyPair(isInitiator ? SignalKeyPair.random() : ourSignedKey)
                .lastRemoteEphemeralKey(Objects.requireNonNull(theirSignedPubKey))
                .remoteIdentityKey(theirIdentityPubKey)
                .baseKey(isInitiator ? ourEphemeralKey.encodedPublicKey() : theirEphemeralPubKey)
                .build();
        if (!isInitiator) {
            return state;
        }

        var initSecret = calculateAgreement(Keys.withoutHeader(theirSignedPubKey), state.ephemeralKeyPair().privateKey());
        var initKey = Hkdf.deriveSecrets(initSecret, state.rootKey(),
                "WhisperRatchet".getBytes(StandardCharsets.UTF_8));
        var chain = new SessionChain(-1, masterKey[1], state.ephemeralKeyPair().publicKey());
        return state.addChain(chain)
                .registrationId(registrationId)
                .rootKey(initKey[0]);
    }
}
