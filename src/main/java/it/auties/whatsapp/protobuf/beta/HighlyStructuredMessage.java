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
public class HighlyStructuredMessage {

  @JsonProperty(value = "9", required = false)
  @JsonPropertyDescription("TemplateMessage")
  private TemplateMessage hydratedHsm;

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("string")
  private String deterministicLc;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("string")
  private String deterministicLg;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("HSMLocalizableParameter")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<HSMLocalizableParameter> localizableParams;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("string")
  private String fallbackLc;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("string")
  private String fallbackLg;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("string")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> params;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String elementName;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String namespace;
}
