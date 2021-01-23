package it.auties.whatsapp4j.request;


import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.utils.BytesArray;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.List;
import java.util.Objects;

public class SolveChallengeRequest extends WhatsappRequest{
    private final @NotNull BytesArray challenge;
    public SolveChallengeRequest(@NotNull WhatsappKeysManager keysManager, @NotNull WhatsappConfiguration options, @NotNull BytesArray challenge) {
        super(keysManager, options);
        this.challenge = challenge;
    }

    @Override
    public @NotNull List<Object> buildBody() {
        return List.of("admin", "challenge", Base64.getEncoder().encodeToString(challenge.data()), Objects.requireNonNull(keysManager.serverToken()), keysManager.clientId());
    }

    @Override
    public @NotNull String tag() {
        return options.tag();
    }
}
