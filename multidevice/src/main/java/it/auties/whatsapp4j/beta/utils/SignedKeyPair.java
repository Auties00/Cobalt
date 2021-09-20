package it.auties.whatsapp4j.beta.utils;

public record SignedKeyPair(byte[] publicKey, byte[] privateKey, byte[] signature) {
}
