package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import it.auties.whatsapp4j.model.WhatsappChat;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record CommonGroupsResponse(int status, List<WhatsappChat> groups) implements JsonResponseModel {
    @JsonCreator
    public CommonGroupsResponse(@JsonProperty("groups") List<String> groups, @JsonProperty("status") int status){
        this(status, groups.stream().map(WhatsappDataManager.singletonInstance()::findChatByJid).map(Optional::orElseThrow).collect(Collectors.toUnmodifiableList()));
    }
}
