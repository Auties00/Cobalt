package com.github.auties00.cobalt.wire.linked.sync.action.business;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A sync action describing the state of a business broadcast list.
 *
 * <p>Business broadcast lists are named groups of recipients that the account can target
 * together. This action captures a complete snapshot of the list: whether it has been deleted,
 * the display name chosen by the operator, the full set of participants as
 * {@link BroadcastListParticipantAction} entries, the set of labels associated with the list,
 * and an optional audience expression that encodes the predicate used to pick members
 * automatically (for example a label-based query). Every linked device consumes this action to
 * keep the list definition in sync.
 *
 * <p>This action is transported in the {@code REGULAR} sync collection and keyed by the
 * broadcast list identifier through {@link BusinessBroadcastListActionArgs}.
 */
@ProtobufMessage(name = "SyncActionValue.BusinessBroadcastListAction")
public final class BusinessBroadcastListAction implements SyncAction<BusinessBroadcastListActionArgs> {
    /**
     * The canonical action name used when encoding this action inside a sync patch index.
     */
    public static final String ACTION_NAME = "business_broadcast_list";

    /**
     * The action version negotiated with the server for this action type.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * The sync patch collection that carries this action between devices.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR;

    /**
     * Returns the canonical action name for this action type.
     *
     * @return the action name
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the action version for this action type.
     *
     * @return the action version
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Flag indicating whether the broadcast list has been deleted by the operator.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean deleted;

    /**
     * The ordered list of participants currently in the broadcast list.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<BroadcastListParticipantAction> participants;

    /**
     * The operator-chosen display name of the broadcast list.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String listName;

    /**
     * The identifiers of the labels attached to this broadcast list, used to group lists by topic.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    List<String> labelIds;

    /**
     * The compiled audience expression that selects which contacts receive this broadcast list.
     *
     * <p>The expression encodes the user-authored audience query as a boolean expression over
     * labels and contact attributes. The resolved form is persisted alongside the participant
     * snapshot so that the same selection can be reproduced on any linked device.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String audienceExpression;


    /**
     * Creates a broadcast list action with all fields populated.
     *
     * @param deleted            whether the list has been deleted
     * @param participants       the list of participants, or {@code null}
     * @param listName           the display name of the list, or {@code null}
     * @param labelIds           the identifiers of attached labels, or {@code null}
     * @param audienceExpression the compiled audience expression, or {@code null}
     */
    BusinessBroadcastListAction(Boolean deleted, List<BroadcastListParticipantAction> participants, String listName, List<String> labelIds, String audienceExpression) {
        this.deleted = deleted;
        this.participants = participants;
        this.listName = listName;
        this.labelIds = labelIds;
        this.audienceExpression = audienceExpression;
    }

    /**
     * Returns whether this action represents a deleted broadcast list.
     *
     * @return {@code true} if the list has been deleted, {@code false} otherwise
     */
    public boolean deleted() {
        return deleted != null && deleted;
    }

    /**
     * Returns the participants of this broadcast list as an unmodifiable view.
     *
     * @return the unmodifiable list of participants, or an empty list if none are set
     */
    public List<BroadcastListParticipantAction> participants() {
        return participants == null ? List.of() : Collections.unmodifiableList(participants);
    }

    /**
     * Returns the operator-chosen display name of the broadcast list.
     *
     * @return the display name, or {@link Optional#empty()} if not set
     */
    public Optional<String> listName() {
        return Optional.ofNullable(listName);
    }

    /**
     * Returns the identifiers of the labels attached to this broadcast list as an unmodifiable view.
     *
     * @return the unmodifiable list of label identifiers, or an empty list if none are attached
     */
    public List<String> labelIds() {
        return labelIds == null ? List.of() : Collections.unmodifiableList(labelIds);
    }

    /**
     * Returns the compiled audience expression for this broadcast list.
     *
     * <p>The audience expression encodes the user-authored predicate (for example label
     * membership combined with contact attributes) that determines which contacts should be
     * included in the broadcast. The resolved expression is persisted so the same selection
     * can be reproduced on linked devices.
     *
     * @return the audience expression, or {@link Optional#empty()} if none was supplied
     */
    public Optional<String> audienceExpression() {
        return Optional.ofNullable(audienceExpression);
    }

    /**
     * Updates the deletion flag of this broadcast list.
     *
     * @param deleted {@code true} to mark the list as deleted, {@code false} or {@code null} otherwise
     */
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * Updates the participants of this broadcast list.
     *
     * @param participants the new participants, or {@code null} to clear them
     */
    public void setParticipants(List<BroadcastListParticipantAction> participants) {
        this.participants = participants;
    }

    /**
     * Updates the display name of this broadcast list.
     *
     * @param listName the new name, or {@code null} to clear it
     */
    public void setListName(String listName) {
        this.listName = listName;
    }

    /**
     * Updates the attached label identifiers.
     *
     * @param labelIds the new label identifiers, or {@code null} to clear them
     */
    public void setLabelIds(List<String> labelIds) {
        this.labelIds = labelIds;
    }

    /**
     * Updates the compiled audience expression for this broadcast list.
     *
     * @param audienceExpression the new audience expression, or {@code null} to clear it
     */
    public void setAudienceExpression(String audienceExpression) {
        this.audienceExpression = audienceExpression;
    }


}
