package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.model.WhatsappChat;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A json model that contains information about the groups in common with a WhatsappContact
 *
 */
@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
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
}
