package it.auties.whatsapp.model.business;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class BusinessLocalizedName implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = STRING)
  private String lg;

  @ProtobufProperty(index = 2, type = STRING)
  private String lc;

  @ProtobufProperty(index = 3, type = STRING)
  private String name;
}
