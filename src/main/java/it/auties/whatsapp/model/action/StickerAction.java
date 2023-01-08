package it.auties.whatsapp.model.action;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
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

  @ProtobufProperty(index = 1, name = "url", type = ProtobufType.STRING)
  private String url;

  @ProtobufProperty(index = 2, name = "fileEncSha256", type = ProtobufType.BYTES)
  private byte[] fileEncSha256;

  @ProtobufProperty(index = 3, name = "mediaKey", type = ProtobufType.BYTES)
  private byte[] mediaKey;

  @ProtobufProperty(index = 4, name = "mimetype", type = ProtobufType.STRING)
  private String mimetype;

  @ProtobufProperty(index = 5, name = "height", type = ProtobufType.UINT32)
  private int height;

  @ProtobufProperty(index = 6, name = "width", type = ProtobufType.UINT32)
  private int width;

  @ProtobufProperty(index = 7, name = "directPath", type = ProtobufType.STRING)
  private String directPath;

  @ProtobufProperty(index = 8, name = "fileLength", type = ProtobufType.UINT64)
  private long fileLength;

  @ProtobufProperty(index = 9, name = "isFavorite", type = ProtobufType.BOOL)
  private boolean favorite;

  @ProtobufProperty(index = 10, name = "deviceIdHint", type = ProtobufType.UINT32)
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
