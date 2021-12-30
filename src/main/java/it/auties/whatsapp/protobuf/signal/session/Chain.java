package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.auties.whatsapp.crypto.SignalHelper;
import it.auties.whatsapp.protobuf.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.util.BytesDeserializer;
import it.auties.whatsapp.util.SignalKeyDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class Chain {
    @JsonProperty("1")
    @JsonPropertyDescription("bytes")
    @JsonDeserialize(using = SignalKeyDeserializer.class)
    private byte[] senderRatchetPublicKey;

    @JsonProperty("2")
    @JsonPropertyDescription("bytes")
    @JsonDeserialize(using = BytesDeserializer.class)
    private byte[] senderRatchetPrivateKey;

    @JsonProperty("3")
    @JsonPropertyDescription("ChainKey")
    private ChainKey chainKey;

    @JsonProperty("4")
    @JsonPropertyDescription("MessageKey")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<MessageKey> messageKeys = new ArrayList<>();

    public Chain(byte[] senderRatchetKey, ChainKey chainKey) {
        this.senderRatchetPublicKey = SignalHelper.removeKeyHeader(senderRatchetKey);
        this.chainKey = chainKey;
    }

    public Chain(SignalKeyPair senderRatchetKeyPair, ChainKey chainKey) {
        this.senderRatchetPublicKey =  SignalHelper.removeKeyHeader(senderRatchetKeyPair.publicKey());
        this.senderRatchetPrivateKey = senderRatchetKeyPair.privateKey();
        this.chainKey = chainKey;
    }

    public SignalKeyPair senderRatchetKeyPair() {
        return new SignalKeyPair(senderRatchetPublicKey, senderRatchetPrivateKey);
    }
}