package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;
import it.auties.whatsapp.protobuf.signal.key.IdentityKeyPair;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.kdf.HKDF;
import org.whispersystems.libsignal.ratchet.ChainKey;

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
  private byte[] remoteIdentityPublic;

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

  public Optional<byte[]> remoteIdentityKey() {
    if(remoteIdentityPublic == null){
      return Optional.empty();
    }
    
    return Optional.of(Arrays.copyOfRange(remoteIdentityPublic, 1, remoteIdentityPublic.length));
  }

  public Optional<byte[]> localIdentityPublic() {
    if(localIdentityPublic == null){
      return Optional.empty();
    }

    return Optional.of(Arrays.copyOfRange(localIdentityPublic, 1, localIdentityPublic.length));
  }
  
  public byte[] publicSenderRatchetKey() {
    return  hasSenderChain() ? Arrays.copyOfRange(senderChain.senderRatchetKey(), 1, senderChain.senderRatchetKey().length)
            : new byte[32];
  }

  public byte[] privateSenderRatchetKey() {
    return hasSenderChain() ? senderChain.senderRatchetKeyPrivate() 
            : new byte[32];
  }

  public IdentityKeyPair senderRatchetKeyPair() {
    return new IdentityKeyPair(publicSenderRatchetKey(), privateSenderRatchetKey());
  }

  public boolean hasSenderChain() {
    return senderChain != null;
  }

  public Optional<Chain.ChainKey> receiverChainKey(byte[] senderEphemeral) {
    for (var index = 0; index < receiverChains.size(); index++) {
      var receiverChain = receiverChains.get(index);
      if (!Arrays.equals(senderEphemeral, Arrays.copyOfRange(receiverChain.senderRatchetKey(), 1, receiverChain.senderRatchetKey().length))) {
        continue;
      }

      return Optional.of(new Chain.ChainKey(index, receiverChain.chainKey().key()));
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

  @AllArgsConstructor
  @NoArgsConstructor
  @Data
  @Builder
  @Accessors(fluent = true)
  public static class Chain {
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

    public Chain(byte[] senderRatchetKey, ChainKey chainKey){
      this.senderRatchetKey = senderRatchetKey;
      this.chainKey = chainKey;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    @Accessors(fluent = true)
    public static class ChainKey {
      @JsonProperty(value = "1")
      @JsonPropertyDescription("uint32")
      private int index;
      
      @JsonProperty(value = "2")
      @JsonPropertyDescription("bytes")
      private byte[] key;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    @Accessors(fluent = true)
    public static class MessageKey {
      @JsonProperty(value = "1")
      @JsonPropertyDescription("uint32")
      private int index;

      @JsonProperty(value = "2")
      @JsonPropertyDescription("bytes")
      private byte[] cipherKey;

      @JsonProperty(value = "3")
      @JsonPropertyDescription("bytes")
      private byte[] macKey;
      
      @JsonProperty(value = "4")
      @JsonPropertyDescription("bytes")
      private byte[] iv;
    }
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Data
  @Builder
  @Accessors(fluent = true)
  public static class PendingKeyExchange {
    @JsonProperty(value = "1")
    @JsonPropertyDescription("uint32")
    private int sequence;

    @JsonProperty(value = "2")
    @JsonPropertyDescription("bytes")
    private byte[] localBaseKey;

    @JsonProperty(value = "3")
    @JsonPropertyDescription("bytes")
    private byte[] localBaseKeyPrivate;

    @JsonProperty(value = "4")
    @JsonPropertyDescription("bytes")
    private byte[] localRatchetKey;

    @JsonProperty(value = "5")
    @JsonPropertyDescription("bytes")
    private byte[] localRatchetKeyPrivate;

    @JsonProperty(value = "7")
    @JsonPropertyDescription("bytes")
    private byte[] localIdentityKey;
    
    @JsonProperty(value = "8")
    @JsonPropertyDescription("bytes")
    private byte[] localIdentityKeyPrivate;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Data
  @Builder
  @Accessors(fluent = true)
  public static class PendingPreKey {
    @JsonProperty(value = "1")
    @JsonPropertyDescription("uint32")
    private int preKeyId;
    
    @JsonProperty(value = "2")
    @JsonPropertyDescription("bytes")
    private byte[] baseKey;

    @JsonProperty(value = "3")
    @JsonPropertyDescription("int32")
    private int signedPreKeyId;
  }
}
