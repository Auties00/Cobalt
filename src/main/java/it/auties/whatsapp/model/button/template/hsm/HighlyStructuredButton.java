package it.auties.whatsapp.model.button.template.hsm;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.model.message.button.HighlyStructuredMessage;

/**
 * A model that represents all types of hydrated buttons
 */
public sealed interface HighlyStructuredButton extends ProtobufMessage permits HighlyStructuredCallButton, HighlyStructuredQuickReplyButton, HighlyStructuredURLButton {
    /**
     * Returns the text of this button
     *
     * @return a non-null structure if the protobuf isn't corrupted
     */
    HighlyStructuredMessage text();

    /**
     * Returns the type of this button
     *
     * @return a non-null type
     */
    HighlyStructuredButtonType buttonType();
}
