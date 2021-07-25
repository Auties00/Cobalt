package it.auties.whatsapp4j.protobuf.model.misc;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class DeviceListMetadata {
  @JsonProperty(value = "10")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Integer> recipientKeyIndexes;

  @JsonProperty(value = "9")
  private long recipientTimestamp;

  @JsonProperty(value = "8")
  private byte[] recipientKeyHash;

  @JsonProperty(value = "3")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Integer> senderKeyIndexes;

  @JsonProperty(value = "2")
  private long senderTimestamp;

  @JsonProperty(value = "1")
  private byte[] senderKeyHash;
}
