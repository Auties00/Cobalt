package com.github.auties00.cobalt.node;

import com.github.auties00.cobalt.exception.MalformedJidException;
import com.github.auties00.cobalt.model.jid.Jid;
import it.auties.protobuf.model.ProtobufString;

import java.io.*;
import java.util.*;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * A sealed interface representing a node in the WhatsApp protocol communication structure.
 * Nodes are the fundamental building blocks of WhatsApp's binary XML-like protocol, where each node
 * consists of a description (tag name), attributes, and optional content.
 *
 * <p>This interface provides various implementations for different content types:
 * <ul>
 *   <li>{@link EmptyNode} - A node without any content</li>
 *   <li>{@link TextNode} - A node containing text content</li>
 *   <li>{@link JidNode} - A node containing a WhatsApp JID reference</li>
 *   <li>{@link BytesNode} - A node containing binary data</li>
 *   <li>{@link ContainerNode} - A node containing child nodes</li>
 * </ul>
 *
 * <p>Nodes are typically used for encoding and decoding WhatsApp protocol messages,
 * which follow a structured format similar to XML but transmitted in a binary format
 * for efficiency.
 *
 * @see NodeBuilder
 * @see NodeEncoder
 * @see NodeDecoder
 */
public sealed interface Node {
    /**
     * Creates and returns an empty node with no description, attributes, or content.
     * This is useful as a placeholder or default value in the WhatsApp protocol.
     *
     * @return an empty node instance
     */
    static Node empty() {
        return EmptyNode.DEFAULT;
    }

    /**
     * Returns the description (tag name) of this node.
     * The description typically identifies the type or purpose of the node in the protocol.
     *
     * @return the node's description as a string
     */
    String description();

    /**
     * Checks if this node's description matches the specified description.
     *
     * @param description the description to compare against
     * @return {@code true} if the descriptions match, {@code false} otherwise
     */
    default boolean hasDescription(String description) {
        return Objects.equals(description(), description);
    }

    /**
     * Returns the attributes of this node as an unmodifiable sequenced map.
     * Attributes provide metadata about the node, similar to XML attributes.
     *
     * @return a sequenced map of attribute names to attribute values
     */
    SequencedMap<String, NodeAttribute> attributes();

    /**
     * Retrieves an optional attribute by key.
     *
     * @param key the attribute key to look up
     * @return an {@link Optional} containing the attribute if present, or empty if not found
     */
    default Optional<NodeAttribute> getAttribute(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return Optional.ofNullable(attributes().get(key));
    }

    default Optional<String> getAttributeAsString(String key) {
        return getAttribute(key)
                .map(NodeAttribute::toValueString);
    }

    default Optional<Boolean> getAttributeAsBool(String key) {
        return getAttribute(key)
                .map(NodeAttribute::toValueString)
                .map(Boolean::parseBoolean);
    }

    default String getAttributeAsString(String key, String defaultValue) {
        return getAttributeAsString(key)
                .orElse(defaultValue);
    }

    default boolean getAttributeAsBool(String key, boolean defaultValue) {
        return getAttributeAsBool(key)
                .orElse(defaultValue);
    }

    default Optional<byte[]> getAttributeAsBytes(String key) {
        return getAttribute(key)
                .map(NodeAttribute::toValueBytes);
    }

    default byte[] getAttributeAsBytes(String key, byte[] defaultValue) {
        return getAttribute(key)
                .map(NodeAttribute::toValueBytes)
                .orElse(defaultValue);
    }

    default Optional<Jid> getAttributeAsJid(String key) {
        return getAttribute(key)
                .flatMap(NodeAttribute::toValueJid);
    }

    default Jid getAttributeAsJid(String key, Jid defaultValue) {
        return getAttribute(key)
                .flatMap(NodeAttribute::toValueJid)
                .orElse(defaultValue);
    }

