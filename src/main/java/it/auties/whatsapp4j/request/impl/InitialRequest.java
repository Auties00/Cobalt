package it.auties.whatsapp4j.request.impl;

import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.request.model.JsonRequest;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import it.auties.whatsapp4j.api.WhatsappConfiguration;
import lombok.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A JSON request used to signal to WhatsappWeb's WebSocket that the authentication process can begin
 */
public abstract class InitialRequest<M extends JsonResponseModel> extends JsonRequest<M> {
    private final @NonNull WhatsappKeysManager whatsappKeys;
    public InitialRequest(@NonNull WhatsappConfiguration configuration, @NonNull WhatsappKeysManager whatsappKeys){
        super(configuration);
        this.whatsappKeys = whatsappKeys;
    }

    @Override
    public @NonNull List<Object> buildBody() {
        var description = List.of(configuration.description(), configuration.shortDescription());
        return List.of("admin", "init", parseVersion(), description, whatsappKeys.clientId(), true);
    }

    private @NonNull List<Integer> parseVersion() {
        return Arrays.stream(configuration.whatsappVersion().split("\\."))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }
}
