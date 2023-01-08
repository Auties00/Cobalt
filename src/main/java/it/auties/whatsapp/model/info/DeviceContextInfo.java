package it.auties.whatsapp.model.info;

import static it.auties.protobuf.base.ProtobufType.INT32;
import static it.auties.protobuf.base.ProtobufType.MESSAGE;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import it.auties.whatsapp.model.sync.DeviceListMetadata;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class DeviceContextInfo
    implements Info {

  @ProtobufProperty(index = 1, type = MESSAGE, implementation = DeviceListMetadata.class)
  @Default
  private DeviceListMetadata deviceListMetadata = DeviceListMetadata.of();

  @ProtobufProperty(index = 2, type = INT32)
  @Default
  private int deviceListMetadataVersion = 2;

  @ProtobufProperty(index = 3, name = "messageSecret", type = ProtobufType.BYTES)
  private byte[] messageSecret;

  @ProtobufProperty(index = 4, name = "paddingBytes", type = ProtobufType.BYTES)
  private byte[] paddingBytes;

  public static DeviceContextInfo of() {
    return DeviceContextInfo.builder()
        .build();
  }

  public Optional<byte[]> messageSecret() {
    return Optional.ofNullable(messageSecret);
  }

  public Optional<byte[]> paddingBytes() {
    return Optional.ofNullable(paddingBytes);
  }
}