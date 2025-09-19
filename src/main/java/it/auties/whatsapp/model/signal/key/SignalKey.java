package it.auties.whatsapp.model.signal.key;

public sealed interface SignalKey permits SignalPrivateKey, SignalPublicKey {
    byte[] encodedPoint();

    int writePoint(byte[] destination, int offset);
}
