package it.auties.whatsapp4j.response.impl.binary;

import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.protobuf.chat.Chat;
import it.auties.whatsapp4j.protobuf.model.Node;
import it.auties.whatsapp4j.response.model.binary.BinaryResponseModel;
import lombok.NonNull;

import java.util.Objects;
import java.util.Optional;

/**
 * A binary model to represent a WhatsappNode that contains {@link Chat}
 * The property {@link ChatResponse#data} is nullable and should be accessed using the accessor {@link ChatResponse#data()}
 */
public final class ChatResponse extends BinaryResponseModel<Optional<Chat>> {
    /**
     * Constructs a new ChatResponse from {@code node}
     * If {@code node} cannot be parsed no exception is thrown
     *
     * @param node the WhatsappNode to parse
     */
    public ChatResponse(@NonNull Node node) {
        super(node);
    }

    /**
     * Parses the response
     *
     * @param node the node to parse
     */
    @Override
    protected @NonNull Optional<Chat> parseResponse(@NonNull Node node) {
        var duplicate = Boolean.parseBoolean(node.attrs().getOrDefault("duplicate", "false"));
        return duplicate ? Optional.empty() : node.childNodes()
                .stream()
                .findFirst()
                .filter(childNode -> childNode.description().equals("chat"))
                .map(Node::attrs)
                .map(Chat::fromAttributes);
    }
}
