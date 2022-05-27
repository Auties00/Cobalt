package it.auties.whatsapp.model.media;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MediaDimensions(@JsonProperty("width") int width, @JsonProperty("height") int height) {
}
