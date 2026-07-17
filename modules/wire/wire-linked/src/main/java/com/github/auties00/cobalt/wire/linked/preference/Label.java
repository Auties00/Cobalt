package com.github.auties00.cobalt.wire.linked.preference;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.LabelEditAction;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SequencedCollection;
import java.util.SequencedSet;

/**
 * Represents a user-defined label that organises chats and contacts inside the
 * WhatsApp Business client.
 *
 * <p>Labels are a WhatsApp Business feature that lets the user tag chats and
 * contacts with a coloured tag (for example "New customer", "Pending payment",
 * or "Order complete") so that related conversations can be grouped and found
 * quickly. A label carries a stable identifier, a human-readable name, a colour
 * index, an ordered set of {@link Jid} assignments pointing at the chats or
 * contacts that wear the tag, and a handful of flags that describe whether the
 * label was predefined by the system, whether it is currently active, and
 * whether it can be deleted or renamed.
 *
 * <p>Instances are mutable: the name, colour, assignments and flags can be
 * updated in place, and the generated {@code LabelBuilder} should be used to
 * construct new labels. The {@code id} is assigned once and never changes.
 *
 * <p>Labels are synchronised across the user's devices through sync actions and
 * are persisted by Cobalt's store so that they survive reconnections.
 *
 * @see LabelEditAction
 */