    default OptionalLong getAttributeAsLong(String key) {
        var result = getAttribute(key);
        return result.isEmpty() ? OptionalLong.empty() : result.get().toValueLong();
    }

    default long getAttributeAsLong(String key, long defaultValue) {
        var result = getAttribute(key);
        return result.isEmpty() ? defaultValue : result.get().toValueLong().orElse(defaultValue);
    }

    default OptionalDouble getAttributeAsDouble(String key) {
        var result = getAttribute(key);
        return result.isEmpty() ? OptionalDouble.empty() : result.get().toValueDouble();
    }

    default double getAttributeAsDouble(String key, double defaultValue) {
        var result = getAttribute(key);
        return result.isEmpty() ? defaultValue : result.get().toValueDouble().orElse(defaultValue);
    }

    /**
     * Retrieves an optional attribute by key.
     *
     * @param key the attribute key to look up
     * @return an {@link Optional} containing the attribute if present, or empty if not found
     */
    default Stream<NodeAttribute> streamAttribute(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return Stream.ofNullable(attributes().get(key));
    }

    default Stream<String> streamAttributeAsString(String key) {
        return streamAttribute(key)
                .map(NodeAttribute::toValueString);
    }

    default Stream<Boolean> streamAttributeAsBool(String key) {
        return streamAttribute(key)
                .map(NodeAttribute::toValueString)
                .map(Boolean::parseBoolean);
    }

    default Stream<byte[]> streamAttributeAsBytes(String key) {
        return streamAttribute(key)
                .map(NodeAttribute::toValueBytes);
    }

    default Stream<Jid> streamAttributeAsJid(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        var attributeValue = attributes().get(key);
        return attributeValue != null
                ? attributeValue.toValueJid().stream()
                : Stream.empty();
    }

    default LongStream streamAttributeAsLong(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        var attributeValue = attributes().get(key);
        return attributeValue != null
                ? attributeValue.toValueLong().stream()
                : LongStream.empty();
    }

    default DoubleStream streamAttributeAsDouble(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        var attributeValue = attributes().get(key);
        return attributeValue != null
                ? attributeValue.toValueDouble().stream()
                : DoubleStream.empty();
    }

    /**
     * Retrieves a required attribute by key.
     *
     * @param key the attribute key to look up
     * @return the attribute value
     * @throws NoSuchElementException if the attribute is not present
     */
    default NodeAttribute getRequiredAttribute(String key) {
        var result = attributes().get(key);
        if(result == null) {
            throw new NoSuchElementException("No attribute with key " + key + " found in node " + description() + " with attributes " + attributes());
        }

        return result;
    }

    default String getRequiredAttributeAsString(String key) {
        return getRequiredAttribute(key)
                .toValueString();
    }

    default boolean getRequiredAttributeAsBool(String key) {
        var result = getRequiredAttribute(key)
                .toValueString();
        return Boolean.parseBoolean(result);
    }

    default byte[] getRequiredAttributeAsBytes(String key) {
        return getRequiredAttribute(key)
                .toValueBytes();
    }

    default Jid getRequiredAttributeAsJid(String key) {
        var requiredAttribute = getRequiredAttribute(key);
        return requiredAttribute
                .toValueJid()
                .orElseThrow(() -> new IllegalArgumentException("Cannot convert required attribute " + key + " to JID. Attribute value: " + requiredAttribute));
    }

    default long getRequiredAttributeAsLong(String key) {
        var requiredAttribute = getRequiredAttribute(key);
        return requiredAttribute
                .toValueLong()
                .orElseThrow(() -> new IllegalArgumentException("Cannot convert required attribute " + key + " to long. Attribute value: " + requiredAttribute));
    }

    default double getRequiredAttributeAsDouble(String key) {
        var requiredAttribute = getRequiredAttribute(key);
        return requiredAttribute
                .toValueDouble()
                .orElseThrow(() -> new IllegalArgumentException("Cannot convert required attribute " + key + " to double. Attribute value: " + requiredAttribute));
    }

