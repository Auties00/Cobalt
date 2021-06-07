package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.protobuf.chat.Chat;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;

import java.util.List;
import java.util.Optional;

/**
 * A json model that contains information about the groups in common with a Contact
 */
public record CommonGroupsResponse(int status, List<Chat> groups) implements JsonResponseModel {
    @JsonCreator
    public CommonGroupsResponse(@JsonProperty("groups") List<String> groups, @JsonProperty("status") int status) {
        this(status, groups.stream().map(WhatsappDataManager.singletonInstance()::findChatByJid).map(Optional::orElseThrow).toList());
    }
}
