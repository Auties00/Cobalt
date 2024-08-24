package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessage
public record NewsletterPicture(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String id,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String type,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        @JsonProperty("direct_path") String directPath
) {

}
