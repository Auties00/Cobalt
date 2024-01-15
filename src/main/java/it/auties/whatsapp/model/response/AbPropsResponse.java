package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AbPropsResponse(@JsonProperty("ab_hash") String abHash) {
}
