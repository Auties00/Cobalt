package it.auties.whatsapp.model.message.button;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.product.ProductBody;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Jacksonized
@SuperBuilder
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
@ProtobufName("InteractiveResponseMessage")
public final class InteractiveResponseMessage extends ContextualMessage {
  @ProtobufProperty(index = 1, name = "body", type = MESSAGE)
  private ProductBody body;

  @ProtobufProperty(index = 2, name = "nativeFlowResponseMessage", type = MESSAGE)
  private NativeFlowResponseMessage nativeFlowResponseMessage;

  @ProtobufProperty(index = 15, name = "contextInfo", type = MESSAGE)
  @Default
  private ContextInfo contextInfo = new ContextInfo();

  @Override
  public MessageType type() {
    return MessageType.INTERACTIVE_RESPONSE;
  }

  @Override
  public MessageCategory category() {
    return MessageCategory.BUTTON;
  }

  public InteractiveMessage.ContentType interactiveResponseMessageType() {
    return InteractiveMessage.ContentType.COLLECTION_MESSAGE;
  }
}