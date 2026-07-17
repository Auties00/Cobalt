package com.github.auties00.cobalt.wire.linked.business.postcode;

import it.auties.protobuf.annotation.ProtobufEnum;

import java.util.Optional;

/**
 * Outcome of a WhatsApp Business postcode-verification query.
 *
 * <p>When a customer's address falls inside the catchment area of a
 * business that operates a delivery catalog, WhatsApp sends a
 * verify-postcode IQ to confirm whether the merchant actually delivers to
 * the supplied postcode. The server replies with one of three canonical
 * outcomes encoded as a wire string: the postcode is recognised and
 * deliverable, the postcode is not a recognised value at all, or the
 * postcode is recognised but lies outside the merchant's serviceable area.
 *
 * <p>Wire strings outside this canonical set are treated as a server
 * error by the WhatsApp Web parser; Cobalt mirrors that contract by
 * returning {@link Optional#empty()} from {@link #ofWire(String)} for
 * unrecognised values, leaving the handling decision to the caller.
 */
@ProtobufEnum
public enum BusinessPostcodeVerificationResult {
    /**
     * The postcode was recognised by the merchant and the location is
     * serviceable. Wire form is {@code "success"}.
     */
    SUCCESS("success"),

    /**
     * The supplied value is not a recognised postcode. Wire form is
     * {@code "invalid_postcode"}.
     */
    INVALID_POSTCODE("invalid_postcode"),

    /**
     * The postcode is recognised, but the merchant does not deliver to
     * that location. Wire form is {@code "unserviceable_location"}.
     */
    UNSERVICEABLE_LOCATION("unserviceable_location");

    /**
     * Wire-format string emitted as the {@code <result_code>} content
     * for this outcome.
     */
    private final String wireValue;

    /**
     * Constructs a {@code BusinessPostcodeVerificationResult} bound to
     * the given wire-format identifier.
     *
     * @param wireValue the wire-format string for this outcome
     */
    BusinessPostcodeVerificationResult(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire-format string identifier for this outcome.
     *
     * @return the wire string, never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a wire-format string to the matching outcome constant.
     *
     * @param wireValue the wire-format string to resolve, possibly {@code null}
     * @return an {@code Optional} containing the matching constant, or
     *         empty when the input is {@code null} or does not match any
     *         canonical wire value
     */
    public static Optional<BusinessPostcodeVerificationResult> ofWire(String wireValue) {
        if (wireValue == null) {
            return Optional.empty();
        }
        for (var value : values()) {
            if (value.wireValue.equals(wireValue)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    /**
     * Resolves a relay GraphQL {@code result_code} string to the matching
     * outcome constant.
     *
     * <p>The relay variant of the verify-postcode operation encodes the
     * outcome as {@code "RESULT_CODE_SUCCESS"} or
     * {@code "RESULT_CODE_UNSERVICEABLE_LOCATION"}; the WhatsApp client maps
     * any other value to {@link #INVALID_POSTCODE}.
     *
     * @param resultCode the relay {@code result_code} string, possibly
     *                   {@code null}
     * @return an {@code Optional} containing the matching constant, or empty
     *         when {@code resultCode} is {@code null}
     */
    public static Optional<BusinessPostcodeVerificationResult> ofRelayResultCode(String resultCode) {
        if (resultCode == null) {
            return Optional.empty();
        }
        return switch (resultCode) {
            case "RESULT_CODE_SUCCESS" -> Optional.of(SUCCESS);
            case "RESULT_CODE_UNSERVICEABLE_LOCATION" -> Optional.of(UNSERVICEABLE_LOCATION);
            default -> Optional.of(INVALID_POSTCODE);
        };
    }
}
