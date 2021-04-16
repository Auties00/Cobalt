package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.response.model.JsonResponse;
import it.auties.whatsapp4j.utils.Validate;
import lombok.Builder;
import jakarta.validation.constraints.NotNull;


import java.util.List;
import java.util.Map;

/**
 * An immutable model class that represents the primary unit used by WhatsappWeb's WebSocket to communicate with the client.
 * This class also offers a builder, accessible using {@link WhatsappNodeBuilder}.
 *
 * @param description a non null String that describes the data that this object holds in its {@code attrs} and {@code content}
 * @param attrs       a non null Map of strings that describe additional information related to the content of this object or an encoded object when sending a message a protobuf object is not optimal
 * @param content     a nullable object, usually a {@link WhatsappNode}, a {@link String} or a {@link WhatsappProtobuf}'s object
 */
@Builder
public record WhatsappNode(@NotNull String description, @NotNull Map<String, String> attrs,  Object content) {
    /**
     * Constructs a WhatsappNode from a list
     *
     * @param list the generic list to parse
     * @return a non null list containing only objects from {@code list} of type WhatsappNode
     */
    @SuppressWarnings("unchecked")
    public static @NotNull WhatsappNode fromList(@NotNull List<?> list) {
        Validate.isTrue(list.size() == 3, "WhatsappAPI: Cannot parse %s as a WhatsappNode", list);
        if(!(list.get(0) instanceof String description)){
            throw new IllegalArgumentException("WhatsappAPI: Cannot parse %s as a WhatsappNode, no description found".formatted(list));
        }

        if(!(list.get(1) instanceof String attrs)){
            throw new IllegalArgumentException("WhatsappAPI: Cannot parse %s as a WhatsappNode, no attrs found".formatted(list));
        }

        return new WhatsappNode(description, (Map<String, String>) JsonResponse.fromJson(attrs).data(), list.get(2));
    }

    /**
     * Constructs a list of WhatsappNodes from a generic List
     *
     * @param list the generic list to parse
     * @return a non null list containing only objects from {@code list} of type WhatsappNode
     */
    public static @NotNull List<WhatsappNode> fromGenericList(@NotNull List<?> list) {
        return list.stream()
                .filter(entry -> entry instanceof WhatsappNode)
                .map(WhatsappNode.class::cast)
                .toList();
    }

    /**
     * Returns a list of child WhatsappNodes
     *
     * @return a non null list containing WhatsappNodes extracted from this node's content
     * @throws NullPointerException     if {@link WhatsappNode#content} is null
     * @throws IllegalArgumentException if {@link WhatsappNode#content} is not a List
     */
    public @NotNull List<WhatsappNode> childNodes() {
        if(content == null){
            return List.of();
        }

        if (!(content instanceof List<?> listContent)) {
            throw new IllegalArgumentException("WhatsappAPI: Cannot extract child nodes from %s: expected List<?> as content".formatted(this));
        }

        return fromGenericList(listContent);
    }
}
