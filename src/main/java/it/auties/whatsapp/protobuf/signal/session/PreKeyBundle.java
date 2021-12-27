package it.auties.whatsapp.protobuf.signal.session;

import it.auties.whatsapp.protobuf.signal.keypair.SignalKeyPair;

public record PreKeyBundle(int registrationId, int deviceId, int preKeyId, byte[] preKeyPublic, int signedPreKeyId,
                           byte[] signedPreKeyPublic, byte[] signedPreKeySignature, SignalKeyPair identityKey) {

}
