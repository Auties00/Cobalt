package it.auties.whatsapp.util;

import it.auties.whatsapp.model.request.Node;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.stream.Collectors;

@UtilityClass
public class Nodes {
    public static List<Node> orNull(Collection<Node> nodes) {
        return nodes == null || nodes.stream()
                .allMatch(Objects::isNull) ?
                null :
                new ArrayList<>(nodes);
    }

    public static LinkedList<Node> findAll(Object list) {
        if (list == null) {
            return new LinkedList<>();
        }

        if (!(list instanceof Collection<?> collection)) {
            return new LinkedList<>();
        }

        return collection.stream()
                .filter(entry -> entry instanceof Node)
                .map(Node.class::cast)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    public static Optional<Node> findFirst(@NonNull List<Node> nodes, @NonNull String description) {
        return nodes.stream()
                .map(node -> node.findNodes(description))
                .flatMap(Collection::stream)
                .findFirst();
    }
}
