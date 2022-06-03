package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.business.BusinessLocalizableParameter;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

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
@Builder(builderMethodName = "newHighlyStructuredMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class ButtonStructureMessage implements ButtonMessage {
  /**
   * Namespace
   */
  @ProtobufProperty(index = 1, type = STRING)
  private String namespace;

  /**
   * Element Name
   */
  @ProtobufProperty(index = 2, type = STRING)
  private String elementName;

  /**
   * Params
   */
  @ProtobufProperty(index = 3, type = STRING, repeated = true)
  private List<String> params;

  /**
   * FallbackLg
   */
  @ProtobufProperty(index = 4, type = STRING)
  private String fallbackLg;

  /**
   * FallbackLc
   */
  @ProtobufProperty(index = 5, type = STRING)
  private String fallbackLc;

  /**
   * Localizable Params
   */
  @ProtobufProperty(index = 6, type = MESSAGE,
          concreteType = BusinessLocalizableParameter.class, repeated = true)
  private List<BusinessLocalizableParameter> localizableParams;

  /**
   * DeterministicLg
   */
  @ProtobufProperty(index = 7, type = STRING)
  private String deterministicLg;

  /**
   * DeterministicLc
   */
  @ProtobufProperty(index = 8, type = STRING)
  private String deterministicLc;

  /**
   * Hydrated message
   */
  @ProtobufProperty(index = 9, type = MESSAGE, concreteType = ButtonTemplateMessage.class)
  private ButtonTemplateMessage hydratedHsm;

  public static class ButtonStructureMessageBuilder {
    public ButtonStructureMessageBuilder params(List<String> params){
      if(this.params == null) this.params = new ArrayList<>();
      this.params.addAll(params);
      return this;
    }

    public ButtonStructureMessageBuilder localizableParams(List<BusinessLocalizableParameter> localizableParams){
      if(this.localizableParams == null) this.localizableParams = new ArrayList<>();
      this.localizableParams.addAll(localizableParams);
      return this;
    }
  }
}
