package it.auties.whatsapp.model.signal.session;

import org.checkerframework.checker.nullness.qual.NonNull;

public record SessionPreKey(Integer preKeyId, byte @NonNull [] baseKey, int signedKeyId) {

}