

package it.auties.whatsapp.io.node;

import it.auties.whatsapp.model.jid.Jid;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * A builder class for constructing WhatsApp protocol {@link Node} instances.
 * <p>
 * This builder provides a fluent API to create nodes with various types of content and attributes.
 * Nodes are the fundamental data structure used in WhatsApp's protocol communication, similar to XML elements.
 *
 * @see Node
 * @see NodeAttribute
 */
public final class NodeBuilder {
    private String description;
    private final SequencedMap<String, NodeAttribute> attributes;
    private String textContent;
    private Jid jidContent;
    private ByteBuffer bufferContent;
    private InputStream inputStreamContent;
    private int inputStreamContentLength;
    private SequencedCollection<Node> childrenContent;

    /**
     * Constructs a new NodeBuilder with empty attributes.
     */
    public NodeBuilder() {
        this.attributes = new LinkedHashMap<>();
    }

    /**
     * Sets the description (tag name) of the node.
     * <p>
     * The description typically represents the type or purpose of the node,
     * similar to an XML element name.
     *
     * @param description the description/tag name of the node
     * @return this builder for method chaining
     */
    public NodeBuilder description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Adds a text attribute to the node if the value is not null.
     *
     * @param key the attribute key
     * @param value the attribute value, or null to skip adding this attribute
     * @return this builder for method chaining
     */
    public NodeBuilder attribute(String key, String value) {
        if(value != null) {
            this.attributes.put(key, new NodeAttribute.TextAttribute(value));
        }
        return this;
    }

    /**
     * Adds a text attribute to the node if the value is not null and the condition is true.
     *
     * @param key the attribute key
     * @param value the attribute value, or null to skip adding this attribute
     * @param condition the condition that must be true to add the attribute
     * @return this builder for method chaining
     */
    public NodeBuilder attribute(String key, String value, boolean condition) {
        if(value != null && condition) {
            this.attributes.put(key, new NodeAttribute.TextAttribute(value));
        }
        return this;
    }

    /**
     * Adds a numeric attribute to the node if the value is not null.
     * The number is converted to its string representation.
     *
     * @param key the attribute key
     * @param value the numeric attribute value, or null to skip adding this attribute
     * @return this builder for method chaining
     */
    public NodeBuilder attribute(String key, Number value) {
        if(value != null) {
            this.attributes.put(key, new NodeAttribute.TextAttribute(value.toString()));
        }
        return this;
    }

    /**
     * Adds a numeric attribute to the node if the value is not null and the condition is true.
     * The number is converted to its string representation.
     *
     * @param key the attribute key
     * @param value the numeric attribute value, or null to skip adding this attribute
     * @param condition the condition that must be true to add the attribute
     * @return this builder for method chaining
     */
    public NodeBuilder attribute(String key, Number value, boolean condition) {
        if(value != null && condition) {
            this.attributes.put(key, new NodeAttribute.TextAttribute(value.toString()));
        }
        return this;
    }

    /**
     * Adds a boolean attribute to the node.
     * The boolean value is converted to its string representation ("true" or "false").
     *
     * @param key the attribute key
     * @param value the boolean attribute value
     * @return this builder for method chaining
     */
    public NodeBuilder attribute(String key, boolean value) {
        this.attributes.put(key, new NodeAttribute.TextAttribute(Boolean.toString(value)));
        return this;
    }

    /**
     * Adds a boolean attribute to the node if the condition is true.
     * The boolean value is converted to its string representation ("true" or "false").
     *
     * @param key the attribute key
     * @param value the boolean attribute value
     * @param condition the condition that must be true to add the attribute
     * @return this builder for method chaining
     */
    public NodeBuilder attribute(String key, boolean value, boolean condition) {
        if(condition) {
            this.attributes.put(key, new NodeAttribute.TextAttribute(Boolean.toString(value)));
        }
        return this;
    }

    /**
     * Adds a JID attribute to the node if the value is not null.
     * JIDs represent WhatsApp user, group, or server identifiers.
     *
     * @param key the attribute key
     * @param value the JID attribute value, or null to skip adding this attribute
     * @return this builder for method chaining
     * @see Jid
     */
    public NodeBuilder attribute(String key, Jid value) {
        if(value != null) {
            this.attributes.put(key, new NodeAttribute.JidAttribute(value));
        }
        return this;
    }

    /**
     * Adds a JID attribute to the node if the value is not null and the condition is true.
     * JIDs represent WhatsApp user, group, or server identifiers.
     *
     * @param key the attribute key
     * @param value the JID attribute value, or null to skip adding this attribute
     * @param condition the condition that must be true to add the attribute
     * @return this builder for method chaining
     * @see Jid
     */
    public NodeBuilder attribute(String key, Jid value, boolean condition) {
        if(value != null && condition) {
            this.attributes.put(key, new NodeAttribute.JidAttribute(value));
        }
        return this;
    }

    /**
     * Adds a binary attribute to the node if the value is not null.
     *
     * @param key the attribute key
     * @param value the binary attribute value, or null to skip adding this attribute
     * @return this builder for method chaining
     */
    public NodeBuilder attribute(String key, byte[] value) {
        if(value != null) {
            this.attributes.put(key, new NodeAttribute.BytesAttribute(value));
        }
        return this;
    }

    /**
     * Adds a binary attribute to the node if the value is not null and the condition is true.
     *
     * @param key the attribute key
     * @param value the binary attribute value, or null to skip adding this attribute
     * @param condition the condition that must be true to add the attribute
     * @return this builder for method chaining
     */
    public NodeBuilder attribute(String key, byte[] value, boolean condition) {
        if(value != null && condition) {
            this.attributes.put(key, new NodeAttribute.BytesAttribute(value));
        }
        return this;
    }

