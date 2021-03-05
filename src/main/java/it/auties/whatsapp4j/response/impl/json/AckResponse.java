package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record AckResponse(String cmd, @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) @JsonProperty("id") String[] ids, int ack, String from, String to, @JsonProperty("t") int timestamp, String participant) implements JsonResponseModel {
}
