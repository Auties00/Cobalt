package it.auties.whatsapp4j.request.impl;

import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.request.model.JsonRequest;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * A JSON request used to signal to WhatsappWeb's WebSocket that the authentication process can begin
 */
public abstract class InitialRequest<M extends JsonResponseModel> extends JsonRequest<M> {
    private final @NotNull WhatsappKeysManager whatsappKeys;
    public InitialRequest(@NotNull WhatsappConfiguration configuration, @NotNull WhatsappKeysManager whatsappKeys){
        super(configuration);
        this.whatsappKeys = whatsappKeys;
    }

    @Override
    public @NotNull List<Object> buildBody() {
        final var version = List.of(2, 2049, 10);
        final var description = List.of(configuration.description(), configuration.shortDescription());
        return List.of("admin", "init", version, description, whatsappKeys.clientId(), true);
    }
}
