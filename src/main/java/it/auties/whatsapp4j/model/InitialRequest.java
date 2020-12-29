package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.configuration.WhatsappConfiguration;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record InitialRequest(WhatsappConfiguration options) implements AbstractRequest{
    @Override
    public @NotNull List<Object> buildBody() {
        final var version = List.of(2, 2049, 10);
        final var description = List.of(options.description(), options.shortDescription());
        return List.of("admin", "init", version, description, options.clientId(), true);
    }

    @Override
    public @NotNull String tag() {
        return options.tag();
    }
}
