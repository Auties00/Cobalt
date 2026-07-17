package com.github.auties00.cobalt.wire.linked.business.profile;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One stanza in the hierarchical tree of business categories WhatsApp offers
 * when an account picks the industry that best describes it.
 *
 * <p>When classifying a WhatsApp Business account, WhatsApp can present the
 * available verticals (for example "Food and Beverage" containing "Restaurant"
 * and "Bakery") as a nested tree rather than a flat list, so an app can let the
 * user drill from a broad area down to a specific category. Each stanza pairs a
 * stable {@link #id() identifier} with a localised {@link #displayName() display
 * name} and holds the {@link #children() child categories} nested directly
 * beneath it; a leaf stanza simply reports an empty child list.
 *
 * <p>The display name is localised to the request locale supplied when the tree
 * was fetched, while the identifier is locale-independent and is the value an
 * app stores when the user selects a category.
 */
@ProtobufMessage
public final class BusinessCategoryNode {
    /**
     * Locale-independent stable identifier of this category, used to record the
     * user's selection. Always present.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Localised, human-readable name of this category in the request locale.
     * Empty when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String displayName;

    /**
     * Child categories nested directly beneath this stanza, in server order.
     * Empty for a leaf stanza. Never {@code null}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final List<BusinessCategoryNode> children;

    /**
     * Constructs a new {@code BusinessCategoryNode}. A {@code null} child list
     * is coerced to an empty list.
     *
     * @param id          the locale-independent category identifier
     * @param displayName the localised display name, or {@code null}
     * @param children    the nested child categories; {@code null} treated as empty
     */
    BusinessCategoryNode(String id, String displayName, List<BusinessCategoryNode> children) {
        this.id = id;
        this.displayName = displayName;
        this.children = children == null ? List.of() : children;
    }

    /**
     * Returns the locale-independent stable identifier of this category.
     *
     * @return the category identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns the localised display name of this category in the request
     * locale.
     *
     * @return an {@code Optional} containing the display name, or empty when the
     *         server omitted it
     */
    public Optional<String> displayName() {
        return Optional.ofNullable(displayName);
    }

    /**
     * Returns the child categories nested directly beneath this stanza.
     *
     * @return an unmodifiable view of the children, in server order; never
     *         {@code null}, empty for a leaf stanza
     */
    public List<BusinessCategoryNode> children() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessCategoryNode) obj;
        return Objects.equals(this.id, that.id) &&
               Objects.equals(this.displayName, that.displayName) &&
               Objects.equals(this.children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayName, children);
    }

    @Override
    public String toString() {
        return "BusinessCategoryNode[" +
               "id=" + id + ", " +
               "displayName=" + displayName + ", " +
               "children=" + children + ']';
    }
}
