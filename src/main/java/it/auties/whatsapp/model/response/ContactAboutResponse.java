package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.core.type.TypeReference;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Json;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record ContactAboutResponse(Optional<String> about, Optional<ZonedDateTime> timestamp) {
    public static ContactAboutResponse ofNode(Node source) {
        return new ContactAboutResponse(
                source.contentAsString(),
                Clock.parseSeconds(source.attributes().getLong("t"))
        );
    }

    @SuppressWarnings("unchecked")
    public static Optional<ContactAboutResponse> ofJson(String json) {
        try {
            var parsedJson = Json.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            var data = (Map<String, ?>) parsedJson.get("data");
            var updates = (List<?>) data.get("xwa2_users_updates_since");
            var latestUpdate = (Map<String, ?>) updates.getFirst();
            var updatesData = (List<?>) latestUpdate.get("updates");
            var latestUpdateData = (Map<String, ?>) updatesData.getFirst();
            return Optional.of(new ContactAboutResponse(Optional.ofNullable((String) latestUpdateData.get("text")), Optional.empty()));
        } catch (Throwable throwable) {
            return Optional.empty();
        }
    }
}
