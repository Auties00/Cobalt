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
public class DeviceListMetadata {

  @JsonProperty(value = "10", required = false)
  @JsonPropertyDescription("uint32")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Integer> recipientKeyIndexes;

  @JsonProperty(value = "9", required = false)
  @JsonPropertyDescription("uint64")
  private long recipientTimestamp;

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] recipientKeyHash;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("uint32")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Integer> senderKeyIndexes;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("uint64")
  private long senderTimestamp;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] senderKeyHash;
}
