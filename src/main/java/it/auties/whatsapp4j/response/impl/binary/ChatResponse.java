package it.auties.whatsapp4j.response.impl.binary;

import it.auties.whatsapp4j.protobuf.chat.Chat;
import it.auties.whatsapp4j.protobuf.model.Node;
import it.auties.whatsapp4j.response.model.binary.BinaryResponseModel;
import jakarta.validation.constraints.NotNull;

/**
 * A binary model to represent a WhatsappNode that contains {@link Chat}
 * The property {@link ChatResponse#data} is nullable and should be accessed using the accessor {@link ChatResponse#data()}
 */
public final class ChatResponse extends BinaryResponseModel<Chat> {
    /**
     * Constructs a new ChatResponse from {@code node}
     * If {@code node} cannot be parsed no exception is thrown
     *
     * @param node the WhatsappNode to parse
     */
    public ChatResponse(@NotNull Node node) {
        super(node);
    }

    /**
     * Parses the response
     *
     * @param node the node to parse
     */
    @Override
    protected @NotNull Chat parseResponse(@NotNull Node node) {
        return node.childNodes()
                .stream()
                .findFirst()
                .filter(childNode -> childNode.description().equals("chat"))
                .map(Node::attrs)
                .map(Chat::fromAttributes)
                .orElseThrow(() -> new IllegalArgumentException("WhatsappAPI: Cannot parse %s as Chat".formatted(node)));
    }
}
