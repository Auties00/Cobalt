package it.auties.whatsapp.model.button.template.hydrated;

import it.auties.protobuf.base.ProtobufMessage;

/**
 * A model that represents all types of hydrated buttons
 */
public sealed interface HydratedButton extends ProtobufMessage permits HydratedCallButton, HydratedQuickReplyButton, HydratedURLButton {
    /**
     * Returns the text of this button
     *
     * @return a non-null string if the protobuf isn't corrupted
     */
    String text();

    /**
     * Returns the type of this button
     *
     * @return a non-null type
     */
    HydratedButtonType buttonType();
}
