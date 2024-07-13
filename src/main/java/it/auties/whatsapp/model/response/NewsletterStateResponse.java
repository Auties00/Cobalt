package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.newsletter.NewsletterState;
import it.auties.whatsapp.util.Json;

import java.util.Optional;

public record NewsletterStateResponse(@JsonProperty("id") Jid jid, @JsonProperty("is_requestor") boolean isRequestor,
                                      NewsletterState state) {
    public static Optional<NewsletterStateResponse> ofJson(String json) {
        return Json.readValue(json, JsonResponse.class)
                .data()
                .map(JsonData::response);
    }

    private record JsonResponse(Optional<JsonData> data) {

    }

    private record JsonData(@JsonProperty("xwa2_notify_newsletter_on_state_change") NewsletterStateResponse response) {

    }
}