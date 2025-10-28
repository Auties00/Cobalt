package com.github.auties00.cobalt.model.message.model;

import com.github.auties00.cobalt.model.message.button.ButtonsResponseMessage;
import com.github.auties00.cobalt.model.message.button.ListResponseMessage;
import com.github.auties00.cobalt.model.message.button.TemplateReplyMessage;

/**
 * A model interface that represents a reply to a button message
 */
public sealed interface ButtonReplyMessage<T extends ButtonReplyMessage<T>> extends ContextualMessage, ButtonMessage permits ListResponseMessage, TemplateReplyMessage, ButtonsResponseMessage {

}
