package it.auties.whatsapp.util;

import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.crypto.Curve;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.protobuf.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.protobuf.signal.session.*;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static java.util.Arrays.copyOfRange;

@UtilityClass
public class Sessions {
    private final int CURRENT_VERSION = 3;
    public void initializeSession(SessionStructure sessionState, SymmetricSignalProtocolParameters parameters) {
        if (Arrays.equals(parameters.ourBaseKey().publicKey(), parameters.theirBaseKey())) {
            var aliceSignalParameters = new AliceSignalProtocolParameters()
                    .ourBaseKey(parameters.ourBaseKey())
                    .ourIdentityKey(parameters.ourIdentityKey())
                    .theirRatchetKey(parameters.theirRatchetKey())
                    .theirIdentityKey(parameters.theirIdentityKey())
                    .theirSignedPreKey(parameters.theirBaseKey())
                    .theirOneTimePreKey(null);
            initializeSession(sessionState, aliceSignalParameters);
            return;
        }

        var bobSignalParameters = new BobSignalProtocolParameters()
                .ourIdentityKey(parameters.ourIdentityKey())
                .ourRatchetKey(parameters.ourRatchetKey())
                .ourSignedPreKey(parameters.ourBaseKey())
                .ourOneTimePreKey(null)
                .theirBaseKey(parameters.theirBaseKey())
                .theirIdentityKey(parameters.theirIdentityKey());

        initializeSession(sessionState, bobSignalParameters);
    }

    @SneakyThrows
    public void initializeSession(SessionStructure sessionState, AliceSignalProtocolParameters parameters) {
        sessionState.version(CURRENT_VERSION);
        sessionState.remoteIdentityKey(parameters.theirIdentityKey());
        sessionState.localIdentityPublic(parameters.ourIdentityKey().publicKey());

        var sendingRatchetKey = SignalKeyPair.random();
        var secrets = createSecrets();
        secrets.write(Curve.calculateSharedSecret(parameters.theirSignedPreKey(), parameters.ourIdentityKey().privateKey()).data());
        secrets.write(Curve.calculateSharedSecret(parameters.theirIdentityKey(), parameters.ourBaseKey().privateKey()).data());
        secrets.write(Curve.calculateSharedSecret(parameters.theirSignedPreKey(), parameters.ourBaseKey().privateKey()).data());
        if (parameters.theirOneTimePreKey() != null) {
            secrets.write(Curve.calculateSharedSecret(parameters.theirOneTimePreKey(), parameters.ourBaseKey().privateKey()).data());
        }

        var derivedKeys = calculateDerivedKeys(secrets.toByteArray());
        var sendingChain = derivedKeys.rootKey().createChain(parameters.theirRatchetKey(), sendingRatchetKey);
        sessionState.addReceiverChain(parameters.theirRatchetKey(), derivedKeys.chainKey());
        sessionState.senderChain(sendingRatchetKey, sendingChain.chainKey());
        sessionState.rootKey(sendingChain.rootKey().key());
    }

    @SneakyThrows
    public void initializeSession(SessionStructure sessionState, BobSignalProtocolParameters parameters) {
        sessionState.version(CURRENT_VERSION);
        sessionState.remoteIdentityKey(parameters.theirIdentityKey());
        if (parameters.ourOneTimePreKey() != null) {
            sessionState.localIdentityPublic(parameters.ourOneTimePreKey().publicKey());
        }

        var secrets = createSecrets();
        secrets.write(Curve.calculateSharedSecret(parameters.theirIdentityKey(), parameters.ourSignedPreKey().privateKey()).data());
        secrets.write(Curve.calculateSharedSecret(parameters.theirBaseKey(), parameters.ourIdentityKey().privateKey()).data());
        secrets.write(Curve.calculateSharedSecret(parameters.theirBaseKey(), parameters.ourSignedPreKey().privateKey()).data());
        if (parameters.ourOneTimePreKey() != null) {
            secrets.write(Curve.calculateSharedSecret(parameters.theirBaseKey(), parameters.ourOneTimePreKey().privateKey()).data());
        }

        var derivedKeys = calculateDerivedKeys(secrets.toByteArray());
        sessionState.senderChain(parameters.ourRatchetKey(), derivedKeys.chainKey());
        sessionState.rootKey(derivedKeys.rootKey().key());
    }

    private ByteArrayOutputStream createSecrets() {
        return BinaryArray.allocate(32).fill((byte) 0xFF).toOutputStream();
    }

    private DerivedKeys calculateDerivedKeys(byte[] masterSecret) {
        var derivedSecretBytes = Hkdf.deriveSecrets(masterSecret, "WhisperText".getBytes(StandardCharsets.UTF_8), 64);
        var root = new RootKey(copyOfRange(derivedSecretBytes, 0, 32));
        var chain = new ChainKey(0, copyOfRange(derivedSecretBytes, 32, 64));
        return new DerivedKeys(root, chain);
    }
}
