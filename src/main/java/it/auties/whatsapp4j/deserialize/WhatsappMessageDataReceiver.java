package it.auties.whatsapp4j.deserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import it.auties.whatsapp4j.model.WhatsappResponse;
import it.auties.whatsapp4j.response.model.JsonListResponse;
import it.auties.whatsapp4j.response.model.JsonResponse;
import it.auties.whatsapp4j.response.model.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static it.auties.whatsapp4j.utils.JsonContext.JACKSON;

public class WhatsappMessageDataReceiver extends JsonDeserializer<Response<?,?>> {

    @Override
    public Response<?,?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        String parse = jsonParser.getValueAsString();
        try {
            Map<String, String> dataString = JACKSON.readerFor(new TypeReference<Map<String, String>>() {}).readValue(parse);


            var response = WhatsappResponse.tag(split[0]);
            var content = parseContent(split[1], 0);
            if (content.isEmpty()) {
                return response.data(new JsonResponse(new HashMap<>())).build();
            }

            var jsonNode = JACKSON.readTree(content);
            if (!jsonNode.isArray()) {
                return response.data(new JsonResponse(JACKSON.readerFor(new TypeReference<>() {
                }).readValue(jsonNode))).build();
            }

            var possibleMap = jsonNode.get(1);
            if (possibleMap == null) {
                return response.data(new JsonListResponse(JACKSON.readerFor(new TypeReference<>() {
                }).readValue(jsonNode))).build();
            }

            var possibleMapContent = possibleMap.toString();
            if (!possibleMapContent.startsWith("{") || !possibleMapContent.endsWith("}")) {
                return response.data(new JsonListResponse(JACKSON.readerFor(new TypeReference<>() {
                }).readValue(jsonNode))).build();
            }

            return response.description(jsonNode.get(0).textValue()).data(new JsonResponse(JACKSON.readerFor(new TypeReference<>() {
            }).readValue(possibleMap))).build();
        } catch (IOException ex) {
            throw new IllegalArgumentException("WhatsappAPI: Cannot deserialize %s into a WhatsappResponse".formatted(parse));
        }

        return null;
    }

}
