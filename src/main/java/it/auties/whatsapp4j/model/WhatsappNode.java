package it.auties.whatsapp4j.model;

import io.soabase.recordbuilder.core.RecordBuilder;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An immutable model class that represents the primary unit used by WhatsappWeb's WebSocket to communicate with the client
 * This class also offers a builder, accessible using {@link WhatsappNodeBuilder}
 * @param description a non null String that describes the data that this object holds in its {@param attrs} and {@param content}
 * @param attrs a non null Map of strings that describe additional information related to the content of this object or an encoded object when sending a message a protobuf object is not optimal
 * @param content a nullable object, usually a {@link WhatsappNode}, a {@link String} or a {@link WhatsappProtobuf}'s object
 */
@RecordBuilder
@ToString
public record WhatsappNode(@NotNull String description, @NotNull Map<String, String> attrs, @Nullable Object content) {
    /**
     * Constructs a list of WhatsappNodes from a generic List
     *
     * @param list the generic list to parse
     * @return a non null list containing only objects from {@param list} of type WhatsappNode
     */
    public static @NotNull List<WhatsappNode> fromGenericList(@NotNull List<?> list){
        return list.stream()
                .filter(entry -> entry instanceof WhatsappNode)
                .map(WhatsappNode.class::cast)
                .collect(Collectors.toUnmodifiableList());
    }
}
