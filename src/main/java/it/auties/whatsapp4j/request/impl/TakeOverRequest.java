package it.auties.whatsapp4j.request.impl;

import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.request.model.JsonRequest;
import it.auties.whatsapp4j.response.impl.DiscardResponse;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * A JSON request used to reclaim a previously started session, interrupted by the user logging in from another location
 */
public abstract class TakeOverRequest extends JsonRequest<DiscardResponse>{
    private final @NotNull WhatsappKeysManager whatsappKeys;
    public TakeOverRequest(@NotNull WhatsappConfiguration configuration, @NotNull WhatsappKeysManager whatsappKeys){
        super(configuration);
        this.whatsappKeys = whatsappKeys;
    }

    @Override
    public @NotNull List<Object> buildBody() {
        return List.of("admin", "login", Objects.requireNonNull(whatsappKeys.clientToken()), Objects.requireNonNull(whatsappKeys.serverToken()), whatsappKeys.clientId(), "takeover");
    }
}
