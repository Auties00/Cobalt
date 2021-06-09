package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.model.CatalogSnapshot;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import it.auties.whatsapp4j.protobuf.miscellanous.ProductSnapshot;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * A model class that represents a WhatsappMessage sent by a WhatsappBusiness account and that holds a product inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Accessors(fluent = true)
public final class ProductMessage extends ContextualMessage {
  /**
   * The catalog where the product that this message wraps is
   */
  @JsonProperty(value = "4")
  private CatalogSnapshot catalog;

  /**
   * The jid of the WhatsappBusiness account that owns the product that this message wraps
   */
  @JsonProperty(value = "2")
  private String businessOwnerJid;

  /**
   * The product that this message wraps
   */
  @JsonProperty(value = "1")
  private ProductSnapshot product;
}
