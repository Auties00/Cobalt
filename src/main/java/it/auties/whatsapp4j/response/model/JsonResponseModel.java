package it.auties.whatsapp4j.response.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.auties.whatsapp4j.response.impl.*;

/**
 * An interface to represent a class that may represent a JSON String sent by WhatsappWeb's WebSocket
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT,property = "description")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UserInformationResponse.class,name = "Conn"),
        @JsonSubTypes.Type(value = BlocklistResponse.class,name = "Blocklist"),
        @JsonSubTypes.Type(value = PropsResponse.class,name = "Props"),
        @JsonSubTypes.Type(value = PresenceResponse.class,name = "Presence"),
        @JsonSubTypes.Type(value = AckResponse.class,name = "Msg"),
        @JsonSubTypes.Type(value = AckResponse.class,name = "MsgInfo"),
        @JsonSubTypes.Type(value = ChatCmdResponse.class,name = "Chat"),
})
public interface JsonResponseModel<J extends JsonResponseModel<J>> extends ResponseModel<J> {
}