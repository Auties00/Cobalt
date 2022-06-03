package it.auties.whatsapp.model.business;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.BusinessAccountInfo;
import it.auties.whatsapp.util.JacksonProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

/**
 * A model class that holds a payload about a business account.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class BusinessAccountPayload implements ProtobufMessage, JacksonProvider {
  /**
   * The certificate of this account
   */
  @ProtobufProperty(index = 1, type = MESSAGE, concreteType = BusinessCertificate.class)
  private BusinessCertificate certificate;

  /**
   * The info about this account
   */
  @ProtobufProperty(index = 2, type = MESSAGE, concreteType = BusinessAccountInfo.class)
  private BusinessAccountInfo info;
}
