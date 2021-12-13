package it.auties.whatsapp.protobuf.temp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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
public class HighlyStructuredMessage {
  @JsonProperty(value = "9")
  @JsonPropertyDescription("TemplateMessage")
  private TemplateMessage hydratedHsm;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("string")
  private String deterministicLc;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("string")
  private String deterministicLg;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("HSMLocalizableParameter")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<HSMLocalizableParameter> localizableParams;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("string")
  private String fallbackLc;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("string")
  private String fallbackLg;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("string")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> params;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("string")
  private String elementName;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
  private String namespace;
}
