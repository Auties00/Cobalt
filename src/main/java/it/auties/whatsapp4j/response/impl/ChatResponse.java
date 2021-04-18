package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.model.WhatsappChat;
import it.auties.whatsapp4j.model.WhatsappNode;
import it.auties.whatsapp4j.response.model.BinaryResponseModel;
import jakarta.validation.constraints.NotNull;

/**
 * A binary model to represent a WhatsappNode that contains {@link WhatsappChat}
 * The property {@link ChatResponse#data} is nullable and should be accessed using the accessor {@link ChatResponse#data()}
 */
public class ChatResponse extends BinaryResponseModel<WhatsappChat> {
    /**
     * Constructs a new ChatResponse from {@code node}
     * If {@code node} cannot be parsed no exception is thrown
     *
     * @param node the WhatsappNode to parse
     */
    public ChatResponse(@NotNull WhatsappNode node) {
        super(node);
        this.data = node.childNodes()
                .stream()
                .findFirst()
                .filter(childNode -> childNode.description().equals("chat"))
                .map(WhatsappNode::attrs)
                .map(WhatsappChat::fromAttributes)
                .orElseThrow(() -> new IllegalArgumentException("WhatsappAPI: Cannot parse %s as WhatsappChat".formatted(node)));
    }
}
