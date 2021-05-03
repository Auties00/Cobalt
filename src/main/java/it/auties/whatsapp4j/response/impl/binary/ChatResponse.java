package it.auties.whatsapp4j.response.impl.binary;

import it.auties.whatsapp4j.model.WhatsappChat;
import it.auties.whatsapp4j.model.WhatsappNode;
import it.auties.whatsapp4j.response.model.binary.BinaryResponseModel;
import jakarta.validation.constraints.NotNull;

/**
 * A binary model to represent a WhatsappNode that contains {@link WhatsappChat}
 * The property {@link ChatResponse#data} is nullable and should be accessed using the accessor {@link ChatResponse#data()}
 */
public final class ChatResponse extends BinaryResponseModel<WhatsappChat> {
    /**
     * Constructs a new ChatResponse from {@code node}
     * If {@code node} cannot be parsed no exception is thrown
     *
     * @param node the WhatsappNode to parse
     */
    public ChatResponse(@NotNull WhatsappNode node) {
        super(node);
    }

    /**
     * Parses the response
     *
     * @param node the node to parse
     */
    @Override
    protected @NotNull WhatsappChat parseResponse(@NotNull WhatsappNode node) {
        return node.childNodes()
                .stream()
                .findFirst()
                .filter(childNode -> childNode.description().equals("chat"))
                .map(WhatsappNode::attrs)
                .map(WhatsappChat::fromAttributes)
                .orElseThrow(() -> new IllegalArgumentException("WhatsappAPI: Cannot parse %s as WhatsappChat".formatted(node)));
    }
}
