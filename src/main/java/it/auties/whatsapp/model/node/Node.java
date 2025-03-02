package it.auties.whatsapp.model.node;

import it.auties.whatsapp.util.Json;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An immutable model class that represents the primary unit used by WhatsappWeb's WebSocket to communicate with the client
 *
 * @param description a non-null String that describes the content of this node
 * @param attributes  a non-null Map that describes the metadata of this object
 * @param content     a nullable object: a List of {@link Node}, a {@link String} or a {@link Number}
 */
public record Node(String description, Attributes attributes, Object content) {
    /**
     * Constructs a Node that only provides a non-null tag
     *
     * @param description a non-null String that describes the data that this object holds
     * @return a new node with the above characteristics
     */
    public static Node of(String description) {
        return new Node(description, Attributes.of(), null);
    }

    /**
     * Constructs a Node that provides a non-null tag and a nullable content
     *
     * @param description         a non-null String that describes the data that this object holds
     * @param contentOrAttributes a nullable object, usually a List of {@link Node}, a {@link String} or a {@link Number}, or the request's attributes
     * @return a new node with the above characteristics
     */
    public static Node of(String description, Object contentOrAttributes) {
        if (contentOrAttributes instanceof Attributes attributes) {
            return new Node(description, attributes, null);
        }

        if (contentOrAttributes instanceof Map<?, ?> attributes) {
            try {
                return new Node(description, getAttributesOrThrow(attributes), null);
            } catch (ClassCastException exception) {
                throw new IllegalArgumentException("Unexpected attributes type: " + contentOrAttributes.getClass().getName(), exception);
            }
        }

        if (contentOrAttributes instanceof List<?> list) {
            try {
                return new Node(description, Attributes.of(), getNodesOrThrow(list));
            } catch (ClassCastException exception) {
                throw new IllegalArgumentException("Unexpected attributes type: " + contentOrAttributes.getClass().getName(), exception);
            }
        }

        if (contentOrAttributes instanceof Node node) {
            return new Node(description, Attributes.of(), List.of(node));
        }

        return new Node(description, Attributes.of(), contentOrAttributes);
    }

    /**
     * Constructs a Node that provides a non-null tag and a nullable content
     *
     * @param description a non-null String that describes the data that this object holds
     * @param content     any number of non-null nodes
     * @return a new node with the above characteristics
     */
    public static Node of(String description, Node... content) {
        return new Node(description, Attributes.of(), getNodesOrThrow(content));
    }

    /**
     * Constructs a Node that provides a non-null tag and a nullable content
     *
     * @param description a non-null String that describes the data that this object holds
     * @param attributes  the attributes of this node
     * @param content     any number of non-null nodes
     * @return a new node with the above characteristics
     */
    public static Node of(String description, Map<String, Object> attributes, Node... content) {
        return of(description, Attributes.of(attributes), getNodesOrThrow(content));
    }

    /**
     * Constructs a Node that provides a non-null tag and a nullable content
     *
     * @param description a non-null String that describes the data that this object holds
     * @param attributes  the attributes of this node
     * @param content     any number of non-null nodes
     * @return a new node with the above characteristics
     */
    public static Node of(String description, Attributes attributes, Node... content) {
        return new Node(description, attributes, getNodesOrThrow(content));
    }

    /**
     * Constructs a Node that provides a non-null tag and a nullable content
     *
     * @param description a non-null String that describes the data that this object holds
     * @param attributes  the attributes of this node
     * @param content     any number of non-null nodes
     * @return a new node with the above characteristics
     */
    public static Node of(String description, Map<String, Object> attributes, Object content) {
        return of(description, Attributes.of(attributes), content);
    }

    /**
     * Constructs a Node that provides a non-null tag and a nullable content
     *
     * @param description a non-null String that describes the data that this object holds
     * @param attributes  the attributes of this node
     * @param content     any number of non-null nodes
     * @return a new node with the above characteristics
     */
    public static Node of(String description, Attributes attributes, Object content) {
        if (content instanceof List<?> list) {
            try {
                return new Node(description, attributes, getNodesOrThrow(list));
            } catch (ClassCastException exception) {
                throw new IllegalArgumentException("Unexpected attributes type: " + content.getClass().getName(), exception);
            }
        }

        if (content instanceof Node node) {
            return new Node(description, attributes, List.of(node));
        }

        return new Node(description, attributes, content);
    }

    @SuppressWarnings("unchecked")
    private static Attributes getAttributesOrThrow(Map<?, ?> attributes) {
        try {
            return Attributes.of((Map<String, Object>) attributes);
        } catch (ClassCastException exception) {
            throw new IllegalArgumentException("Unexpected attributes type: " + attributes.getClass().getName(), exception);
        }
    }


