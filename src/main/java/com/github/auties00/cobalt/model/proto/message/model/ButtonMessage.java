package com.github.auties00.cobalt.model.proto.message.model;

import com.github.auties00.cobalt.model.proto.button.template.highlyStructured.HighlyStructuredMessage;
import com.github.auties00.cobalt.model.proto.message.button.*;
import com.github.auties00.cobalt.model.proto.message.standard.ProductMessage;

/**
 * A model interface that represents a button message
 */
public sealed interface ButtonMessage extends Message permits ButtonsMessage, HighlyStructuredMessage, ListMessage, NativeFlowResponseMessage, TemplateMessage, ButtonReplyMessage, InteractiveMessage, ProductMessage {
    @Override
    default Category category() {
        return Category.BUTTON;
    }
}
