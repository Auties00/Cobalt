package it.auties.whatsapp.protobuf.model;

import it.auties.whatsapp.protobuf.model.IdentityKeyPair;

public record SignedKeyPair(int id, IdentityKeyPair keyPair, byte[] signature) {
}