    /**
     * Adds multiple attributes to the node from a map if the map is not null.
     * All existing attributes are retained, and new attributes are added or overwrite existing ones with the same key.
     *
     * @param attributes the map of attributes to add, or null to skip adding any attribute
     * @return this builder for method chaining
     */
    public NodeBuilder attributes(Map<String, ? extends NodeAttribute> attributes) {
        if(attributes != null) {
            this.attributes.putAll(attributes);
        }
        return this;
    }

    /**
     * Sets the content of the node to a text value.
     * This method clears any other content previously set (JID, buffer, stream, or children).
     *
     * @param value the text content value
     * @return this builder for method chaining
     */
    public NodeBuilder content(String value) {
        this.textContent = value;
        this.jidContent = null;
        this.bufferContent = null;
        this.inputStreamContent = null;
        this.childrenContent = null;
        return this;
    }

    /**
     * Sets the content of the node to a numeric value converted to its string representation.
     * This method clears any other content previously set (JID, buffer, stream, or children).
     *
     * @param value the numeric content value
     * @return this builder for method chaining
     */
    public NodeBuilder content(Number value) {
        this.textContent = Objects.toString(value);
        this.jidContent = null;
        this.bufferContent = null;
        this.inputStreamContent = null;
        this.childrenContent = null;
        return this;
    }

    /**
     * Sets the content of the node to a boolean value converted to its string representation.
     * This method clears any other content previously set (JID, buffer, stream, or children).
     *
     * @param value the boolean content value
     * @return this builder for method chaining
     */
    public NodeBuilder content(boolean value) {
        this.textContent = Objects.toString(value);
        this.jidContent = null;
        this.bufferContent = null;
        this.inputStreamContent = null;
        this.childrenContent = null;
        return this;
    }

    /**
     * Sets the content of the node to a JID.
     * This method clears any other content previously set (text, buffer, stream, or children).
     *
     * @param value the JID content value
     * @return this builder for method chaining
     * @see Jid
     */
    public NodeBuilder content(Jid value) {
        this.textContent = null;
        this.jidContent = value;
        this.bufferContent = null;
        this.inputStreamContent = null;
        this.childrenContent = null;
        return this;
    }

    /**
     * Sets the content of the node to binary data as a ByteBuffer.
     * This method clears any other content previously set (text, JID, stream, or children).
     *
     * @param value the ByteBuffer content value
     * @return this builder for method chaining
     */
    public NodeBuilder content(ByteBuffer value) {
        this.textContent = null;
        this.jidContent = null;
        this.bufferContent = value;
        this.inputStreamContent = null;
        this.childrenContent = null;
        return this;
    }

    /**
     * Sets the content of the node to binary data as an InputStream.
     * This method clears any other content previously set (text, JID, buffer, or children).
     *
     * @param value the InputStream content value
     * @param length the length of the InputStream
     * @return this builder for method chaining
     */
    public NodeBuilder content(InputStream value, int length) {
        this.textContent = null;
        this.jidContent = null;
        this.bufferContent = null;
        this.inputStreamContent = value;
        this.inputStreamContentLength = length;
        this.childrenContent = null;
        return this;
    }

    /**
     * Sets the content of the node to a collection of child nodes.
     * This method clears any other content previously set (text, JID, buffer, or stream).
     *
     * @param value the collection of child nodes
     * @return this builder for method chaining
     */
    public NodeBuilder content(SequencedCollection<Node> value) {
        this.textContent = null;
        this.jidContent = null;
        this.bufferContent = null;
        this.inputStreamContent = null;
        this.childrenContent = value;
        return this;
    }

    /**
     * Sets the content of the node to a varargs array of child nodes.
     * This method clears any other content previously set (text, JID, buffer, or stream).
     * The array is wrapped in a lightweight list implementation to avoid unnecessary copying.
     *
     * @param nodes the varargs array of child nodes
     * @return this builder for method chaining
     */
    public NodeBuilder content(Node... nodes) {
        this.textContent = null;
        this.jidContent = null;
        this.bufferContent = null;
        this.inputStreamContent = null;
        this.childrenContent = new AbstractList<>() {
            @Override
            public Node get(int index) {
                return nodes[Objects.checkIndex(index, nodes.length)];
            }

            @Override
            public int size() {
                return nodes.length;
            }
        };
        return this;
    }

    /**
     * Builds and returns the constructed Node instance.
     * <p>
     * The type of node returned depends on the content type that was set:
     * <ul>
     *   <li>{@link Node.TextNode} - if text content was set</li>
     *   <li>{@link Node.JidNode} - if JID content was set</li>
     *   <li>{@link Node.BufferNode} - if ByteBuffer content was set</li>
     *   <li>{@link Node.StreamNode} - if InputStream content was set</li>
     *   <li>{@link Node.ContainerNode} - if child nodes were set</li>
     *   <li>{@link Node.EmptyNode} - if no content was set</li>
     * </ul>
     * <p>
     * If no description was set, an empty string is used as the default.
     *
     * @return the constructed Node instance
     * @see Node
     */
    public Node build() {
        var description = Objects.requireNonNullElse(this.description, "");
        if(textContent != null) {
            return new Node.TextNode(description, attributes, textContent);
        }else if(jidContent != null){
            return new Node.JidNode(description, attributes, jidContent);
        }else if(bufferContent != null){
            return new Node.BufferNode(description, attributes, bufferContent);
        }else if (inputStreamContent != null){
            return new Node.StreamNode(description, attributes, inputStreamContent, inputStreamContentLength);
        }else if(childrenContent != null){
            return new Node.ContainerNode(description, attributes, childrenContent);
        }else {
            return new Node.EmptyNode(description, attributes);
        }
    }
}