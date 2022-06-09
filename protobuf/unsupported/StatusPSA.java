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
public class StatusPSA {

  @ProtobufProperty(index = 44, type = STRING)
  @NonNull
  private String campaignId;

  @ProtobufProperty(index = 45, type = UINT64)
  private long campaignExpirationTimestamp;
}
