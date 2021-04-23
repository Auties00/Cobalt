package it.auties.whatsapp4j.response.model;

import it.auties.whatsapp4j.model.WhatsappNode;
import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;

/**
 * A record that wraps a WhatsappNode sent by WhatsappWeb's WebSocket as response for a request.
 * This WhatsappNode can be converted to a ResponseModel using {@link BinaryResponse#toModel(Class)}.
 * This class is final, this means that it cannot be extended.
 */
public final class BinaryResponse extends Response<WhatsappNode> {
    public BinaryResponse(@NotNull String tag, @NotNull WhatsappNode content) {
        super(tag, null, content);
    }

    /**
     * Converts this object to a BinaryResponseModel
     *
     * @param clazz a Class that represents {@code <T>}
     * @param <T> the specific raw type of the model
     * @return an instance of the type of model requested
     */
    @Override
    @SneakyThrows
    public <T extends ResponseModel> @NotNull T toModel(@NotNull Class<T> clazz) {
        return clazz.getConstructor(WhatsappNode.class).newInstance(content);
    }
}
