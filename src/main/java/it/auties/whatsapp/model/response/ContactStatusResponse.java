package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.core.type.TypeReference;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Json;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record ContactStatusResponse(Optional<String> status, Optional<ZonedDateTime> timestamp) {
    public static ContactStatusResponse ofNode(Node source) {
        return new ContactStatusResponse(
                source.contentAsString(),
                Clock.parseSeconds(source.attributes().getLong("t"))
        );
    }

    @SuppressWarnings("unchecked")
    public static Optional<ContactStatusResponse> ofJson(String json) {
        try {
            var parsedJson = Json.readValue(json, new TypeReference<Map<String, Object>>() {});
            var data = (Map<String, ?>) parsedJson.get("data");
            var updates = (List<?>) data.get("xwa2_users_updates_since");
            var latestUpdate = (Map<String, ?>) updates.get(0);
            var updatesData = (List<?>) latestUpdate.get("updates");
            var latestUpdateData = (Map<String, ?>) updatesData.get(0);
            return Optional.of(new ContactStatusResponse(Optional.ofNullable((String) latestUpdateData.get("text")), Optional.empty()));
        } catch (Throwable throwable) {
            return Optional.empty();
        }
    }
}
