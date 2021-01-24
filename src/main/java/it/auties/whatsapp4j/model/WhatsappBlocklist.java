package it.auties.whatsapp4j.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WhatsappBlocklist {
    @JsonProperty("id")
    private int id;
    @JsonProperty("blocklist")
    private List<String> blocklist;
}
