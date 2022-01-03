package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.auties.whatsapp.protobuf.signal.keypair.SignalKeyPair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.Delegate;

import java.util.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SessionState {
    private static final int MAX_MESSAGE_KEYS = 2000;
    private static final String AES = "AES";
    private static final String HMAC = "HmacSHA256";

    @JsonProperty("1")
    @JsonPropertyDescription("uint32")
    private int version;

    @JsonProperty("2")
    @JsonPropertyDescription("bytes")
    private byte[] localIdentityPublic;

    @JsonProperty("3")
    @JsonPropertyDescription("bytes")
    private byte[] remoteIdentityKey;

    @JsonProperty("4")
    @JsonPropertyDescription("bytes")
    private byte[] rootKey;

    @JsonProperty("5")
    @JsonPropertyDescription("uint32")
    private int previousCounter;

    @JsonProperty("6")
    @JsonPropertyDescription("Chain")
    @Delegate
    private SessionChain senderChain;

    @JsonProperty("7")
    @JsonPropertyDescription("Chain")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<SessionChain> receiverChains = new ArrayList<>();

    @JsonProperty("8")
    @JsonPropertyDescription("PendingKeyExchange")
    private SessionPendingKey pendingKeyExchange;

    @JsonProperty("9")
    @JsonPropertyDescription("PendingPreKey")
    private SessionPreKey pendingPreKey;

    @JsonProperty("10")
    @JsonPropertyDescription("uint32")
    private int remoteRegistrationId;

    @JsonProperty("11")
    @JsonPropertyDescription("uint32")
    private int localRegistrationId;

    @JsonProperty("12")
    @JsonPropertyDescription("bool")
    private boolean needsRefresh;

    @JsonProperty("13")
    @JsonPropertyDescription("bytes")
    private byte[] baseKey;

    public SessionState senderChain(SignalKeyPair senderRatchetKeyPair, SessionChainKey chainKey) {
        this.senderChain = new SessionChain(senderRatchetKeyPair, chainKey);
        return this;
    }

    public boolean hasReceiverChain(byte[] senderEphemeral) {
        return receiverChains.stream()
                .anyMatch(receiverChain -> Arrays.equals(senderEphemeral, receiverChain.publicKey()));
    }

    public Optional<SessionChain> findReceiverChain(byte[] senderEphemeral) {
        return receiverChains.stream()
                .filter(receiverChain -> Arrays.equals(senderEphemeral, receiverChain.publicKey()))
                .findFirst();
    }

    public SessionState addReceiverChain(byte[] senderRatchetKey, SessionChainKey chainKey) {
        var chain = new SessionChain(senderRatchetKey, chainKey);
        addReceiverChain(chain);
        return this;
    }

    public SessionState addReceiverChain(SessionChain chain) {
        receiverChains.add(chain);
        if (receiverChains().size() <= 5) {
            return this;
        }

        receiverChains.remove(0);
        return this;
    }

    public boolean contentEquals(int version, byte[] baseKey){
        return version() == version && Arrays.equals(baseKey(), baseKey);
    }
}
