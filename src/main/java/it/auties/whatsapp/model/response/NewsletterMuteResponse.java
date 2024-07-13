package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.util.Json;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record NewsletterMuteResponse(@JsonProperty("id") Jid jid, @JsonProperty("mute") boolean mute) {
    @JsonCreator
    NewsletterMuteResponse(Map<String, String> json) {
        this(Jid.of(json.get("id")), Objects.equals(json.get("mute"), "ON"));
    }

    public static Optional<NewsletterMuteResponse> ofJson(String json) {
        return Json.readValue(json, JsonData.class)
                .data()
                .map(JsonResponse::response);
    }

    private record JsonData(Optional<JsonResponse> data) {

    }

    private record JsonResponse(
            @JsonProperty("xwa2_notify_newsletter_on_mute_change") NewsletterMuteResponse response) {

    }
}