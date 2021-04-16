package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Objects;

public final class GroupActionResponse implements JsonResponseModel<GroupActionResponse> {
    private final @NotNull List<String> participants;

    public GroupActionResponse(@NotNull List<String> participants) {
        this.participants = participants;
    }

    public @NotNull List<String> participants() {
        return participants;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (GroupActionResponse) obj;
        return Objects.equals(this.participants, that.participants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(participants);
    }

    @Override
    public String toString() {
        return "GroupActionResponse[" +
                "participants=" + participants + ']';
    }


}
