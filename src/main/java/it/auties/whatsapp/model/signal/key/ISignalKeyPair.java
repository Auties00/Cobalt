package it.auties.whatsapp.model.signal.key;

public sealed interface ISignalKeyPair permits SignalKeyPair, SignalPreKeyPair, SignalSignedKeyPair {
    SignalPublicKey publicKey();
    SignalPrivateKey privateKey();
}
