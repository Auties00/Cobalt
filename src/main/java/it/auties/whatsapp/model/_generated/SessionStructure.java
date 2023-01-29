package it.auties.whatsapp.model._generated;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Jacksonized
@Builder
@ProtobufName("SessionStructure")
public class SessionStructure implements ProtobufMessage {
  @ProtobufProperty(index = 1, name = "sessionVersion", type = ProtobufType.UINT32)
  private Integer sessionVersion;

  @ProtobufProperty(index = 2, name = "localIdentityPublic", type = ProtobufType.BYTES)
  private byte[] localIdentityPublic;

  @ProtobufProperty(index = 3, name = "remoteIdentityPublic", type = ProtobufType.BYTES)
  private byte[] remoteIdentityPublic;

  @ProtobufProperty(index = 4, name = "rootKey", type = ProtobufType.BYTES)
  private byte[] rootKey;

  @ProtobufProperty(index = 5, name = "previousCounter", type = ProtobufType.UINT32)
  private Integer previousCounter;

  @ProtobufProperty(index = 6, name = "senderChain", type = ProtobufType.MESSAGE)
  private Chain senderChain;

  @ProtobufProperty(implementation = Chain.class, index = 7, name = "receiverChains", repeated = true, type = ProtobufType.MESSAGE)
  private List<Chain> receiverChains;

  @ProtobufProperty(index = 8, name = "pendingKeyExchange", type = ProtobufType.MESSAGE)
  private PendingKeyExchange pendingKeyExchange;

  @ProtobufProperty(index = 9, name = "pendingPreKey", type = ProtobufType.MESSAGE)
  private PendingPreKey pendingPreKey;

  @ProtobufProperty(index = 10, name = "remoteRegistrationId", type = ProtobufType.UINT32)
  private Integer remoteRegistrationId;

  @ProtobufProperty(index = 11, name = "localRegistrationId", type = ProtobufType.UINT32)
  private Integer localRegistrationId;

  @ProtobufProperty(index = 12, name = "needsRefresh", type = ProtobufType.BOOL)
  private Boolean needsRefresh;

  @ProtobufProperty(index = 13, name = "aliceBaseKey", type = ProtobufType.BYTES)
  private byte[] aliceBaseKey;
}