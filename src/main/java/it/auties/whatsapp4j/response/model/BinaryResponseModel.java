package it.auties.whatsapp4j.response.model;

import it.auties.whatsapp4j.model.WhatsappNode;
import lombok.ToString;
import jakarta.validation.constraints.NotNull;
import lombok.experimental.Accessors;


import java.util.Optional;

/**
 * An abstract class to represent a class that may represent a WhatsappNode sent by WhatsappWeb's WebSocket
 *
 * @param <T> the type of the data this object holds
 */
@ToString
@Accessors(chain = true)
public abstract class BinaryResponseModel<J extends BinaryResponseModel<J, T>, T> implements ResponseModel<J> {
    private final WhatsappNode node;
    /**
     * The data that this response wraps
     */
    protected T data;

    /**
     * Constructs a new BinaryResponseModel from {@code node}
     *
     * @param node the node to parse
     */
    protected BinaryResponseModel(@NotNull WhatsappNode node) {

        this.node = node;
    }

    /**
     * Returns an optional that wraps {@link BinaryResponseModel#data}
     *
     * @return a non empty optional if {@link BinaryResponseModel#data} isn't null, otherwise an empty one
     */
    public Optional<T> data() {
        return Optional.ofNullable(data);
    }
}
