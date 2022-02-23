package it.auties.whatsapp.protobuf.signal.session;

import it.auties.whatsapp.protobuf.signal.keypair.SignalKeyPair;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
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

    private int version;

    private int registrationId;

    @NonNull
    private byte[] rootKey;

    @NonNull
    private SignalKeyPair ephemeralKeyPair;

    @NonNull
    private byte[] lastRemoteEphemeralKey;

    private int previousCounter;

    @NonNull
    private byte[] remoteIdentityKey;

    @NonNull
    @Default
    private List<SessionChain> chains = new ArrayList<>();

    private SessionPreKey pendingPreKey;

    @NonNull
    private byte[] baseKey;

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
