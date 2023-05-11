package it.auties.whatsapp.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubUpload(@JsonProperty("key_id") String keyId,
                           @JsonProperty("encrypted_value") String encryptedValue) {

}
