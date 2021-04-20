package it.auties.whatsapp4j.request.impl;

import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.model.WhatsappNode;
import it.auties.whatsapp4j.request.model.BinaryRequest;
import it.auties.whatsapp4j.response.model.ResponseModel;
import jakarta.validation.constraints.NotNull;

/**
 * A binary request used to transmit a {@link WhatsappNode} to WhatsappWeb's WebSocket
 */
public abstract class NodeRequest<M extends ResponseModel> extends BinaryRequest<M> {
    private final @NotNull WhatsappNode node;
    public NodeRequest(@NotNull String tag, @NotNull WhatsappConfiguration configuration, @NotNull WhatsappNode node){
        super(tag, configuration);
        this.node = node;
    }

    public NodeRequest(@NotNull WhatsappConfiguration configuration, @NotNull WhatsappNode node){
        super(configuration);
        this.node = node;
    }

    @Override
    public @NotNull WhatsappNode buildBody() {
        return node;
    }
}
