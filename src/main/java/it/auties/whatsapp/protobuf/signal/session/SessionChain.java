package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.auties.whatsapp.crypto.SignalHelper;
import it.auties.whatsapp.protobuf.signal.keypair.SignalKeyPair;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNullElseGet;

@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SessionChain {
    @JsonProperty("1")
    @JsonPropertyDescription("bytes")
    private byte[] publicKey;

    @JsonProperty("2")
    @JsonPropertyDescription("bytes")
    private byte[] privateKey;

    @JsonProperty("3")
    @JsonPropertyDescription("ChainKey")
    private SessionChainKey key;

    @JsonProperty("4")
    @JsonPropertyDescription("MessageKey")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<SessionChainKey> messageKeys = new ArrayList<>();

    public SessionChain(byte[] publicKey, byte[] privateKey, SessionChainKey key, List<SessionChainKey> messageKeys) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.key = key;
        this.messageKeys = requireNonNullElseGet(messageKeys, ArrayList::new);
    }

    public SessionChain(SignalKeyPair senderRatchetKeyPair, SessionChainKey chainKey) {
        this.publicKey = senderRatchetKeyPair.publicKey();
        this.privateKey = senderRatchetKeyPair.privateKey();
        this.key = chainKey;
    }

    public SessionChain(byte[] senderRatchetKey, SessionChainKey chainKey) {
        this.publicKey = senderRatchetKey;
        this.key = chainKey;
    }

    public boolean hasMessageKey(int counter){
        return messageKeys()
                .stream()
                .map(SessionChainKey::index)
                .anyMatch(index -> index == counter);
    }
}