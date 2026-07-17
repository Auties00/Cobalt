package com.github.auties00.cobalt.wire.linked.business.postcode;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Aggregated response of a WhatsApp Business postcode-verification IQ.
 *
 * <p>Delivery-enabled business catalogs ask the server to confirm that a
 * customer's postcode is serviceable by the merchant. The response carries
 * the canonical {@link BusinessPostcodeVerificationResult verdict} together
 * with an optional encrypted location-name payload that the merchant
 * server can later decrypt to recover the human-readable name of the
 * resolved location (for example a neighbourhood or city). The
 * location-name payload is delivered as an opaque ciphertext string and
 * Cobalt forwards it untouched.
 */
@ProtobufMessage
public final class BusinessPostcodeVerification {
    /**
     * Verdict returned by the server for the postcode-verification query.
     * Required: a successful response always carries one of the canonical
     * {@link BusinessPostcodeVerificationResult} values.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    final BusinessPostcodeVerificationResult result;

    /**
     * Server-encrypted opaque payload describing the resolved location
     * name. Populated when the merchant has configured location-name
     * encryption; absent when the response only carries the verdict.
     * Cobalt stores the payload as the raw wire string and leaves
     * decryption to the caller.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String encryptedLocationName;

    /**
     * Constructs a new {@code BusinessPostcodeVerification} response
     * pairing a verdict with an optional encrypted location-name payload.
     *
     * @param result                the postcode-verification verdict; must not be {@code null}
     * @param encryptedLocationName the encrypted location-name payload, or {@code null}
     * @throws NullPointerException if {@code result} is {@code null}
     */
    BusinessPostcodeVerification(BusinessPostcodeVerificationResult result, String encryptedLocationName) {
        this.result = Objects.requireNonNull(result, "result cannot be null");
        this.encryptedLocationName = encryptedLocationName;
    }

    /**
     * Returns the postcode-verification verdict.
     *
     * @return the verdict, never {@code null}
     */
    public BusinessPostcodeVerificationResult result() {
        return result;
    }

    /**
     * Returns the server-encrypted opaque payload describing the resolved
     * location name.
     *
     * @return an {@code Optional} containing the encrypted payload, or
     *         empty when the wire response did not include a location-name
     *         block
     */
    public Optional<String> encryptedLocationName() {
        return Optional.ofNullable(encryptedLocationName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessPostcodeVerification) obj;
        return Objects.equals(this.result, that.result) &&
               Objects.equals(this.encryptedLocationName, that.encryptedLocationName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result, encryptedLocationName);
    }

    @Override
    public String toString() {
        return "BusinessPostcodeVerification[" +
               "result=" + result + ", " +
               "encryptedLocationName=" + encryptedLocationName + ']';
    }
}
