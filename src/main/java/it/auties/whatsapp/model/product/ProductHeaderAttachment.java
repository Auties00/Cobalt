package it.auties.whatsapp.model.product;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.VideoMessage;

public sealed interface ProductHeaderAttachment extends ProtobufMessage permits DocumentMessage, ImageMessage, ProductHeaderThumbnail, VideoMessage {
}
