package it.auties.whatsapp.model.media;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MediaDimensions(@JsonProperty("width") int width, @JsonProperty("height") int height) {
    private static final MediaDimensions DEFAULT = new MediaDimensions(128, 128);

    public static MediaDimensions defaultDimensions() {
        return DEFAULT;
    }
}
