package it.auties.whatsapp.exception;

/**
 * A security exception that is thrown when HMAC (Hash-based Message Authentication Code) signature validation fails.
 * <p>
 * This exception indicates that data integrity verification has failed during cryptographic operations,
 * which typically occurs in the following scenarios:
 * <ul>
 *   <li>Media file decryption when the ciphertext MAC doesn't match the expected value</li>
 *   <li>Web app state synchronization when patch or snapshot MACs fail validation</li>
 *   <li>Device identity verification when signature validation fails during login</li>
 *   <li>Mutation record decoding when index or message MACs don't match</li>
 * </ul>
 * <p>
 * The exception's message refers to the location where the failure occurred.
 *
 * @see SecurityException
 */
public final class HmacValidationException extends SecurityException {
    /**
     * Constructs a new HMAC validation exception with the specified location identifier.
     *
     * @param location a string identifying where the HMAC validation failure occurred
     */
    public HmacValidationException(String location) {
        super(location);
    }
}