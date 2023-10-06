package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NewsletterPicture(String id, String text, @JsonProperty("direct_path") String directPath) {

}
