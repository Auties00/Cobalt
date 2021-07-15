package it.auties.whatsapp4j.test.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubKey(String key, @JsonProperty("key_id") String keyId) {
}
