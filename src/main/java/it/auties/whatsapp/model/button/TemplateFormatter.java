package it.auties.whatsapp.model.button;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.model.message.button.InteractiveMessage;

public sealed interface TemplateFormatter extends ProtobufMessage permits FourRowTemplate, HydratedFourRowTemplate, InteractiveMessage {

}
