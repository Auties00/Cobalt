package it.auties.whatsapp4j.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.auties.whatsapp4j.model.WhatsappResponseNode;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface Response {
    ObjectMapper JACKSON = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    @SneakyThrows
    static @NotNull MapResponse fromWhatsappResponse(@NotNull String json) {
        var index = json.indexOf("{");
        return new MapResponse(index == -1 ? Map.of() : JACKSON.readValue(json.substring(index), new TypeReference<>() {}));
    }

    @SneakyThrows
    static @NotNull WhatsappResponseNode fromJson(@NotNull String parse) {
        var split = parse.split(",", 2);
        var tag = split[0];
        var contentJson = JACKSON.readTree(split[1]);

        try {
            var contentMap = JACKSON.convertValue(contentJson, new TypeReference<HashMap<String, Object>>() {});
            return new WhatsappResponseNode(tag, null, new MapResponse(contentMap));
        }catch (IllegalArgumentException ignored){
            var contentList = JACKSON.convertValue(contentJson, new TypeReference<List<Object>>() {});
            if(contentList.get(1) instanceof Map<?, ?> content){
                return new WhatsappResponseNode(tag, (String) contentList.get(0), new MapResponse((Map<String, Object>) content));
            }

            return new WhatsappResponseNode(tag, null, new ListResponse(contentList));
        }
    }
}