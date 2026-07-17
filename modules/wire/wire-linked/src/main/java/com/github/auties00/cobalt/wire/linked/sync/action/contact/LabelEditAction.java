package com.github.auties00.cobalt.wire.linked.sync.action.contact;

import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import java.time.Instant;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A sync action that records the creation, edit or deletion of a label.
 *
 * <p>Labels are the colour-coded tags and filter lists that WhatsApp users
 * apply to chats to group them (for example a custom "Work" label or a
 * predefined "Unread" filter). This action carries the metadata of a single
 * label, including its display name, colour, ordering position and the kind
 * of list it represents. It is emitted whenever a label is added, renamed,
 * recoloured, deactivated, deleted, muted or reordered, so that every
 * linked device shows the same set of labels.
 *
 * <p>Each label is indexed by its identifier through
 * {@link LabelEditActionArgs} and is replicated via the
 * {@link SyncPatchType#REGULAR} collection.
 */
@ProtobufMessage(name = "SyncActionValue.LabelEditAction")
public final class LabelEditAction implements SyncAction<LabelEditActionArgs> {
    /**
     * The canonical action name {@code "label_edit"} used to identify this
     * action inside a sync patch.
     */
    public static final String ACTION_NAME = "label_edit";

    /**
     * The canonical action version for this action type.
     */
    public static final int ACTION_VERSION = 3;

    /**
     * The sync collection this action is carried in.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR;

    /**
     * Returns the canonical action name {@code "label_edit"}.
     *
     * @return the string {@code "label_edit"}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the action version declared by this action type.
     *
     * @return the action version
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * The display name of the label as it appears in the user interface.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String name;

    /**
     * The colour index assigned to the label, resolved by the client into a
     * concrete colour swatch.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    Integer color;

    /**
     * The predefined list identifier when the label represents a built-in
     * filter rather than a user-created label.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    Integer predefinedId;

    /**
     * Whether the label has been deleted and should no longer be displayed.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    Boolean deleted;

    /**
     * The position of the label within the ordered list shown to the user.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT32)
    Integer orderIndex;

    /**
     * Whether the label is currently active and should appear in the list of
     * filters.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    Boolean isActive;

    /**
     * The kind of list this entry represents, selecting between custom user
     * labels and the various predefined filters.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.ENUM)
    ListType type;

    /**
     * Whether the label is immutable and must not be edited or deleted by
     * the user.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
    Boolean isImmutable;

    /**
     * The instant at which any mute applied to the chats under this label
     * expires.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant muteEndTimeMs;


    /**
     * Constructs a new {@code LabelEditAction}. Intended to be invoked by the
     * generated builder and by the protobuf deserializer.
     *
     * @param name          the display name, or {@code null}
     * @param color         the colour index, or {@code null}
     * @param predefinedId  the predefined list identifier, or {@code null}
     * @param deleted       whether the label is deleted, or {@code null}
     * @param orderIndex    the ordering position, or {@code null}
     * @param isActive      whether the label is active, or {@code null}
     * @param type          the kind of list, or {@code null}
     * @param isImmutable   whether the label is immutable, or {@code null}
     * @param muteEndTimeMs the instant at which the associated mute expires,
     *                      or {@code null}
     */
    LabelEditAction(String name, Integer color, Integer predefinedId, Boolean deleted, Integer orderIndex, Boolean isActive, ListType type, Boolean isImmutable, Instant muteEndTimeMs) {
        this.name = name;
        this.color = color;
        this.predefinedId = predefinedId;
        this.deleted = deleted;
        this.orderIndex = orderIndex;
        this.isActive = isActive;
        this.type = type;
        this.isImmutable = isImmutable;
        this.muteEndTimeMs = muteEndTimeMs;
    }

    /**
     * Returns the display name of the label.
     *
     * @return the name, or an empty {@link Optional} if none was provided
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the colour index assigned to the label.
     *
     * @return the colour index, or an empty {@link OptionalInt} if none was
     *         provided
     */
    public OptionalInt color() {
        return color == null ? OptionalInt.empty() : OptionalInt.of(color);
    }

    /**
     * Returns the predefined list identifier when the label is a built-in
     * filter.
     *
     * @return the predefined list identifier, or an empty
     *         {@link OptionalInt} if the label is not a predefined filter
     */
    public OptionalInt predefinedId() {
        return predefinedId == null ? OptionalInt.empty() : OptionalInt.of(predefinedId);
    }

    /**
     * Returns whether the label has been deleted, coalescing an absent value
     * to {@code false}.
     *
     * @return {@code true} if the label is deleted, otherwise {@code false}
     */
    public boolean deleted() {
        return deleted != null && deleted;
    }

    /**
     * Returns the position of the label within the ordered list shown to the
     * user.
     *
     * @return the ordering position, or an empty {@link OptionalInt} if none
     *         was provided
     */
    public OptionalInt orderIndex() {
        return orderIndex == null ? OptionalInt.empty() : OptionalInt.of(orderIndex);
    }

    /**
     * Returns whether the label is currently active, coalescing an absent
     * value to {@code false}.
     *
     * @return {@code true} if the label is active, otherwise {@code false}
     */
    public boolean isActive() {
        return isActive != null && isActive;
    }

    /**
     * Returns the kind of list this entry represents.
     *
     * @return the list kind, or an empty {@link Optional} if none was
     *         provided
     */
    public Optional<ListType> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns whether the label is immutable, coalescing an absent value to
     * {@code false}.
     *
     * @return {@code true} if the label is immutable, otherwise {@code false}
     */
    public boolean isImmutable() {
        return isImmutable != null && isImmutable;
    }

    /**
     * Returns the instant at which any mute applied through this label
     * expires.
     *
     * @return the mute end instant, or an empty {@link Optional} if none was
     *         provided
     */
    public Optional<Instant> muteEndTimeMs() {
        return Optional.ofNullable(muteEndTimeMs);
    }

    /**
     * Updates the display name of the label.
     *
     * @param name the new name, or {@code null} to clear it
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Updates the colour index assigned to the label.
     *
     * @param color the new colour index, or {@code null} to clear it
     */
    public void setColor(Integer color) {
        this.color = color;
    }

    /**
     * Updates the predefined list identifier of the label.
     *
     * @param predefinedId the new predefined list identifier, or {@code null}
     *                     to clear it
     */
    public void setPredefinedId(Integer predefinedId) {
        this.predefinedId = predefinedId;
    }

    /**
     * Updates whether the label has been deleted.
     *
     * @param deleted the new flag value, or {@code null} to clear the field
     */
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * Updates the ordering position of the label.
     *
     * @param orderIndex the new ordering position, or {@code null} to clear
     *                   it
     */
    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    /**
     * Updates whether the label is active.
     *
     * @param isActive the new flag value, or {@code null} to clear the field
     */
    public void setActive(Boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Updates the kind of list this entry represents.
     *
     * @param type the new list kind, or {@code null} to clear it
     */
    public void setType(ListType type) {
        this.type = type;
    }

    /**
     * Updates whether the label is immutable.
     *
     * @param isImmutable the new flag value, or {@code null} to clear the
     *                    field
     */
    public void setImmutable(Boolean isImmutable) {
        this.isImmutable = isImmutable;
    }

    /**
     * Updates the instant at which any mute applied through this label
     * expires.
     *
     * @param muteEndTimeMs the new mute end instant, or {@code null} to clear
     *                      it
     */
    public void setMuteEndTimeMs(Instant muteEndTimeMs) {
        this.muteEndTimeMs = muteEndTimeMs;
    }

    /**
     * Enumerates the kinds of list entries that a {@link LabelEditAction} can
     * describe, distinguishing user-defined labels from the built-in filters
     * WhatsApp exposes in the labels drawer.
     */
    @ProtobufEnum(name = "SyncActionValue.LabelEditAction.ListType")
    public static enum ListType {
        /**
         * No specific list kind.
         */
        NONE(0),
        /**
         * The predefined list that shows only unread chats.
         */
        UNREAD(1),
        /**
         * The predefined list that shows only group chats.
         */
        GROUPS(2),
        /**
         * The predefined list that shows chats marked as favourites.
         */
        FAVORITES(3),
        /**
         * A generic predefined list delivered by the server.
         */
        PREDEFINED(4),
        /**
         * A label created by the user.
         */
        CUSTOM(5),
        /**
         * The predefined list that shows community chats.
         */
        COMMUNITY(6),
        /**
         * A list whose membership is assigned by the server rather than by
         * the user.
         */
        SERVER_ASSIGNED(7),
        /**
         * The predefined list that shows chats with unsent drafts.
         */
        DRAFTED(8),
        /**
         * The predefined list that groups chats that have been handed off to
         * AI assistants.
         */
        AI_HANDOFF(9),
        /**
         * The predefined list that shows channels the user is following.
         */
        CHANNELS(10);

        /**
         * Constructs a new {@code ListType} with the given protobuf index.
         *
         * @param index the protobuf wire index that identifies this variant
         */
        ListType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index that identifies this variant.
         */
        final int index;

        /**
         * Returns the protobuf wire index that identifies this variant.
         *
         * @return the wire index
         */
        public int index() {
            return this.index;
        }
    }


}
