package it.auties.whatsapp.protobuf.signal.session;

import org.whispersystems.libsignal.ecc.ECPublicKey;

public record UnacknowledgedPreKeyMessageItems(int preKeyId, int signedPreKeyId, byte[] baseKey) {

}