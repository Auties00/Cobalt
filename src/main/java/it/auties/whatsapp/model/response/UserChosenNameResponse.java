package it.auties.whatsapp.model.response;

import io.avaje.jsonb.Json;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Json
public final class UserChosenNameResponse {
    private static final UserChosenNameResponse EMPTY = new UserChosenNameResponse(null);

    private final String name;

    private UserChosenNameResponse(String name) {
        this.name = name;
    }

    @Json.Creator
    static UserChosenNameResponse of(@Json.Unmapped Map<String, Object> json) {
        if(!(json.get("data") instanceof Map<?,?> data)) {
            return EMPTY;
        }

        if(!(data.get("xwa2_users_updates_since") instanceof List<?> responses) || responses.isEmpty()) {
            return EMPTY;
        }

        if(!(responses.getFirst() instanceof Map<?,?> response)) {
            return EMPTY;
        }

        if(!(response.get("updates") instanceof List<?> updates) || updates.isEmpty()) {
            return EMPTY;
        }

        if(!(updates.getFirst() instanceof Map<?,?> update)) {
            return EMPTY;
        }

        if(!(update.get("text") instanceof String name)) {
            return EMPTY;
        }

        return new UserChosenNameResponse(name);
    }

    public Optional<String> name() {
        return Optional.ofNullable(name);
    }
}