package it.auties.whatsapp.utils;

import it.auties.whatsapp.protobuf.model.Node;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.stream.Collectors;

@UtilityClass
public class Nodes {
    /**
     * Returns {@code nodes} if any non-null value is found in the collection.
     * Otherwise, returns null.
     *
     * @param nodes the nullable list of nodes to check
     * @return a nullable list of nodes
     */
    public static List<Node> orNull(List<Node> nodes) {
        return nodes == null || nodes.stream().allMatch(Objects::isNull) ? null
                : nodes;
    }

    /**
     * Constructs a list of WhatsappNodes from a generic List
     *
     * @param list the generic list to parse
     * @return a non-null list containing only objects from {@code list} of type WhatsappNode
     */
    public static @NonNull LinkedList<Node> filter(Object list) {
        if(list == null){
            return new LinkedList<>();
        }

        if(!(list instanceof Collection<?> collection)){
            return new LinkedList<>();
        }

        return collection.stream()
                .filter(entry -> entry instanceof Node)
                .map(Node.class::cast)
                .collect(Collectors.toCollection(LinkedList::new));
    }
}
