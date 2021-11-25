package it.auties.whatsapp.protobuf.model;

import lombok.NonNull;

public record IdentityKeyPair(byte @NonNull [] publicKey, byte @NonNull [] privateKey) {

}
