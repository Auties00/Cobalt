package it.auties.whatsapp4j.media;

import jakarta.validation.constraints.NotNull;

/**
 * An immutable model class that represents the connection between WhatsappWeb4j and WhatsappWeb's server used to decrypt media message.
 *
 * @param auth the non auth token
 * @param ttl  the time to live for the auth token in seconds
 */
public record MediaConnection(@NotNull String auth, int ttl) {
}