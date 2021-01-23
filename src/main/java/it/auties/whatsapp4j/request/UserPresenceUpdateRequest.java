package it.auties.whatsapp4j.request;

import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UserPresenceUpdateRequest extends WhatsappRequest {
    private final @NotNull String jid;
    public UserPresenceUpdateRequest(@NotNull WhatsappKeysManager keysManager, @NotNull WhatsappConfiguration options, @NotNull String jid) {
        super(keysManager, options);
        this.jid = jid;
    }

    @Override
    public @NotNull List<Object> buildBody() {
        return List.of("action", "presence", "subscribe", jid);
    }

    @Override
    public @NotNull String tag() {
        return options.tag();
    }
}
