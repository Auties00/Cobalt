package it.auties.whatsapp.response;

import it.auties.whatsapp.protobuf.model.misc.Node;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * An abstract class to represent a class that may represent a WhatsappNode sent by WhatsappWeb's WebSocket
 *
 * @param <T> the type of the data this object holds
 */
@Data
@Accessors(fluent = true)
public non-sealed abstract class BinaryResponseModel<T> implements ResponseModel {
    /**
     * The data that this response wraps
     */
    protected @NonNull T data;

    /**
     * Constructs a new BinaryResponseModel from {@code node}
     *
     * @param node the node to parse
     */
    protected BinaryResponseModel(@NonNull Node node) {
        this.data = parseResponse(node);
    }

    /**
     * Parses the response
     *
     * @param node the node to parse
     */
    protected abstract @NonNull T parseResponse(@NonNull Node node);
}
