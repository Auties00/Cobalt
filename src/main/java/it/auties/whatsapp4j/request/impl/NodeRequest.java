package it.auties.whatsapp4j.request.impl;

import it.auties.whatsapp4j.model.WhatsappConfiguration;
import it.auties.whatsapp4j.request.model.BinaryRequest;
import it.auties.whatsapp4j.response.model.shared.ResponseModel;
import it.auties.whatsapp4j.model.WhatsappNode;
import org.jetbrains.annotations.NotNull;

public abstract class NodeRequest<M extends ResponseModel> extends BinaryRequest<M> {
    private final @NotNull WhatsappNode node;
    private final boolean completable;
    public NodeRequest(@NotNull String tag, @NotNull WhatsappConfiguration configuration, @NotNull WhatsappNode node){
        super(tag, configuration);
        this.node = node;
        this.completable = true;
    }
    public NodeRequest(@NotNull WhatsappConfiguration configuration, @NotNull WhatsappNode node){
        super(configuration);
        this.node = node;
        this.completable = true;
    }

    public NodeRequest(@NotNull WhatsappConfiguration configuration, @NotNull WhatsappNode node, boolean completable){
        super(configuration);
        this.node = node;
        this.completable = completable;
    }


    @Override
    public @NotNull WhatsappNode buildBody() {
        return node;
    }

    @Override
    public boolean isCompletable() {
        return completable;
    }
}
