package it.auties.whatsapp4j.common.protobuf.model.biz;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
  private long privacyModeTs;

  @JsonProperty(value = "6")
  private BizIdentityInfoActualActorsType actualActors;

  @JsonProperty(value = "5")
  private BizIdentityInfoHostStorageType hostStorage;

  @JsonProperty(value = "4")
  private boolean revoked;

  @JsonProperty(value = "3")
  private boolean signed;

  @JsonProperty(value = "2")
  private VerifiedNameCertificate vnameCert;

  @JsonProperty(value = "1")
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
