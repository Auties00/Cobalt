package it.auties.whatsapp4j.response.model.binary;

import it.auties.whatsapp4j.model.WhatsappNode;
import it.auties.whatsapp4j.response.model.shared.Response;
import it.auties.whatsapp4j.response.model.shared.ResponseModel;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;

/**
 * A record that wraps a WhatsappNode sent by WhatsappWeb's WebSocket as response for a request
 * This WhatsappNode can be converted to a ResponseModel using {@link BinaryResponse#toModel(Class)}
 * This class is final, this means that it cannot be extended
 */
public final record BinaryResponse(@NotNull WhatsappNode node) implements Response {
    /**
     * Converts this object to a BinaryResponseModel
     *
     * @param clazz a Class that represents {@param <T>}
     * @param <T> the specific raw type of the model
     * @return an instance of the type of model requested
     */
    @Override
    public <T extends ResponseModel> @NotNull T toModel(@NotNull Class<T> clazz) throws ClassCastException{
        try {
            return clazz.getConstructor(WhatsappNode.class).newInstance(node);
        }catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex){
            throw new ClassCastException(ex.getMessage());
        }
    }
}
