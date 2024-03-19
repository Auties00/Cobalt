package it.auties.whatsapp.model.mobile;

import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.util.Validate;

import java.util.Base64;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record SixPartsKeys(PhoneNumber phoneNumber, SignalKeyPair noiseKeyPair, SignalKeyPair identityKeyPair, byte[] identityId) {
    public static SixPartsKeys of(String sixParts) {
        Objects.requireNonNull(sixParts, "Invalid six parts");
        var parts = sixParts.trim().split(",", 6);
        Validate.isTrue(parts.length == 6, "Invalid format");
        var phoneNumber = PhoneNumber.of(parts[0]);
        var noisePublicKey = Base64.getDecoder().decode(parts[1]);
        var noisePrivateKey = Base64.getDecoder().decode(parts[2]);
        var identityPublicKey = Base64.getDecoder().decode(parts[3]);
        var identityPrivateKey = Base64.getDecoder().decode(parts[4]);
        var identityId = Base64.getDecoder().decode(parts[5]);
        var noiseKeyPair = new SignalKeyPair(noisePublicKey, noisePrivateKey);
        var identityKeyPair = new SignalKeyPair(identityPublicKey, identityPrivateKey);
        return new SixPartsKeys(phoneNumber, noiseKeyPair, identityKeyPair, identityId);
    }

    @Override
    public String toString() {
        var cryptographicKeys = Stream.of(noiseKeyPair.publicKey(), noiseKeyPair.privateKey(), identityKeyPair.publicKey(), identityKeyPair.privateKey(), identityId())
                .map(Base64.getEncoder()::encodeToString)
                .collect(Collectors.joining(","));
        return phoneNumber + "," + cryptographicKeys;
    }
}
