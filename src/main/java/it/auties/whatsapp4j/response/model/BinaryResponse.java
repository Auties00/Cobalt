package it.auties.whatsapp4j.response.model;

import it.auties.whatsapp4j.model.WhatsappNode;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.NoSuchElementException;

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
