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
public class BizAccountLinkInfo {
  @JsonProperty(value = "5")
  @JsonPropertyDescription("BizAccountLinkInfoAccountType")
  private BizAccountLinkInfoAccountType accountType;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("BizAccountLinkInfoHostStorageType")
  private BizAccountLinkInfoHostStorageType hostStorage;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("uint64")
  private long issueTime;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("string")
  private String whatsappAcctNumber;

  @JsonProperty(value = "1")
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
