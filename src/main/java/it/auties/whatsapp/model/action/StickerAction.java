package it.auties.whatsapp.model.action;

import static it.auties.protobuf.base.ProtobufType.BOOL;
import static it.auties.protobuf.base.ProtobufType.BYTES;
import static it.auties.protobuf.base.ProtobufType.STRING;
import static it.auties.protobuf.base.ProtobufType.UINT32;
import static it.auties.protobuf.base.ProtobufType.UINT64;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("StickerAction")
public final class StickerAction
    implements Action {

  @ProtobufProperty(index = 1, name = "url", type = STRING)
  private String url;

  @ProtobufProperty(index = 2, name = "fileEncSha256", type = BYTES)
  private byte[] fileEncSha256;

  @ProtobufProperty(index = 3, name = "mediaKey", type = BYTES)
  private byte[] mediaKey;

  @ProtobufProperty(index = 4, name = "mimetype", type = STRING)
  private String mimetype;

  @ProtobufProperty(index = 5, name = "height", type = UINT32)
  private int height;

  @ProtobufProperty(index = 6, name = "width", type = UINT32)
  private int width;

  @ProtobufProperty(index = 7, name = "directPath", type = STRING)
  private String directPath;

  @ProtobufProperty(index = 8, name = "fileLength", type = UINT64)
  private long fileLength;

  @ProtobufProperty(index = 9, name = "isFavorite", type = BOOL)
  private boolean favorite;

  @ProtobufProperty(index = 10, name = "deviceIdHint", type = UINT32)
  private Integer deviceIdHint;

  /**
   * Always throws an exception as this action cannot be serialized
   *
   * @return an exception
   */
  @Override
  public String indexName() {
    throw new UnsupportedOperationException("Cannot send action: no index name");
  }
}
