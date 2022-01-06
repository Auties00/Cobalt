package it.auties.whatsapp.protobuf.signal.session;

import lombok.NonNull;

public record SessionPreKey(int preKeyId, byte @NonNull [] baseKey, int signedPreKeyId) {

}