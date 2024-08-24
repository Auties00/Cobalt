package it.auties.whatsapp.model.media;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;


@ProtobufMessage(name = "MediaData")
public record MediaData(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String localPath
) {

}