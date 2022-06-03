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
public class VerifiedNameDetails {

  @ProtobufProperty(index = 1, type = UINT64)
  private Long serial;

  @ProtobufProperty(index = 2, type = STRING)
  private String issuer;

  @ProtobufProperty(index = 4, type = STRING)
  private String verifiedName;

  @ProtobufProperty(index = 8, type = MESSAGE, concreteType = LocalizedName.class, repeated = true)
  private List<LocalizedName> localizedNames;

  @ProtobufProperty(index = 10, type = UINT64)
  private Long issueTime;

  public static class VerifiedNameDetailsBuilder {

    public VerifiedNameDetailsBuilder localizedNames(List<LocalizedName> localizedNames) {
      if (this.localizedNames == null) this.localizedNames = new ArrayList<>();
      this.localizedNames.addAll(localizedNames);
      return this;
    }
  }
}
