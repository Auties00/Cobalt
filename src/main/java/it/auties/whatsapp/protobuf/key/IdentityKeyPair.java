package it.auties.whatsapp.protobuf.key;

import lombok.NonNull;

public record IdentityKeyPair(byte @NonNull [] publicKey, byte @NonNull [] privateKey) {

}
