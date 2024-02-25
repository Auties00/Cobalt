package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.util.Json;

import java.util.List;
import java.util.Optional;

public record RecommendedNewslettersResponse(@JsonProperty("result") List<Newsletter> newsletters) {
    public static Optional<RecommendedNewslettersResponse> ofJson(String json) {
        return Json.readValue(json, JsonResponse.class)
                .data()
                .map(JsonData::response);
    }

    private record JsonResponse(Optional<JsonData> data) {

    }

    private record JsonData(
            @JsonAlias({"xwa2_newsletter_update", "xwa2_newsletter_create", "xwa2_newsletter_subscribed", "xwa2_newsletters_directory_list"}) RecommendedNewslettersResponse response) {

    }
}