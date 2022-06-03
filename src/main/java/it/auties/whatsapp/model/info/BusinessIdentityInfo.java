package it.auties.whatsapp.model.info;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.business.BusinessActorsType;
import it.auties.whatsapp.model.business.BusinessCertificate;
import it.auties.whatsapp.model.business.BusinessStorageType;
import it.auties.whatsapp.model.business.BusinessVerifiedLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

/**
 * A model class that holds the information related to the identity of a business account.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class BusinessIdentityInfo implements Info {
  /**
   * The level of verification of this account
   */
  @ProtobufProperty(index = 1, type = MESSAGE, concreteType = BusinessVerifiedLevel.class)
  private BusinessVerifiedLevel level;

  /**
   * The certificate of this account
   */
  @ProtobufProperty(index = 2, type = MESSAGE, concreteType = BusinessCertificate.class)
  private BusinessCertificate certificate;

  /**
   * Indicates whether this account has a signed certificate
   */
  @ProtobufProperty(index = 3, type = BOOLEAN)
  private boolean signed;

  /**
   * Indicates whether the signed certificate of this account has been revoked
   */
  @ProtobufProperty(index = 4, type = BOOLEAN)
  private boolean revoked;

  /**
   * Indicates where this account is hosted
   */
  @ProtobufProperty(index = 5, type = MESSAGE, concreteType = BusinessStorageType.class)
  private BusinessStorageType hostStorage;

  /**
   * The actual actors of this account
   */
  @ProtobufProperty(index = 6, type = MESSAGE, concreteType = BusinessActorsType.class)
  private BusinessActorsType actualActors;

  /**
   * The privacy mode of this account
   */
  @ProtobufProperty(index = 7, type = UINT64)
  private Long privacyModeTs;
}
