package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * A json model that contains information about the status of a modification made to a participant of a group
 *
 * @param code                 the http status code for the original request
 * @param inviteCode           if {@code code != 200}, an invitation code to send in a {@link it.auties.whatsapp4j.protobuf.message.GroupInviteMessage}
 * @param inviteCodeExpiration if {@code code != 200}, the expiration for {@code inviteCode} to use in a {@link it.auties.whatsapp4j.protobuf.message.GroupInviteMessage}
 */
public record ModificationForParticipantStatus(int code, @NotNull @JsonProperty("invite_code") String inviteCode,
                                               @JsonProperty("invite_code_exp") long inviteCodeExpiration) {
}
