package it.auties.whatsapp.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.business.BusinessActorsType;
import it.auties.whatsapp.model.business.BusinessCertificate;
import it.auties.whatsapp.model.business.BusinessStorageType;
import it.auties.whatsapp.model.business.BusinessVerifiedLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * A model class that holds the information related to the identity of a business account.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public final class BusinessIdentityInfo implements Info {
  /**
   * The level of verification of this account
   */
  @JsonProperty("1")
  @JsonPropertyDescription("BizIdentityInfoVerifiedLevelValue")
  private BusinessVerifiedLevel level;

  /**
   * The certificate of this account
   */
  @JsonProperty("2")
  @JsonPropertyDescription("VerifiedNameCertificate")
  private BusinessCertificate certificate;

  /**
   * Indicates whether this account has a signed certificate
   */
  @JsonProperty("3")
  @JsonPropertyDescription("bool")
  private boolean signed;

  /**
   * Indicates whether the signed certificate of this account has been revoked
   */
  @JsonProperty("4")
  @JsonPropertyDescription("bool")
  private boolean revoked;

  /**
   * Indicates where this account is hosted
   */
  @JsonProperty("5")
  @JsonPropertyDescription("BizIdentityInfoHostStorageType")
  private BusinessStorageType hostStorage;

  /**
   * The actual actors of this account
   */
  @JsonProperty("6")
  @JsonPropertyDescription("BizIdentityInfoActualActorsType")
  private BusinessActorsType actualActors;

  /**
   * The privacy mode of this account
   */
  @JsonProperty("7")
  @JsonPropertyDescription("uint64")
  private long privacyModeTs;
}
