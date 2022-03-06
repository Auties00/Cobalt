package it.auties.whatsapp.model.signal.session;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class UserReceipt {

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("string")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> deliveredDeviceJid;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("string")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> pendingDeviceJid;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("int64")
  private long playedTimestamp;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("int64")
  private long readTimestamp;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("int64")
  private long receiptTimestamp;

  @JsonProperty(value = "1", required = true)
  @JsonPropertyDescription("string")
  private String userJid;
}
