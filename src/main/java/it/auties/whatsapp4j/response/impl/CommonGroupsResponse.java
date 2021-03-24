package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.model.WhatsappChat;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.Builder;
import lombok.Builder;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A json model that contains information about the groups in common with a WhatsappContact
 *
 * @param status the http status code for the original request
 * @param groups a nullable list of groups that are in common with said WhatsappContact, might be empty
 */
public record CommonGroupsResponse(int status, List<WhatsappChat> groups) implements JsonResponseModel {
    @JsonCreator
    public CommonGroupsResponse(@JsonProperty("groups") List<String> groups, @JsonProperty("status") int status){
        this(status, groups.stream().map(WhatsappDataManager.singletonInstance()::findChatByJid).map(Optional::orElseThrow).collect(Collectors.toUnmodifiableList()));
    }
}
