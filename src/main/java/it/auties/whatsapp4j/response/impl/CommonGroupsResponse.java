package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.model.WhatsappChat;
import it.auties.whatsapp4j.response.model.JsonResponseModel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A json model that contains information about the groups in common with a WhatsappContact
 *
 */
public final class CommonGroupsResponse implements JsonResponseModel {
    private final int status;
    private final List<WhatsappChat> groups;

    /**
     * @param status the http status code for the original request
     * @param groups a nullable list of groups that are in common with said WhatsappContact, might be empty
     */
    public CommonGroupsResponse(int status, List<WhatsappChat> groups) {
        this.status = status;
        this.groups = groups;
    }

    @JsonCreator
    public CommonGroupsResponse(@JsonProperty("groups") List<String> groups, @JsonProperty("status") int status) {
        this(status, groups.stream().map(WhatsappDataManager.singletonInstance()::findChatByJid).map(Optional::orElseThrow).toList());
    }

    public int status() {
        return status;
    }

    public List<WhatsappChat> groups() {
        return groups;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CommonGroupsResponse) obj;
        return this.status == that.status &&
                Objects.equals(this.groups, that.groups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, groups);
    }

    @Override
    public String toString() {
        return "CommonGroupsResponse[" +
                "status=" + status + ", " +
                "groups=" + groups + ']';
    }

}
