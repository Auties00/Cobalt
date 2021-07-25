package it.auties.whatsapp4j.protobuf.model.product;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ProductListHeaderImage {
  @JsonProperty(value = "2")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "1")
  private String productId;
}
