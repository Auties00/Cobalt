package it.auties.whatsapp4j.common.protobuf.model.misc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.auties.whatsapp4j.common.utils.Validate;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable model class that represents the primary unit used by WhatsappWeb's WebSocket to communicate with the client.
 *
 * @param description a non-null String that describes the data that this object holds in its {@code attrs} and {@code content}
 * @param attrs       a non-null Map of strings that describe additional information related to the content of this object or an encoded object when sending a message a protobuf object is not optimal
 * @param content     a nullable object, usually a List of {@link Node}, a {@link String} or a {@link Number}
 */
public record Node(@NonNull String description,
                   @NonNull Map<String, Object> attrs, Object content) {
    private static final ObjectMapper JACKSON = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Constructs a Node that only provides a non-null tag
     *
     * @param description a non-null String that describes the data that this object holds
     */
    public Node(@NonNull String description){
        this(description, Map.of(), null);
    }

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

    /**
     * Constructs a list of WhatsappNodes from a generic List
     *
     * @param list the generic list to parse
     * @return a non-null list containing only objects from {@code list} of type WhatsappNode
     */
    public static @NonNull List<Node> fromGenericList(@NonNull Collection<?> list) {
        return list.stream()
                .filter(entry -> entry instanceof Node)
                .map(Node.class::cast)
                .toList();
    }

    /**
     * Returns a list of child WhatsappNodes
     *
     * @return a non-null list containing WhatsappNodes extracted from this node's content
     */
    public @NonNull List<Node> childNodes() {
        if (!hasContent()) {
            return List.of();
        }

        if (!(content instanceof List<?> listContent)) {
            return List.of();
        }

        return fromGenericList(listContent);
    }

    /**
     * Returns whether this object's content is non-null
     *
     * @return true if this object has a content
     */
    public boolean hasContent(){
        return Objects.nonNull(content);
    }

    /**
     * Returns whether this object's content is non-null
     *
     * @return true if this object has a content
     */
    public int size(){
        var descriptionSize = 1;
        var attributesSize = 2 * attrs.size();
        var contentSize = hasContent() ? 1 : 0;
        return descriptionSize + attributesSize + contentSize;
    }
}
