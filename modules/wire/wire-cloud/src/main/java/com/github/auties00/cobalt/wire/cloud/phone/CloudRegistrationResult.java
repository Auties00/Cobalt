package com.github.auties00.cobalt.wire.cloud.phone;

import java.util.Optional;

/**
 * The outcome of a Cloud API phone-number registration request.
 *
 * <p>The registration edge returns a simple acknowledgement; this model projects whether it succeeded
 * and carries any message the server returned.
 */
public final class CloudRegistrationResult {
    /**
     * Whether the registration request succeeded.
     */
    private final boolean success;

    /**
     * An optional message returned by the server, or {@code null} when none.
     */
    private final String message;

    /**
     * Constructs a new registration result.
     *
     * @param success whether the registration succeeded
     * @param message an optional server message, or {@code null}
     */
    public CloudRegistrationResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    /**
     * Returns whether the registration succeeded.
     *
     * @return {@code true} if the server accepted the registration
     */
    public boolean success() {
        return success;
    }

    /**
     * Returns the optional server message.
     *
     * <p>The registration edge's success body carries no message field and registration failures surface
     * upstream as a Cloud API exception rather than a result, so this field is reserved and is not
     * populated by the register operation.
     *
     * @return an {@link Optional} carrying the message, or empty when none was returned
     */
    public Optional<String> message() {
        return Optional.ofNullable(message);
    }
}
