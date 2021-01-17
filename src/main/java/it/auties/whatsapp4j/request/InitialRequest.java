package it.auties.whatsapp4j.request;

import it.auties.whatsapp4j.model.WhatsappKeys;
import it.auties.whatsapp4j.api.WhatsappConfiguration;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record InitialRequest(@NotNull WhatsappKeys keys, @NotNull WhatsappConfiguration options) implements Request {
    @Override
    public @NotNull List<Object> buildBody() {
        final var version = List.of(2, 2049, 10);
        final var description = List.of(options.description(), options.shortDescription());
        return List.of("admin", "init", version, description, keys.clientId(), true);
    }

    @Override
    public @NotNull String tag() {
        return options.tag();
    }
}
