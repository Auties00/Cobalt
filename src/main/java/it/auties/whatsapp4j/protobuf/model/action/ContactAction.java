package it.auties.whatsapp4j.protobuf.model.action;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
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
