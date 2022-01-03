package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.auties.whatsapp.crypto.SignalHelper;
import it.auties.whatsapp.protobuf.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.util.BytesDeserializer;
import it.auties.whatsapp.util.SignalKeyDeserializer;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SessionChain {
    @JsonProperty("1")
    @JsonPropertyDescription("bytes")
    @JsonDeserialize(using = SignalKeyDeserializer.class)
    private byte[] publicKey;

    @JsonProperty("2")
    @JsonPropertyDescription("bytes")
    @JsonDeserialize(using = BytesDeserializer.class)
    private byte[] privateKey;

    @JsonProperty("3")
    @JsonPropertyDescription("ChainKey")
    private SessionChainKey key;

    @JsonProperty("4")
    @JsonPropertyDescription("MessageKey")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<SessionChainKey> messageKeys = new ArrayList<>();

    public SessionChain(byte[] senderRatchetKey, SessionChainKey chainKey) {
        this.publicKey = SignalHelper.removeKeyHeader(senderRatchetKey);
        this.key = chainKey;
    }

    public SessionChain(SignalKeyPair senderRatchetKeyPair, SessionChainKey chainKey) {
        this.publicKey =  SignalHelper.removeKeyHeader(senderRatchetKeyPair.publicKey());
        this.privateKey = senderRatchetKeyPair.privateKey();
        this.key = chainKey;
    }

    public boolean hasMessageKey(int counter){
        return messageKeys()
                .stream()
                .map(SessionChainKey::index)
                .anyMatch(index -> index == counter);
    }
}