    default boolean hasAttribute(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return attributes().containsKey(key);
    }

    default boolean hasAttribute(String key, String value) {
        Objects.requireNonNull(key, "key cannot be null");
        var attribute = attributes().get(key);
        return attribute != null
                && attribute.toValueString().equals(value);
    }

    default boolean hasAttribute(String key, byte[] value) {
        Objects.requireNonNull(key, "key cannot be null");
        var attribute = attributes().get(key);
        return attribute != null
               && Arrays.equals(attribute.toValueBytes(), value);
    }

    default boolean hasAttribute(String key, Jid value) {
        Objects.requireNonNull(key, "key cannot be null");
        var attribute = attributes().get(key);
        if(attribute == null) {
            return false;
        }

        var attributeValue = attribute.toValueJid();
        return attributeValue.isPresent()
               && Objects.equals(attributeValue.get(), value);
    }

    default boolean hasAttribute(String key, long value) {
        Objects.requireNonNull(key, "key cannot be null");
        var attribute = attributes().get(key);
        if(attribute == null) {
            return false;
        }

        var attributeValue = attribute.toValueLong();
        return attributeValue.isPresent()
                && attributeValue.getAsLong() == value;
    }

    default boolean hasAttribute(String key, double value) {
        Objects.requireNonNull(key, "key cannot be null");
        var attribute = attributes().get(key);
        if(attribute == null) {
            return false;
        }

        var attributeValue = attribute.toValueDouble();
        return attributeValue.isPresent()
                && attributeValue.getAsDouble() == value;
    }

    default boolean hasAttribute(String key, boolean value) {
        Objects.requireNonNull(key, "key cannot be null");
        var attribute = attributes().get(key);
        if(attribute == null) {
            return false;
        }

        var attributeValue = attribute.toValueString();
        return Boolean.parseBoolean(attributeValue) == value;
    }

    /**
     * Checks whether this node has content.
     * Empty nodes return {@code false}, while all other node types return {@code true}.
     *
     * @return {@code true} if the node has content, {@code false} otherwise
     */
    boolean hasContent();
    
    boolean hasContent(String content);
    
    boolean hasContent(Jid content);
    
    boolean hasContent(byte[] content);

    boolean hasContent(InputStream content) throws IOException;

    /**
     * Calculates the size of the node based on its attributes and whether it contains content.
     * The size is computed as:
     * <ul>
     *   <li>one unit for the description</li>
     *   <li>two units for each attribute (key and value)</li>
     *   <li>one unit if the node contains content/li>
     * </ul>
     *
     * @return the calculated size of the node
     */
    default int size() {
        return 1 // Description
                + (attributes().size() * 2) // Attributes
                + (hasContent() ? 1 : 0); // Content
    }

    /**
     * Converts the content of this node to a buffer, if possible.
     *
     * @return an {@code Optional} containing the content a buffer if possible, otherwise an empty {@code Optional}
     */
    Optional<byte[]> toContentBytes();

    default Stream<byte[]> streamContentBytes() {
        return toContentBytes()
                .stream();
    }

    /**
     * Converts the content of this node to an InputStream, if possible.
     *
     * @return an {@code Optional} containing the content an InputStream if possible, otherwise an empty {@code Optional}
     */
    Optional<InputStream> toContentStream();

    default Stream<InputStream> streamContentStream() {
        return toContentStream()
                .stream();
    }

    /**
     * Converts the content of this node to a string, if possible.
     *
     * @return an {@code Optional} containing the content as a string if possible, otherwise an empty {@code Optional}
     */
    Optional<String> toContentString();

    default Stream<String> streamContentString() {
        return toContentString()
                .stream();
    }

    default Optional<Boolean> toContentBool() {
        return toContentString()
                .map(Boolean::parseBoolean);
    }

    default Stream<Boolean> streamContentBool() {
        return toContentBool()
                .stream();
    }

