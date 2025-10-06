package com.github.auties00.cobalt.exception;

import java.util.Optional;

/**
 * An exception thrown when mobile phone number registration with the WhatsApp API fails.
 * <p>
 * This exception occurs during various stages of the mobile registration process, including:
 * <ul>
 *   <li>Phone number validation failures (malformed or invalid phone numbers)</li>
 *   <li>Rate limiting when too many registration attempts are made in a short period</li>
 *   <li>Registration blocking by WhatsApp's anti-spam mechanisms</li>
 *   <li>Verification code request failures (when requesting SMS or call verification)</li>
 *   <li>Verification code submission failures (invalid or expired codes)</li>
 *   <li>Network or I/O errors during registration API calls</li>
 *   <li>Unsupported platform configurations for mobile registration</li>
 * </ul>
 * <p>
 * When available, the exception may contain the raw JSON response from the WhatsApp registration
 * API, which can be retrieved via {@link #erroneousResponse()} for debugging purposes.
 */
public class MobileRegistrationException extends RuntimeException {
    private final String erroneousResponse;

    /**
     * Constructs a new mobile registration exception with a descriptive message and the raw API response.
     * <p>
     * This constructor should be used when a registration failure occurs and the WhatsApp API
     * returns an error response that may contain additional diagnostic information.
     *
     * @param message            a descriptive error message explaining the registration failure
     * @param erroneousResponse  the raw response from the WhatsApp registration API (typically JSON format)
     */
    public MobileRegistrationException(String message, String erroneousResponse) {
        super(message);
        this.erroneousResponse = erroneousResponse;
    }

    /**
     * Constructs a new mobile registration exception with a descriptive message.
     * <p>
     * This constructor should be used for registration failures that occur before or without
     * communication with the WhatsApp API, such as validation errors or unsupported configurations.
     *
     * @param message a descriptive error message explaining the registration failure
     */
    public MobileRegistrationException(String message) {
        super(message);
        this.erroneousResponse = null;
    }

    /**
     * Constructs a new mobile registration exception that wraps an underlying cause.
     * <p>
     * This constructor should be used when a registration failure is caused by an underlying
     * exception, such as network errors, I/O failures, or interrupted operations.
     *
     * @param cause the underlying exception that caused the registration to fail
     */
    public MobileRegistrationException(Throwable cause) {
        super(cause);
        this.erroneousResponse = null;
    }

    /**
     * Returns the raw API response that caused this exception, if available.
     * <p>
     * The response, when present, typically contains a JSON-formatted error message from the
     * WhatsApp registration API with details such as error codes, reasons, and additional metadata.
     *
     * @return an {@link Optional} containing the erroneous API response, or empty if no response is available
     */
    public Optional<String> erroneousResponse() {
        return Optional.ofNullable(erroneousResponse);
    }
}
