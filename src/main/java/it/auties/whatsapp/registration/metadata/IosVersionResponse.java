package it.auties.whatsapp.registration.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.whatsapp.model.signal.auth.Version;

import java.util.List;
import java.util.Map;
import java.util.Optional;

final class IosVersionResponse {
    private static final IosVersionResponse EMPTY = new IosVersionResponse(null);
    private final Version version;
    IosVersionResponse(Version version) {
        this.version = version;
    }

    @SuppressWarnings("unchecked")
    @JsonCreator
    public static IosVersionResponse of(Map<String, Object> json) {
        var results = (List<Map<String, Object>>) json.get("results");
        if (results.isEmpty()) {
            return EMPTY;
        }

        var result = (String) results.getFirst().get("version");
        if(result == null) {
            return EMPTY;
        }

        return new IosVersionResponse(Version.of("2." + result));
    }

    public Optional<Version> version() {
        return Optional.of(version);
    }
}
