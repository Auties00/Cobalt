package it.auties.whatsapp.protobuf.model.biz;

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
public class BizAccountLinkInfo {
  @JsonProperty(value = "5")
  private BizAccountLinkInfoAccountType accountType;

  @JsonProperty(value = "4")
  private BizAccountLinkInfoHostStorageType hostStorage;

  @JsonProperty(value = "3")
  private long issueTime;

  @JsonProperty(value = "2")
  private String whatsappAcctNumber;

  @JsonProperty(value = "1")
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
