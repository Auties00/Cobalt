package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.Data;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@Data
@ToString
public class UserStatusResponse implements JsonResponseModel {
    private String status;

    @JsonProperty("status")
    public void setStatus(@NotNull Object val) {
        if(val instanceof Integer){
            this.status = null;
            return;
        }

        this.status = (String) val;
    }
}
