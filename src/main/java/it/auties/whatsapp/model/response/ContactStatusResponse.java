package it.auties.whatsapp.model.response;

import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.Clock;
import lombok.NonNull;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;

public record ContactStatusResponse(@NonNull String status, @NonNull ZonedDateTime timestamp) implements ResponseWrapper {
    public ContactStatusResponse(@NonNull Node source) {
        this(
                new String(source.bytes(), StandardCharsets.UTF_8),
                Clock.parse(source.attributes().getLong("t"))
                        .orElse(ZonedDateTime.now())
        );
    }
}
