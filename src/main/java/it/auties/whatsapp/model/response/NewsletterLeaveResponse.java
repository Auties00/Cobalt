package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.util.Json;

import java.util.Optional;

public record NewsletterLeaveResponse(@JsonProperty("id") Jid jid) {
    public static Optional<NewsletterLeaveResponse> ofJson(String json) {
        return Json.readValue(json, JsonResponse.class)
                .data()
                .map(JsonData::response);
    }

    private record JsonResponse(Optional<JsonData> data) {

    }

    private record JsonData(@JsonProperty("xwa2_notify_newsletter_on_leave") NewsletterLeaveResponse response) {

    }
}