package it.auties.whatsapp4j.protobuf.model.misc;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class PaymentBackground {
  @JsonProperty(value = "8")
  private int subtextArgb;

  @JsonProperty(value = "7")
  private int textArgb;

  @JsonProperty(value = "6")
  private int placeholderArgb;

  @JsonProperty(value = "5")
  private String mimetype;

  @JsonProperty(value = "4")
  private int height;

  @JsonProperty(value = "3")
  private int width;

  @JsonProperty(value = "2")
  private String fileLength;

  @JsonProperty(value = "1")
  private String id;
}
