package it.auties.whatsapp.exchange;

import it.auties.whatsapp.util.Attributes;
import it.auties.whatsapp.util.Nodes;
import it.auties.whatsapp.util.WhatsappUtils;
import lombok.NonNull;

import java.util.*;

/**
 * An immutable model class that represents the primary unit used by WhatsappWeb's WebSocket to communicate with the client.
 *
 * @param description a non-null String that describes the content of this node
 * @param attributes  a non-null Map of object map
 * @param content     a nullable object, usually a List of {@link Node}, a {@link String} or a {@link Number}
 */
public record Node(@NonNull String description,
                   @NonNull Attributes attributes, Object content) {
    /**
     * Constructs a Node that only provides a non-null tag
     *
     * @param description a non-null String that describes the data that this object holds
     */
    public static Node with(@NonNull String description){
        return new Node(description, Attributes.empty(), null);
    }

    /**
     * Constructs a Node that only provides a non-null tag
     *
     * @param description a non-null String that describes the data that this object holds
     * @param content     a nullable object, usually a List of {@link Node}, a {@link String} or a {@link Number}
     */
    public static Node with(@NonNull String description, Object content) {
        return new Node(description, Attributes.empty(), content);
    }

    /**
     * Constructs a Node that only provides a non-null tag
     *
     * @param description a non-null String that describes the data that this object holds
     * @param attributes  a non-null Map of object map
     * @param content     a nullable object, usually a List of {@link Node}, a {@link String} or a {@link Number}
     */
    public static Node with(@NonNull String description, @NonNull Map<String, Object> attributes, Object content) {
        return new Node(description,  Attributes.of(attributes), content);
    }

    /**
     * Constructs a Node that only provides a non-null tag
     *
     * @param description a non-null String that describes the data that this object holds
     * @param children    the non-null children of this node
     */
    public static Node withChildren(@NonNull String description, Node... children) {
        return new Node(description, Attributes.empty(), Nodes.orNull(Arrays.asList(children)));
    }

    /**
     * Constructs a Node that only provides a non-null tag
     *
     * @param description a non-null String that describes the data that this object holds
     * @param attributes  a non-null Map of object map
     * @param children    the non-null children of this node
     */
    public static Node withChildren(@NonNull String description, @NonNull Map<String, Object> attributes, Node... children) {
        return new Node(description, Attributes.of(attributes), Nodes.orNull(Arrays.asList(children)));
    }

    public byte[] bytes(){
        if(content instanceof byte[] bytes){
            return bytes;
        }

        var contentType = content == null ? null : content.getClass().getName();
        throw new UnsupportedOperationException("Unsupported content type: %s".formatted(contentType));
    }

    /**
     * Returns a list of child WhatsappNodes
     *
     * @return a non-null list containing WhatsappNodes extracted from this body's content
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
    public Node findNode(String description) {
        return children().stream()
                .filter(node -> Objects.equals(node.description(), description))
                .findFirst()
                .orElse(null);
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
     * Constructs a new request from this node
     *
     * @return a non null request
     */
    public Request toRequest(){
        return toRequest(true);
    }

    /**
     * Constructs a new request from this node
     *
     * @param needsId       whether an id attribute should be added if missing
     * @return a non null request
     */
    public Request toRequest(boolean needsId){
        if (needsId && WhatsappUtils.readNullableId(this) == null) {
            attributes.map().put("id", WhatsappUtils.buildRequestTag());
        }

        return Request.with(this);
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
