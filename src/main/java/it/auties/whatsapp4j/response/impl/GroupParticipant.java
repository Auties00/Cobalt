package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * A json model that contains information about the participant of a group
 * This record should only be used by {@link GroupMetadataResponse}
 * @param jid          the jid of the participant
 * @param isAdmin      a flag to determine whether the participant is an admin
 * @param isSuperAdmin a flag to determine whether the participant is a super admin
 */
public record GroupParticipant(@JsonProperty("id") @NotNull String jid, boolean isAdmin,
                               boolean isSuperAdmin) {
}