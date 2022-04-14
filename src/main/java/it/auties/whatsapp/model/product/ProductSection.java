package it.auties.whatsapp.model.product;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
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

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ProductSection implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = STRING)
  private String title;

  @ProtobufProperty(index = 2, type = MESSAGE,
          concreteType = Product.class, repeated = true)
  private List<Product> products;

  public static class ProductSectionBuilder {
    public ProductSectionBuilder products(List<Product> products) {
      if (this.products == null) this.products = new ArrayList<>();
      this.products.addAll(products);
      return this;
    }
  }
}
