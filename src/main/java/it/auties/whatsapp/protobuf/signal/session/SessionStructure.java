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

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
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

    @JsonProperty("1")
    @JsonPropertyDescription("uint32")
    private int sessionVersion;

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
    private Chain senderChain;

    @JsonProperty("7")
    @JsonPropertyDescription("Chain")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<Chain> receiverChains;

    @JsonProperty("8")
    @JsonPropertyDescription("PendingKeyExchange")
    private PendingKeyExchange pendingKeyExchange;

    @JsonProperty("9")
    @JsonPropertyDescription("PendingPreKey")
    private PendingPreKey pendingPreKey;

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

    public Optional<Chain> receiverChain(byte[] senderEphemeral) {
        return receiverChains.stream()
                .filter(receiverChain -> Arrays.equals(senderEphemeral, Arrays.copyOfRange(receiverChain.senderRatchetKey(), 1, receiverChain.senderRatchetKey().length)))
                .findFirst();
    }

    public Optional<ChainKey> receiverChainKey(byte[] senderEphemeral) {
        return receiverChain(senderEphemeral)
                .map(chain -> new ChainKey(receiverChains.indexOf(chain), chain.chainKey().key()));
    }

    public void receiverChainKey(byte[] senderEphemeral, ChainKey chainKey) {
        receiverChain(senderEphemeral)
                .ifPresent(chain -> chain.chainKey(chainKey));
    }

    public void addReceiverChain(byte[] senderRatchetKey, ChainKey chainKey) {
        var chain = new Chain(senderRatchetKey, chainKey);
        addReceiverChain(chain);
    }

    public void addReceiverChain(Chain chain) {
        receiverChains.add(chain);
        if (receiverChains().size() <= 5) {
            return;
        }

        receiverChains.remove(0);
    }

    public void senderChain(SignalKeyPair senderRatchetKeyPair, ChainKey chainKey) {
        this.senderChain = new Chain(senderRatchetKeyPair, chainKey);
    }

    public boolean hasUnacknowledgedPreKeyMessage() {
        return pendingPreKey() != null;
    }

    public void unacknowledgedPreKeyMessage(int preKeyId, int signedPreKeyId, byte[] baseKey) {
        this.pendingPreKey = new PendingPreKey(preKeyId, baseKey, signedPreKeyId);
    }

    public UnacknowledgedPreKeyMessageItems unacknowledgedPreKeyMessageItems() {
        var key = Arrays.copyOfRange(pendingPreKey().baseKey(), 0, pendingPreKey().baseKey().length);
        return new UnacknowledgedPreKeyMessageItems(pendingPreKey.preKeyId(), pendingPreKey.signedPreKeyId(), key);
    }

    public boolean hasMessageKeys(byte[] senderEphemeral, int counter) {
        var chain = receiverChain(senderEphemeral);
        return chain.isPresent() && chain.get()
                .messageKeys()
                .stream()
                .anyMatch(messageKey -> messageKey.index() == counter);
    }

    public Optional<MessageKeys> removeMessageKeys(byte[] senderEphemeral, int counter) {
        var chain = receiverChain(senderEphemeral);
        if(chain.isEmpty()){
            return Optional.empty();
        }

        return chain.get().messageKeys()
                .stream()
                .filter(messageKey -> messageKey.index() == counter)
                .findFirst()
                .map(messageKey -> createMessageKeys(chain.get(), messageKey));
    }

    private MessageKeys createMessageKeys(Chain chain, MessageKey messageKey) {
        chain.messageKeys().remove(messageKey);
        return new MessageKeys(new SecretKeySpec(messageKey.cipherKey(), "AES"),
                new SecretKeySpec(messageKey.macKey(), "HmacSHA256"),
                new IvParameterSpec(messageKey.iv()),
                messageKey.index());
    }

    public void messageKeys(byte[] senderEphemeral, MessageKeys messageKeys) {
        receiverChain(senderEphemeral).ifPresent(chain -> {
            var messageKey = new MessageKey(messageKeys.counter(), messageKeys.cipherKey().getEncoded(), messageKeys.macKey().getEncoded(), messageKeys.iv().getIV());
            chain.messageKeys().add(messageKey);
            if (chain.messageKeys().size() <= MAX_MESSAGE_KEYS) {
                return;
            }

            chain.messageKeys().remove(0);
        });
    }
}
