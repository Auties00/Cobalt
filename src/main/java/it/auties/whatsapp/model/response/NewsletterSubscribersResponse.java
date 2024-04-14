package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.util.Json;

import java.util.Optional;

public record NewsletterSubscribersResponse(@JsonProperty("subscribers_count") Long subscribersCount) {
    public static Optional<NewsletterSubscribersResponse> ofJson(String json) {
        return Json.readValue(json, JsonResponse.class)
                .data()
                .map(response -> response.result().response());
    }

    private record JsonResponse(Optional<JsonData> data) {

    }

    private record JsonData(@JsonProperty("xwa2_newsletter") WrappedResult result) {

    }

    private record WrappedResult(@JsonProperty("thread_metadata") NewsletterSubscribersResponse response) {

    }
}