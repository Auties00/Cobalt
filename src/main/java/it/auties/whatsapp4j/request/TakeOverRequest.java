package it.auties.whatsapp4j.request;


import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class TakeOverRequest extends WhatsappRequest {
    public TakeOverRequest(@NotNull WhatsappKeysManager keysManager, @NotNull WhatsappConfiguration options) {
        super(keysManager, options);
    }

    @Override
    public @NotNull List<Object> buildBody() {
        return List.of("admin", "login", Objects.requireNonNull(keysManager.clientToken()), Objects.requireNonNull(keysManager.serverToken()), keysManager.clientId(), "takeover");
    }

    @Override
    public @NotNull String tag() {
        return options.tag();
    }
}
