package it.auties.whatsapp.protobuf.beta;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class BizAccountLinkInfo {

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("BizAccountLinkInfoAccountType")
  private BizAccountLinkInfoAccountType accountType;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("BizAccountLinkInfoHostStorageType")
  private BizAccountLinkInfoHostStorageType hostStorage;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("uint64")
  private long issueTime;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String whatsappAcctNumber;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("uint64")
  private long whatsappBizAcctFbid;

  @Accessors(fluent = true)
  public enum BizAccountLinkInfoHostStorageType {
    ON_PREMISE(0),
    FACEBOOK(1);

    private final @Getter int index;

    BizAccountLinkInfoHostStorageType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static BizAccountLinkInfoHostStorageType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @Accessors(fluent = true)
  public enum BizAccountLinkInfoAccountType {
    ENTERPRISE(0),
    PAGE(1);

    private final @Getter int index;

    BizAccountLinkInfoAccountType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static BizAccountLinkInfoAccountType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
