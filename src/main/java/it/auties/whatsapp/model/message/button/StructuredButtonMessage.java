package it.auties.whatsapp.model.message.button;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.business.BusinessLocalizableParameter;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * A model class that represents a WhatsappMessage that contains a highly structured message inside.
 * Not much is known about this type of message as no one has encountered it.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder(builderClassName = "newHighlyStructuredMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class StructuredButtonMessage implements ButtonMessage {
  /**
   * Namespace
   */
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String namespace;

  /**
   * Element Name
   */
  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private String elementName;

  /**
   * Params
   */
  @JsonProperty("3")
  @JsonPropertyDescription("string")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> params;

  /**
   * FallbackLg
   */
  @JsonProperty("4")
  @JsonPropertyDescription("string")
  private String fallbackLg;

  /**
   * FallbackLc
   */
  @JsonProperty("5")
  @JsonPropertyDescription("string")
  private String fallbackLc;

  /**
   * Localizable Params
   */
  @JsonProperty("6")
  @JsonPropertyDescription("params")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<BusinessLocalizableParameter> localizableParams;

  /**
   * DeterministicLg
   */
  @JsonProperty("7")
  @JsonPropertyDescription("string")
  private String deterministicLg;

  /**
   * DeterministicLc
   */
  @JsonProperty("8")
  @JsonPropertyDescription("string")
  private String deterministicLc;

  /**
   * Hydrated message
   */
  @JsonProperty("9")
  @JsonPropertyDescription("message")
  private TemplateMessage hydratedHsm;
}
