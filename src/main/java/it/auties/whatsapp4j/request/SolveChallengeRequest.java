package it.auties.whatsapp4j.request;


import it.auties.whatsapp4j.model.WhatsappKeys;
import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.utils.BytesArray;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.List;
import java.util.Objects;

public record SolveChallengeRequest(@NotNull BytesArray challenge, @NotNull WhatsappKeys keys, @NotNull WhatsappConfiguration options) implements Request {
    @Override
    public @NotNull List<Object> buildBody() {
        return List.of("admin", "challenge", Base64.getEncoder().encodeToString(challenge.data()), Objects.requireNonNull(keys.serverToken()), keys.clientId());
    }

    @Override
    public @NotNull String tag() {
        return options.tag();
    }
}
