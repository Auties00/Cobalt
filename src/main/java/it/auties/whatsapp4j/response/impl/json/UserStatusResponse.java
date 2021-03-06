package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;

import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Jacksonized
@ToString
public class UserStatusResponse implements JsonResponseModel {
    @JsonProperty("status")
    private Object status;

    public @NotNull Optional<String> status(){
        return status instanceof Integer ? Optional.empty() : Optional.of((String) status);
    }
}
