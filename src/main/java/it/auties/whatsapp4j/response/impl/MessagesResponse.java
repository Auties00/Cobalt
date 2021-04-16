package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.model.WhatsappMessages;
import it.auties.whatsapp4j.model.WhatsappNode;
import it.auties.whatsapp4j.model.WhatsappProtobuf;
import it.auties.whatsapp4j.response.model.BinaryResponseModel;
import it.auties.whatsapp4j.utils.WhatsappMessageFactory;
import jakarta.validation.constraints.NotNull;

import java.util.stream.Collectors;

/**
 * A binary model to represent a WhatsappNode that contains {@link WhatsappMessages}
 * The property {@link MessagesResponse#data} is nullable and should be accessed using the accessor {@link MessagesResponse#data()}
 */
public class MessagesResponse extends BinaryResponseModel<MessagesResponse,WhatsappMessages> {
    /**
     * Constructs a new MessagesResponse from {@code node}
     * If {@code node} cannot be parsed no exception is thrown
     *
     * @param node the WhatsappNode to parse
     */
    public MessagesResponse(@NotNull WhatsappNode node) {
        super(node);
        this.data =  node.childNodes()
                .stream()
                .filter(childNode -> childNode.description().equals("message"))
                .map(WhatsappNode::content)
                .filter(childContent -> childContent instanceof WhatsappProtobuf.WebMessageInfo)
                .map(WhatsappProtobuf.WebMessageInfo.class::cast)
                .map(WhatsappMessageFactory::buildMessageFromProtobuf)
                .collect(Collectors.toCollection(WhatsappMessages::new));
    }
}
