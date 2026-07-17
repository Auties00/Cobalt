package com.github.auties00.cobalt.wire.linked.sync.action.chat;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Sync action that records the user's preferred chat-start mode when
 * starting a new conversation via a WhatsApp username.
 *
 * <p>WhatsApp identifiers can be resolved either to a Linked Identity (LID)
 * address or to the underlying phone number (PN). This action lets the user
 * express a global preference for the resolution that should happen when a
 * chat is initiated from a username, so that every linked device agrees on
 * which identifier is used to open the conversation.
 *
 * <p>The action carries no index arguments and uses
 * {@link SyncActionEmptyArgs} as its argument type, meaning the single
 * account-scoped setting is represented by an empty index.
 */
@ProtobufMessage(name = "SyncActionValue.UsernameChatStartModeAction")
public final class UsernameChatStartModeAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name written as the first element of the sync index.
     */
    public static final String ACTION_NAME = "usernameChatStartMode";

    /**
     * Schema version declared by this action.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * Returns the canonical action name for this action type.
     *
     * @return the string {@code "usernameChatStartMode"}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version of this action type.
     *
     * @return the integer value {@code 1}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * The preferred chat-start mode used when initiating a conversation
     * from a WhatsApp username.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    ChatStartMode chatStartMode;


    /**
     * Constructs a new {@code UsernameChatStartModeAction} with the given
     * mode.
     *
     * @param chatStartMode the chat-start mode
     */
    UsernameChatStartModeAction(ChatStartMode chatStartMode) {
        this.chatStartMode = chatStartMode;
    }

    /**
     * Returns the preferred chat-start mode.
     *
     * @return an {@link Optional} containing the {@link ChatStartMode}, or
     *         an empty {@code Optional} when no preference has been stored
     */
    public Optional<ChatStartMode> chatStartMode() {
        return Optional.ofNullable(chatStartMode);
    }

    /**
     * Sets the preferred chat-start mode.
     *
     * @param chatStartMode the new chat-start mode, or {@code null} to clear
     */
    public void setChatStartMode(ChatStartMode chatStartMode) {
        this.chatStartMode = chatStartMode;
    }

    /**
     * Enumeration of the chat-start modes supported by
     * {@link UsernameChatStartModeAction}.
     */
    @ProtobufEnum(name = "SyncActionValue.UsernameChatStartModeAction.ChatStartMode")
    public static enum ChatStartMode {
        /**
         * Indicates that chats started from a username should resolve to the
         * contact's Linked Identity (LID) address.
         */
        LID(1),
        /**
         * Indicates that chats started from a username should resolve to the
         * contact's phone-number (PN) address.
         */
        PN(2);

        /**
         * Constructs an enum constant with the given protobuf index.
         *
         * @param index the protobuf wire value for this constant
         */
        ChatStartMode(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Protobuf wire value associated with this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire value for this constant.
         *
         * @return the protobuf wire value
         */
        public int index() {
            return this.index;
        }
    }
}
