package com.github.auties00.cobalt.io.node;

import com.github.auties00.cobalt.exception.MalformedJidException;
import com.github.auties00.cobalt.exception.MissingRequiredNodeAttributeException;
import com.github.auties00.cobalt.model.jid.Jid;
import it.auties.protobuf.model.ProtobufString;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
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
     * Returns the attributes of this node as an unmodifiable sequenced map.
     * Attributes provide metadata about the node, similar to XML attributes.
     *
     * @return a sequenced map of attribute names to attribute values
     */
    SequencedMap<String, NodeAttribute> attributes();

    /**
     * Checks whether this node has children.
     * Empty nodes return {@code false}, while all other node types return {@code true}.
     *
     * @return {@code true} if the node has children, {@code false} otherwise
     */
    boolean hasContent();

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
     * Retrieves an optional attribute by key.
     *
     * @param key the attribute key to look up
     * @return an {@link Optional} containing the attribute if present, or empty if not found
     */
    default Optional<NodeAttribute> getOptionalAttribute(String key) {
        return Optional.ofNullable(attributes().get(key));
    }

    /**
     * Retrieves a required attribute by key.
     * If the attribute is not present, a {@link MissingRequiredNodeAttributeException} is thrown.
     *
     * @param key the attribute key to look up
     * @return the attribute value
     * @throws MissingRequiredNodeAttributeException if the attribute is not present
     */
    default NodeAttribute getRequiredAttribute(String key) {
        var result = attributes().get(key);
        if(result == null) {
            throw new MissingRequiredNodeAttributeException(this, key);
        }

        return result;
    }

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
     * @return an {@code Optional} containing the content a a buffer if possible, otherwise an empty {@code Optional}
     */
    Optional<byte[]> toContentBytes();

    /**
     * Converts the content of this node to a string, if possible.
     *
     * @return an {@code Optional} containing the content as a string if possible, otherwise an empty {@code Optional}
     */
    Optional<String> toContentString();

    /**
     * Converts the content of this node to a jid, if possible.
     *
     * @return an {@code Optional} containing the content as a jid if possible, otherwise an empty {@code Optional}
     */
    Optional<Jid> toContentJid();

    /**
     * Retrieves the first child Node in this container, if present.
     *
     * @return an {@code Optional} containing the first child Node if it exists,
     *         otherwise an empty {@code Optional}.
     */
    Optional<Node> firstChild();

    /**
     * Retrieves the last child Node in this container, if present.
     *
     * @return an {@code Optional} containing the last child Node if it exists,
     *         otherwise an empty {@code Optional}.
     */
    Optional<Node> lastChild();

    /**
     * Retrieves the child nodes contained in this container node, if any.
     *
     * @return a sequenced collection of child nodes contained in this container node
     */
    SequencedCollection<Node> children();

    /**
     * Finds a child node by its description within the current container node.
     * If the given description is null, or if no child node with the specified
     * description exists, an empty {@code Optional} is returned.
     *
     * @param description the description of the child node to find; can be null
     * @return an {@code Optional} containing the child node if one is found
     *         with the specified description, otherwise an empty {@code Optional}
     */
    Optional<Node> firstChildByDescription(String description);

    /**
     * Finds a child node by traversing the hierarchy based on the provided sequence of descriptions.
     * Each description in the sequence is used to locate a child node within the current container node.
     * If any description is null, or if a node matching the sequence cannot be found,
     * an empty {@code Optional} is returned.
     *
     * @param description a sequence of descriptions used to locate a child node; must not be null
     * @return an {@code Optional} containing the located child node if the sequence matches,
     *         otherwise an empty {@code Optional}
     */
    Optional<Node> firstChildByDescription(String... description);

    /**
     * Finds all children nodes by their descriptions within the current container node.
     * If the given description is null, or if no child node with the specified
     * description exists, an empty {@code Stream} is returned.
     *
     * @param description the description of the children nodes to find; can be null
     * @return an {@code Stream} containing the children nodes
     */
    Stream<Node> streamChildrenByDescription(String description);

    /**
     * Finds all children nodes by traversing the hierarchy based on the provided sequence of descriptions.
     * Each description in the sequence is used to locate a child node within the current container node.
     * If any description is null, or if no nodes matching the sequence can be found,
     * an empty {@code Optional} is returned.
     *
     * @param description a sequence of descriptions used to locate a child node; must not be null
     * @return an {@code Stream} containing the children nodes
     */
    Stream<Node> streamChildrenByDescription(String... description);

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
        public Optional<byte[]> toContentBytes() {
            return Optional.empty();
        }

        @Override
        public Optional<Node> firstChild() {
            return Optional.empty();
        }

        @Override
        public Optional<Node> lastChild() {
            return Optional.empty();
        }

        @Override
        public SequencedCollection<Node> children() {
            return List.of();
        }

        @Override
        public Optional<Node> firstChildByDescription(String description) {
            return Optional.empty();
        }

        @Override
        public Optional<Node> firstChildByDescription(String... description) {
            return Optional.empty();
        }

        @Override
        public Stream<Node> streamChildrenByDescription(String description) {
            return Stream.empty();
        }

        @Override
        public Stream<Node> streamChildrenByDescription(String... description) {
            return Stream.empty();
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
        public Optional<Node> firstChild() {
            return Optional.empty();
        }

        @Override
        public Optional<Node> lastChild() {
            return Optional.empty();
        }

        @Override
        public SequencedCollection<Node> children() {
            return List.of();
        }

        @Override
        public Optional<Node> firstChildByDescription(String description) {
            return Optional.empty();
        }

        @Override
        public Optional<Node> firstChildByDescription(String... description) {
            return Optional.empty();
        }

        @Override
        public Stream<Node> streamChildrenByDescription(String description) {
            return Stream.empty();
        }

        @Override
        public Stream<Node> streamChildrenByDescription(String... description) {
            return Stream.empty();
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
        public boolean hasContent() {
            return true;
        }

        @Override
        public Optional<Node> firstChild() {
            return Optional.empty();
        }

        @Override
        public Optional<Node> lastChild() {
            return Optional.empty();
        }

        @Override
        public SequencedCollection<Node> children() {
            return List.of();
        }

        @Override
        public Optional<Node> firstChildByDescription(String description) {
            return Optional.empty();
        }

        @Override
        public Optional<Node> firstChildByDescription(String... description) {
            return Optional.empty();
        }

        @Override
        public Stream<Node> streamChildrenByDescription(String description) {
            return Stream.empty();
        }

        @Override
        public Stream<Node> streamChildrenByDescription(String... description) {
            return Stream.empty();
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
        public Optional<Node> firstChild() {
            return Optional.empty();
        }

        @Override
        public Optional<Node> lastChild() {
            return Optional.empty();
        }

        @Override
        public SequencedCollection<Node> children() {
            return List.of();
        }

        @Override
        public Optional<Node> firstChildByDescription(String description) {
            return Optional.empty();
        }

        @Override
        public Optional<Node> firstChildByDescription(String... description) {
            return Optional.empty();
        }

        @Override
        public Stream<Node> streamChildrenByDescription(String description) {
            return Stream.empty();
        }

        @Override
        public Stream<Node> streamChildrenByDescription(String... description) {
            return Stream.empty();
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
        public StreamNode {
            Objects.requireNonNull(description, "description cannot be null");
            Objects.requireNonNull(attributes, "attributes cannot be null");
            Objects.requireNonNull(content, "content cannot be null");
            if(contentLength < 0) {
                throw new IllegalArgumentException("contentLength cannot be negative");
            }
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
        public Optional<Node> firstChild() {
            return Optional.empty();
        }

        @Override
        public Optional<Node> lastChild() {
            return Optional.empty();
        }

        @Override
        public SequencedCollection<Node> children() {
            return List.of();
        }

        @Override
        public Optional<Node> firstChildByDescription(String description) {
            return Optional.empty();
        }

        @Override
        public Optional<Node> firstChildByDescription(String... description) {
            return Optional.empty();
        }

        @Override
        public Stream<Node> streamChildrenByDescription(String description) {
            return Stream.empty();
        }

        @Override
        public Stream<Node> streamChildrenByDescription(String... description) {
            return Stream.empty();
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

        /**
         * Retrieves the first child Node in this container, if present.
         *
         * @return an {@code Optional} containing the first child Node if it exists,
         *         otherwise an empty {@code Optional}.
         */
        @Override
        public Optional<Node> firstChild() {
            return children.isEmpty()
                    ? Optional.empty()
                    : Optional.ofNullable(children.getFirst());
        }

        /**
         * Retrieves the last child Node in this container, if present.
         *
         * @return an {@code Optional} containing the last child Node if it exists,
         *         otherwise an empty {@code Optional}.
         */
        @Override
        public Optional<Node> lastChild() {
            return children.isEmpty()
                    ? Optional.empty()
                    : Optional.ofNullable(children.getLast());
        }

        /**
         * Finds a child node by its description within the current container node.
         * If the given description is null, or if no child node with the specified
         * description exists, an empty {@code Optional} is returned.
         *
         * @param description the description of the child node to find; can be null
         * @return an {@code Optional} containing the child node if one is found
         *         with the specified description, otherwise an empty {@code Optional}
         */
        @Override
        public Optional<Node> firstChildByDescription(String description) {
            return description == null
                    ? Optional.empty()
                    : streamChildrenByDescription(description).findFirst();
        }

        /**
         * Finds a child node by traversing the hierarchy based on the provided sequence of descriptions.
         * Each description in the sequence is used to locate a child node within the current container node.
         * If any description is null, or if a node matching the sequence cannot be found,
         * an empty {@code Optional} is returned.
         *
         * @param description a sequence of descriptions used to locate a child node; must not be null
         * @return an {@code Optional} containing the located child node if the sequence matches,
         *         otherwise an empty {@code Optional}
         */
        @Override
        public Optional<Node> firstChildByDescription(String... description) {
            if(description == null || description.length == 0) {
                throw new IllegalArgumentException("description cannot be null or empty");
            }

            Node currentNode = this;
            for(var currentDescription : description) {
                if(currentDescription == null) {
                    return Optional.empty();
                }

                if(!(currentNode instanceof ContainerNode currentContainerNode)) {
                    return Optional.empty();
                }


                var nextNode = currentContainerNode.firstChildByDescription(currentDescription);
                if(nextNode.isEmpty()) {
                    return Optional.empty();
                }

                currentNode = nextNode.get();
            }
            return Optional.of(currentNode);
        }

        @Override
        public Stream<Node> streamChildrenByDescription(String description) {
            return children.stream()
                    .filter(node -> node.hasDescription(description));
        }

        @Override
        public Stream<Node> streamChildrenByDescription(String... description) {
            if(description == null || description.length == 0) {
                throw new IllegalArgumentException("description cannot be null or empty");
            }

            var descriptionIndex = 0;
            var resultsQueue = new LinkedList<Node>();
            resultsQueue.add(this);
            while(!resultsQueue.isEmpty() && descriptionIndex < description.length) {
                var path = description[descriptionIndex++];
                var resultsIterator = resultsQueue.listIterator();
                while (resultsIterator.hasNext()) {
                    var currentNode = resultsIterator.next();
                    if(!(currentNode instanceof ContainerNode currentContainerNode)) {
                        resultsIterator.remove();
                        continue;
                    }

                    var nextNode = currentContainerNode.firstChildByDescription(path);
                    if(nextNode.isEmpty()) {
                        resultsIterator.remove();
                        continue;
                    }

                    resultsIterator.set(nextNode.get());
                }
            }
            return resultsQueue.stream();
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
    }
}