package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.util.Json;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

public record NewsletterResponse(Newsletter newsletter) {
    @JsonCreator
    NewsletterResponse(Map<String, Newsletter> json) {
        this(json.values()
                .stream()
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Missing newsletter")));
    }

    public static Optional<NewsletterResponse> ofJson(String json) {
        return Json.readValue(json, JsonResponse.class).data();
    }

    private record JsonResponse(Optional<NewsletterResponse> data) {

    }
}