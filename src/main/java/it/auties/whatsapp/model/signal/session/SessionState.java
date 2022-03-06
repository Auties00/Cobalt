package it.auties.whatsapp.model.signal.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class SessionState {
    private static final int MAX_SESSIONS = 40;

    @JsonProperty("version")
    private int version;

    @JsonProperty("id")
    private int registrationId;

    @JsonProperty("root_key")
    private byte @NonNull [] rootKey;

    @JsonProperty("ephemeral_key_pair")
    @NonNull
    private SignalKeyPair ephemeralKeyPair;

    @JsonProperty("last_remote_ephemeral_key")
    private byte @NonNull [] lastRemoteEphemeralKey;

    @JsonProperty("previous_counter")
    private int previousCounter;

    @JsonProperty("remote_identity_key")
    private byte @NonNull [] remoteIdentityKey;

    @JsonProperty("chains")
    @NonNull
    @Default
    private List<SessionChain> chains = new ArrayList<>();

    @JsonProperty("pending_pre_key")
    private SessionPreKey pendingPreKey;

    @JsonProperty("base_key")
    private byte @NonNull [] baseKey;

    @JsonProperty("closed")
    private boolean closed;

    public boolean hasChain(byte[] senderEphemeral) {
        return chains.stream()
                .anyMatch(receiverChain -> Arrays.equals(senderEphemeral, receiverChain.owner()));
    }

    public Optional<SessionChain> findChain(byte[] senderEphemeral) {
        return chains.stream()
                .filter(receiverChain -> Arrays.equals(senderEphemeral, receiverChain.owner()))
                .findFirst();
    }

    public SessionState addChain(SessionChain chain) {
        chains.add(chain);
        if (chains().size() <= MAX_SESSIONS) {
            return this;
        }

        chains.remove(0);
        return this;
    }

    public boolean contentEquals(int version, byte[] baseKey){
        return version() == version
                && Arrays.equals(baseKey(), baseKey);
    }
}
