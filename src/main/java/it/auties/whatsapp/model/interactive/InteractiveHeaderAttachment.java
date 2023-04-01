package it.auties.whatsapp.model.interactive;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.VideoMessage;

/**
 * A sealed class that describes the various types of headers
 */
public sealed interface InteractiveHeaderAttachment extends ProtobufMessage permits DocumentMessage, ImageMessage, InteractiveHeaderThumbnail, VideoMessage {
}
