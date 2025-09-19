package it.auties.whatsapp.socket.message.signal;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.model.signal.SignalProtocol;
import it.auties.whatsapp.model.signal.key.SignalKeyPair;
import it.auties.whatsapp.model.signal.key.SignalPrivateKey;
import it.auties.whatsapp.model.signal.key.SignalPublicKey;
import it.auties.whatsapp.model.signal.parameters.AliceSignalProtocolParametersBuilder;
import it.auties.whatsapp.model.signal.parameters.BobSignalProtocolParametersBuilder;
import it.auties.whatsapp.model.signal.ratchet.SignalAliceParameters;
import it.auties.whatsapp.model.signal.ratchet.SignalBobParameters;
import it.auties.whatsapp.model.signal.ratchet.SignalChainKeyBuilder;
import it.auties.whatsapp.model.signal.ratchet.SignalSymmetricParameters;
import it.auties.whatsapp.model.signal.state.SignalSessionChainBuilder;
import it.auties.whatsapp.model.signal.state.SignalSessionState;

import javax.crypto.KDF;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public class SignalRatchetingSession {

    public static final int DISCONTINUITY_BYTES_LENGTH = 32;
    private static final int KEY_AGREEMENT_LENGTH = 32;

    public static void initializeSession(SignalSessionState sessionState, SignalSymmetricParameters parameters) {
        if (isAlice(parameters.ourBaseKey().publicKey(), parameters.theirBaseKey())) {
            var aliceParameters = new AliceSignalProtocolParametersBuilder()
                    .ourBaseKey(parameters.ourBaseKey())
                    .ourIdentityKey(parameters.ourIdentityKey())
                    .theirRatchetKey(parameters.theirRatchetKey())
                    .theirIdentityKey(parameters.theirIdentityKey())
                    .theirSignedPreKey(parameters.theirBaseKey())
                    .build();
            initializeSession(sessionState, aliceParameters);
        } else {
            var bobParameters = new BobSignalProtocolParametersBuilder()
                    .ourIdentityKey(parameters.ourIdentityKey())
                    .ourRatchetKey(parameters.ourRatchetKey())
                    .ourSignedPreKey(parameters.ourBaseKey())
                    .theirBaseKey(parameters.theirBaseKey())
                    .theirIdentityKey(parameters.theirIdentityKey())
                    .build();
            initializeSession(sessionState, bobParameters);
        }
    }

    public static void initializeSession(SignalSessionState sessionState, SignalAliceParameters parameters) {
        try {
            var kdf = KDF.getInstance("HKDF-SHA256");

            sessionState.setSessionVersion(SignalProtocol.CURRENT_VERSION);
            sessionState.setRemoteIdentityPublic(parameters.theirIdentityKey());
            sessionState.setLocalIdentityPublic(parameters.ourIdentityKey().publicKey());

            var sendingRatchetKey = SignalKeyPair.random();
            var hasOneTimePreKey = parameters.theirOneTimePreKey() != null;
            var offset = 0;
            var secrets = new byte[DISCONTINUITY_BYTES_LENGTH + KEY_AGREEMENT_LENGTH + KEY_AGREEMENT_LENGTH + KEY_AGREEMENT_LENGTH + (hasOneTimePreKey ? KEY_AGREEMENT_LENGTH : 0)];
            offset = writeDiscontinuityBytes(secrets, offset);
            offset = writeKeyAgreement(parameters.theirSignedPreKey(), parameters.ourIdentityKey().privateKey(), secrets, offset);
            offset = writeKeyAgreement(parameters.theirIdentityKey(), parameters.ourBaseKey().privateKey(), secrets, offset);
            offset = writeKeyAgreement(parameters.theirSignedPreKey(), parameters.ourBaseKey().privateKey(), secrets, offset);
            if (hasOneTimePreKey) {
                offset = writeKeyAgreement(parameters.theirOneTimePreKey(), parameters.ourBaseKey().privateKey(), secrets, offset);
            }

            if(offset != secrets.length) {
                throw new InternalError("Offset is not equal to the length of the array");
            }

            var receiverParams = HKDFParameterSpec.ofExtract()
                    .addIKM(secrets)
                    .thenExpand("WhisperText".getBytes(StandardCharsets.UTF_8), 64);
            var receiverDerivedSecrets = kdf.deriveData(receiverParams);
            var receiverRootKeyData = Arrays.copyOfRange(receiverDerivedSecrets, 0, 32);
            var receiverChainKeyData = Arrays.copyOfRange(receiverDerivedSecrets, 32, 64);
            var receiverChainKey = new SignalChainKeyBuilder()
                    .key(receiverChainKeyData)
                    .index(0)
                    .build();
            var receiverChain = new SignalSessionChainBuilder()
                    .senderRatchetKey(parameters.theirRatchetKey())
                    .chainKey(receiverChainKey)
                    .build();
            sessionState.addReceiverChain(receiverChain);

            var sharedSecret = Curve25519.sharedKey(parameters.theirRatchetKey().encodedPoint(), sendingRatchetKey.privateKey().encodedPoint());
            var senderParams = HKDFParameterSpec.ofExtract()
                    .addIKM(new SecretKeySpec(sharedSecret, "AES"))
                    .addSalt(receiverRootKeyData)
                    .thenExpand("WhisperRatchet".getBytes(StandardCharsets.UTF_8), 64);
            var senderDerivedSecrets = kdf.deriveData(senderParams);
            var senderRootKeyData = SignalPublicKey.of(senderDerivedSecrets, 0, 32);
            var senderChainKeyData = Arrays.copyOfRange(senderDerivedSecrets, 32, 64);
            var senderChainKey = new SignalChainKeyBuilder()
                    .key(senderChainKeyData)
                    .index(0)
                    .build();
            var senderChain = new SignalSessionChainBuilder()
                    .senderRatchetKey(sendingRatchetKey.publicKey())
                    .chainKey(senderChainKey)
                    .build();
            sessionState.setSenderChain(senderChain);
            sessionState.setRootKey(senderRootKeyData);
        } catch (GeneralSecurityException exception) {
            throw new AssertionError(exception);
        }
    }

    public static void initializeSession(SignalSessionState sessionState, SignalBobParameters parameters) {

        try {
            sessionState.setSessionVersion(SignalProtocol.CURRENT_VERSION);
            sessionState.setRemoteIdentityPublic(parameters.theirIdentityKey());
            sessionState.setLocalIdentityPublic(parameters.ourIdentityKey().publicKey());

            var hasOneTimePreKey = parameters.ourOneTimePreKey() != null;
            var secrets = new byte[DISCONTINUITY_BYTES_LENGTH + KEY_AGREEMENT_LENGTH + KEY_AGREEMENT_LENGTH + KEY_AGREEMENT_LENGTH + (hasOneTimePreKey ? KEY_AGREEMENT_LENGTH : 0)];
            var offset = writeDiscontinuityBytes(secrets, 0);
            offset = writeKeyAgreement(parameters.theirIdentityKey(), parameters.ourSignedPreKey().privateKey(), secrets, offset);
            offset = writeKeyAgreement(parameters.theirBaseKey(), parameters.ourIdentityKey().privateKey(), secrets, offset);
            offset = writeKeyAgreement(parameters.theirBaseKey(), parameters.ourSignedPreKey().privateKey(), secrets, offset);

            if (parameters.ourOneTimePreKey() != null) {
                offset = writeKeyAgreement(parameters.theirBaseKey(), parameters.ourOneTimePreKey().privateKey(), secrets, offset);
            }

            if(offset != secrets.length) {
                throw new InternalError("Offset is not equal to the length of the array");
            }

            var kdf = KDF.getInstance("HKDF-SHA256");
            var senderParams = HKDFParameterSpec.ofExtract()
                    .addIKM(secrets)
                    .thenExpand("WhisperText".getBytes(StandardCharsets.UTF_8), 64);
            var senderDerivedSecrets = kdf.deriveData(senderParams);
            var senderRootKeyData = SignalPublicKey.of(senderDerivedSecrets, 0, 32);
            var senderChainKeyData = Arrays.copyOfRange(senderDerivedSecrets, 32, 64);
            var senderChainKey = new SignalChainKeyBuilder()
                    .key(senderChainKeyData)
                    .index(0)
                    .build();
            var senderChain = new SignalSessionChainBuilder()
                    .senderRatchetKey(parameters.ourRatchetKey().publicKey())
                    .chainKey(senderChainKey)
                    .build();
            sessionState.setSenderChain(senderChain);
            sessionState.setRootKey(senderRootKeyData);
        } catch (GeneralSecurityException e) {
            throw new AssertionError(e);
        }
    }

    private static int writeKeyAgreement(SignalPublicKey publicKey, SignalPrivateKey privateKey, byte[] output, int offset) {
        Curve25519.sharedKey(publicKey.encodedPoint(), privateKey.encodedPoint(), output, offset);
        return offset + KEY_AGREEMENT_LENGTH;
    }

    private static int writeDiscontinuityBytes(byte[] output, int offset) {
        Arrays.fill(output, 0, DISCONTINUITY_BYTES_LENGTH, (byte) 0xFF);
        return offset + DISCONTINUITY_BYTES_LENGTH;
    }
    private static boolean isAlice(SignalPublicKey ourKey, SignalPublicKey theirKey) {
        return ourKey.compareTo(theirKey) < 0;
    }
}
