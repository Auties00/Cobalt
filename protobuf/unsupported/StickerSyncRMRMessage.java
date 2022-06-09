package it.auties.whatsapp;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

import it.auties.protobuf.api.model.ProtobufProperty;
import java.util.*;
import lombok.*;
import lombok.experimental.*;
import lombok.extern.jackson.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class StickerSyncRMRMessage {

  @ProtobufProperty(index = 1, type = STRING, repeated = true)
  private List<String> filehash;

  @ProtobufProperty(index = 2, type = STRING)
  private String rmrSource;

  @ProtobufProperty(index = 3, type = INT64)
  private long requestTimestamp;

  public static class StickerSyncRMRMessageBuilder {

    public StickerSyncRMRMessageBuilder filehash(List<String> filehash) {
      if (this.filehash == null) this.filehash = new ArrayList<>();
      this.filehash.addAll(filehash);
      return this;
    }
  }
}
