package it.auties.whatsapp.model.sync;

import static it.auties.protobuf.base.ProtobufType.BYTES;
import static it.auties.protobuf.base.ProtobufType.INT64;
import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;
import static it.auties.protobuf.base.ProtobufType.UINT32;
import static it.auties.protobuf.base.ProtobufType.UINT64;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.media.AttachmentProvider;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class HistorySyncNotification implements ProtobufMessage, AttachmentProvider {
  @ProtobufProperty(index = 1, type = BYTES)
  private byte[] mediaSha256;

  @ProtobufProperty(index = 2, type = UINT64)
  private long mediaSize;

  @ProtobufProperty(index = 3, type = BYTES)
  private byte[] mediaKey;

  @ProtobufProperty(index = 4, type = BYTES)
  private byte[] mediaEncryptedSha256;

  @ProtobufProperty(index = 5, type = STRING)
  private String mediaDirectPath;

  @ProtobufProperty(index = 6, type = MESSAGE, implementation = HistorySyncNotification.HistorySyncNotificationHistorySyncType.class)
  private HistorySyncNotificationHistorySyncType syncType;

  @ProtobufProperty(index = 7, type = UINT32)
  private Integer chunkOrder;

  @ProtobufProperty(index = 8, type = STRING)
  private String originalMessageId;

  @ProtobufProperty(index = 9, name = "progress", type = UINT32)
  private Integer progress;

  @ProtobufProperty(index = 10, name = "oldestMsgInChunkTimestampSec", type = INT64)
  private Long oldestMsgInChunkTimestampSec;

  @Override
  public String mediaUrl() {
    return null;
  }

  @Override
  public AttachmentProvider mediaUrl(String mediaUrl) {
    return this;
  }

  @Override
  public String mediaName() {
    return "WhatsApp History Keys";
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  @ProtobufName("HistorySyncType")
  public enum HistorySyncNotificationHistorySyncType {

    INITIAL_BOOTSTRAP(0),
    INITIAL_STATUS_V3(1),
    FULL(2),
    RECENT(3),
    PUSH_NAME(4);
    @Getter
    private final int index;

    @JsonCreator
    public static HistorySyncNotificationHistorySyncType of(int index) {
      return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst()
          .orElse(null);
    }
  }
}