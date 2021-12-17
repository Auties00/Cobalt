package it.auties.whatsapp.protobuf.unknown;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class BizIdentityInfo {
  @JsonProperty(value = "7")
  @JsonPropertyDescription("uint64")
  private long privacyModeTs;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("BizIdentityInfoActualActorsType")
  private BizIdentityInfoActualActorsType actualActors;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("BizIdentityInfoHostStorageType")
  private BizIdentityInfoHostStorageType hostStorage;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("bool")
  private boolean revoked;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("bool")
  private boolean signed;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("VerifiedNameCertificate")
  private VerifiedNameCertificate vnameCert;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("BizIdentityInfoVerifiedLevelValue")
  private BizIdentityInfoVerifiedLevelValue vlevel;

  @Accessors(fluent = true)
  public enum BizIdentityInfoVerifiedLevelValue {
    UNKNOWN(0),
    LOW(1),
    HIGH(2);

    private final @Getter int index;

    BizIdentityInfoVerifiedLevelValue(int index) {
      this.index = index;
    }

    @JsonCreator
    public static BizIdentityInfoVerifiedLevelValue forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @Accessors(fluent = true)
  public enum BizIdentityInfoHostStorageType {
    ON_PREMISE(0),
    FACEBOOK(1);

    private final @Getter int index;

    BizIdentityInfoHostStorageType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static BizIdentityInfoHostStorageType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @Accessors(fluent = true)
  public enum BizIdentityInfoActualActorsType {
    SELF(0),
    BSP(1);

    private final @Getter int index;

    BizIdentityInfoActualActorsType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static BizIdentityInfoActualActorsType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
