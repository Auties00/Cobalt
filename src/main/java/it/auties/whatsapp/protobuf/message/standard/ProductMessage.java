package it.auties.whatsapp.protobuf.message.standard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.protobuf.message.model.ContextualMessage;
import it.auties.whatsapp.protobuf.message.model.Message;
import it.auties.whatsapp.protobuf.product.ProductCatalog;
import it.auties.whatsapp.protobuf.product.ProductSnapshot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
import lombok.experimental.SuperBuilder;

/**
 * A model class that represents a WhatsappMessage sent by a WhatsappBusiness account and that holds a product inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newProductMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class ProductMessage extends ContextualMessage implements Message {
  /**
   * The product that this message wraps
   */
  @JsonProperty("1")
  @JsonPropertyDescription("product")
  private ProductSnapshot product;

  /**
   * The jid of the WhatsappBusiness account that owns the product that this message wraps
   */
  @JsonProperty("2")
  @JsonPropertyDescription("businessOwnerId")
  private ContactJid businessOwnerId;

  /**
   * The catalog where the product that this message wraps is
   */
  @JsonProperty("4")
  @JsonPropertyDescription("catalog")
  private ProductCatalog catalog;
}
