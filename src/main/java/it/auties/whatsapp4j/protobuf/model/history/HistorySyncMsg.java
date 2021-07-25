package it.auties.whatsapp4j.protobuf.model.history;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.protobuf.info.MessageInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class HistorySyncMsg {
  @JsonProperty(value = "2")
  private long msgOrderId;

  @JsonProperty(value = "1")
  private MessageInfo message;
}
