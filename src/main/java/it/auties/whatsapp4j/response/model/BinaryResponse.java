package it.auties.whatsapp4j.response.model;

import it.auties.whatsapp4j.model.WhatsappNode;
import lombok.SneakyThrows;
import jakarta.validation.constraints.NotNull;

/**
 * A record that wraps a WhatsappNode sent by WhatsappWeb's WebSocket as response for a request.
 * This WhatsappNode can be converted to a ResponseModel using {@link BinaryResponse#toModel(Class)}.
 * This class is final, this means that it cannot be extended.
 */
public final class BinaryResponse implements Response<BinaryResponse,WhatsappNode> {

    private final WhatsappNode node;

    public BinaryResponse(@NotNull WhatsappNode node)  {
        this.node = node;
    }

    /**
     * Converts this object to a BinaryResponseModel
     *
     * @param clazz a Class that represents {@code <T>}
     * @return an instance of the type of model requested
     */
    @Override
    @SneakyThrows
    public WhatsappNode toModel(@NotNull Class<WhatsappNode> clazz) {
        return clazz.getConstructor(WhatsappNode.class).newInstance(node);
    }
}
