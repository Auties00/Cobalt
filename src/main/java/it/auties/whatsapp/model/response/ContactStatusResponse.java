package it.auties.whatsapp.model.response;

import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.Clock;
import lombok.NonNull;

import java.time.ZonedDateTime;
import java.util.NoSuchElementException;

public record ContactStatusResponse(@NonNull String status, @NonNull ZonedDateTime timestamp) implements ResponseWrapper {
    public ContactStatusResponse(@NonNull Node source) {
        this(
                source.contentAsString().orElseThrow(() -> new NoSuchElementException("Missing status")),
                Clock.parseSeconds(source.attributes().getLong("t"))
        );
    }
}
