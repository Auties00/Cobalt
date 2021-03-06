package it.auties.whatsapp4j.response.model.binary;

import it.auties.whatsapp4j.model.WhatsappNode;
import it.auties.whatsapp4j.response.model.shared.Response;
import it.auties.whatsapp4j.response.model.shared.ResponseModel;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;

public record BinaryResponse(@NotNull WhatsappNode node) implements Response {
    public <T extends ResponseModel> @NotNull T toModel(@NotNull Class<T> clazz) {
        try {
            var result = clazz.getConstructor().newInstance();
            if (!(result instanceof BinaryResponseModel binaryResponseModel)) throw new IllegalArgumentException("WhatsappAPI: Cannot convert to BinaryResponse: expected BinaryResponseModel");
            binaryResponseModel.populateWithData(node);
            return result;
        }catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex){
            throw new RuntimeException(ex.getMessage());
        }
    }
}
