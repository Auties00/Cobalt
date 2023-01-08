package it.auties.whatsapp.model.sync;

import static it.auties.protobuf.base.ProtobufType.BYTES;
import static it.auties.protobuf.base.ProtobufType.UINT32;
import static it.auties.protobuf.base.ProtobufType.UINT64;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@NoArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class DeviceListMetadata
    implements ProtobufMessage {

  @ProtobufProperty(index = 1, type = BYTES)
  private byte[] senderKeyHash;

  @ProtobufProperty(index = 2, type = UINT64)
  private Long senderTimestamp;

  @ProtobufProperty(index = 3, type = UINT32, repeated = true, packed = true)
  private List<Integer> senderKeyIndexes;

  @ProtobufProperty(index = 8, type = BYTES)
  private byte[] recipientKeyHash;

  @ProtobufProperty(index = 9, type = UINT64)
  private Long recipientTimestamp;

  @ProtobufProperty(index = 10, type = UINT32, repeated = true, packed = true)
  private List<Integer> recipientKeyIndexes;

  public static class DeviceListMetadataBuilder {

    public DeviceListMetadataBuilder senderKeyIndexes(List<Integer> senderKeyIndexes) {
      if (this.senderKeyIndexes == null) {
        this.senderKeyIndexes = new ArrayList<>();
      }
      this.senderKeyIndexes.addAll(senderKeyIndexes);
      return this;
    }

    public DeviceListMetadataBuilder recipientKeyIndexes(List<Integer> recipientKeyIndexes) {
      if (this.recipientKeyIndexes == null) {
        this.recipientKeyIndexes = new ArrayList<>();
      }
      this.recipientKeyIndexes.addAll(recipientKeyIndexes);
      return this;
    }
  }
}
