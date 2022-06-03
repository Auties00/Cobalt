package it.auties.whatsapp.model.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import it.auties.whatsapp.util.JacksonProvider;
import lombok.NonNull;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public record MessageSync(@NonNull String type, String chatJid, String messageId, boolean fromMe) implements JacksonProvider {
    public static MessageSync ofJson(@NonNull String json){
        try {
            var array = JSON.readValue(json, new TypeReference<List<String>>() {});
            var type = getProperty(array, 0)
                    .orElseThrow(() -> new NoSuchElementException("Cannot parse MessageSync: missing type"));
            var chatJid = getProperty(array, 1)
                    .orElse(null);
            var messageId = getProperty(array, 2)
                    .orElse(null);
            var fromMe = getProperty(array, 3)
                    .map(Boolean::parseBoolean)
                    .orElse(false);
            return new MessageSync(type, chatJid, messageId, fromMe);
        }catch (JsonProcessingException exception){
            throw new IllegalStateException("Cannot parse MessageSync: a json exception occurred while parsing %s", exception);
        }
    }

    private static Optional<String> getProperty(List<String> list, int index){
        return list.size() > index ? Optional.ofNullable(list.get(index))
                : Optional.empty();
    }
}
