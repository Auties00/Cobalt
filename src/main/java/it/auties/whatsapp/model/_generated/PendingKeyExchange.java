package it.auties.whatsapp.model._generated;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Jacksonized
@Builder
@ProtobufName("PendingKeyExchange")
public class PendingKeyExchange implements ProtobufMessage {
  @ProtobufProperty(index = 1, name = "sequence", type = ProtobufType.UINT32)
  private Integer sequence;

  @ProtobufProperty(index = 2, name = "localBaseKey", type = ProtobufType.BYTES)
  private byte[] localBaseKey;

  @ProtobufProperty(index = 3, name = "localBaseKeyPrivate", type = ProtobufType.BYTES)
  private byte[] localBaseKeyPrivate;

  @ProtobufProperty(index = 4, name = "localRatchetKey", type = ProtobufType.BYTES)
  private byte[] localRatchetKey;

  @ProtobufProperty(index = 5, name = "localRatchetKeyPrivate", type = ProtobufType.BYTES)
  private byte[] localRatchetKeyPrivate;

  @ProtobufProperty(index = 7, name = "localIdentityKey", type = ProtobufType.BYTES)
  private byte[] localIdentityKey;

  @ProtobufProperty(index = 8, name = "localIdentityKeyPrivate", type = ProtobufType.BYTES)
  private byte[] localIdentityKeyPrivate;
}