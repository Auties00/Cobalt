package com.github.auties00.cobalt.wire.linked.sync.action.chat;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Sync action that records whether an assigned WhatsApp Business agent has
 * opened a chat.
 *
 * <p>This complements {@link ChatAssignmentAction} by tracking, per-agent,
 * whether the conversation has been viewed. It allows a business account to
 * know that an agent has looked at an assigned chat so that UI indicators,
 * metrics and routing logic stay consistent across the business owner's
 * linked devices.
 */
@ProtobufMessage(name = "SyncActionValue.ChatAssignmentOpenedStatusAction")
public final class ChatAssignmentOpenedStatusAction implements SyncAction<ChatAssignmentOpenedStatusActionArgs> {
    /**
     * Canonical action name written as the first element of the sync index.
     */
    public static final String ACTION_NAME = "agentChatAssignmentOpenedStatus";

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
     * @return the string {@code "agentChatAssignmentOpenedStatus"}
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
     * Whether the assigned agent has opened the chat ({@code true}) or not
     * ({@code false}). {@code null} is treated as {@code false}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean chatOpened;


    /**
     * Constructs a new {@code ChatAssignmentOpenedStatusAction} with the
     * given opened flag.
     *
     * @param chatOpened whether the assigned agent has opened the chat
     */
    ChatAssignmentOpenedStatusAction(Boolean chatOpened) {
        this.chatOpened = chatOpened;
    }

    /**
     * Returns whether the assigned agent has opened the chat.
     *
     * <p>If the underlying field is {@code null} this method returns
     * {@code false}.
     *
     * @return {@code true} if the chat has been opened by the agent,
     *         {@code false} otherwise
     */
    public boolean chatOpened() {
        return chatOpened != null && chatOpened;
    }

    /**
     * Sets whether the assigned agent has opened the chat.
     *
     * @param chatOpened the new opened flag, or {@code null} to clear it
     */
    public void setChatOpened(Boolean chatOpened) {
        this.chatOpened = chatOpened;
    }


}
