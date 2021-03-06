package it.auties.whatsapp4j.response.impl.shared;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.soabase.recordbuilder.core.RecordBuilder;
import it.auties.whatsapp4j.response.impl.WhatsappResponseBuilder;
import it.auties.whatsapp4j.response.model.json.JsonListResponse;
import it.auties.whatsapp4j.response.model.json.JsonResponse;
import it.auties.whatsapp4j.response.model.shared.Response;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;

@RecordBuilder
@ToString
public record WhatsappResponse(@NotNull String tag, @Nullable String description, @NotNull Response data) {
    private static final ObjectMapper JACKSON = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    public static @NotNull WhatsappResponse fromJson(@NotNull String parse) {
        try {
            var split = parse.split(",", 2);
            if(split.length != 2 && parse.startsWith("!")){
                return new WhatsappResponse(parse, "pong", new JsonResponse(new HashMap<>()));
            }

            var response = WhatsappResponseBuilder.builder().tag(split[0]);
            var content = parseContent(split[1], 0);
            if(content.isEmpty()){
                return response.data(new JsonResponse(new HashMap<>())).build();
            }
      
            var jsonNode = JACKSON.readTree(content);
            if (!jsonNode.isArray()) {
                return response.data(new JsonResponse(JACKSON.readerFor(new TypeReference<>(){}).readValue(jsonNode))).build();
            }

            var possibleMap = jsonNode.get(1);
            if (possibleMap == null) {
                return response.data(new JsonListResponse(JACKSON.readerFor(new TypeReference<>(){}).readValue(jsonNode))).build();
            }

            var possibleMapContent = possibleMap.toString();
            if (!possibleMapContent.startsWith("{") || !possibleMapContent.endsWith("}")) {
                return response.data(new JsonListResponse(JACKSON.readerFor(new TypeReference<>(){}).readValue(jsonNode))).build();
            }

            return response.description(jsonNode.get(0).textValue()).data(new JsonResponse(JACKSON.readerFor(new TypeReference<>(){}).readValue(possibleMap))).build();
        }catch (IOException ex){
            throw new IllegalArgumentException("WhatsappAPI: Cannot deserialize %s into a WhatsappResponse".formatted(parse));
        }
    }

    private static @NotNull String parseContent(@NotNull String content, int index){
        return content.length() > index && content.charAt(index) == ',' ? parseContent(content, index + 1) : content.substring(index);
    }
}
