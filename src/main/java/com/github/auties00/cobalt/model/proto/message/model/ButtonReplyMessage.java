package com.github.auties00.cobalt.model.proto.message.model;

import com.github.auties00.cobalt.model.proto.message.button.ButtonsResponseMessage;
import com.github.auties00.cobalt.model.proto.message.button.ListResponseMessage;
import com.github.auties00.cobalt.model.proto.message.button.TemplateReplyMessage;

/**
 * A model interface that represents a reply to a button message
 */
public sealed interface ButtonReplyMessage<T extends ButtonReplyMessage<T>> extends ContextualMessage, ButtonMessage permits ListResponseMessage, TemplateReplyMessage, ButtonsResponseMessage {

}
