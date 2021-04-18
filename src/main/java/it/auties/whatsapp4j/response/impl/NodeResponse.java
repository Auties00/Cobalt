package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.model.WhatsappNode;
import it.auties.whatsapp4j.response.model.BinaryResponseModel;
import jakarta.validation.constraints.NotNull;

/**
 * A binary model to represent a WhatsappNode that contains {@link WhatsappNode}
 * The property {@link NodeResponse#data} is nullable and should be accessed using the accessor {@link NodeResponse#data()}
 */
public class NodeResponse extends BinaryResponseModel<WhatsappNode> {
    /**
     * Constructs a new NodeResponse from {@code node}
     *
     * @param node the node to wrap
     */
    protected NodeResponse(@NotNull WhatsappNode node) {
        super(node);
    }
}
