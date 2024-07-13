package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.core.type.TypeReference;
import it.auties.whatsapp.util.Json;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record UserChosenNameResponse(Optional<String> name) {

    @SuppressWarnings("unchecked")
    public static Optional<UserChosenNameResponse> ofJson(String json) {
        try {
            var parsedJson = Json.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            var data = (Map<String, ?>) parsedJson.get("data");
            var updates = (List<?>) data.get("xwa2_users_updates_since");
            var latestUpdate = (Map<String, ?>) updates.getFirst();
            var updatesData = (List<?>) latestUpdate.get("updates");
            var latestUpdateData = (Map<String, ?>) updatesData.getFirst();
            return Optional.of(new UserChosenNameResponse(Optional.ofNullable((String) latestUpdateData.get("text"))));
        } catch (Throwable throwable) {
            return Optional.empty();
        }
    }
}