    /**
     * Converts the content of this node to a jid, if possible.
     *
     * @return an {@code Optional} containing the content as a jid if possible, otherwise an empty {@code Optional}
     */
    Optional<Jid> toContentJid();

    default Stream<Jid> streamContentJid() {
        return toContentJid()
                .stream();
    }

    /**
     * Retrieves the child nodes contained in this container node, if any.
     *
     * @return a sequenced collection of child nodes contained in this container node
     */
    SequencedCollection<Node> children();

    default Stream<Node> streamChildren() {
        return children()
                .stream();
    }

    /**
     * Retrieves the first child Node in this container, if present.
     *
     * @return an {@code Optional} containing the first child Node if it exists,
     *         otherwise an empty {@code Optional}.
     */
    default Optional<Node> getChild() {
        var children = children();
        return children.isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(children.getFirst());
    }

    /**
     * Retrieves the first child Node in this container, if present.
     *
     * @return a {@code Stream} containing the first child Node if it exists,
     *         otherwise an empty {@code Stream}.
     */
    default Stream<Node> streamChild() {
        var children = children();
        return children.isEmpty()
                ? Stream.empty()
                : Stream.of(children.getFirst());
    }

    /**
     * Finds a child node by its description within the current container node.
     * If no child node with the specified description exists, an empty {@code Optional} is returned.
     *
     * @param description the description of the child node to find; cannot be null
     * @return an {@code Optional} containing the child node if one is found
     *         with the specified description, otherwise an empty {@code Optional}
     * @throws NullPointerException if the given description is null
     */
    default Optional<Node> getChild(String description) {
        Objects.requireNonNull(description, "description cannot be null");
        return streamChildren(description)
                .findFirst();
    }

    /**
     * Finds a child node by its description within the current container node.
     * If no child node with the specified description exists, an {@code IllegalArgumentException} is thrown.
     *
     * @param description the description of the child node to find; cannot be null
     * @return the first child node with the specified description
     * @throws NullPointerException if the given description is null
     * @throws IllegalArgumentException if no child node with the specified description exists
     */
    default Node getRequiredChild(String description) {
        Objects.requireNonNull(description, "description cannot be null");
        return streamChildren(description)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No child node found with description: " + description));
    }

    /**
     * Finds a child node by its description within the current container node.
     * If no child node with the specified description exists, an empty {@code Stream} is returned.
     *
     * @param description the description of the child node to find; cannot be null
     * @return a {@code Stream} containing the child node if one is found
     *         with the specified description, otherwise an empty {@code Stream}
     * @throws NullPointerException if the given description is null
     */
    default Stream<Node> streamChild(String description) {
        Objects.requireNonNull(description, "description cannot be null");
        return streamChildren(description)
                .findFirst()
                .stream();
    }

    /**
     * Finds all content nodes by their descriptions within the current container node.
     * If no child node with the specified description exists, an empty {@code SequencedCollection} is returned.
     *
     * @param description the description of the content nodes to find; cannot be null
     * @return a {@code SequencedCollection} containing the content nodes
     * @throws NullPointerException if the given description is null
     */
    default SequencedCollection<Node> getChildren(String description) {
        Objects.requireNonNull(description, "description cannot be null");
        return children()
                .stream()
                .filter(node -> node.hasDescription(description))
                .toList();
    }

    /**
     * Finds all content nodes by their descriptions within the current container node.
     * If no child node with the specified description exists, an empty {@code Stream} is returned.
     *
     * @param description the description of the content nodes to find; cannot be null
     * @return an {@code Stream} containing the content nodes
     * @throws NullPointerException if the given description is null
     */
    default Stream<Node> streamChildren(String description) {
        Objects.requireNonNull(description, "description cannot be null");
        return children()
                .stream()
                .filter(node -> node.hasDescription(description));
    }

