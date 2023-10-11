package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.util.Json;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

public record SubscribedNewslettersResponse(List<Newsletter> newsletters) {
    @JsonCreator
    SubscribedNewslettersResponse(Map<String, List<Newsletter>> json) {
        this(json.values()
                .stream()
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Missing newsletters")));
    }

    public static Optional<SubscribedNewslettersResponse> ofJson(String json) {
        return Json.readValue(json, JsonResponse.class).data();
    }

    private record JsonResponse(Optional<SubscribedNewslettersResponse> data) {

    }
}