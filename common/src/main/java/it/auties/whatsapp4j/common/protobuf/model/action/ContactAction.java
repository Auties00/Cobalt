package it.auties.whatsapp4j.common.protobuf.model.action;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ContactAction {
  @JsonProperty(value = "2")
  private String firstName;

  @JsonProperty(value = "1")
  private String fullName;
}
