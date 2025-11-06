

package com.github.auties00.cobalt.node;

import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidProvider;

import java.io.InputStream;
import java.util.*;

/**
 * A builder class for constructing WhatsApp protocol {@link Node} instances.
 * <p>
 * This builder provides a fluent API to create nodes with various types of children and attributes.
 * Nodes are the fundamental data structure used in WhatsApp's protocol communication, similar to XML elements.
 *
 * @see Node
 * @see NodeAttribute
 */
public final class NodeBuilder {
    private String description;
    private final SequencedMap<String, NodeAttribute> attributes;
    private String textContent;
    private JidProvider jidContent;
    private byte[] bytesContent;
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
    public NodeBuilder attribute(String key, JidProvider value) {
        if(value != null) {
            this.attributes.put(key, new NodeAttribute.JidAttribute(value.toJid()));
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
    public NodeBuilder attribute(String key, JidProvider value, boolean condition) {
        if(value != null && condition) {
            this.attributes.put(key, new NodeAttribute.JidAttribute(value.toJid()));
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
     * Sets the children of the node to a text value.
     * This method clears any other children previously set (JID, bytes, stream, or children).
     *
     * @param value the text children value
     * @return this builder for method chaining
     */
    public NodeBuilder content(String value) {
        this.textContent = value;
        this.jidContent = null;
        this.bytesContent = null;
        this.inputStreamContent = null;
        this.childrenContent = null;
        return this;
    }

    /**
     * Sets the children of the node to a numeric value converted to its string representation.
     * This method clears any other children previously set (JID, bytes, stream, or children).
     *
     * @param value the numeric children value
     * @return this builder for method chaining
     */
    public NodeBuilder content(Number value) {
        this.textContent = Objects.toString(value);
        this.jidContent = null;
        this.bytesContent = null;
        this.inputStreamContent = null;
        this.childrenContent = null;
        return this;
    }

    /**
     * Sets the children of the node to a boolean value converted to its string representation.
     * This method clears any other children previously set (JID, bytes, stream, or children).
     *
     * @param value the boolean children value
     * @return this builder for method chaining
     */
    public NodeBuilder content(boolean value) {
        this.textContent = Objects.toString(value);
        this.jidContent = null;
        this.bytesContent = null;
        this.inputStreamContent = null;
        this.childrenContent = null;
        return this;
    }

    /**
     * Sets the children of the node to a JID.
     * This method clears any other children previously set (text, bytes, stream, or children).
     *
     * @param value the JID children value
     * @return this builder for method chaining
     * @see Jid
     */
    public NodeBuilder content(JidProvider value) {
        this.textContent = null;
        this.jidContent = value;
        this.bytesContent = null;
        this.inputStreamContent = null;
        this.childrenContent = null;
        return this;
    }
    
    /**
     * Sets the children of the node to binary data as a ByteBuffer.
     * This method clears any other children previously set (text, JID, stream, or children).
     *
     * @param value the buffer children value
     * @return this builder for method chaining
     */
    public NodeBuilder content(byte[] value) {
        this.textContent = null;
        this.jidContent = null;
        this.bytesContent = value;
        this.inputStreamContent = null;
        this.childrenContent = null;
        return this;
    }

    /**
     * Sets the children of the node to binary data as an InputStream.
     * This method clears any other children previously set (text, JID, bytes, or children).
     *
     * @param value the InputStream children value
     * @param length the length of the InputStream
     * @return this builder for method chaining
     */
    public NodeBuilder content(InputStream value, int length) {
        this.textContent = null;
        this.jidContent = null;
        this.bytesContent = null;
        this.inputStreamContent = value;
        this.inputStreamContentLength = length;
        this.childrenContent = null;
        return this;
    }

    /**
     * Sets the children of the node to a collection of child nodes.
     * This method clears any other children previously set (text, JID, bytes, or stream).
     * If a content of type children was already set, the two values will be merged into a single collection.
     *
     * @param nodes the collection of child nodes
     * @return this builder for method chaining
     */
    public NodeBuilder content(SequencedCollection<Node> nodes) {
        this.textContent = null;
        this.jidContent = null;
        this.bytesContent = null;
        this.inputStreamContent = null;
        if(childrenContent == null) {
            this.childrenContent = new ArrayList<>();
        }
        if(nodes != null) {
            for(var node : nodes) {
                if(node != null) {
                    this.childrenContent.add(node);
                }
            }
        }
        return this;
    }

    /**
     * Sets the children of the node to a varargs array of child nodes.
     * This method clears any other children previously set (text, JID, bytes, or stream).
     * If a content of type children was already set, the two values will be merged into a single collection.
     *
     * @param nodes the varargs array of child nodes
     * @return this builder for method chaining
     */
    public NodeBuilder content(Node... nodes) {
        this.textContent = null;
        this.jidContent = null;
        this.bytesContent = null;
        this.inputStreamContent = null;
        if(childrenContent == null) {
            this.childrenContent = new ArrayList<>();
        }
        if(nodes != null) {
            for(var node : nodes) {
                if(node != null) {
                    this.childrenContent.add(node);
                }
            }
        }
        return this;
    }

    /**
     * Checks if an attribute with the specified key is present in this builder.
     *
     * @param key the attribute key to check
     * @return true if an attribute with the given key exists, false otherwise
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    /**
     * Checks if any content has been set for this node.
     * <p>
     * This method returns true if any of the following content types have been set:
     * <ul>
     *   <li>Text content</li>
     *   <li>JID content</li>
     *   <li>ByteBuffer content</li>
     *   <li>InputStream content</li>
     *   <li>Children nodes</li>
     * </ul>
     *
     * @return true if any content has been set, false otherwise
     */
    public boolean hasContent() {
        return textContent != null
               || jidContent != null
               || bytesContent != null
               || inputStreamContent != null
               || childrenContent != null;
    }

    /**
     * Builds and returns the constructed Node instance.
     * <p>
     * The type of node returned depends on the children type that was set:
     * <ul>
     *   <li>{@link Node.TextNode} - if text children was set</li>
     *   <li>{@link Node.JidNode} - if JID children was set</li>
     *   <li>{@link Node.BytesNode} - if ByteBuffer children was set</li>
     *   <li>{@link Node.StreamNode} - if InputStream children was set</li>
     *   <li>{@link Node.ContainerNode} - if child nodes were set</li>
     *   <li>{@link Node.EmptyNode} - if no children was set</li>
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
            return new Node.JidNode(description, attributes, jidContent.toJid());
        }else if(bytesContent != null){
            return new Node.BytesNode(description, attributes, bytesContent);
        }else if (inputStreamContent != null){
            return new Node.StreamNode(description, attributes, inputStreamContent, inputStreamContentLength);
        }else if(childrenContent != null){
            return new Node.ContainerNode(description, attributes, childrenContent);
        }else {
            return new Node.EmptyNode(description, attributes);
        }
    }
}