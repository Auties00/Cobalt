package it.auties.whatsapp.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.auties.whatsapp.protobuf.model.misc.Node;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UtilityClass
public class Nodes {
    /**
     * Jackson instance
     */
    private static final ObjectMapper JACKSON = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Constructs a WhatsappNode from a list where the content is always a JSON String
     *
     * @param list the generic list to parse
     * @return a non-null list containing only objects from {@code list} of type WhatsappNode
     */
    public static @NonNull Node fromList(@NonNull List<?> list) {
        Validate.isTrue(list.size() == 3, "WhatsappAPI: Cannot parse %s as a WhatsappNode", list);
        if (!(list.get(0) instanceof String description)) {
            throw new IllegalArgumentException("WhatsappAPI: Cannot parse %s as a WhatsappNode, no description found".formatted(list));
        }

        if (!(list.get(1) instanceof String attrs)) {
            throw new IllegalArgumentException("WhatsappAPI: Cannot parse %s as a WhatsappNode, no attrs found".formatted(list));
        }

        try {
            return new Node(description, parseListAttrs(attrs), JACKSON.writeValueAsString(list.get(2)));
        }catch (JsonProcessingException exception){
            throw new IllegalArgumentException("Cannot parse node from list", exception);
        }
    }

    /**
     * Constructs a list of WhatsappNodes from a generic List
     *
     * @param list the generic list to parse
     * @return a non-null list containing only objects from {@code list} of type WhatsappNode
     */
    public static @NonNull LinkedList<Node> validNodes(@NonNull Collection<?> list) {
        return list.stream()
                .filter(entry -> entry instanceof Node)
                .map(Node.class::cast)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private static @NonNull Map<String, Object> parseListAttrs(String attrs){
        if(attrs == null){
            return Map.of();
        }

        if (!attrs.startsWith("}") || !attrs.endsWith("}")) {
            return Map.of("content", attrs);
        }

        try {
            return JACKSON.readValue(attrs, new TypeReference<>() {});
        }catch (JsonProcessingException exception){
            throw new IllegalArgumentException("Cannot parse attributes from string", exception);
        }
    }
}
