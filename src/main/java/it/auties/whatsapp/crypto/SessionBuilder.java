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
import static java.util.Arrays.copyOfRange;

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
        var sessionPreKey = new SessionPreKey(preKey == null ? 0 : preKey.id(), baseKey.publicKey(), signedPreKey.id());
        state.pendingPreKey(sessionPreKey);
        var session = keys.findSessionByAddress(address)
                .map(lastSession -> lastSession.fresh(false).promoteState(state))
                .orElseGet(Session::new);
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
        var signedKeyPair = keys.findSignedKeyPairById(message.signedPreKeyId());
        session.fresh(false);
        var nextState = createState(false, preKeyPair == null ? null : preKeyPair.toGenericKeyPair(), signedKeyPair.toGenericKeyPair(),
                message.identityKey(), message.baseKey(),
                null, message.registrationId(), message.version());
        session.promoteState(nextState);
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
        var a1 = Curve.calculateSharedSecret(theirSignedPubKey, ourIdentityKey.privateKey());
        var a2 = Curve.calculateSharedSecret(theirIdentityPubKey, ourSignedKey.privateKey());
        var a3 = Curve.calculateSharedSecret(theirSignedPubKey, ourSignedKey.privateKey());
        sharedSecret.write(a1.data());
        sharedSecret.write(a2.data());
        sharedSecret.write(a3.data());
        if (ourEphemeralKey != null && theirEphemeralPubKey != null) {
            var a4 = Curve.calculateSharedSecret(theirEphemeralPubKey, ourEphemeralKey.privateKey());
            sharedSecret.write(a4.data());
        }

        var masterKey = Hkdf.deriveSecrets(sharedSecret.toByteArray(), "WhisperText".getBytes(StandardCharsets.UTF_8), 96);
        var state = new SessionState()
                .version(version)
                .localIdentityPublic(theirSignedPubKey)
                .localRegistrationId(registrationId)
                .rootKey(copyOfRange(masterKey,0, 32))
                .senderChain(isInitiator ? SignalKeyPair.random() : ourSignedKey, new SessionChainKey())
                .baseKey(isInitiator ? ourEphemeralKey.publicKey() : theirEphemeralPubKey)
                .remoteIdentityKey(theirIdentityPubKey)
                .previousCounter(0);
        if (!isInitiator) {
            return state;
        }

        var initSecret = Curve.calculateSharedSecret(theirSignedPubKey, state.senderChain().privateKey());
        var initKey =  Hkdf.deriveSecrets(initSecret.data(), state.rootKey(),
                "WhisperRatchet".getBytes(StandardCharsets.UTF_8), 96);
        var initChain = new SessionChainKey(-1, copyOfRange(initKey, 32, 64));
        state.addReceiverChain(state.senderChain().publicKey(), initChain);
        state.rootKey(copyOfRange(initKey, 0, 32));
        return state;
    }

    private ByteArrayOutputStream createSharedSecret(SignalKeyPair ourEphemeralKey, byte[] theirEphemeralPubKey){
        var size = ourEphemeralKey == null || theirEphemeralPubKey == null ? 128 : 160;
        return allocate(size)
                .fill((byte) 0xff)
                .toOutputStream();
    }
}
