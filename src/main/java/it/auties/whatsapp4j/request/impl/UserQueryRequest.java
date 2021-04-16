package it.auties.whatsapp4j.request.impl;


import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.request.model.JsonRequest;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A JSON request used to transmit a query request to WhatsappWeb's WebSocket
 */
public abstract class UserQueryRequest<M extends JsonResponseModel> extends JsonRequest<M> {
    private final @NotNull String jid;
    private final @NotNull QueryType queryType;
    public UserQueryRequest(@NotNull WhatsappConfiguration configuration, @NotNull String jid, @NotNull QueryType queryType) {
        super(configuration);
        this.jid = jid;
        this.queryType = queryType;
    }

    @Override
    public @NotNull List<Object> buildBody() {
        var list = new ArrayList<>();
        list.add("query");
        list.addAll(queryType.data());
        list.add(jid);
        return list;
    }

    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum QueryType {
        CHAT_PICTURE(List.of("ProfilePicThumb")),
        GROUP_METADATA(List.of("GroupMetadata")),
        USER_STATUS(List.of("Status")),
        GROUPS_IN_COMMON(List.of("group","common"));

        @Getter
        private final List<String> data;
    }
}
