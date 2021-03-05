package it.auties.whatsapp4j.request.impl;


import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.binary.BinaryArray;
import it.auties.whatsapp4j.model.WhatsappConfiguration;
import it.auties.whatsapp4j.request.model.JsonRequest;
import it.auties.whatsapp4j.response.impl.json.DiscardResponse;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.List;
import java.util.Objects;
public abstract class SolveChallengeRequest extends JsonRequest<DiscardResponse> {
    private final @NotNull WhatsappKeysManager whatsappKeys;
    private final @NotNull BinaryArray challenge;

    public SolveChallengeRequest(@NotNull WhatsappConfiguration configuration, @NotNull WhatsappKeysManager whatsappKeys, @NotNull BinaryArray challenge) {
        super(configuration);
        this.whatsappKeys = whatsappKeys;
        this.challenge = challenge;
    }

    @Override
    public @NotNull List<Object> buildBody() {
        return List.of("admin", "challenge", Base64.getEncoder().encodeToString(challenge.data()), Objects.requireNonNull(whatsappKeys.serverToken()), whatsappKeys.clientId());
    }
}
