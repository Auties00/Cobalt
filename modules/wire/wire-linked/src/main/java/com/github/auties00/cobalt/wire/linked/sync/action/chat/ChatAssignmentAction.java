package com.github.auties00.cobalt.wire.linked.sync.action.chat;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Sync action that assigns a chat to a specific WhatsApp Business agent.
 *
 * <p>WhatsApp Business accounts can route incoming conversations to different
 * agents (e.g. in a customer-support team). This action records that a chat
 * has been assigned to a particular agent identified by a device-agent ID and
 * replicates the assignment across the business owner's linked devices.
 *
 * <p>A {@code null} or empty agent ID is interpreted as unassigning the chat.
 */
@ProtobufMessage(name = "SyncActionValue.ChatAssignmentAction")
public final class ChatAssignmentAction implements SyncAction<ChatAssignmentActionArgs> {
    /**
     * Canonical action name written as the first element of the sync index.
     */
    public static final String ACTION_NAME = "agentChatAssignment";

    /**
     * Schema version declared by this action.
     */
    public static final int ACTION_VERSION = 7;

    /**
     * Collection this action is stored in when encoded into an app state patch.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR;

    /**
     * Returns the canonical action name for this action type.
     *
     * @return the string {@code "agentChatAssignment"}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version of this action type.
     *
     * @return the integer value {@code 7}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Identifier of the device-agent the chat has been assigned to.
     *
     * <p>A {@code null} value typically represents an unassignment.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String deviceAgentID;


    /**
     * Constructs a new {@code ChatAssignmentAction} with the given agent ID.
     *
     * @param deviceAgentID the device-agent identifier, or {@code null} to unassign
     */
    ChatAssignmentAction(String deviceAgentID) {
        this.deviceAgentID = deviceAgentID;
    }

    /**
     * Returns the device-agent identifier this chat is assigned to.
     *
     * @return an {@link Optional} containing the agent identifier, or an empty
     *         {@code Optional} when the chat is unassigned
     */
    public Optional<String> deviceAgentID() {
        return Optional.ofNullable(deviceAgentID);
    }

    /**
     * Sets the device-agent identifier this chat is assigned to.
     *
     * @param deviceAgentID the new agent identifier, or {@code null} to unassign
     */
    public void setDeviceAgentID(String deviceAgentID) {
        this.deviceAgentID = deviceAgentID;
    }


}
