package it.auties.whatsapp.model.request;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.auties.whatsapp.util.JacksonProvider;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;

/**
 * An immutable model class that represents the primary unit used by WhatsappWeb's WebSocket to
 * communicate with the client
 *
 * @param description a non-null String that describes the content of this node
 * @param attributes  a non-null Map that describes the metadata of this object
 * @param content     a nullable object: a List of {@link Node}, a {@link String} or a
 *                    {@link Number}
 */
public record Node(@NonNull String description, @NonNull Attributes attributes, Object content)
    implements JacksonProvider {

  /**
   * Constructs a Node that only provides a non-null tag
   *
   * @param description a non-null String that describes the data that this object holds
   * @return a new node with the above characteristics
   */
  public static Node of(@NonNull String description) {
    return new Node(description, Attributes.of(), null);
  }

  /**
   * Constructs a Node that provides a non-null tag and a nullable content
   *
   * @param description a non-null String that describes the data that this object holds
   * @param content     a nullable object, usually a List of {@link Node}, a {@link String} or a
   *                    {@link Number}
   * @return a new node with the above characteristics
   */
  public static Node of(@NonNull String description, Object content) {
    return new Node(description, Attributes.of(), content);
  }

  /**
   * Constructs a Node that provides a non-null tag, a non-null toMap of attributes and a nullable
   * content
   *
   * @param description a non-null String that describes the data that this object holds
   * @param attributes  a non-null Map that describes the metadata of this object
   * @param content     a nullable object, usually a List of {@link Node}, a {@link String} or a
   *                    {@link Number}
   * @return a new node with the above characteristics
   */
  public static Node of(@NonNull String description, @NonNull Map<String, Object> attributes,
      Object content) {
    return new Node(description, Attributes.ofNullable(attributes), content);
  }

  /**
   * Constructs a Node that provides a non-null tag and a non-null toMap of attributes
   *
   * @param description a non-null String that describes the data that this object holds
   * @param attributes  a non-null Map that describes the metadata of this object
   * @return a new node with the above characteristics
   */
  public static Node ofAttributes(@NonNull String description,
      @NonNull Map<String, Object> attributes) {
    return new Node(description, Attributes.ofNullable(attributes), null);
  }


  /**
   * Constructs a Node that provides a non-null tag and a nullable var-args of children
   *
   * @param description a non-null String that describes the data that this object holds
   * @param children    the nullable children of this node
   * @return a new node with the above characteristics
   */
  public static Node ofChildren(@NonNull String description, Node... children) {
    return ofChildren(description, Arrays.asList(children));
  }

  /**
   * Constructs a Node that provides a non-null tag and a nullable var-args of children
   *
   * @param description a non-null String that describes the data that this object holds
   * @param children    the nullable children of this node
   * @return a new node with the above characteristics
   */
  public static Node ofChildren(@NonNull String description, Collection<Node> children) {
    return new Node(description, Attributes.of(), requireNonNullNodes(children));
  }

  /**
   * Constructs a Node that provides a non-null tag, a non-null toMap of attributes and a nullable
   * var-args of children
   *
   * @param description a non-null String that describes the data that this object holds
   * @param attributes  a non-null Map that describes the metadata of this object
   * @param children    the nullable children of this node
   * @return a new node with the above characteristics
   */
  public static Node ofChildren(@NonNull String description,
      @NonNull Map<String, Object> attributes,
      Collection<Node> children) {
    return new Node(description, Attributes.ofNullable(attributes), requireNonNullNodes(children));
  }

  /**
   * Constructs a Node that provides a non-null tag, a non-null toMap of attributes and a nullable
   * var-args of children
   *
   * @param description a non-null String that describes the data that this object holds
   * @param attributes  a non-null Map that describes the metadata of this object
   * @param children    the nullable children of this node
   * @return a new node with the above characteristics
   */
  public static Node ofChildren(@NonNull String description,
      @NonNull Map<String, Object> attributes,
      Node... children) {
    return ofChildren(description, attributes, Arrays.asList(children));
  }

  private static List<Node> requireNonNullNodes(Collection<Node> nodes) {
    if (nodes == null) {
      return null;
    }
    var results = nodes.stream()
        .filter(Objects::nonNull)
        .toList();
    return results.isEmpty() ? null : results;
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
   * Returns the content of this object as string
   *
   * @return an optional
   */
  public Optional<String> contentAsString() {
    return Optional.ofNullable(switch (content) {
      case String string -> string;
      case byte[] bytes -> new String(bytes, StandardCharsets.UTF_8);
      case null, default -> null;
    });
  }

  /**
   * Returns the content of this object as bytes
   *
   * @return an optional
   */
  public Optional<byte[]> contentAsBytes() {
    return content instanceof byte[] bytes ?
        Optional.of(bytes) :
        Optional.empty();
  }

  /**
   * Returns the content of this object as a long
   *
   * @return an optional
   */
  public Optional<Long> contentAsLong() {
    return content instanceof Number number ?
        Optional.of(number.longValue()) :
        Optional.empty();
  }

  /**
   * Returns the content of this object as a double
   *
   * @return an optional
   */
  @SuppressWarnings("unused")
  public Optional<Double> contentAsDouble() {
    return content instanceof Number number ?
        Optional.of(number.doubleValue()) :
        Optional.empty();
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
   * Checks whether the child node with the given description exists
   *
   * @return true if a child node with the given description exists
   */
  public boolean hasNode(String description) {
    return children().stream()
        .anyMatch(node -> Objects.equals(node.description(), description));
  }

  /**
   * Checks whether this node's description is equal to the one provided
   *
   * @param description the non-null description to check against
   * @return a boolean
   */
  public boolean hasDescription(@NonNull String description) {
    return Objects.equals(description(), description);
  }

  /**
   * Finds the first child node
   *
   * @return an optional
   */
  public Optional<Node> findNode() {
    return children().stream()
        .findFirst();
  }

  /**
   * Returns the first node that matches the description provided
   *
   * @return an optional
   */
  public Optional<Node> findNode(String description) {
    return children().stream()
        .filter(node -> Objects.equals(node.description(), description))
        .findFirst();
  }

  /**
   * Returns all the nodes that match the description provided
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
  public boolean hasContent() {
    return Objects.nonNull(content);
  }

  /**
   * Returns the size of this object
   *
   * @return an unsigned int
   */
  public int size() {
    var descriptionSize = 1;
    var attributesSize = 2 * attributes.toMap()
        .size();
    var contentSize = hasContent() ?
        1 :
        0;
    return descriptionSize + attributesSize + contentSize;
  }

  /**
   * Constructs a new request from this node. If this node doesn't provide an jid, the one provided
   * as a parameter will be used.
   *
   * @param id the nullable jid of this request
   * @return a non null request
   * @throws NullPointerException if no valid jid can be found
   */
  public Request toRequest(String id, Function<Node, Boolean> filter) {
    if (id() == null) {
      attributes.toMap()
          .put("id", requireNonNull(id, "No valid jid can be used to create a request"));
    }
    return Request.of(this, filter);
  }

  /**
   * Checks if this object is equal to another
   *
   * @param other the reference object with which to compare
   * @return whether {@code other} is equal to this object
   */
  @Override
  public boolean equals(Object other) {
    return other instanceof Node that && Objects.equals(this.description(), that.description())
        && Objects.equals(
        this.attributes(), that.attributes()) && (Objects.equals(this.content(),
        that.content()) || this.content() instanceof byte[] theseBytes
        && that.content() instanceof byte[] thoseBytes && Arrays.equals(
        theseBytes, thoseBytes));
  }

  /**
   * Converts this node into a JSON String
   *
   * @return a non null String
   */
  public String toJson() {
    try {
      return JSON.writerWithDefaultPrettyPrinter()
          .writeValueAsString(this);
    } catch (JsonProcessingException exception) {
      throw new RuntimeException("Cannot convert node to json", exception);
    }
  }

  /**
   * Converts this node into a String
   *
   * @return a non null String
   */
  @Override
  public String toString() {
    var description = this.description.isBlank() || this.description.isEmpty() ?
        "" :
        "description=%s".formatted(this.description);
    var attributes = this.attributes.toMap()
        .isEmpty() ?
        "" :
        ", attributes=%s".formatted(this.attributes.toMap());
    var content = this.content == null ?
        "" :
        ", content=%s".formatted(this.content instanceof byte[] bytes ?
            Arrays.toString(bytes) :
            this.content);
    return "Node[%s%s%s]".formatted(description, attributes, content);
  }
}
