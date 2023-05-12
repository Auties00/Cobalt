package it.auties.whatsapp.exception;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * An unchecked exception that is thrown when a session is created using {@link it.auties.whatsapp.api.ConnectionType#KNOWN}, but the uuid doesn't exist
 */
public class UnknownSessionException extends RuntimeException{
    private long phoneNumber;
    private UUID uuid;
    public UnknownSessionException() {
        this(null);
    }

    public UnknownSessionException(UUID uuid) {
        super("Missing session for phone number");
        this.uuid = uuid;
    }

    public UnknownSessionException(long phoneNumber) {
        super("Missing session for uuid");
        this.phoneNumber = phoneNumber;
    }

    public OptionalLong phoneNumber() {
        return OptionalLong.of(phoneNumber);
    }

    public Optional<UUID> uuid() {
        return Optional.ofNullable(uuid);
    }
}
