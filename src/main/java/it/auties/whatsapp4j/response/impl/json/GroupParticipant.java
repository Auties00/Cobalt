package it.auties.whatsapp4j.response.impl.json;

import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;

/**
 * A json model that contains information about the participant of a group
 * This record should only be used by {@link GroupMetadataResponse}
 *
 * @param jid the jid of the participant
 * @param isAdmin a flag to determine whether the participant is an admin
 * @param isSuperAdmin a flag to determine whether the participant is a super admin
 */
@Jacksonized
public record GroupParticipant(@NotNull String jid, boolean isAdmin, boolean isSuperAdmin) {

}