package it.auties.whatsapp4j.utils;

public record SignedKeyPair(int id, IdentityKeyPair keyPair, byte[] signature) {
}
