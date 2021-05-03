package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * A json model that contains information about an update about the metadata of a WhatsappChat
 *
 * @param jid  the jid of the WhatsappChat this update regards
 * @param cmd  a nullable String used to describe the update
 * @param data a list of objects that represent the encoded update
 */
public final record ChatCmdResponse(@JsonProperty("id") @NotNull String jid, String cmd,
                                    @NotNull List<Object> data) implements JsonResponseModel {

}