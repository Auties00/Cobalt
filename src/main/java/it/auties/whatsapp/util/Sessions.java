package it.auties.whatsapp.util;

import it.auties.whatsapp.crypto.Curve;
import it.auties.whatsapp.protobuf.signal.key.SignalKeyPair;
import it.auties.whatsapp.protobuf.signal.session.AliceSignalProtocolParameters;
import it.auties.whatsapp.protobuf.signal.session.BobSignalProtocolParameters;
import it.auties.whatsapp.protobuf.signal.session.SessionStructure;
import it.auties.whatsapp.protobuf.signal.session.SymmetricSignalProtocolParameters;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

@UtilityClass
public class Sessions {
    private final int CURRENT_VERSION = 3;
    public void initializeSession(SessionStructure sessionState, SymmetricSignalProtocolParameters parameters) {
        if (isAlice(parameters.ourBaseKey(), parameters.theirBaseKey())) {
            var aliceSignalParameters = new AliceSignalProtocolParameters()
                    .ourBaseKey(parameters.ourBaseKey())
                    .ourIdentityKey(parameters.ourIdentityKey())
                    .theirRatchetKey(parameters.theirRatchetKey())
                    .theirIdentityKey(parameters.theirIdentityKey())
                    .theirSignedPreKey(parameters.theirBaseKey())
                    .theirOneTimePreKey(-1);
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

    public void initializeSession(SessionStructure sessionState, AliceSignalProtocolParameters parameters) {
        try {
            sessionState.sessionVersion(CURRENT_VERSION);
            sessionState.remoteIdentityKey(parameters.theirIdentityKey());
            sessionState.localIdentityPublic(parameters.ourIdentityKey());

            var             sendingRatchetKey = SignalKeyPair.random();
            var secrets           = new ByteArrayOutputStream();

            secrets.write(getDiscontinuityBytes());

            secrets.write(Curve.calculateSharedSecret(parameters.theirSignedPreKey(),
                    parameters.ourIdentityKey().getPrivateKey()));
            secrets.write(Curve.calculateAgreement(parameters.getTheirIdentityKey().getPublicKey(),
                    parameters.getOurBaseKey().getPrivateKey()));
            secrets.write(Curve.calculateAgreement(parameters.getTheirSignedPreKey(),
                    parameters.getOurBaseKey().getPrivateKey()));

            if (parameters.getTheirOneTimePreKey().isPresent()) {
                secrets.write(Curve.calculateAgreement(parameters.getTheirOneTimePreKey().get(),
                        parameters.getOurBaseKey().getPrivateKey()));
            }

            org.whispersystems.libsignal.ratchet.RatchetingSession.DerivedKeys derivedKeys  = calculateDerivedKeys(secrets.toByteArray());
            Pair<RootKey, ChainKey> sendingChain = derivedKeys.getRootKey().createChain(parameters.getTheirRatchetKey(), sendingRatchetKey);

            sessionState.addReceiverChain(parameters.getTheirRatchetKey(), derivedKeys.getChainKey());
            sessionState.setSenderChain(sendingRatchetKey, sendingChain.second());
            sessionState.setRootKey(sendingChain.first());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public void initializeSession(SessionState sessionState, BobSignalProtocolParameters parameters)
            throws InvalidKeyException
    {

        try {
            sessionState.setSessionVersion(CiphertextMessage.CURRENT_VERSION);
            sessionState.setRemoteIdentityKey(parameters.getTheirIdentityKey());
            sessionState.setLocalIdentityKey(parameters.getOurIdentityKey().getPublicKey());

            ByteArrayOutputStream secrets = new ByteArrayOutputStream();

            secrets.write(getDiscontinuityBytes());

            secrets.write(Curve.calculateAgreement(parameters.getTheirIdentityKey().getPublicKey(),
                    parameters.getOurSignedPreKey().getPrivateKey()));
            secrets.write(Curve.calculateAgreement(parameters.getTheirBaseKey(),
                    parameters.getOurIdentityKey().getPrivateKey()));
            secrets.write(Curve.calculateAgreement(parameters.getTheirBaseKey(),
                    parameters.getOurSignedPreKey().getPrivateKey()));

            if (parameters.getOurOneTimePreKey().isPresent()) {
                secrets.write(Curve.calculateAgreement(parameters.getTheirBaseKey(),
                        parameters.getOurOneTimePreKey().get().getPrivateKey()));
            }

            org.whispersystems.libsignal.ratchet.RatchetingSession.DerivedKeys derivedKeys = calculateDerivedKeys(secrets.toByteArray());

            sessionState.setSenderChain(parameters.getOurRatchetKey(), derivedKeys.getChainKey());
            sessionState.setRootKey(derivedKeys.getRootKey());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private byte[] getDiscontinuityBytes() {
        byte[] discontinuity = new byte[32];
        Arrays.fill(discontinuity, (byte) 0xFF);
        return discontinuity;
    }

    private org.whispersystems.libsignal.ratchet.RatchetingSession.DerivedKeys calculateDerivedKeys(byte[] masterSecret) {
        HKDF     kdf                = new HKDFv3();
        byte[]   derivedSecretBytes = kdf.deriveSecrets(masterSecret, "WhisperText".getBytes(), 64);
        byte[][] derivedSecrets     = ByteUtil.split(derivedSecretBytes, 32, 32);

        return new org.whispersystems.libsignal.ratchet.RatchetingSession.DerivedKeys(new RootKey(kdf, derivedSecrets[0]),
                new ChainKey(kdf, derivedSecrets[1], 0));
    }

    private boolean isAlice(ECPublicKey ourKey, ECPublicKey theirKey) {
        return ourKey.compareTo(theirKey) < 0;
    }

    private class DerivedKeys {
        private final RootKey   rootKey;
        private final ChainKey  chainKey;

        private DerivedKeys(RootKey rootKey, ChainKey chainKey) {
            this.rootKey   = rootKey;
            this.chainKey  = chainKey;
        }

        public RootKey getRootKey() {
            return rootKey;
        }

        public ChainKey getChainKey() {
            return chainKey;
        }
    }
}
