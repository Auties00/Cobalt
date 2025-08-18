package it.auties.whatsapp.model.response;

import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;

public final class UserAboutResponse {
    private final String about;
    private final ZonedDateTime timestamp;

    private UserAboutResponse(String about, ZonedDateTime timestamp) {
        this.about = about;
        this.timestamp = timestamp;
    }

    public static UserAboutResponse of(Node node) {
        var about = node.contentAsString()
                .orElse(null);
        var timestamp = node.attributes()
                .getLong("t");
        var parsedTimestamp = Clock.parseSeconds(timestamp)
                .orElse(null);
        return new UserAboutResponse(about, parsedTimestamp);
    }

    public Optional<String> about() {
        return Optional.ofNullable(about);
    }

    public Optional<ZonedDateTime> timestamp() {
        return Optional.ofNullable(timestamp);
    }
}