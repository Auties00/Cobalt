package it.auties.whatsapp4j.response.impl.binary;

import it.auties.whatsapp4j.model.WhatsappNode;
import it.auties.whatsapp4j.response.model.binary.BinaryResponseModel;
import org.jetbrains.annotations.NotNull;

/**
 * A binary model to represent a WhatsappNode that contains {@link WhatsappNode}
 * The property {@link NodeResponse#data} is nullable and should be accessed using the accessor {@link NodeResponse#data()}
 */
public class NodeResponse extends BinaryResponseModel<WhatsappNode> {
    /**
     * Constructs a new NodeResponse from {@param node}
     *
     * @param node the node to wrap
     */
    protected NodeResponse(@NotNull WhatsappNode node) {
        super(node);
    }
}
