package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import org.jetbrains.annotations.NotNull;

/**
 * A json model that contains information about a change in a WhatsappGroup's subject
 *
 * @param subject the new subject
 * @param timestamp the timestamp in seconds since {@link java.time.Instant#EPOCH}
 * @param authorJid the jid of the participant that changed the subject
 */
public record SubjectChangeResponse(@NotNull String subject, @JsonProperty("s_t") long timestamp,
                                    @JsonProperty("s_o") @NotNull String authorJid) implements JsonResponseModel {
}