    /**
     * Checks whether this node has a child node with the specified description.
     *
     * @param description the description of the child node to check for; cannot be null
     * @return {@code true} if a child node with the specified description exists, {@code false} otherwise
     */
    default boolean hasChild(String description) {
        Objects.requireNonNull(description, "description cannot be null");
        return children()
                .stream()
                .anyMatch(node -> node.hasDescription(description));
    }

    /**
     * Represents a node without any content (no content, text, binary data, or streams).
     *
     * @param description the node's description (tag name); cannot be null
     * @param attributes the node's attributes as an unmodifiable sequenced map; cannot be null
     * @see Node#empty()
     */
    record EmptyNode(String description, SequencedMap<String, NodeAttribute> attributes) implements Node {
        private static final EmptyNode DEFAULT = new EmptyNode("", new LinkedHashMap<>());

        public EmptyNode(String description, SequencedMap<String, NodeAttribute> attributes) {
            this.description = Objects.requireNonNull(description, "description cannot be null");
            this.attributes = Collections.unmodifiableSequencedMap(Objects.requireNonNull(attributes, "attributes cannot be null"));
        }

        @Override
        public boolean hasContent() {
            return false;
        }

        @Override
        public boolean hasContent(String content) {
            return content == null || content.isEmpty();
        }

        @Override
        public boolean hasContent(Jid content) {
            // A Jid is never empty because of the server attribute
            return content == null;
        }

        @Override
        public boolean hasContent(byte[] content) {
            return content == null || content.length == 0;
        }

        @Override
        public boolean hasContent(InputStream content) throws IOException {
            return content == null || content.read() == -1;
        }

        @Override
        public Optional<String> toContentString() {
            return Optional.empty();
        }

        @Override
        public Optional<Jid> toContentJid() {
            return Optional.empty();
        }

        @Override
        public SequencedCollection<Node> children() {
            return List.of();
        }

        @Override
        public Optional<byte[]> toContentBytes() {
            return Optional.empty();
        }

        @Override
        public Optional<InputStream> toContentStream() {
            return Optional.empty();
        }

        @Override
        public boolean equals(Object o) {
            return switch (o) {
                case EmptyNode(var thatDescription, var thatAttributes) -> Objects.equals(description, thatDescription) && Objects.equals(attributes, thatAttributes);
                case TextNode(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription) && Objects.equals(attributes, thatAttributes) && hasContent(thatContent);
                case BytesNode(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription) && Objects.equals(attributes, thatAttributes) && hasContent(thatContent);
                case null, default -> false;
            };
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(description, attributes);
        }

        @Override
        public String toString() {
            var result = new StringBuilder();
            result.append("Node[description=");
            result.append(description);

            if(!attributes.isEmpty()) {
                result.append(", attributes=");
                result.append(attributes);
            }

            result.append("]");

            return result.toString();
        }
    }

    /**
     * Represents a node containing text content as a string.
     *
     * @param description the node's description (tag name); cannot be null
     * @param attributes the node's attributes as an unmodifiable sequenced map; cannot be null
     * @param content the text content of the node; cannot be null but may be empty
     */
    record TextNode(String description, SequencedMap<String, NodeAttribute> attributes, String content) implements Node {
        /**
         * Constructs a TextNode with the specified description, attributes, and text content.
         *
         * @param description the node's description; cannot be null
         * @param attributes the node's attributes; cannot be null
         * @param content the text content; cannot be null
         * @throws NullPointerException if any parameter is null
         */
        public TextNode(String description, SequencedMap<String, NodeAttribute> attributes, String content) {
            this.description = Objects.requireNonNull(description, "description cannot be null");
            this.attributes = Collections.unmodifiableSequencedMap(Objects.requireNonNull(attributes, "attributes cannot be null"));
            this.content = Objects.requireNonNull(content, "content cannot be null");
        }

        @Override
        public boolean hasContent() {
            return !content.isEmpty();
        }

        @Override
        public boolean hasContent(String content) {
            return content != null && Objects.equals(this.content, content);
        }

