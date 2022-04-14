package it.auties.whatsapp.model.signal.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import it.auties.bytes.Bytes;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
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
    private Map<String, SessionChain> chains = new HashMap<>();

    @JsonProperty("pending_pre_key")
    private SessionPreKey pendingPreKey;

    @JsonProperty("base_key")
    private byte @NonNull [] baseKey;

    @JsonProperty("closed")
    private boolean closed;

    public boolean hasChain(byte[] senderEphemeral) {
        return chains.containsKey(Bytes.of(senderEphemeral).toHex());
    }

    public Optional<SessionChain> findChain(byte[] senderEphemeral) {
        return Optional.ofNullable(chains.get(Bytes.of(senderEphemeral).toHex()));
    }

    public SessionState addChain(byte[] senderEphemeral, SessionChain chain) {
        chains.put(Bytes.of(senderEphemeral).toHex(), chain);
        return this;
    }

    public SessionState removeChain(byte[] senderEphemeral) {
        Objects.requireNonNull(chains.remove(Bytes.of(senderEphemeral).toHex()),
                "Cannot remove chain");
        return this;
    }

    public boolean hasPreKey(){
        return pendingPreKey != null;
    }

    public boolean contentEquals(int version, byte[] baseKey){
        return version() == version
                && Arrays.equals(baseKey(), baseKey);
    }

    public boolean equals(Object other){
        return other instanceof SessionState that
                && contentEquals(that.version(), that.baseKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(version(), Arrays.hashCode(baseKey()));
    }
}
