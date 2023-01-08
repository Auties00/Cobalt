package it.auties.whatsapp.model.product;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ProductSection
    implements ProtobufMessage {

  @ProtobufProperty(index = 1, type = STRING)
  private String title;

  @ProtobufProperty(index = 2, type = MESSAGE, implementation = Product.class, repeated = true)
  private List<Product> products;

  public static class ProductSectionBuilder {

    public ProductSectionBuilder products(List<Product> products) {
      if (this.products == null) {
        this.products = new ArrayList<>();
      }
      this.products.addAll(products);
      return this;
    }
  }
}
