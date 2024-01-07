package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

public record NewsletterSettings(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        @JsonProperty("reaction_codes")
        NewsletterReactionSettings reactionCodes
) implements ProtobufMessage {

}
