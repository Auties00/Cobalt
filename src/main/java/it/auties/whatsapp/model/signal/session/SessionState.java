package it.auties.whatsapp.model.signal.session;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
@Builder
@Jacksonized
@Accessors(fluent = true)
public class SessionState {
    @Getter
    private final int version;

    @Getter
    private final int registrationId;

    @Getter
    private final byte @NonNull [] baseKey;

    @Getter
    private final byte @NonNull [] remoteIdentityKey;

    @NonNull
    @Default
    private final ConcurrentHashMap<String, SessionChain> chains = new ConcurrentHashMap<>();

    @Getter
    @Setter
    private byte @NonNull [] rootKey;

    @Getter
    @Setter
    private SessionPreKey pendingPreKey;

    @NonNull
    @Getter
    @Setter
    private SignalKeyPair ephemeralKeyPair;

    @Getter
    @Setter
    private byte @NonNull [] lastRemoteEphemeralKey;

    @Getter
    @Setter
    private int previousCounter;

    @Getter
    @Setter
    private boolean closed;

    public boolean hasChain(byte[] senderEphemeral) {
        return chains.containsKey(Bytes.of(senderEphemeral)
                .toHex());
    }

    public Optional<SessionChain> findChain(byte[] senderEphemeral) {
        return Optional.ofNullable(chains.get(Bytes.of(senderEphemeral)
                .toHex()));
    }

    public SessionState addChain(byte[] senderEphemeral, SessionChain chain) {
        chains.put(Bytes.of(senderEphemeral)
                .toHex(), chain);
        return this;
    }

    public void removeChain(byte[] senderEphemeral) {
        var hex = Bytes.of(senderEphemeral)
                .toHex();
        Objects.requireNonNull(chains.remove(hex), "Cannot remove chain");
    }

    public boolean hasPreKey() {
        return pendingPreKey != null;
    }

    public boolean contentEquals(int version, byte[] baseKey) {
        return version() == version && Arrays.equals(baseKey(), baseKey);
    }

    public boolean equals(Object other) {
        return other instanceof SessionState that && contentEquals(that.version(), that.baseKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(version(), Arrays.hashCode(baseKey()));
    }
}
