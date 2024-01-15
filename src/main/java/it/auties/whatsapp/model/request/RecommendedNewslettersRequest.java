package it.auties.whatsapp.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RecommendedNewslettersRequest(Variable variables) {
    public record Variable(Input input) {

    }

    public record Input(String view, Filters filters, int limit) {

    }

    public record Filters(@JsonProperty("country_codes") List<String> countyCodes) {

    }
}
