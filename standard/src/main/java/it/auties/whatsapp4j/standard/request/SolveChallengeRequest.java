package it.auties.whatsapp4j.standard.request;

import it.auties.whatsapp4j.common.api.WhatsappConfiguration;
import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.manager.WhatsappDataManager;
import it.auties.whatsapp4j.common.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.common.request.JsonRequest;
import it.auties.whatsapp4j.common.response.JsonResponseModel;
import lombok.NonNull;

import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * A JSON request used to transmit to WhatsappWeb's WebSocket the resolved challenge.
 * WhatsappWeb's WebSocket sends a challenge when it needs to verify that a pair of keys previously used are still valid.
 * This doesn't happen everytime after the first login, but it's important to handle this case.
 */
public abstract class SolveChallengeRequest<M extends JsonResponseModel> extends JsonRequest<M> {
    private final @NonNull WhatsappKeysManager whatsappKeys;
    private final @NonNull BinaryArray challenge;

    public SolveChallengeRequest(@NonNull WhatsappConfiguration configuration, @NonNull WhatsappKeysManager whatsappKeys, @NonNull BinaryArray challenge) {
        super(configuration);
        this.whatsappKeys = whatsappKeys;
        this.challenge = challenge;
    }

    @Override
    public @NonNull List<Object> buildBody() {
        return List.of("admin", "challenge", Base64.getEncoder().encodeToString(challenge.data()), Objects.requireNonNull(whatsappKeys.serverToken()), whatsappKeys.clientId());
    }
}
