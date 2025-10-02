package it.auties.whatsapp.exception;

import it.auties.whatsapp.io.node.Node;

/**
 * Exception thrown when a required attribute is missing from a WhatsApp protocol node.
 *
 * <p>In the WhatsApp protocol, nodes are fundamental building blocks that carry structured data
 * similar to XML elements. Each node can have a description (tag name), attributes, and content.
 * Some protocol operations require specific attributes to be present on a node for proper
 * message handling and validation.
 *
 * <p>This exception is thrown by {@link Node#getRequiredAttribute(String)} when
 * attempting to retrieve an attribute that doesn't exist in the node's attribute map.
 * It provides access to both the problematic node and the name of the missing attribute to
 * aid in debugging and error reporting.
 *
 * @see Node
 * @see Node#getRequiredAttribute(String)
 */
public class MissingRequiredNodeAttributeException extends RuntimeException {
    private final Node node;
    private final String attributeName;

    /**
     * Constructs a new exception indicating that a required attribute is missing from a node.
     *
     * <p>The exception message is automatically formatted as:
     * {@code "[attributeName] is required in [node]"}
     *
     * @param node the node missing the required attribute, must not be null
     * @param attributeName the name of the missing attribute, must not be null
     */
    public MissingRequiredNodeAttributeException(Node node, String attributeName) {
        super(attributeName + " is required in " + node);
        this.node = node;
        this.attributeName = attributeName;
    }

    /**
     * Returns the node missing the required attribute.
     *
     * @return the node that triggered this exception
     */
    public Node node() {
        return node;
    }

    /**
     * Returns the name of the missing attribute.
     *
     * @return the name of the attribute that was expected but not present
     */
    public String attributeName() {
        return attributeName;
    }
}
