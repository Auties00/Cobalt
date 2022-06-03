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
public class PBMediaData {

  @ProtobufProperty(index = 1, type = BYTES)
  private byte[] mediaKey;

  @ProtobufProperty(index = 2, type = INT64)
  private Long mediaKeyTimestamp;

  @ProtobufProperty(index = 3, type = BYTES)
  private byte[] fileSha256;

  @ProtobufProperty(index = 4, type = BYTES)
  private byte[] fileEncSha256;

  @ProtobufProperty(index = 5, type = STRING)
  private String directPath;
}
