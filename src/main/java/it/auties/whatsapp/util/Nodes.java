package it.auties.whatsapp.util;

import it.auties.whatsapp.socket.Node;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A utility class used to work with {@link Node}
 */
@UtilityClass
public class Nodes {
    /**
     * Returns {@code nodes} if any non-null value is found in the internal.
     * Otherwise, returns null.
     *
     * @param nodes the nullable list of nodes to check
     * @return a nullable list of nodes
     */
    public static List<Node> orNull(Collection<Node> nodes) {
        return nodes == null || nodes.stream().allMatch(Objects::isNull) ? null
                : new ArrayList<>(nodes);
    }

    /**
     * Constructs a list of WhatsappNodes from a generic List
     *
     * @param list the generic list to parse
     * @return a non-null list containing only objects from {@code list} of type WhatsappNode
     */
    public static LinkedList<Node> findAll(Object list) {
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

    /**
     * Finds the first Node that matches the provided description
     *
     * @param nodes the non-null list of nodes to look through
     * @param description the non-null description to look for
     * @return a non-null list of nodes
     */
    public static List<Node> findAll(@NonNull List<Node> nodes, @NonNull String description) {
        return nodes.stream()
                .map(node -> node.findNodes(description))
                .flatMap(Collection::stream)
                .toList();
    }
}
