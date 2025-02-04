package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessage
public record NewsletterSettings(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        @JsonProperty("reaction_codes")
        NewsletterReactionSettings reactionCodes
) {

}
