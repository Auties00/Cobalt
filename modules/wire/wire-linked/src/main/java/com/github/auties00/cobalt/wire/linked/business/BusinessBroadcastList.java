package com.github.auties00.cobalt.wire.linked.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A model representing a single WhatsApp Business broadcast list (saved
 * audience).
 *
 * <p>A broadcast list is a reusable group of recipients that the business
 * targets with marketing messages — a lightweight one-to-many distribution
 * channel, distinct from regular group chats. Each list has a stable
 * {@linkplain #id() identifier}, a {@linkplain #deleted() deletion flag},
 * an ordered roster of {@linkplain #participants() participants}, an
 * operator-chosen {@linkplain #listName() display name}, the set of
 * {@linkplain #labelIds() business-label identifiers} attached to the
 * list, and the compiled {@linkplain #audienceExpression() audience
 * expression} that selects which contacts the list targets.
 *
 * <p>Cobalt persists each list independently so callers can resolve a
 * single list by id without scanning the entire library.
 *
 * <p>This class is a local model only. Modifying its fields does not send
 * any request to the WhatsApp servers; it simply reflects the locally
 * cached state.
 */
@ProtobufMessage
public final class BusinessBroadcastList {
    /**
     * The non-{@code null} stable identifier of the broadcast list. Used
     * as the primary key by Cobalt's store.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * The tombstone flag set to {@code true} when the operator has
     * deleted the list.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    boolean deleted;

    /**
     * The ordered list of participants currently in the broadcast list,
     * or {@code null} when no roster has been set.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    List<BroadcastListParticipant> participants;

    /**
     * The operator-chosen display name of the list, or {@code null} when
     * no name has been set.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String listName;

    /**
     * The identifiers of the business labels attached to this list, used
     * to group lists by topic, or {@code null} when no labels are
     * attached.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    List<String> labelIds;

    /**
     * The compiled audience expression selecting which contacts the list
     * targets, or {@code null} when no expression has been supplied. The
     * expression encodes the user-authored audience query as a boolean
     * expression over labels and contact attributes; it is persisted so
     * the same selection can be reproduced on every linked device.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String audienceExpression;

    /**
     * Constructs a new broadcast list with the given fields.
     *
     * @param id                 the non-{@code null} list identifier
     * @param deleted            whether the list is tombstoned
     * @param participants       the ordered participant list, or
     *                           {@code null}
     * @param listName           the display name, or {@code null}
     * @param labelIds           the attached label identifiers, or
     *                           {@code null}
     * @param audienceExpression the compiled audience expression, or
     *                           {@code null}
     */
    BusinessBroadcastList(String id, boolean deleted, List<BroadcastListParticipant> participants,
                          String listName, List<String> labelIds, String audienceExpression) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.deleted = deleted;
        this.participants = participants;
        this.listName = listName;
        this.labelIds = labelIds;
        this.audienceExpression = audienceExpression;
    }

    /**
     * Returns the non-{@code null} list identifier.
     *
     * @return the list identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns whether the list is tombstoned.
     *
     * @return {@code true} if the list has been deleted
     */
    public boolean deleted() {
        return deleted;
    }

    /**
     * Updates the deletion flag of this list.
     *
     * @param deleted the new flag
     * @return this list instance for method chaining
     */
    public BusinessBroadcastList setDeleted(boolean deleted) {
        this.deleted = deleted;
        return this;
    }

    /**
     * Returns an unmodifiable view of the participants currently in this
     * broadcast list, in roster order.
     *
     * @return an unmodifiable list of participants, empty when no roster
     *         has been set
     */
    public List<BroadcastListParticipant> participants() {
        return participants == null ? List.of() : Collections.unmodifiableList(participants);
    }

    /**
     * Updates the participant roster of this list.
     *
     * @param participants the new participant list, or {@code null} to
     *                     clear
     * @return this list instance for method chaining
     */
    public BusinessBroadcastList setParticipants(List<BroadcastListParticipant> participants) {
        this.participants = participants;
        return this;
    }

    /**
     * Returns the operator-chosen display name of the list.
     *
     * @return an {@code Optional} containing the display name, or empty
     *         if not set
     */
    public Optional<String> listName() {
        return Optional.ofNullable(listName);
    }

    /**
     * Updates the display name of the list.
     *
     * @param listName the new name, or {@code null} to clear
     * @return this list instance for method chaining
     */
    public BusinessBroadcastList setListName(String listName) {
        this.listName = listName;
        return this;
    }

    /**
     * Returns an unmodifiable view of the business-label identifiers
     * attached to this list.
     *
     * @return an unmodifiable list of label identifiers, empty when no
     *         labels are attached
     */
    public List<String> labelIds() {
        return labelIds == null ? List.of() : Collections.unmodifiableList(labelIds);
    }

    /**
     * Updates the attached business-label identifiers.
     *
     * @param labelIds the new label identifiers, or {@code null} to
     *                 clear
     * @return this list instance for method chaining
     */
    public BusinessBroadcastList setLabelIds(List<String> labelIds) {
        this.labelIds = labelIds;
        return this;
    }

    /**
     * Returns the compiled audience expression for this list.
     *
     * @return an {@code Optional} containing the audience expression, or
     *         empty if not set
     */
    public Optional<String> audienceExpression() {
        return Optional.ofNullable(audienceExpression);
    }

    /**
     * Updates the compiled audience expression of this list.
     *
     * @param audienceExpression the new expression, or {@code null} to
     *                           clear
     * @return this list instance for method chaining
     */
    public BusinessBroadcastList setAudienceExpression(String audienceExpression) {
        this.audienceExpression = audienceExpression;
        return this;
    }

    /**
     * Returns a hash code derived from this list's
     * {@linkplain #id() identifier}.
     *
     * @return the hash code of the list identifier
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /**
     * Returns whether this list is equal to the given object.
     *
     * <p>Two lists are considered equal when they share the same
     * {@linkplain #id() identifier}, regardless of the other fields.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a {@code BusinessBroadcastList}
     *         with the same identifier
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof BusinessBroadcastList that && Objects.equals(this.id, that.id);
    }
}
