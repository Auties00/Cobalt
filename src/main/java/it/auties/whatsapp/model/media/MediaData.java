package it.auties.whatsapp.model.media;

import static it.auties.protobuf.base.ProtobufType.STRING;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class MediaData implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = STRING)
  private String localPath;

  @ProtobufProperty(index = 2, name = "mediaKeyTimestamp", type = ProtobufType.INT64)
  private Long mediaKeyTimestamp;

  @ProtobufProperty(index = 3, name = "fileSha256", type = ProtobufType.BYTES)
  private byte[] fileSha256;

  @ProtobufProperty(index = 4, name = "fileEncSha256", type = ProtobufType.BYTES)
  private byte[] fileEncSha256;

  @ProtobufProperty(index = 5, name = "directPath", type = ProtobufType.STRING)
  private String directPath;
}