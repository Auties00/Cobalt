package it.auties.whatsapp.protobuf.model.action;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class QuickReplyAction {
  @JsonProperty(value = "5")
  private boolean deleted;

  @JsonProperty(value = "4")
  private int count;

  @JsonProperty(value = "3")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> keywords;

  @JsonProperty(value = "2")
  private String message;

  @JsonProperty(value = "1")
  private String shortcut;
}
