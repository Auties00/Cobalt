package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
public final record AckResponse(String cmd,
                                @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) @JsonProperty("id") String[] ids,
                                int ack, String from, String to, @JsonProperty("t") int timestamp,
                                String participant) implements JsonResponseModel {
}
