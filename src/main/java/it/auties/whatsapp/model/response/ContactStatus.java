package it.auties.whatsapp.model.response;

import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.Clock;
import lombok.NonNull;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;

public record ContactStatus(@NonNull Node response, @NonNull String status, @NonNull ZonedDateTime timestamp) implements ResponseWrapper {
    public ContactStatus(Node source) {
        this(source,
                new String(source.bytes(), StandardCharsets.UTF_8),
                Clock.parse(source.attributes().getLong("t")).orElse(ZonedDateTime.now()));
    }
}
