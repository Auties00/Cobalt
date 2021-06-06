package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.protobuf.model.HSMLocalizableParameter;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class HighlyStructuredMessage implements Message {
  @JsonProperty(value = "9")
  private TemplateMessage hydratedHsm;

  @JsonProperty(value = "8")
  private String deterministicLc;

  @JsonProperty(value = "7")
  private String deterministicLg;

  @JsonProperty(value = "6")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<HSMLocalizableParameter> localizableParams;

  @JsonProperty(value = "5")
  private String fallbackLc;

  @JsonProperty(value = "4")
  private String fallbackLg;

  @JsonProperty(value = "3")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> params;

  @JsonProperty(value = "2")
  private String elementName;

  @JsonProperty(value = "1")
  private String namespace;
}
