package it.auties.whatsapp.model.media;

import io.avaje.jsonb.Json;

@Json
public record MediaUpload(
        @Json.Property("direct_path") String directPath,
        @Json.Property("url") String url,
        @Json.Property("handle") String handle
) {

}
