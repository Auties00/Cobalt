package it.auties.whatsapp.protobuf.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.business.BusinessAccountType;
import it.auties.whatsapp.protobuf.business.BusinessStorageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * A model class that holds the information related to a business account.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class BusinessAccountInfo {
  /**
   * The facebook id
   */
  @JsonProperty(value = "1")
  @JsonPropertyDescription("uint64")
  private long facebookId;

  /**
   * The account phone number
   */
  @JsonProperty(value = "2")
  @JsonPropertyDescription("string")
  private String accountNumber;

  /**
   * The timestamp of the account
   */
  @JsonProperty(value = "3")
  @JsonPropertyDescription("uint64")
  private long timestamp;

  /**
   * Indicates here this account is hosted
   */
  @JsonProperty(value = "4")
  @JsonPropertyDescription("BizAccountLinkInfoHostStorageType")
  private BusinessStorageType hostStorage;

  /**
   * The type of this account
   */
  @JsonProperty(value = "5")
  @JsonPropertyDescription("BizAccountLinkInfoAccountType")
  private BusinessAccountType accountType;
}
