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
public class VerifiedNameCertificate {

  @ProtobufProperty(index = 1, type = BYTES)
  private byte[] details;

  @ProtobufProperty(index = 2, type = BYTES)
  private byte[] signature;

  @ProtobufProperty(index = 3, type = BYTES)
  private byte[] serverSignature;
}
