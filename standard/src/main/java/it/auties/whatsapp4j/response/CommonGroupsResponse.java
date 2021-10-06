package it.auties.whatsapp4j.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.common.manager.WhatsappDataManager;
import it.auties.whatsapp4j.common.protobuf.chat.Chat;
import it.auties.whatsapp4j.common.response.JsonResponseModel;

import java.util.List;
import java.util.Optional;

/**
 * A json model that contains information about the groups in common with a Contact
 */
public final record CommonGroupsResponse(int status, List<Chat> groups) implements JsonResponseModel {
    @JsonCreator
    public CommonGroupsResponse(@JsonProperty("groups") List<String> groups, @JsonProperty("status") int status) {
        this(status, groups.stream().map(WhatsappDataManager.singletonInstance()::findChatByJid).map(Optional::orElseThrow).toList());
    }
}
