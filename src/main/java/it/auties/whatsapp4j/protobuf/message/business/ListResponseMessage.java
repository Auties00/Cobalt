package it.auties.whatsapp4j.protobuf.message.business;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import it.auties.whatsapp4j.protobuf.model.button.SingleSelectReply;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ListResponseMessage {
  @JsonProperty(value = "5")
  private String description;

  @JsonProperty(value = "4")
  private ContextInfo contextInfo;

  @JsonProperty(value = "3")
  private SingleSelectReply singleSelectReply;

  @JsonProperty(value = "2")
  private ListResponseMessageListType listType;

  @JsonProperty(value = "1")
  private String title;

  @Accessors(fluent = true)
  public enum ListResponseMessageListType {
    UNKNOWN(0),
    SINGLE_SELECT(1);

    private final @Getter int index;

    ListResponseMessageListType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static ListResponseMessageListType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
