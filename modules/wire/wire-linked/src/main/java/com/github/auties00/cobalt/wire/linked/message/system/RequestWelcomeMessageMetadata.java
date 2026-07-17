package com.github.auties00.cobalt.wire.linked.message.system;

import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A system message carried inside a protocol message that requests the
 * delivery of a welcome message for a chat and provides enough context for
 * the server to pick the right variant.
 *
 * <p>When a user opens a conversation for the first time, WhatsApp can inject
 * an automated welcome entry (for example the standard "contact added"
 * notice or a business profile greeting). This request informs the server
 * whether the local conversation already has content, which influences the
 * decision to emit the welcome entry at all.
 */
@ProtobufMessage(name = "Message.RequestWelcomeMessageMetadata")
public final class RequestWelcomeMessageMetadata implements Message {
    /**
     * Describes the current local state of the chat for the purposes of
     * welcome-message generation.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    LocalChatState localChatState;


    /**
     * Constructs a new request welcome message metadata record.
     *
     * @param localChatState the current local chat state, may be {@code null}
     */
    RequestWelcomeMessageMetadata(LocalChatState localChatState) {
        this.localChatState = localChatState;
    }

    /**
     * Returns the current local state of the chat.
     *
     * @return an {@link Optional} containing the local chat state, or
     *         {@link Optional#empty()} if the state is not set
     */
    public Optional<LocalChatState> localChatState() {
        return Optional.ofNullable(localChatState);
    }

    /**
     * Sets the current local state of the chat.
     *
     * @param localChatState the new local chat state, or {@code null} to clear it
     */
    public void setLocalChatState(LocalChatState localChatState) {
        this.localChatState = localChatState;
    }

    /**
     * Enumerates the possible local states of a chat when requesting a
     * welcome message.
     */
    @ProtobufEnum(name = "Message.RequestWelcomeMessageMetadata.LocalChatState")
    public static enum LocalChatState {
        /**
         * The chat contains no local messages.
         */
        EMPTY(0),
        /**
         * The chat already contains at least one local message.
         */
        NON_EMPTY(1);

        /**
         * Constructs a new enum constant with the given protobuf index.
         *
         * @param index the protobuf wire index for this constant
         */
        LocalChatState(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index associated with this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index associated with this constant.
         *
         * @return the protobuf wire index
         */
        public int index() {
            return this.index;
        }
    }
}
