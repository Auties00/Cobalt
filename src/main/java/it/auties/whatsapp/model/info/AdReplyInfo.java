package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;


/**
 * A model class that holds the information related to an companion reply.
 */
public record AdReplyInfo(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String advertiserName,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        @NonNull
        AdReplyInfoMediaType mediaType,
        @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
        Optional<byte[]> thumbnail,
        @ProtobufProperty(index = 17, type = ProtobufType.STRING)
        Optional<String> caption
) implements Info, ProtobufMessage {

}