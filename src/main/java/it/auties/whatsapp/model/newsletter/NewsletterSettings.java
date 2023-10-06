package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NewsletterSettings(@JsonProperty("reaction_codes") NewsletterReactionSettings reactionCodes) {

}
