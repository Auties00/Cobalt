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
public class LocalizedName {

  @ProtobufProperty(index = 1, type = STRING)
  private String lg;

  @ProtobufProperty(index = 2, type = STRING)
  private String lc;

  @ProtobufProperty(index = 3, type = STRING)
  private String verifiedName;
}