        @Override
        public boolean hasContent(Jid content) {
            if(content == null) {
                return false;
            }

            try {
                var jid = Jid.of(this.content);
                return Objects.equals(jid, content);
            } catch (MalformedJidException e) {
                return false;
            }
        }

        @Override
        public boolean hasContent(byte[] content) {
            return content != null && Objects.equals(this.content, new String(content));
        }

        @Override
        public boolean hasContent(InputStream content) throws IOException {
            if(content == null) {
                return this.content.isEmpty();
            }

            var bytes = content.readAllBytes();
            return Objects.equals(this.content, new String(bytes));
        }

        @Override
        public Optional<byte[]> toContentBytes() {
            return Optional.of(content.getBytes());
        }

        @Override
        public Optional<InputStream> toContentStream() {
            return Optional.of(new ByteArrayInputStream(content.getBytes()));
        }

        @Override
        public Optional<Jid> toContentJid() {
            try {
                var result = Jid.of(content);
                return Optional.of(result);
            }catch (MalformedJidException exception) {
                return Optional.empty();
            }
        }

        @Override
        public Optional<String> toContentString() {
            return Optional.of(content);
        }

        @Override
        public SequencedCollection<Node> children() {
            return List.of();
        }

        @Override
        public boolean equals(Object o) {
            return switch (o) {
                case EmptyNode(var thatDescription, var thatAttributes) -> Objects.equals(description, thatDescription) 
                        && Objects.equals(attributes, thatAttributes) 
                        && content.isEmpty();
                case TextNode(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription) 
                        && Objects.equals(attributes, thatAttributes) 
                        && hasContent(thatContent);
                case JidNode(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription) 
                        && Objects.equals(attributes, thatAttributes) 
                        && hasContent(thatContent);
                case BytesNode(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription) 
                        && Objects.equals(attributes, thatAttributes) 
                        && hasContent(thatContent);
                case null, default -> false;
            };
        }

        @Override
        public int hashCode() {
            return Objects.hash(description, attributes, content);
        }

        @Override
        public String toString() {
            var result = new StringBuilder();
            result.append("Node[description=");
            result.append(description());

            if(!attributes.isEmpty()) {
                result.append(", attributes=");
                result.append(attributes);
            }

            if(content != null) {
                result.append(", content=");
                result.append(content);
            }

            result.append("]");

            return result.toString();
        }
    }

    /**
     * Represents a node containing a WhatsApp JID.
     *
     * @param description the node's description (tag name); cannot be null
     * @param attributes the node's attributes as an unmodifiable sequenced map; cannot be null
     * @param content the JID content of the node; cannot be null
     */
    record JidNode(String description, SequencedMap<String, NodeAttribute> attributes, Jid content) implements Node {
        public JidNode(String description, SequencedMap<String, NodeAttribute> attributes, Jid content) {
            this.description = Objects.requireNonNull(description, "description cannot be null");
            this.attributes = Collections.unmodifiableSequencedMap(Objects.requireNonNull(attributes, "attributes cannot be null"));
            this.content = Objects.requireNonNull(content, "content cannot be null");
        }

        @Override
        public boolean hasContent() {
            return true;
        }

        @Override
        public boolean hasContent(String content) {
            if(content == null) {
                return false;
            }
            return Objects.equals(this.content.toString(), content);
        }

        @Override
        public boolean hasContent(Jid content) {
            return Objects.equals(this.content, content);
        }

        @Override
        public boolean hasContent(byte[] content) {
            if(content == null) {
                return false;
            }
            return Objects.equals(this.content.toString(), new String(content));
        }

        @Override
        public boolean hasContent(InputStream content) throws IOException {
            if(content == null) {
                return false;
            }

            var bytes = content.readAllBytes();
            return Objects.equals(this.content.toString(), new String(bytes));
        }

        @Override
        public Optional<String> toContentString() {
            return Optional.of(content.toString());
        }

        @Override
        public Optional<Jid> toContentJid() {
            return Optional.of(content);
        }

