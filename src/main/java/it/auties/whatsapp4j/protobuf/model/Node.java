package it.auties.whatsapp4j.protobuf.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.auties.whatsapp4j.utils.Validate;
import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Map;

/**
 * An immutable model class that represents the primary unit used by WhatsappWeb's WebSocket to communicate with the client.
 *
 * @param description a non null String that describes the data that this object holds in its {@code attrs} and {@code content}
 * @param attrs       a non null Map of strings that describe additional information related to the content of this object or an encoded object when sending a message a protobuf object is not optimal
 * @param content     a nullable object, usually a List of {@link Node}, a {@link String} or a {@link Number}
 */
public record Node(@NotNull String description,
                   @NotNull Map<String, String> attrs, Object content) {
    private static final ObjectMapper JACKSON = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Constructs a WhatsappNode from a list where the content is always a JSON String
     *
     * @param list the generic list to parse
     * @return a non null list containing only objects from {@code list} of type WhatsappNode
     */
    @SneakyThrows
    public static @NotNull Node fromList(@NotNull List<?> list) {
        Validate.isTrue(list.size() == 3, "WhatsappAPI: Cannot parse %s as a WhatsappNode", list);
        if (!(list.get(0) instanceof String description)) {
            throw new IllegalArgumentException("WhatsappAPI: Cannot parse %s as a WhatsappNode, no description found".formatted(list));
        }

        if (!(list.get(1) instanceof String attrs)) {
            throw new IllegalArgumentException("WhatsappAPI: Cannot parse %s as a WhatsappNode, no attrs found".formatted(list));
        }

        return new Node(description, parseListAttrs(attrs), JACKSON.writeValueAsString(list.get(2)));
    }

    @SneakyThrows
    private static @NotNull Map<String, String> parseListAttrs(String attrs){
        return attrs == null ? Map.of() : attrs.startsWith("}") && attrs.endsWith("}") ? JACKSON.readValue(attrs, new TypeReference<>() {}) : Map.of("content", attrs);
    }

    /**
     * Constructs a list of WhatsappNodes from a generic List
     *
     * @param list the generic list to parse
     * @return a non null list containing only objects from {@code list} of type WhatsappNode
     */
    public static @NotNull List<Node> fromGenericList(@NotNull List<?> list) {
        return list.stream()
                .filter(entry -> entry instanceof Node)
                .map(Node.class::cast)
                .toList();
    }

    /**
     * Returns a list of child WhatsappNodes
     *
     * @return a non null list containing WhatsappNodes extracted from this node's content
     * @throws NullPointerException     if {@link Node#content} is null
     * @throws IllegalArgumentException if {@link Node#content} is not a List
     */
    public @NotNull List<Node> childNodes() {
        if (content == null) {
            return List.of();
        }

        if (!(content instanceof List<?> listContent)) {
            throw new IllegalArgumentException("WhatsappAPI: Cannot extract child nodes from %s: expected List<?> as content".formatted(this));
        }

        return fromGenericList(listContent);
    }
}
