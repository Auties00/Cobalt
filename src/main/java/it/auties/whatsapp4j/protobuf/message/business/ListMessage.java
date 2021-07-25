package it.auties.whatsapp4j.protobuf.message.business;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import it.auties.whatsapp4j.protobuf.info.ProductListInfo;
import it.auties.whatsapp4j.protobuf.model.button.Section;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ListMessage {
  @JsonProperty(value = "8")
  private ContextInfo contextInfo;

  @JsonProperty(value = "7")
  private String footerText;

  @JsonProperty(value = "6")
  private ProductListInfo productListInfo;

  @JsonProperty(value = "5")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Section> sections;

  @JsonProperty(value = "4")
  private ListMessageListType listType;

  @JsonProperty(value = "3")
  private String buttonText;

  @JsonProperty(value = "2")
  private String description;

  @JsonProperty(value = "1")
  private String title;

  @Accessors(fluent = true)
  public enum ListMessageListType {
    UNKNOWN(0),
    SINGLE_SELECT(1),
    PRODUCT_LIST(2);

    private final @Getter int index;

    ListMessageListType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static ListMessageListType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
