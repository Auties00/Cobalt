package it.auties.whatsapp.io.node;

import it.auties.whatsapp.exception.MissingRequiredNodeAttributeException;
import it.auties.whatsapp.model.jid.Jid;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;

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
 *   <li>{@link BufferNode} - A node containing binary data</li>
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
     * Returns the attributes of this node as an unmodifiable sequenced map.
     * Attributes provide metadata about the node, similar to XML attributes.
     *
     * @return a sequenced map of attribute names to attribute values
     */
    SequencedMap<String, NodeAttribute> attributes();

    /**
     * Checks whether this node has content.
     * Empty nodes return {@code false}, while all other node types return {@code true}.
     *
     * @return {@code true} if the node has content, {@code false} otherwise
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
     * Retrieves the first child Node in this container, if present.
     * The order of child nodes is determined by their insertion order in the content map.
     *
     * @return an {@code Optional} containing the first child Node if it exists,
     *         otherwise an empty {@code Optional}.
     */
    Optional<Node> firstChild();

    /**
     * Retrieves the last child Node in this container, if present.
     * The order of child nodes is determined by their insertion order in the content map.
     *
     * @return an {@code Optional} containing the last child Node if it exists,
     *         otherwise an empty {@code Optional}.
     */
    Optional<Node> lastChild();

    /**
     * Finds a child node by its description within the current container node.
     * If the given description is null, or if no child node with the specified
     * description exists, an empty {@code Optional} is returned.
     *
     * @param description the description of the child node to find; can be null
     * @return an {@code Optional} containing the child node if one is found
     *         with the specified description, otherwise an empty {@code Optional}
     */
    Optional<Node> findChildByDescription(String description);

    /**
     * Finds a child node by traversing the hierarchy based on the provided sequence of descriptions.
     * Each description in the sequence is used to locate a child node within the current container node.
     * If any description is null, or if a node matching the sequence cannot be found,
     * an empty {@code Optional} is returned.
     *
     * @param description a sequence of descriptions used to locate a child node; must not be null
     *                     and must not contain null elements
     * @return an {@code Optional} containing the located child node if the sequence matches,
     *         otherwise an empty {@code Optional}
     */
    Optional<Node> findChildByDescription(String... description);

    /**
     * Represents a node without any content.
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
        public Optional<Node> firstChild() {
            return Optional.empty();
        }

        @Override
        public Optional<Node> lastChild() {
            return Optional.empty();
        }

        @Override
        public Optional<Node> findChildByDescription(String description) {
            return Optional.empty();
        }

        @Override
        public Optional<Node> findChildByDescription(String... description) {
            return Optional.empty();
        }
    }

    /**
     * Represents a node containing text content.
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
        public Optional<Node> firstChild() {
            return Optional.empty();
        }

        @Override
        public Optional<Node> lastChild() {
            return Optional.empty();
        }

        @Override
        public Optional<Node> findChildByDescription(String description) {
            return Optional.empty();
        }

        @Override
        public Optional<Node> findChildByDescription(String... description) {
            return Optional.empty();
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
        public Optional<Node> findChildByDescription(String description) {
            return Optional.empty();
        }

        @Override
        public Optional<Node> findChildByDescription(String... description) {
            return Optional.empty();
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
    record BufferNode(String description, SequencedMap<String, NodeAttribute> attributes, ByteBuffer content) implements Node {
        public BufferNode {
            Objects.requireNonNull(description, "description cannot be null");
            Objects.requireNonNull(attributes, "attributes cannot be null");
            Objects.requireNonNull(content, "content cannot be null");
        }

        /**
         * Overload to create a node containing binary data as a ByteBuffer from an array of bytes
         *
         * @param description the node's description
         * @param attributes the node's attributes
         * @param content the binary content of the node as an array of bytes
         */
        public BufferNode(String description, SequencedMap<String, NodeAttribute> attributes, byte[] content) {
            this(description, attributes, ByteBuffer.wrap(content));
        }

        /**
         * Overload to create a node containing binary data as a ByteBuffer from an array of bytes
         *
         * @param description the node's description
         * @param attributes the node's attributes
         * @param content the binary content of the node as an array of bytes
         * @param offset the offset of the first byte to read
         * @param length the number of bytes to read from the array
         */
        public BufferNode(String description, SequencedMap<String, NodeAttribute> attributes, byte[] content, int offset, int length) {
            this(description, attributes, ByteBuffer.wrap(content, offset, length));
        }

        @Override
        public SequencedMap<String, NodeAttribute> attributes() {
            return Collections.unmodifiableSequencedMap(attributes);
        }

        /**
         * Returns a read-only view of the content buffer.
         * This prevents external modification of the node's content.
         *
         * @return a read-only ByteBuffer containing the node's content
         */
        @Override
        public ByteBuffer content() {
            return content.asReadOnlyBuffer();
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
        public Optional<Node> findChildByDescription(String description) {
            return Optional.empty();
        }

        @Override
        public Optional<Node> findChildByDescription(String... description) {
            return Optional.empty();
        }
    }

    /**
     * Represents a node containing streaming data.
     * This is used for protocol messages that carry large amounts of data
     * that are better processed as a stream rather than loaded entirely into memory.
     *
     * @param description the node's description
     * @param attributes the node's attributes
     * @param content the streaming content of the node as an InputStream
     * @param contentLength the full length of the content
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
        public Optional<Node> findChildByDescription(String description) {
            return Optional.empty();
        }

        @Override
        public Optional<Node> findChildByDescription(String... description) {
            return Optional.empty();
        }
    }

    /**
     * Represents a node containing a collection of child nodes.
     * This creates a hierarchical structure similar to XML, allowing complex
     * protocol messages to be represented as trees of nodes.
     *
     * @param description the node's description
     * @param attributes the node's attributes
     * @param content the child nodes contained in this node
     */
    record ContainerNode(String description, SequencedMap<String, NodeAttribute> attributes, SequencedMap<String, Node> content) implements Node {
        public ContainerNode {
            Objects.requireNonNull(description, "description cannot be null");
            Objects.requireNonNull(attributes, "attributes cannot be null");
            Objects.requireNonNull(content, "content cannot be null");
        }

        @Override
        public SequencedMap<String, NodeAttribute> attributes() {
            return Collections.unmodifiableSequencedMap(attributes);
        }

        @Override
        public SequencedMap<String, Node> content() {
            return Collections.unmodifiableSequencedMap(content);
        }

        /**
         * Retrieves the first child Node in this container, if present.
         * The order of child nodes is determined by their insertion order in the content map.
         *
         * @return an {@code Optional} containing the first child Node if it exists,
         *         otherwise an empty {@code Optional}.
         */
        @Override
        public Optional<Node> firstChild() {
            return content.isEmpty() ? Optional.empty() : Optional.ofNullable(content.firstEntry().getValue());
        }

        /**
         * Retrieves the last child Node in this container, if present.
         * The order of child nodes is determined by their insertion order in the content map.
         *
         * @return an {@code Optional} containing the last child Node if it exists,
         *         otherwise an empty {@code Optional}.
         */
        @Override
        public Optional<Node> lastChild() {
            return content.isEmpty() ? Optional.empty() : Optional.ofNullable(content.lastEntry().getValue());
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
        public Optional<Node> findChildByDescription(String description) {
            return description == null ? Optional.empty() : Optional.ofNullable(content.get(description));
        }

        /**
         * Finds a child node by traversing the hierarchy based on the provided sequence of descriptions.
         * Each description in the sequence is used to locate a child node within the current container node.
         * If any description is null, or if a node matching the sequence cannot be found,
         * an empty {@code Optional} is returned.
         *
         * @param description a sequence of descriptions used to locate a child node; must not be null
         *                     and must not contain null elements
         * @return an {@code Optional} containing the located child node if the sequence matches,
         *         otherwise an empty {@code Optional}
         */
        @Override
        public Optional<Node> findChildByDescription(String... description) {
            if(description == null) {
                return Optional.empty();
            }

            Node currentNode = this;
            for(var currentDescription : description) {
                if(currentDescription == null) {
                    return Optional.empty();
                }

                if(!(currentNode instanceof ContainerNode currentContainerNode)) {
                    return Optional.empty();
                }


                var nextNode = currentContainerNode.findChildByDescription(currentDescription);
                if(nextNode.isEmpty()) {
                    return Optional.empty();
                }

                currentNode = nextNode.get();
            }
            return Optional.of(currentNode);
        }

        @Override
        public boolean hasContent() {
            return true;
        }
    }
}