    private static Collection<Node> getNodesOrThrow(Node[] entries) {
        if (entries == null) {
            return null;
        }

        return Arrays.stream(entries)
                .filter(Objects::nonNull)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static Collection<Node> getNodesOrThrow(Collection<?> entries) {
        try {
            if (entries == null) {
                return null;
            }

            var results = (Collection<Node>) entries;
            if (results.isEmpty()) {
                return null;
            }

            return results.stream()
                    .filter(Objects::nonNull)
                    .toList();
        } catch (ClassCastException exception) {
            throw new IllegalArgumentException("Unexpected payload type: expected nodes collection", exception);
        }
    }

    /**
     * Returns the content of this object as string
     *
     * @return an optional
     */
    public Optional<String> contentAsString() {
        if (content instanceof String string) {
            return Optional.of(string);
        } else if (content instanceof byte[] bytes) {
            return Optional.of(new String(bytes, StandardCharsets.UTF_8));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns the content of this object as bytes
     *
     * @return an optional
     */
    public Optional<byte[]> contentAsBytes() {
        return content instanceof byte[] bytes ? Optional.of(bytes) : Optional.empty();
    }

    /**
     * Returns the content of this object as a long
     *
     * @return an optional
     */
    @SuppressWarnings("unused")
    public OptionalLong contentAsLong() {
        return content instanceof Number number ? OptionalLong.of(number.longValue()) : OptionalLong.empty();
    }

    /**
     * Returns the content of this object as a double
     *
     * @return an optional
     */
    @SuppressWarnings("unused")
    public OptionalDouble contentAsDouble() {
        return content instanceof Number number ? OptionalDouble.of(number.doubleValue()) : OptionalDouble.empty();
    }

    /**
     * Returns the content of this object as a double
     *
     * @return an optional
     */
    @SuppressWarnings("unused")
    public Optional<Boolean> contentAsBoolean() {
        if (content instanceof String string) {
            return Optional.of(Boolean.parseBoolean(string.toLowerCase(Locale.ROOT)));
        } else if (content instanceof byte[] bytes) {
            return Optional.of(Boolean.parseBoolean(new String(bytes, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT)));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Checks whether the child node with the given description exists
     *
     * @return true if a child node with the given description exists
     */
    public boolean hasNode(String description) {
        return children().stream().anyMatch(node -> Objects.equals(node.description(), description));
    }

    /**
     * Returns a non-null list of children of this node
     *
     * @return a non-null list
     */
    public LinkedList<Node> children() {
        if (content == null) {
            return new LinkedList<>();
        }
        if (!(content instanceof Collection<?> collection)) {
            return new LinkedList<>();
        }
        return collection.stream()
                .filter(entry -> entry instanceof Node)
                .map(entry -> (Node) entry)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Checks whether this node's description is equal to the one provided
     *
     * @param description the non-null description to check against
     * @return a boolean
     */
    public boolean hasDescription(String description) {
        return Objects.equals(description(), description);
    }

    /**
     * Finds the first child node
     *
     * @return an optional
     */
    public Optional<Node> findChild() {
        return children().stream().findFirst();
    }

    /**
     * Returns the first node that matches the description provided
     *
     * @param description the nullable description
     * @return an optional
     */
    public Optional<Node> findChild(String description) {
        return children().stream().filter(node -> Objects.equals(node.description(), description)).findFirst();
    }

    /**
     * Returns all the nodes that match the description provided
     *
     * @param description the nullable description
     * @return a list
     */
    public List<Node> listChildren(String description) {
        return streamChildren(description)
                .toList();
    }

    /**
     * Returns all the nodes that match the description provided
     *
     * @param description the nullable description
     * @return a stream
     */
    public Stream<Node> streamChildren(String description) {
        return children()
                .stream()
                .filter(node -> Objects.equals(node.description(), description));
    }

    /**
     * Returns the size of this object
     *
     * @return an unsigned int
     */
    public int size() {
        var descriptionSize = 1;
        var attributesSize = 2 * attributes.toMap().size();
        var contentSize = hasContent() ? 1 : 0;
        return descriptionSize + attributesSize + contentSize;
    }

    /**
     * Returns whether this object's content is non-null
     *
     * @return true if this object has a content
     */
    public boolean hasContent() {
        return Objects.nonNull(content);
    }

    /**
     * Returns the nullable id of this node
     *
     * @return a nullable String
     */
    public String id() {
        return attributes.getString("id", null);
    }

    /**
     * Checks if this object is equal to another
     *
     * @param other the reference object with which to compare
     * @return whether {@code other} is equal to this object
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof Node that
                && Objects.equals(this.description(), that.description())
                && Objects.equals(this.attributes(), that.attributes())
                && (Objects.equals(this.content(), that.content()) || this.content() instanceof byte[] theseBytes && that.content() instanceof byte[] thoseBytes && Arrays.equals(theseBytes, thoseBytes));
    }

    /**
     * Converts this node into a String
     *
     * @return a non null String
     */
    @Override
    public String toString() {
        var description = this.description.isBlank() ? "" : "description=%s".formatted(this.description);
        var attributes = this.attributes.toMap().isEmpty() ? "" : ", attributes=%s".formatted(this.attributes.toMap());
        var content = this.content == null ? "" : ", content=%s".formatted(contentToString());
        return "Node[%s%s%s]".formatted(description, attributes, content);
    }

    private Object contentToString() {
        if (!(this.content instanceof byte[] bytes)) {
            return this.content;
        }

        return hasDescription("result") || hasDescription("query") || hasDescription("body")
                ? new String(bytes) : Arrays.toString(bytes);
    }

    /**
     * Converts this node into a JSON String
     *
     * @return a non null String
     */
    public String toJson() {
        return Json.writeValueAsString(this, true);
    }
}