        @Override
        public Optional<byte[]> toContentBytes() {
            return Optional.of(content.toString().getBytes());
        }

        @Override
        public Optional<InputStream> toContentStream() {
            return Optional.of(new ByteArrayInputStream(content.toString().getBytes()));
        }

        @Override
        public SequencedCollection<Node> children() {
            return List.of();
        }

        @Override
        public boolean equals(Object o) {
            return switch (o) {
                case TextNode(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription) 
                        && Objects.equals(attributes, thatAttributes) 
                        && hasContent(thatContent);
                case JidNode(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription) 
                        && Objects.equals(attributes, thatAttributes) 
                        && hasContent(thatContent);
                case BytesNode(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription) 
                        && Objects.equals(attributes, thatAttributes) 
                        && hasContent(thatContent);
                case null, default -> false;
            };
        }

        @Override
        public int hashCode() {
            return Objects.hash(description, attributes, content);
        }

        @Override
        public String toString() {
            var result = new StringBuilder();
            result.append("Node[description=");
            result.append(description());

            if(!attributes.isEmpty()) {
                result.append(", attributes=");
                result.append(attributes);
            }

            if(content != null) {
                result.append(", content=");
                result.append(content);
            }

            result.append("]");

            return result.toString();
        }
    }

    /**
     * Represents a node containing binary data as a byte array.
     *
     * @param description the node's description (tag name); cannot be null
     * @param attributes the node's attributes as an unmodifiable sequenced map; cannot be null
     * @param content the binary content of the node as a byte array; cannot be null but may be empty
     */
    record BytesNode(String description, SequencedMap<String, NodeAttribute> attributes, byte[] content) implements Node {
        public BytesNode(String description, SequencedMap<String, NodeAttribute> attributes, byte[] content) {
            this.description = Objects.requireNonNull(description, "description cannot be null");
            this.attributes = Collections.unmodifiableSequencedMap(Objects.requireNonNull(attributes, "attributes cannot be null"));
            this.content = Objects.requireNonNull(content, "content cannot be null");
        }

        @Override
        public boolean hasContent() {
            return content.length != 0;
        }

        @Override
        public boolean hasContent(String content) {
            return content != null && Objects.equals(new String(this.content), content);
        }

        @Override
        public boolean hasContent(Jid content) {
            if(content == null) {
                return false;
            }
            try {
                var jid = Jid.of(ProtobufString.lazy(this.content));
                return Objects.equals(jid, content);
            } catch (MalformedJidException e) {
                return false;
            }
        }

        @Override
        public boolean hasContent(byte[] content) {
            return content != null && Arrays.equals(this.content, content);
        }

        @Override
        public boolean hasContent(InputStream content) throws IOException {
            var thisPosition = 0;
            var thisLength = this.content.length;
            int thatRead;
            while (thisPosition < thisLength && (thatRead = content.read()) != -1) {
                if(this.content[thisPosition++] != thatRead) {
                    return false;
                }
            }
            return thisPosition == thisLength;
        }

        @Override
        public Optional<byte[]> toContentBytes() {
            return Optional.of(content);
        }

        @Override
        public Optional<InputStream> toContentStream() {
            return Optional.of(new ByteArrayInputStream(content));
        }

        @Override
        public Optional<Jid> toContentJid() {
            try {
                var result = Jid.of(ProtobufString.lazy(content));
                return Optional.of(result);
            } catch (MalformedJidException exception) {
                return Optional.empty();
            }
        }

        @Override
        public Optional<String> toContentString() {
            var decoded = new String(content);
            return Optional.of(decoded);
        }

        @Override
        public SequencedCollection<Node> children() {
            return List.of();
        }

