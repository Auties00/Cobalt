package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.util.Json;

import java.util.Map;
import java.util.Optional;

public record AcceptAdminInviteNewsletterResponse(@JsonProperty("id") Jid jid) {
    @JsonCreator
    AcceptAdminInviteNewsletterResponse(Map<String, String> json) {
        this(Jid.of(json.get("id")));
    }

    public static Optional<AcceptAdminInviteNewsletterResponse> ofJson(String json) {
        return Json.readValue(json, JsonData.class)
                .data()
                .map(JsonResponse::response);
    }

    private record JsonData(Optional<JsonResponse> data) {

    }

    private record JsonResponse(
            @JsonProperty("xwa2_newsletter_admin_invite_accept") AcceptAdminInviteNewsletterResponse response) {

    }
}