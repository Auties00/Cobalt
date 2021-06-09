package it.auties.whatsapp4j.response.impl.binary;

import it.auties.whatsapp4j.protobuf.info.MessageInfo;
import it.auties.whatsapp4j.protobuf.model.Messages;
import it.auties.whatsapp4j.protobuf.model.Node;
import it.auties.whatsapp4j.response.model.binary.BinaryResponseModel;
import jakarta.validation.constraints.NotNull;

import java.util.stream.Collectors;

/**
 * A binary model to represent a WhatsappNode that contains {@link Messages}
 * The property {@link MessagesResponse#data} is nullable and should be accessed using the accessor {@link MessagesResponse#data()}
 */
public final class MessagesResponse extends BinaryResponseModel<Messages> {
    /**
     * Constructs a new MessagesResponse from {@code node}
     * If {@code node} cannot be parsed no exception is thrown
     *
     * @param node the WhatsappNode to parse
     */
    public MessagesResponse(@NotNull Node node) {
        super(node);
    }

    /**
     * Parses the response
     *
     * @param node the node to parse
     */
    @Override
    protected @NotNull Messages parseResponse(@NotNull Node node) {
        return node.childNodes()
                .stream()
                .filter(childNode -> childNode.description().equals("message"))
                .map(Node::content)
                .filter(childContent -> childContent instanceof MessageInfo)
                .map(MessageInfo.class::cast)
                .collect(Collectors.toCollection(Messages::new));
    }
}
