package it.auties.whatsapp4j.request.impl;

import it.auties.whatsapp4j.model.WhatsappConfiguration;
import it.auties.whatsapp4j.request.model.JsonRequest;
import it.auties.whatsapp4j.response.impl.json.DiscardResponse;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class SubscribeUserPresence extends JsonRequest<DiscardResponse> {
    private final @NotNull String jid;

    public SubscribeUserPresence(@NotNull WhatsappConfiguration configuration, @NotNull String jid) {
        super(configuration);
        this.jid = jid;
    }

    @Override
    public @NotNull List<Object> buildBody() {
        return List.of("action", "presence", "subscribe", jid);
    }
}
