package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.protobuf.signal.key.SignalKeyPair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SessionStructure {
    private static final int MAX_MESSAGE_KEYS = 2000;

    @JsonProperty(value = "1")
    @JsonPropertyDescription("uint32")
    private int sessionVersion;

    @JsonProperty(value = "2")
    @JsonPropertyDescription("bytes")
    private byte[] localIdentityPublic;

    @JsonProperty(value = "3")
    @JsonPropertyDescription("bytes")
    private byte[] remoteIdentityKey;

    @JsonProperty(value = "4")
    @JsonPropertyDescription("bytes")
    private byte[] rootKey;

    @JsonProperty(value = "5")
    @JsonPropertyDescription("uint32")
    private int previousCounter;

    @JsonProperty(value = "6")
    @JsonPropertyDescription("Chain")
    private Chain senderChain;

    @JsonProperty(value = "7")
    @JsonPropertyDescription("Chain")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<Chain> receiverChains;

    @JsonProperty(value = "8")
    @JsonPropertyDescription("PendingKeyExchange")
    private PendingKeyExchange pendingKeyExchange;

    @JsonProperty(value = "9")
    @JsonPropertyDescription("PendingPreKey")
    private PendingPreKey pendingPreKey;

    @JsonProperty(value = "10")
    @JsonPropertyDescription("uint32")
    private int remoteRegistrationId;

    @JsonProperty(value = "11")
    @JsonPropertyDescription("uint32")
    private int localRegistrationId;

    @JsonProperty(value = "12")
    @JsonPropertyDescription("bool")
    private boolean needsRefresh;

    @JsonProperty(value = "13")
    @JsonPropertyDescription("bytes")
    private byte[] aliceBaseKey;

    public int sessionVersion() {
        return sessionVersion == 0 ? 2 : sessionVersion;
    }

    public byte[] remoteIdentityKey() {
        return remoteIdentityKey != null ? Arrays.copyOfRange(remoteIdentityKey, 1, remoteIdentityKey.length)
                : new byte[32];
    }

    public byte[] localIdentityPublic() {
        return localIdentityPublic != null ? Arrays.copyOfRange(localIdentityPublic, 1, localIdentityPublic.length)
                : new byte[32];
    }

    public byte[] publicSenderRatchetKey() {
        return hasSenderChain() ? Arrays.copyOfRange(senderChain.senderRatchetKey(), 1, senderChain.senderRatchetKey().length)
                : new byte[32];
    }

    public byte[] privateSenderRatchetKey() {
        return hasSenderChain() ? senderChain.senderRatchetKeyPrivate()
                : new byte[32];
    }

    public SignalKeyPair senderRatchetKeyPair() {
        return new SignalKeyPair(publicSenderRatchetKey(), privateSenderRatchetKey());
    }

    public boolean hasSenderChain() {
        return senderChain != null;
    }

    public Optional<ChainKey> receiverChainKey(byte[] senderEphemeral) {
        for (var index = 0; index < receiverChains.size(); index++) {
            var receiverChain = receiverChains.get(index);
            if (!Arrays.equals(senderEphemeral, Arrays.copyOfRange(receiverChain.senderRatchetKey(), 1, receiverChain.senderRatchetKey().length))) {
                continue;
            }

            return Optional.of(new ChainKey(index, receiverChain.chainKey().key()));
        }

        return Optional.empty();
    }

    public void addReceiverChain(Chain chain) {
        receiverChains.add(chain);
        if (receiverChains().size() <= 5) {
            return;
        }

        receiverChains.remove(0);
    }

    public boolean hasUnacknowledgedPreKeyMessage() {
        return pendingPreKey() != null;
    }

    public UnacknowledgedPreKeyMessageItems unacknowledgedPreKeyMessageItems() {
        var key = Arrays.copyOfRange(pendingPreKey().baseKey(), 0, pendingPreKey().baseKey().length);
        return new UnacknowledgedPreKeyMessageItems(pendingPreKey.preKeyId(), pendingPreKey.signedPreKeyId(), key);
    }
}
