package it.auties.whatsapp.protobuf.media;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MediaUpload(@JsonProperty("direct_path") String directPath, @JsonProperty("url") String url) {
}
