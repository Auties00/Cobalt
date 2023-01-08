package it.auties.whatsapp.model.media;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MediaDimensions(@JsonProperty("width") int width,
                              @JsonProperty("height") int height) {

  public static final MediaDimensions DEFAULT = new MediaDimensions(128, 128);
}
