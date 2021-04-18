package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * A json model that contains information about the participant of a group
 * This record should only be used by {@link GroupMetadataResponse}
 *
 */
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

    @JsonProperty("id")
    public @NotNull String jid() {
        return jid;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public boolean isSuperAdmin() {
        return isSuperAdmin;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (GroupParticipant) obj;
        return Objects.equals(this.jid, that.jid) &&
                this.isAdmin == that.isAdmin &&
                this.isSuperAdmin == that.isSuperAdmin;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jid, isAdmin, isSuperAdmin);
    }

    @Override
    public String toString() {
        return "GroupParticipant[" +
                "jid=" + jid + ", " +
                "isAdmin=" + isAdmin + ", " +
                "isSuperAdmin=" + isSuperAdmin + ']';
    }


}