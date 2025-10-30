package com.github.auties00.cobalt.node;

import com.github.auties00.cobalt.exception.MalformedJidException;
import com.github.auties00.cobalt.model.jid.Jid;
import it.auties.protobuf.model.ProtobufString;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * A sealed interface representing a node in the WhatsApp protocol communication structure.
 * Nodes are the fundamental building blocks of WhatsApp's binary XML-like protocol, where each node
 * consists of a description (tag name), attributes, and optional children.
 *
 * <p>This interface provides various implementations for different content types:
 * <ul>
 *   <li>{@link EmptyNode} - A node without any children</li>
 *   <li>{@link TextNode} - A node containing text children</li>
 *   <li>{@link JidNode} - A node containing a WhatsApp JID reference</li>
 *   <li>{@link BytesContent} - A node containing binary data</li>
 *   <li>{@link StreamNode} - A node containing streaming data</li>
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
     * Creates and returns an empty node with no description, attributes, or children.
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
                .map(NodeAttribute::toString);
    }

    default Optional<Boolean> getAttributeAsBool(String key) {
        return getAttribute(key)
                .map(NodeAttribute::toString)
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
                .map(NodeAttribute::toBytes);
    }

    default byte[] getAttributeAsBytes(String key, byte[] defaultValue) {
        return getAttribute(key)
                .map(NodeAttribute::toBytes)
                .orElse(defaultValue);
    }

    default Optional<Jid> getAttributeAsJid(String key) {
        return getAttribute(key)
                .flatMap(NodeAttribute::toJid);
    }

    default Jid getAttributeAsJid(String key, Jid defaultValue) {
        return getAttribute(key)
                .flatMap(NodeAttribute::toJid)
                .orElse(defaultValue);
    }

    default OptionalLong getAttributeAsLong(String key) {
        var result = getAttribute(key);
        return result.isEmpty() ? OptionalLong.empty() : result.get().toLong();
    }

    default long getAttributeAsLong(String key, long defaultValue) {
        var result = getAttribute(key);
        return result.isEmpty() ? defaultValue : result.get().toLong().orElse(defaultValue);
    }

    default OptionalDouble getAttributeAsDouble(String key) {
        var result = getAttribute(key);
        return result.isEmpty() ? OptionalDouble.empty() : result.get().toDouble();
    }

    default double getAttributeAsDouble(String key, double defaultValue) {
        var result = getAttribute(key);
        return result.isEmpty() ? defaultValue : result.get().toDouble().orElse(defaultValue);
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
                .map(NodeAttribute::toString);
    }

    default Stream<Boolean> streamAttributeAsBool(String key) {
        return streamAttribute(key)
                .map(NodeAttribute::toString)
                .map(Boolean::parseBoolean);
    }

    default Stream<byte[]> streamAttributeAsBytes(String key) {
        return streamAttribute(key)
                .map(NodeAttribute::toBytes);
    }

    default Stream<Jid> streamAttributeAsJid(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        var attributeValue = attributes().get(key);
        return attributeValue != null
                ? attributeValue.toJid().stream()
                : Stream.empty();
    }

    default LongStream streamAttributeAsLong(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        var attributeValue = attributes().get(key);
        return attributeValue != null
                ? attributeValue.toLong().stream()
                : LongStream.empty();
    }

    default DoubleStream streamAttributeAsDouble(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        var attributeValue = attributes().get(key);
        return attributeValue != null
                ? attributeValue.toDouble().stream()
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
                .toString();
    }

    default boolean getRequiredAttributeAsBool(String key) {
        var result = getRequiredAttribute(key)
                .toString();
        return Boolean.parseBoolean(result);
    }

    default byte[] getRequiredAttributeAsBytes(String key) {
        return getRequiredAttribute(key)
                .toBytes();
    }

    default Jid getRequiredAttributeAsJid(String key) {
        var requiredAttribute = getRequiredAttribute(key);
        return requiredAttribute
                .toJid()
                .orElseThrow(() -> new IllegalArgumentException("Cannot convert required attribute " + key + " to JID. Attribute value: " + requiredAttribute));
    }

    default long getRequiredAttributeAsLong(String key) {
        var requiredAttribute = getRequiredAttribute(key);
        return requiredAttribute
                .toLong()
                .orElseThrow(() -> new IllegalArgumentException("Cannot convert required attribute " + key + " to long. Attribute value: " + requiredAttribute));
    }

    default double getRequiredAttributeAsDouble(String key) {
        var requiredAttribute = getRequiredAttribute(key);
        return requiredAttribute
                .toDouble()
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
                && attribute.toString().equals(value);
    }

    default boolean hasAttribute(String key, byte[] value) {
        Objects.requireNonNull(key, "key cannot be null");
        var attribute = attributes().get(key);
        return attribute != null
               && Arrays.equals(attribute.toBytes(), value);
    }

    default boolean hasAttribute(String key, Jid value) {
        Objects.requireNonNull(key, "key cannot be null");
        var attribute = attributes().get(key);
        if(attribute == null) {
            return false;
        }

        var attributeValue = attribute.toJid();
        return attributeValue.isPresent()
               && Objects.equals(attributeValue.get(), value);
    }

    default boolean hasAttribute(String key, long value) {
        Objects.requireNonNull(key, "key cannot be null");
        var attribute = attributes().get(key);
        if(attribute == null) {
            return false;
        }

        var attributeValue = attribute.toLong();
        return attributeValue.isPresent()
                && attributeValue.getAsLong() == value;
    }

    default boolean hasAttribute(String key, double value) {
        Objects.requireNonNull(key, "key cannot be null");
        var attribute = attributes().get(key);
        if(attribute == null) {
            return false;
        }

        var attributeValue = attribute.toDouble();
        return attributeValue.isPresent()
                && attributeValue.getAsDouble() == value;
    }

    default boolean hasAttribute(String key, boolean value) {
        Objects.requireNonNull(key, "key cannot be null");
        var attribute = attributes().get(key);
        if(attribute == null) {
            return false;
        }

        var attributeValue = attribute.toString();
        return Boolean.parseBoolean(attributeValue) == value;
    }

    /**
     * Checks whether this node has children.
     * Empty nodes return {@code false}, while all other node types return {@code true}.
     *
     * @return {@code true} if the node has children, {@code false} otherwise
     */
    boolean hasContent();

    /**
     * Calculates the size of the node based on its attributes and whether it contains children.
     * The size is computed as:
     * <ul>
     *   <li>one unit for the description</li>
     *   <li>two units for each attribute (key and value)</li>
     *   <li>one unit if the node contains children/li>
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
     * Finds all children nodes by their descriptions within the current container node.
     * If no child node with the specified description exists, an empty {@code SequencedCollection} is returned.
     *
     * @param description the description of the children nodes to find; cannot be null
     * @return a {@code SequencedCollection} containing the children nodes
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
     * Finds all children nodes by their descriptions within the current container node.
     * If no child node with the specified description exists, an empty {@code Stream} is returned.
     *
     * @param description the description of the children nodes to find; cannot be null
     * @return an {@code Stream} containing the children nodes
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
     * Represents a node without any children.
     * This is typically used for protocol messages that only need a description and attributes.
     *
     * @param description the node's description
     * @param attributes the node's attributes
     */
    record EmptyNode(String description, SequencedMap<String, NodeAttribute> attributes) implements Node {
        private static final EmptyNode DEFAULT = new EmptyNode("", new LinkedHashMap<>());

        public EmptyNode {
            Objects.requireNonNull(description, "description cannot be null");
            Objects.requireNonNull(attributes, "attributes cannot be null");
        }

        @Override
        public SequencedMap<String, NodeAttribute> attributes() {
            return Collections.unmodifiableSequencedMap(attributes);
        }

        @Override
        public boolean hasContent() {
            return false;
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
     * Represents a node containing text children.
     * This is commonly used for protocol messages that carry string data.
     *
     * @param description the node's description
     * @param attributes the node's attributes
     * @param content the text content of the node
     */
    record TextNode(String description, SequencedMap<String, NodeAttribute> attributes, String content) implements Node {
        public TextNode {
            Objects.requireNonNull(description, "description cannot be null");
            Objects.requireNonNull(attributes, "attributes cannot be null");
            Objects.requireNonNull(content, "content cannot be null");
        }

        @Override
        public SequencedMap<String, NodeAttribute> attributes() {
            return Collections.unmodifiableSequencedMap(attributes);
        }

        @Override
        public boolean hasContent() {
            return true;
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
     * JIDs are used to uniquely identify users, groups, and other entities in WhatsApp.
     *
     * @param description the node's description
     * @param attributes the node's attributes
     * @param content the JID content of the node
     */
    record JidNode(String description, SequencedMap<String, NodeAttribute> attributes, Jid content) implements Node {
        public JidNode {
            Objects.requireNonNull(description, "description cannot be null");
            Objects.requireNonNull(attributes, "attributes cannot be null");
            Objects.requireNonNull(content, "content cannot be null");
        }

        @Override
        public SequencedMap<String, NodeAttribute> attributes() {
            return Collections.unmodifiableSequencedMap(attributes);
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
        public boolean hasContent() {
            return true;
        }

        @Override
        public SequencedCollection<Node> children() {
            return List.of();
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
     * Represents a node containing binary data as a ByteBuffer.
     * This is typically used for protocol messages that carry raw binary data,
     * such as encrypted payloads or media thumbnails.
     *
     * @param description the node's description
     * @param attributes the node's attributes
     * @param content the binary content of the node as a ByteBuffer
     */
    record BytesContent(String description, SequencedMap<String, NodeAttribute> attributes, byte[] content) implements Node {
        public BytesContent {
            Objects.requireNonNull(description, "description cannot be null");
            Objects.requireNonNull(attributes, "attributes cannot be null");
            Objects.requireNonNull(content, "content cannot be null");
        }

        @Override
        public SequencedMap<String, NodeAttribute> attributes() {
            return Collections.unmodifiableSequencedMap(attributes);
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
        public boolean hasContent() {
            return true;
        }

        @Override
        public SequencedCollection<Node> children() {
            return List.of();
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
     * Represents a node containing streaming data.
     * This is used for protocol messages that carry large amounts of data
     * that are better processed as a stream rather than loaded entirely into memory.
     *
     * @param description the node's description
     * @param attributes the node's attributes
     * @param content the streaming children of the node as an InputStream
     * @param contentLength the full length of the children
     */
    record StreamNode(String description, SequencedMap<String, NodeAttribute> attributes, InputStream content, int contentLength) implements Node {
        public StreamNode(String description, SequencedMap<String, NodeAttribute> attributes, InputStream content, int contentLength) {
            Objects.requireNonNull(description, "description cannot be null");
            Objects.requireNonNull(attributes, "attributes cannot be null");
            Objects.requireNonNull(content, "content cannot be null");
            if(contentLength < 0) {
                throw new IllegalArgumentException("contentLength cannot be negative");
            }
            this.description = description;
            this.attributes = attributes;
            this.content = new BoundedInputStream(content, contentLength);
            this.contentLength = contentLength;
        }

        @Override
        public Optional<Jid> toContentJid() {
            try {
                var result = content.readNBytes(contentLength);
                var converted = Jid.of(ProtobufString.lazy(result));
                return Optional.of(converted);
            }catch (IOException | MalformedJidException exception) {
                return Optional.empty();
            }
        }

        @Override
        public Optional<byte[]> toContentBytes() {
            try {
                var result = content.readNBytes(contentLength);
                return Optional.of(result);
            }catch (IOException exception) {
                return Optional.empty();
            }
        }

        @Override
        public Optional<InputStream> toContentStream() {
            return Optional.of(content);
        }

        @Override
        public Optional<String> toContentString() {
            try {
                var result = content.readNBytes(contentLength);
                var converted = new String(result);
                return Optional.of(converted);
            }catch (IOException exception) {
                return Optional.empty();
            }
        }

        @Override
        public SequencedMap<String, NodeAttribute> attributes() {
            return Collections.unmodifiableSequencedMap(attributes);
        }

        @Override
        public boolean hasContent() {
            return true;
        }

        @Override
        public SequencedCollection<Node> children() {
            return List.of();
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
                result.append(", content=<stream with ")
                        .append(contentLength)
                        .append(" bytes>");
            }

            result.append("]");

            return result.toString();
        }

        private static final class BoundedInputStream extends InputStream {
            private final InputStream stream;
            private int remaining;
            private int mark;

            private BoundedInputStream(InputStream stream, int remaining) {
                this.stream = stream;
                this.remaining = remaining;
            }

            @Override
            public int read() throws IOException {
                if (remaining <= 0) {
                    return -1;
                }

                remaining--;
                var result = stream.read();
                if(result == -1) {
                    throw new IOException("Unexpected end of stream");
                }

                return result;
            }

            @Override
            public int read(byte[] b) throws IOException {
                return read(b, 0, b.length);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (remaining <= 0) {
                    return -1;
                }

                var result = stream.read(b, 0, Math.min(remaining, len));
                if(result == -1) {
                    throw new IOException("Unexpected end of stream");
                }
                remaining -= result;
                return result;
            }

            @Override
            public byte[] readAllBytes() throws IOException {
                var len = remaining;
                var alloc = new byte[len];
                if(readBytes(alloc, 0, len) != len) {
                    throw new IOException("Unexpected end of stream");
                }
                return alloc;
            }

            @Override
            public byte[] readNBytes(int len) throws IOException {
                if(len >= remaining) {
                    return readAllBytes();
                }else {
                    var alloc = new byte[len];
                    readBytes(alloc, 0, len);
                    return alloc;
                }
            }

            @Override
            public int readNBytes(byte[] b, int off, int len) throws IOException {
                return readBytes(b, off, Math.min(remaining, len));
            }

            private int readBytes(byte[] result, int offset, int len) throws IOException {
                var totalRead = 0;
                while (len > 0) {
                    var currentRead = stream.read(result, offset + totalRead, len);
                    if (currentRead == -1) {
                        break;
                    }
                    len -= currentRead;
                    totalRead += currentRead;
                }
                remaining -= totalRead;
                return totalRead;
            }

            @Override
            public long skip(long n) throws IOException {
                var result = stream.skip(n);
                if(result >= Integer.MAX_VALUE) {
                    remaining = 0;
                }else {
                    remaining -= Math.toIntExact(result);
                }
                return result;
            }

            @Override
            public void skipNBytes(long n) throws IOException {
                stream.skipNBytes(n);
                remaining -= n;
            }

            @Override
            public int available() {
                return remaining;
            }

            @Override
            public void close() throws IOException {
                stream.close();
            }

            @Override
            public void mark(int readlimit) {
                stream.mark(readlimit);
                mark = remaining;
            }

            @Override
            public void reset() throws IOException {
                stream.reset();
                remaining = mark;
            }

            @Override
            public boolean markSupported() {
                return stream.markSupported();
            }
        }
    }

    /**
     * Represents a node containing a collection of child nodes.
     * This creates a hierarchical structure similar to XML, allowing complex
     * protocol messages to be represented as trees of nodes.
     *
     * @param description the node's description
     * @param attributes the node's attributes
     * @param children the child nodes contained in this node
     */
    record ContainerNode(String description, SequencedMap<String, NodeAttribute> attributes, SequencedCollection<Node> children) implements Node {
        public ContainerNode {
            Objects.requireNonNull(description, "description cannot be null");
            Objects.requireNonNull(attributes, "attributes cannot be null");
            Objects.requireNonNull(children, "children cannot be null");
        }

        @Override
        public SequencedMap<String, NodeAttribute> attributes() {
            return Collections.unmodifiableSequencedMap(attributes);
        }

        @Override
        public SequencedCollection<Node> children() {
            return Collections.unmodifiableSequencedCollection(children);
        }

        @Override
        public boolean hasContent() {
            return true;
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
        public String toString() {
            var result = new StringBuilder();
            result.append("Node[description=");
            result.append(description());

            if(!attributes.isEmpty()) {
                result.append(", attributes=");
                result.append(attributes);
            }

            if(!children.isEmpty()) {
                result.append(", children=");
                result.append(children);
            }

            result.append("]");

            return result.toString();
        }
    }
}