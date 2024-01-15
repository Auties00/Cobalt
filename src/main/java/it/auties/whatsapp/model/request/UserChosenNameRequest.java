package it.auties.whatsapp.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record UserChosenNameRequest(List<Variable> variables) {
    public record Variable(@JsonProperty("user_id") String userId) {

    }
}
