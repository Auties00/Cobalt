package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NewsletterPreview(String id, String type, @JsonProperty("direct_path") String directPath) {

}
