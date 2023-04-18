package it.auties.whatsapp.model.button.template;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.model.button.template.hsm.HighlyStructuredFourRowTemplate;
import it.auties.whatsapp.model.button.template.hydrated.HydratedFourRowTemplate;
import it.auties.whatsapp.model.message.button.InteractiveMessage;
import it.auties.whatsapp.model.message.button.TemplateFormatterType;

/**
 * A formatter used to structure a button message
 */
public sealed interface TemplateFormatter extends ProtobufMessage permits HighlyStructuredFourRowTemplate, HydratedFourRowTemplate, InteractiveMessage {
    /**
     * Returns the type of this formatter
     *
     * @return a non-null type
     */
    TemplateFormatterType templateType();
}
