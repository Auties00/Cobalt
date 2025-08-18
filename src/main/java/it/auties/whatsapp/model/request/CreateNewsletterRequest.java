package it.auties.whatsapp.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateNewsletterRequest(Variable variables) {
    public record Variable(@JsonProperty("input") NewsletterInput newsletterInput) {

    }

    public record NewsletterInput(String name, String description, String picture) {

    }
}