        @Override
        public boolean equals(Object o) {
            return switch (o) {
                case TextNode(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription) 
                        && Objects.equals(attributes, thatAttributes) 
                        && hasContent(thatContent);
                case JidNode(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription) 
                        && Objects.equals(attributes, thatAttributes) 
                        && hasContent(thatContent);
                case BytesNode(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription) 
                        && Objects.equals(attributes, thatAttributes) 
                        && hasContent(thatContent);
                case null, default -> false;
            };
        }

        @Override
        public int hashCode() {
            return Objects.hash(description, attributes, Arrays.hashCode(content));
        }

        @Override
        public String toString() {
            var result = new StringBuilder();
            result.append("Node[description=");
            result.append(description());

            if(!attributes.isEmpty()) {
                result.append(", attributes=");
                result.append(attributes);
            }

            if(content != null) {
                if(hasDescription("result") || hasDescription("query") || hasDescription("body")) {
                    result.append(", content=");
                    result.append(new String(content));
                }else {
                    result.append(", content=");
                    result.append(Arrays.toString(content));
                }
            }

            result.append("]");

            return result.toString();
        }
    }

    /**
     * Represents a node containing a collection of child nodes, creating a hierarchical structure.
     *
     * @param description the node's description (tag name); cannot be null
     * @param attributes the node's attributes as an unmodifiable sequenced map; cannot be null
     * @param content the child nodes contained in this node as an unmodifiable collection; cannot be null but may be empty
     */
    record ContainerNode(String description, SequencedMap<String, NodeAttribute> attributes, SequencedCollection<Node> content) implements Node {
        public ContainerNode(String description, SequencedMap<String, NodeAttribute> attributes, SequencedCollection<Node> content) {
            this.description = Objects.requireNonNull(description, "description cannot be null");
            this.attributes = Collections.unmodifiableSequencedMap(Objects.requireNonNull(attributes, "attributes cannot be null"));
            this.content = Collections.unmodifiableSequencedCollection(Objects.requireNonNull(content, "content cannot be null"));
        }

        @Override
        public SequencedCollection<Node> children() {
            return content;
        }

        @Override
        public boolean hasContent() {
            return !content.isEmpty();
        }

        @Override
        public boolean hasContent(String content) {
            return false;
        }

        @Override
        public boolean hasContent(Jid content) {
            return false;
        }

        @Override
        public boolean hasContent(byte[] content) {
            return false;
        }

        @Override
        public boolean hasContent(InputStream content) {
            return false;
        }

        @Override
        public Optional<byte[]> toContentBytes() {
            return Optional.empty();
        }

        @Override
        public Optional<String> toContentString() {
            return Optional.empty();
        }

        @Override
        public Optional<Jid> toContentJid() {
            return Optional.empty();
        }

        @Override
        public Optional<InputStream> toContentStream() {
            return Optional.empty();
        }

        @Override
        public boolean equals(Object o) {
            return switch (o) {
                case EmptyNode(var thatDescription, var thatAttributes) -> Objects.equals(description, thatDescription)
                                                                           && Objects.equals(attributes, thatAttributes)
                                                                           && content.isEmpty();
                case TextNode(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription)
                        && Objects.equals(attributes, thatAttributes)
                        && hasContent(thatContent);
                case JidNode(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription)
                        && Objects.equals(attributes, thatAttributes)
                        && hasContent(thatContent);
                case BytesNode(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription)
                        && Objects.equals(attributes, thatAttributes)
                        && hasContent(thatContent);
                case ContainerNode(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription)
                        && Objects.equals(attributes, thatAttributes)
                        && Objects.equals(content, thatContent);
                case null, default -> false;
            };
        }

        @Override
        public int hashCode() {
            return Objects.hash(description, attributes, content);
        }

        @Override
        public String toString() {
            var result = new StringBuilder();
            result.append("Node[description=");
            result.append(description());

            if(!attributes.isEmpty()) {
                result.append(", attributes=");
                result.append(attributes);
            }

            if(!content.isEmpty()) {
                result.append(", content=");
                result.append(content);
            }

            result.append("]");

            return result.toString();
        }
    }
}