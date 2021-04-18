package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * A json model that contains information about the participant of a group
 * This record should only be used by {@link GroupMetadataResponse}
 *
 */
@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
public final class GroupParticipant {
    @JsonProperty("id")
    private final @NotNull String jid;
    private final boolean isAdmin;
    private final boolean isSuperAdmin;

    /**
     * @param jid the jid of the participant
     * @param isAdmin a flag to determine whether the participant is an admin
     * @param isSuperAdmin a flag to determine whether the participant is a super admin
     */
    public GroupParticipant(@NotNull @JsonProperty("id") String jid, boolean isAdmin, boolean isSuperAdmin) {
        this.jid = jid;
        this.isAdmin = isAdmin;
        this.isSuperAdmin = isSuperAdmin;
    }
}