@ProtobufMessage
public final class Label {
    /**
     * The stable identifier of this label.
     *
     * <p>Assigned once when the label is created and used as the primary key by
     * Cobalt's store and by all label-related sync actions.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * The user-visible name of this label, for example {@code "New customer"}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String name;

    /**
     * The colour index of this label.
     *
     * <p>The value is a palette index rather than an RGB colour. Clients map
     * the index to a concrete colour using their own palette, so the same
     * numeric value always produces the same visual colour across devices.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    int color;

    /**
     * The ordered set of chats or contacts that currently wear this label.
     *
     * <p>Each entry is a {@link Jid} that identifies either a chat or a
     * contact. Insertion order is preserved so that the UI can display
     * assignments in the order they were added.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    SequencedSet<Jid> assignments;

    /**
     * The predefined identifier when this label was created by WhatsApp rather
     * than by the user, or {@code null} when the label is fully custom.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT32)
    Integer predefinedId;

    /**
     * The position of this label in the user's label list, or {@code null} when
     * no explicit order has been set.
     *
     * <p>Lower values are displayed first.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.INT32)
    Integer orderIndex;

    /**
     * Whether this label is currently active and should be shown to the user,
     * or {@code null} when the active state is not specified.
     *
     * <p>Inactive labels are kept in the store but hidden from the UI.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    Boolean isActive;

    /**
     * The kind of entities this label can be applied to (for example chats or
     * contacts), or {@code null} when the label accepts any entity.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.ENUM)
    LabelEditAction.ListType type;

    /**
     * Whether this label is immutable and therefore cannot be renamed, recoloured
     * or deleted, or {@code null} when the flag is not specified.
     *
     * <p>Predefined labels supplied by WhatsApp are typically immutable.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.BOOL)
    Boolean isImmutable;

    /**
     * Constructs a new label with the given field values.
     *
     * <p>This constructor is package-private. Application code should obtain
     * instances through the generated {@code LabelBuilder}.
     *
     * @param id           the stable identifier of the label
     * @param name         the user-visible name
     * @param color        the palette colour index
     * @param assignments  the ordered set of {@code Jid} assignments
     * @param predefinedId the predefined identifier, or {@code null} for custom labels
     * @param orderIndex   the explicit display order, or {@code null} when unset
     * @param isActive     whether the label is active, or {@code null} when unset
     * @param type         the kind of entities the label can be applied to, or {@code null}
     * @param isImmutable  whether the label is immutable, or {@code null} when unset
     */
    Label(String id, String name, int color, SequencedSet<Jid> assignments, Integer predefinedId, Integer orderIndex, Boolean isActive, LabelEditAction.ListType type, Boolean isImmutable) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.assignments = assignments;
        this.predefinedId = predefinedId;
        this.orderIndex = orderIndex;
        this.isActive = isActive;
        this.type = type;
        this.isImmutable = isImmutable;
    }

    /**
     * Returns the stable identifier of this label.
     *
     * @return the identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns the user-visible name of this label.
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Updates the user-visible name of this label.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the palette colour index of this label.
     *
     * @return the colour index
     */
    public int color() {
        return color;
    }

    /**
     * Updates the palette colour index of this label.
     *
     * @param color the new colour index
     */
    public void setColor(int color) {
        this.color = color;
    }

    /**
     * Returns the predefined identifier of this label when it was created by
     * WhatsApp rather than by the user.
     *
     * @return an {@link OptionalInt} holding the predefined identifier, or an
     *         empty optional when the label is fully custom
     */
    public OptionalInt predefinedId() {
        return predefinedId == null ? OptionalInt.empty() : OptionalInt.of(predefinedId);
    }

    /**
     * Updates the predefined identifier of this label.
     *
     * @param predefinedId the new predefined identifier, or {@code null} to
     *                     mark the label as custom
     */
    public void setPredefinedId(Integer predefinedId) {
        this.predefinedId = predefinedId;
    }

    /**
     * Returns the explicit display order of this label.
     *
     * <p>Lower values are shown first. When no order has been set the user
     * interface typically falls back to insertion or alphabetical order.
     *
     * @return an {@link OptionalInt} holding the order index, or an empty
     *         optional when no explicit order has been set
     */
    public OptionalInt orderIndex() {
        return orderIndex == null ? OptionalInt.empty() : OptionalInt.of(orderIndex);
    }

    /**
     * Updates the explicit display order of this label.
     *
     * @param orderIndex the new order index, or {@code null} to clear it
     */
    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    /**
     * Returns whether this label is currently active.
     *
     * <p>Inactive labels are kept in the store but hidden from the UI.
     *
     * @return an {@link Optional} holding {@code true} when the label is
     *         active, {@code false} when it is inactive, or empty when the
     *         active state has never been specified
     */
    public Optional<Boolean> isActive() {
        return Optional.ofNullable(isActive);
    }

    /**
     * Updates the active flag of this label.
     *
     * @param isActive the new active flag, or {@code null} to clear it
     */
    public void setActive(Boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Returns the kind of entities this label can be applied to (for example
     * chats or contacts).
     *
     * @return an {@link Optional} holding the list type, or empty when the
     *         label accepts any entity
     */
    public Optional<LabelEditAction.ListType> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Updates the kind of entities this label can be applied to.
     *
     * @param type the new list type, or {@code null} to accept any entity
     */
    public void setType(LabelEditAction.ListType type) {
        this.type = type;
    }

    /**
     * Returns whether this label is immutable.
     *
     * <p>Immutable labels cannot be renamed, recoloured or deleted.
     *
     * @return an {@link Optional} holding {@code true} when the label is
     *         immutable, {@code false} when it can be freely edited, or empty
     *         when the flag has never been specified
     */
    public Optional<Boolean> isImmutable() {
        return Optional.ofNullable(isImmutable);
    }

    /**
     * Updates the immutable flag of this label.
     *
     * @param isImmutable the new immutable flag, or {@code null} to clear it
     */
    public void setImmutable(Boolean isImmutable) {
        this.isImmutable = isImmutable;
    }

    /**
     * Returns an unmodifiable view of the chats or contacts that currently
     * wear this label.
     *
     * <p>The returned collection preserves insertion order. To add or remove
     * assignments use {@link #addAssignment(Jid)} and
     * {@link #removeAssignment(Jid)}.
     *
     * @return an unmodifiable sequenced collection of {@link Jid} assignments
     */
    public SequencedCollection<Jid> assignments() {
        return Collections.unmodifiableSequencedCollection(assignments);
    }

    /**
     * Applies this label to the given chat or contact.
     *
     * <p>Adding a {@link Jid} that is already present is a no-op.
     *
     * @param jid the chat or contact identifier to tag with this label
     */
    public void addAssignment(Jid jid) {
        assignments.add(jid);
    }

    /**
     * Removes this label from the given chat or contact.
     *
     * @param jid the chat or contact identifier to untag
     * @return {@code true} when the assignment existed and was removed,
     *         {@code false} when the {@link Jid} was not assigned
     */
    public boolean removeAssignment(Jid jid) {
        return assignments.remove(jid);
    }
}
