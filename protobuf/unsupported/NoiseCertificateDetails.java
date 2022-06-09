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
public class NoiseCertificateDetails {

  @ProtobufProperty(index = 1, type = UINT32)
  private int serial;

  @ProtobufProperty(index = 2, type = STRING)
  private String issuer;

  @ProtobufProperty(index = 3, type = UINT64)
  private long expires;

  @ProtobufProperty(index = 4, type = STRING)
  private String subject;

  @ProtobufProperty(index = 5, type = BYTES)
  private byte[] key;
}
