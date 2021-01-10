package it.auties.whatsapp4j.model;


import it.auties.whatsapp4j.api.WhatsappKeys;
import it.auties.whatsapp4j.configuration.WhatsappConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public record TakeOverRequest(@NotNull WhatsappKeys keys, @NotNull WhatsappConfiguration options) implements Request {
    @Override
    public @NotNull List<Object> buildBody() {
        return List.of("admin", "login", Objects.requireNonNull(keys.clientToken()), Objects.requireNonNull(keys.serverToken()), keys.clientId(), "takeover");
    }

    @Override
    public @NotNull String tag() {
        return options.tag();
    }
}
