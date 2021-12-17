package it.auties.whatsapp.protobuf.signal.session;

import it.auties.whatsapp.protobuf.signal.keypair.SignalKeyPair;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.ecc.ECPublicKey;

public record PreKeyBundle(int registrationId, int deviceId, int preKeyId, byte[] preKeyPublic, int signedPreKeyId,
                           byte[] signedPreKeyPublic, byte[] signedPreKeySignature, SignalKeyPair identityKey) {

}
