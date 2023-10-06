package it.auties.whatsapp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubKey(String key, @JsonProperty("key_id") String keyId, String message) {

}
