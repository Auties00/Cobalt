package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.protobuf.signal.keypair.SignalKeyPair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class Chain {
    @JsonProperty(value = "1")
    @JsonPropertyDescription("bytes")
    private byte[] senderRatchetKey;

    @JsonProperty(value = "2")
    @JsonPropertyDescription("bytes")
    private byte[] senderRatchetKeyPrivate;

    @JsonProperty(value = "3")
    @JsonPropertyDescription("ChainKey")
    private ChainKey chainKey;

    @JsonProperty(value = "4")
    @JsonPropertyDescription("MessageKey")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<MessageKey> messageKeys;

    public Chain(byte[] senderRatchetKey, ChainKey chainKey) {
        this.senderRatchetKey = senderRatchetKey;
        this.chainKey = chainKey;
    }

    public Chain(SignalKeyPair senderRatchetKeyPair, ChainKey chainKey) {
        this.senderRatchetKey = senderRatchetKeyPair.publicKey();
        this.senderRatchetKeyPrivate = senderRatchetKeyPair.privateKey();
        this.chainKey = chainKey;
    }
}