package it.auties.whatsapp.protobuf.message.button;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.business.BusinessLocalizableParameter;
import it.auties.whatsapp.protobuf.message.model.ButtonMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

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
@Builder(builderClassName = "newHighlyStructuredMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class StructuredButtonMessage implements ButtonMessage {
  /**
   * Namespace
   */
  @JsonProperty("1")
  private String namespace;

  /**
   * Element Name
   */
  @JsonProperty("2")
  private String elementName;

  /**
   * Params
   */
  @JsonProperty("3")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> params;

  /**
   * FallbackLg
   */
  @JsonProperty("4")
  private String fallbackLg;

  /**
   * FallbackLc
   */
  @JsonProperty("5")
  private String fallbackLc;

  /**
   * Localizable Params
   */
  @JsonProperty("6")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<BusinessLocalizableParameter> localizableParams;

  /**
   * DeterministicLg
   */
  @JsonProperty("7")
  private String deterministicLg;

  /**
   * DeterministicLc
   */
  @JsonProperty("8")
  private String deterministicLc;

  /**
   * Hydrated message
   */
  @JsonProperty("9")
  private TemplateMessage hydratedHsm;
}
