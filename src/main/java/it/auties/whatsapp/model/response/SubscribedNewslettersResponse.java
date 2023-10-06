package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.util.Json;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Optional;

public record SubscribedNewslettersResponse(@JsonAlias({"xwa2_newsletter_update", "xwa2_newsletter_create", "xwa2_newsletter_subscribed", "xwa2_newsletters_directory_list"}) List<Newsletter> newsletters) {
    public static Optional<SubscribedNewslettersResponse> ofJson(@NonNull String json) {
        return Json.readValue(json, JsonResponse.class).data();
    }

    private record JsonResponse(Optional<SubscribedNewslettersResponse> data) {

    }
}