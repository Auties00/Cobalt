package it.auties.whatsapp.crypto;

import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.protobuf.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.protobuf.signal.keypair.SignalSignedKeyPair;
import it.auties.whatsapp.protobuf.signal.message.SignalPreKeyMessage;
import it.auties.whatsapp.protobuf.signal.session.*;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static it.auties.whatsapp.binary.BinaryArray.allocate;

public record SessionBuilder(@NonNull SessionAddress address, @NonNull WhatsappKeys keys) {
    private static final int CURRENT_VERSION = 3;

    public void createOutgoing(int id, byte[] identityKey, SignalSignedKeyPair signedPreKey, SignalSignedKeyPair preKey){
        Validate.isTrue(keys.hasTrust(address, identityKey),
                "Untrusted key", SecurityException.class);
        Validate.isTrue(Curve.verifySignature(identityKey, signedPreKey.keyPair().publicKey(), signedPreKey.signature()),
                "Signature mismatch", SecurityException.class);

        var baseKey = SignalKeyPair.random();
        var state = createState(true, baseKey, null,
                identityKey, preKey == null ? null : preKey.keyPair().publicKey(),
                signedPreKey.keyPair().publicKey(), id, CURRENT_VERSION);

        var pendingPreKey = new SessionPreKey(preKey == null ? 0 : preKey.id(), baseKey.publicKey(), signedPreKey.id());
        state.pendingPreKey(pendingPreKey);

        var session = keys.findSessionByAddress(address)
                .map(Session::closeCurrentState)
                .orElseGet(Session::new)
                .addState(state);
        keys.addSession(address, session);
    }

    public int createIncoming(Session session, SignalPreKeyMessage message){
        Validate.isTrue(keys.hasTrust(address, message.identityKey()),
                "Untrusted key", SecurityException.class);
        if(session.hasState(message.version(), message.baseKey())){
            return 0;
        }

        var preKeyPair = keys.findPreKeyById(message.preKeyId())
                .orElse(null);
        Validate.isTrue(message.preKeyId() == 0 || preKeyPair != null,
                "Invalid pre key id: %s", message.preKeyId());

        session.closeCurrentState();
        var nextState = createState(false, preKeyPair != null ? preKeyPair.toGenericKeyPair() : null,
                keys.findSignedKeyPairById(message.signedPreKeyId()).toGenericKeyPair(),
                message.identityKey(), message.baseKey(),
                null, message.registrationId(), message.version());

        session.addState(nextState);
        return message.preKeyId();
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

        var sharedSecret = createSharedSecret(ourEphemeralKey, theirEphemeralPubKey);
        var ourIdentityKey = keys.identityKeyPair();
        var first = Curve.calculateSharedSecret(theirSignedPubKey, ourIdentityKey.privateKey());
        var second = Curve.calculateSharedSecret(theirIdentityPubKey, ourSignedKey.privateKey());
        var third = Curve.calculateSharedSecret(theirSignedPubKey, ourSignedKey.privateKey());
        sharedSecret.write(isInitiator ? first.data() : second.data());
        sharedSecret.write(isInitiator ? second.data() : first.data());
        sharedSecret.write(third.data());
        if (ourEphemeralKey != null && theirEphemeralPubKey != null) {
            var fourth = Curve.calculateSharedSecret(theirEphemeralPubKey, ourEphemeralKey.privateKey());
            sharedSecret.write(fourth.data());
        }

        var masterKey = Hkdf.deriveSecrets(sharedSecret.toByteArray(),
                "WhisperText".getBytes(StandardCharsets.UTF_8));
        var state = SessionState.builder()
                .version(version)
                .rootKey(masterKey[0])
                .ephemeralKeyPair(isInitiator ? SignalKeyPair.random() : ourSignedKey.toGenericKeyPair())
                .lastRemoteEphemeralKey(theirSignedPubKey)
                .remoteIdentityKey(theirIdentityPubKey)
                .baseKey(isInitiator ? ourEphemeralKey.publicKey() : theirEphemeralPubKey)
                .build();
        if (!isInitiator) {
            return state;
        }

        var initSecret = Curve.calculateSharedSecret(theirSignedPubKey, state.ephemeralKeyPair().privateKey());
        var initKey = Hkdf.deriveSecrets(initSecret.data(), state.rootKey(),
                "WhisperRatchet".getBytes(StandardCharsets.UTF_8));
        var chain = new SessionChain(-1, masterKey[1], state.ephemeralKeyPair().publicKey());
        return state.addChain(chain)
                .rootKey(initKey[0]);
    }

    private ByteArrayOutputStream createSharedSecret(SignalKeyPair ourEphemeralKey, byte[] theirEphemeralPubKey){
        var size = ourEphemeralKey == null || theirEphemeralPubKey == null ? 128 : 160;
        return allocate(size)
                .fill((byte) 0xff, 32)
                .toOutputStream();
    }
}
