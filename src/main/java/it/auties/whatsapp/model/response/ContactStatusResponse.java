package it.auties.whatsapp.model.response;

import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.util.Clock;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.ZonedDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

public record ContactStatusResponse(@NonNull String status, @NonNull Optional<ZonedDateTime> timestamp) implements ResponseWrapper {
    public ContactStatusResponse(@NonNull Node source) {
        this(
                source.contentAsString().orElseThrow(() -> new NoSuchElementException("Missing status")),
                Clock.parseSeconds(source.attributes().getLong("t"))
        );
    }
}
