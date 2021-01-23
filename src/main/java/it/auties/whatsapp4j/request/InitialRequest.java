package it.auties.whatsapp4j.request;

import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class InitialRequest extends WhatsappRequest {
    public InitialRequest(@NotNull WhatsappKeysManager keysManager, @NotNull WhatsappConfiguration options) {
        super(keysManager, options);
    }

    @Override
    public @NotNull List<Object> buildBody() {
        final var version = List.of(2, 2049, 10);
        final var description = List.of(options.description(), options.shortDescription());
        return List.of("admin", "init", version, description, keysManager.clientId(), true);
    }

    @Override
    public @NotNull String tag() {
        return options.tag();
    }
}
