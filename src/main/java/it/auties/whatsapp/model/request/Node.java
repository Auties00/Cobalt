package it.auties.whatsapp.model.request;

import it.auties.whatsapp.util.Attributes;
import it.auties.whatsapp.util.Nodes;
import lombok.NonNull;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * An immutable model class that represents the primary unit used by WhatsappWeb's WebSocket to communicate with the client.
 *
 * @param description a non-null String that describes the content of this node
 * @param attributes  a non-null Map that describes the metadata of this object
 * @param content     a nullable object, usually a List of {@link Node}, a {@link String} or a {@link Number}
 */
public record Node(@NonNull String description,
                   @NonNull Attributes attributes, Object content) {
    /**
     * Constructs a Node that only provides a non-null tag
     *
     * @param description a non-null String that describes the data that this object holds
     * @return a new node with the above characteristics
     */
    public static Node with(@NonNull String description){
        return new Node(description, Attributes.empty(), null);
    }

    /**
     * Constructs a Node that provides a non-null tag and a nullable content
     *
     * @param description a non-null String that describes the data that this object holds
     * @param content     a nullable object, usually a List of {@link Node}, a {@link String} or a {@link Number}
     * @return a new node with the above characteristics
     */
    public static Node with(@NonNull String description, Object content) {
        return new Node(description, Attributes.empty(), content);
    }

    /**
     * Constructs a Node that provides a non-null tag, a non-null map of attributes and a nullable content
     *
     * @param description a non-null String that describes the data that this object holds
     * @param attributes  a non-null Map that describes the metadata of this object
     * @param content     a nullable object, usually a List of {@link Node}, a {@link String} or a {@link Number}
     * @return a new node with the above characteristics
     */
    public static Node with(@NonNull String description, @NonNull Map<String, Object> attributes, Object content) {
        return new Node(description,  Attributes.of(attributes), content);
    }

    /**
     * Constructs a Node that provides a non-null tag and a non-null map of attributes
     *
     * @param description a non-null String that describes the data that this object holds
     * @param attributes  a non-null Map that describes the metadata of this object
     * @return a new node with the above characteristics
     */
    public static Node withAttributes(@NonNull String description, @NonNull Map<String, Object> attributes) {
        return new Node(description, Attributes.of(attributes), null);
    }


    /**
     * Constructs a Node that provides a non-null tag and a nullable var-args of children
     *
     * @param description a non-null String that describes the data that this object holds
     * @param children    the nullable children of this node
     * @return a new node with the above characteristics
     */
    public static Node withChildren(@NonNull String description, Node... children) {
        return withChildren(description, Arrays.asList(children));
    }

    /**
     * Constructs a Node that provides a non-null tag and a nullable var-args of children
     *
     * @param description a non-null String that describes the data that this object holds
     * @param children    the nullable children of this node
     * @return a new node with the above characteristics
     */
    public static Node withChildren(@NonNull String description, Collection<Node> children) {
        return new Node(description, Attributes.empty(), Nodes.orNull(children));
    }


    /**
     * Constructs a Node that provides a non-null tag, a non-null map of attributes and a nullable var-args of children
     *
     * @param description a non-null String that describes the data that this object holds
     * @param attributes  a non-null Map that describes the metadata of this object
     * @param children    the nullable children of this node
     * @return a new node with the above characteristics
     */
    public static Node withChildren(@NonNull String description, @NonNull Map<String, Object> attributes, Collection<Node> children) {
        return new Node(description, Attributes.of(attributes), Nodes.orNull(children));
    }

    /**
     * Constructs a Node that provides a non-null tag, a non-null map of attributes and a nullable var-args of children
     *
     * @param description a non-null String that describes the data that this object holds
     * @param attributes  a non-null Map that describes the metadata of this object
     * @param children    the nullable children of this node
     * @return a new node with the above characteristics
     */
    public static Node withChildren(@NonNull String description, @NonNull Map<String, Object> attributes, Node... children) {
        return withChildren(description, attributes, Arrays.asList(children));
    }

    /**
     * Returns the nullable jid of this node
     *
     * @return a nullable String
     */
    public String id(){
        return attributes.getString("id", null);
    }

    /**
     * Returns the content of this object as bytes
     *
     * @throws UnsupportedOperationException if this node doesn't wrap an array of bytes
     * @return a non-null array of bytes
     */
    public byte[] bytes(){
        if(content instanceof byte[] bytes){
            return bytes;
        }

        throw new UnsupportedOperationException("Unsupported content type: %s"
                .formatted(content == null ? null : content.getClass().getName()));
    }

    /**
     * Returns a non-null list of children of this node
     *
     * @return a non-null list
     */
    public LinkedList<Node> children() {
        return Nodes.findAll(content);
    }

    /**
     * Checks whether the child node with the given description exists
     *
     * @return true if a child node with the given description exists
     */
    public boolean hasNode(String description) {
        return children().stream()
                .anyMatch(node -> Objects.equals(node.description(), description));
    }

    /**
     * Returns a body that matches the nullable description provided
     *
     * @return a nullable node
     */
    public Optional<Node> findNode(String description) {
        return children().stream()
                .filter(node -> Objects.equals(node.description(), description))
                .findFirst();
    }

    /**
     * Returns a body that matches the nullable description provided
     *
     * @return an optional body, present if a result was found
     */
    public List<Node> findNodes(String description) {
        return children().stream()
                .filter(node -> Objects.equals(node.description(), description))
                .toList();
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
        var attributesSize = 2 * attributes.map().size();
        var contentSize = hasContent() ? 1 : 0;
        return descriptionSize + attributesSize + contentSize;
    }

    /**
     * Constructs a new request from this node.
     * If this node doesn't provide an jid, the one provided as a parameter will be used.
     *
     * @param id the nullable jid of this request
     * @throws NullPointerException if no valid jid can be found
     * @return a non null request
     */
    public Request toRequest(String id){
        if (id() == null) {
            attributes.map().put("id", requireNonNull(id, "No valid jid can be used to create a request"));
        }

        return Request.with(this);
    }

    /**
     * Checks if this object is equal to another
     * @param other the reference object with which to compare
     *
     * @return whether {@code other} is equal to this object
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof Node that
                && Objects.equals(this.description(), that.description())
                && Objects.equals(this.attributes(), that.attributes())
                && (Objects.equals(this.content(), that.content())
                    || this.content() instanceof byte[] theseBytes
                        && that.content() instanceof byte[] thoseBytes
                        && Arrays.equals(theseBytes, thoseBytes));
    }

    /**
     * Converts this node into a String
     *
     * @return a non null String
     */
    @Override
    public String toString() {
        var description = this.description.isBlank() || this.description.isEmpty() ? ""
                : "description=%s".formatted(this.description);
        var attributes = this.attributes.map().isEmpty() ? ""
                : ", attributes=%s".formatted(this.attributes.map());
        var content = this.content == null ? ""
                : ", content=%s".formatted(this.content instanceof byte[] bytes ? Arrays.toString(bytes) : this.content);
        return "Node[%s%s%s]".formatted(description, attributes, content);
    }
}
