package it.auties.whatsapp.protobuf.model.key;

import it.auties.whatsapp.protobuf.model.key.IdentityKeyPair;

public record SignedKeyPair(int id, IdentityKeyPair keyPair, byte[] signature) {
}
