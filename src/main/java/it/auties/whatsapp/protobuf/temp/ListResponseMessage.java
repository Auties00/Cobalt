package it.auties.whatsapp.protobuf.temp;

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
public class ListResponseMessage {
  @JsonProperty(value = "5")
  @JsonPropertyDescription("string")
  private String description;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("SingleSelectReply")
  private SingleSelectReply singleSelectReply;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("ListResponseMessageListType")
  private ListResponseMessageListType listType;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
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
