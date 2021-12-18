package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class InteractiveMessage {

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("NativeFlowMessage")
  private NativeFlowMessage nativeFlowMessage;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("CollectionMessage")
  private CollectionMessage collectionMessage;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("ShopMessage")
  private ShopMessage shopStorefrontMessage;

  @JsonProperty(value = "15", required = false)
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("Footer")
  private Footer footer;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("Body")
  private Body body;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("Header")
  private Header header;

  public InteractiveMessage interactiveMessageCase() {
    if (shopStorefrontMessage != null) return InteractiveMessage.SHOP_STOREFRONT_MESSAGE;
    if (collectionMessage != null) return InteractiveMessage.COLLECTION_MESSAGE;
    if (nativeFlowMessage != null) return InteractiveMessage.NATIVE_FLOW_MESSAGE;
    return InteractiveMessage.UNKNOWN;
  }

  @Accessors(fluent = true)
  public enum InteractiveMessage {
    UNKNOWN(0),
    SHOP_STOREFRONT_MESSAGE(4),
    COLLECTION_MESSAGE(5),
    NATIVE_FLOW_MESSAGE(6);

    private final @Getter int index;

    InteractiveMessage(int index) {
      this.index = index;
    }

    @JsonCreator
    public static InteractiveMessage forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(InteractiveMessage.UNKNOWN);
    }
  }
}
