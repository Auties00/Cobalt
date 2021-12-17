package it.auties.whatsapp.protobuf.signal.session;


import it.auties.whatsapp.crypto.Curve;
import it.auties.whatsapp.protobuf.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.crypto.Hkdf;

public record RootKey(byte[] key) {
    public DerivedKeys createChain(byte[] theirRatchetKey, SignalKeyPair ourRatchetKey) {
        var sharedSecret = Curve.calculateSharedSecret(theirRatchetKey, ourRatchetKey.privateKey());
        var derivedSecretBytes = Hkdf.deriveSecrets(sharedSecret.data(), key, "WhisperRatchet".getBytes(), DerivedRootSecrets.SIZE);
        var derivedSecrets = new DerivedRootSecrets(derivedSecretBytes);

        var newRootKey = new RootKey(derivedSecrets.rootKey());
        var newChainKey = new ChainKey(0, derivedSecrets.chainKey());

        return new DerivedKeys(newRootKey, newChainKey);
    }
}