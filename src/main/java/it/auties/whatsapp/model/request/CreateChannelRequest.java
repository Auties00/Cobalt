package it.auties.whatsapp.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateChannelRequest(Variable variables) {
    public record Variable(@JsonProperty("newsletter_input") NewsletterInput newsletterInput) {

    }

    public record NewsletterInput(String name, String description, String picture) {

    }
}
