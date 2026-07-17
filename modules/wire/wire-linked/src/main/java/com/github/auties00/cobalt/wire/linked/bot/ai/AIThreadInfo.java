package com.github.auties00.cobalt.wire.linked.bot.ai;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents metadata about an AI conversation thread within a WhatsApp chat.
 *
 * <p>AI threads allow users to maintain separate, named conversations with Meta AI
 * within the same chat. Each thread is described by both
 * {@linkplain #serverInfo() server-side metadata} (such as the server-assigned
 * thread title) and {@linkplain #clientInfo() client-side metadata} (such as the
 * privacy type chosen by the user when the thread was created).
 *
 * <p>This type is embedded within the bot metadata of AI messages and is also
 * persisted in the threads metadata store for offline access.
 */
@ProtobufMessage(name = "AIThreadInfo")
public final class AIThreadInfo {
    /**
     * The server-side metadata for this AI thread. Currently contains the
     * server-assigned thread title, which may be automatically generated based
     * on the conversation content.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    AIThreadServerInfo serverInfo;

    /**
     * The client-side metadata for this AI thread, including the privacy type
     * selected by the user when the thread was created (such as default or
     * incognito mode).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    AIThreadClientInfo clientInfo;


    /**
     * Constructs a new {@code AIThreadInfo} with the specified values.
     *
     * @param serverInfo the server-side thread metadata, or {@code null} if unavailable
     * @param clientInfo the client-side thread metadata, or {@code null} if unavailable
     */
    AIThreadInfo(AIThreadServerInfo serverInfo, AIThreadClientInfo clientInfo) {
        this.serverInfo = serverInfo;
        this.clientInfo = clientInfo;
    }

    /**
     * Returns the server-side metadata for this AI thread, including the
     * server-assigned title.
     *
     * @return an {@code Optional} containing the server info, or an empty
     *         {@code Optional} if not set
     */
    public Optional<AIThreadServerInfo> serverInfo() {
        return Optional.ofNullable(serverInfo);
    }

    /**
     * Returns the client-side metadata for this AI thread, including the
     * privacy type.
     *
     * @return an {@code Optional} containing the client info, or an empty
     *         {@code Optional} if not set
     */
    public Optional<AIThreadClientInfo> clientInfo() {
        return Optional.ofNullable(clientInfo);
    }

    /**
     * Sets the server-side metadata for this AI thread.
     *
     * @param serverInfo the new server info, or {@code null} to clear
     */
    public void setServerInfo(AIThreadServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    /**
     * Sets the client-side metadata for this AI thread.
     *
     * @param clientInfo the new client info, or {@code null} to clear
     */
    public void setClientInfo(AIThreadClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    /**
     * Represents the client-side metadata for an AI conversation thread.
     *
     * <p>The client stores the privacy {@linkplain #type() type} chosen by the user
     * when the thread was created. This determines whether the conversation history
     * is retained and potentially used for AI personalization, or treated as
     * ephemeral (incognito mode).
     */
    @ProtobufMessage(name = "AIThreadInfo.AIThreadClientInfo")
    public static final class AIThreadClientInfo {
        /**
         * The privacy type of this AI thread, controlling whether the conversation
         * is retained for personalization or treated as ephemeral.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        AIThreadClientInfo.AIThreadType type;


        /**
         * Constructs a new {@code AIThreadClientInfo} with the specified privacy type.
         *
         * @param type the thread privacy type, or {@code null} if unspecified
         */
        AIThreadClientInfo(AIThreadType type) {
            this.type = type;
        }

        /**
         * Returns the privacy type of this AI thread.
         *
         * @return an {@code Optional} containing the thread type, or an empty
         *         {@code Optional} if not set
         */
        public Optional<AIThreadType> type() {
            return Optional.ofNullable(type);
        }

        /**
         * Sets the privacy type of this AI thread.
         *
         * @param type the new thread type, or {@code null} to clear
         */
        public void setType(AIThreadType type) {
            this.type = type;
    }

        /**
         * Enumerates the privacy types of an AI conversation thread.
         *
         * <p>The privacy type determines how the conversation history is handled
         * by the AI system, particularly whether it is retained for personalization
         * or discarded after the session ends.
         */
        @ProtobufEnum(name = "AIThreadInfo.AIThreadClientInfo.AIThreadType")
        public static enum AIThreadType {
            /**
             * The thread type is unknown or was not specified by the client.
             */
            UNKNOWN(0),

            /**
             * A standard AI thread where conversation history is retained by the
             * server and may be used to personalize future responses based on
             * prior interactions.
             */
            DEFAULT(1),

            /**
             * An incognito AI thread where conversation history is not persisted
             * on the server and is not used to train or personalize the AI model.
             * The conversation is treated as ephemeral once the session ends.
             */
            INCOGNITO(2);

            /**
             * Constructs an {@code AIThreadType} with the given protobuf index.
             *
             * @param index the protobuf index value
             */
            AIThreadType(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The protobuf index value associated with this enum constant.
             */
            final int index;

            /**
             * Returns the protobuf index value associated with this enum constant.
             *
             * @return the protobuf index
             */
            public int index() {
                return this.index;
            }
        }
    }

    /**
     * Represents the server-side metadata for an AI conversation thread.
     *
     * <p>The server maintains a {@linkplain #title() title} for each thread, which
     * may be automatically generated based on the conversation content or explicitly
     * set by the user. This title is displayed in the thread list and thread header
     * in the chat UI.
     */
    @ProtobufMessage(name = "AIThreadInfo.AIThreadServerInfo")
    public static final class AIThreadServerInfo {
        /**
         * The server-assigned title for this AI thread. This title is typically
         * auto-generated from the conversation content, for example
         * {@code "Trip planning to Rome"}.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String title;


        /**
         * Constructs a new {@code AIThreadServerInfo} with the specified title.
         *
         * @param title the server-assigned thread title, or {@code null} if not yet assigned
         */
        AIThreadServerInfo(String title) {
            this.title = title;
        }

        /**
         * Returns the server-assigned title for this AI thread.
         *
         * @return an {@code Optional} containing the title, or an empty
         *         {@code Optional} if the server has not assigned one
         */
        public Optional<String> title() {
            return Optional.ofNullable(title);
        }

        /**
         * Sets the server-assigned title for this AI thread.
         *
         * @param title the new title, or {@code null} to clear
         */
        public void setTitle(String title) {
            this.title = title;
    }
    }
}
