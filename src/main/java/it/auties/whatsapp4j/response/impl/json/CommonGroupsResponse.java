package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import it.auties.whatsapp4j.model.WhatsappChat;
import lombok.Data;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@ToString
public class CommonGroupsResponse implements JsonResponseModel {
    private int status;
    private List<WhatsappChat> groups;

    @JsonProperty("groups")
    public void setGroups(@NotNull List<String> val) {
        final var manager = WhatsappDataManager.singletonInstance();
        this.groups = val.stream()
                .map(manager::findChatByJid)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toUnmodifiableList());
    }
}
