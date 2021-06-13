package it.auties.whatsapp4j.request.impl;


import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.request.model.JsonRequest;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import lombok.NonNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * A JSON request used to transmit a query request to WhatsappWeb's WebSocket
 */
public abstract class UserQueryRequest<M extends JsonResponseModel> extends JsonRequest<M> {
    private final @NonNull String jid;
    private final @NonNull QueryType queryType;
    public UserQueryRequest(@NonNull WhatsappConfiguration configuration, @NonNull String jid, @NonNull QueryType queryType) {
        super(configuration);
        this.jid = jid;
        this.queryType = queryType;
    }

    @Override
    public @NonNull List<Object> buildBody() {
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
        GROUP_INVITE_CODE(List.of("inviteCode")),
        GROUPS_IN_COMMON(List.of("group","common"));

        @Getter
        private final List<String> data;
    }
}
