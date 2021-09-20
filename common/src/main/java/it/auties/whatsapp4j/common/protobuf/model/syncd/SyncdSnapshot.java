package it.auties.whatsapp4j.common.protobuf.model.syncd;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.common.protobuf.model.key.KeyId;
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
public class SyncdSnapshot {
  @JsonProperty(value = "4")
  private KeyId keyId;

  @JsonProperty(value = "3")
  private byte[] mac;

  @JsonProperty(value = "2")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<SyncdRecord> records;

  @JsonProperty(value = "1")
  private SyncdVersion version;
}
