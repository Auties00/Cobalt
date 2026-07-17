package com.github.auties00.cobalt.wire.linked.chat.group;

import com.github.auties00.cobalt.wire.linked.message.system.history.MessageHistoryBundle;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Tracks the processing state of a group history message bundle for a specific
 * message.
 *
 * <p>When a new member joins a group that has the "share group history" feature
 * enabled, existing members send a history bundle containing recent messages so
 * the new participant can see prior conversation context. This class records
 * whether such a bundle has been received and processed (injected into the local
 * message store), and retains a reference to the bundle payload itself.
 *
 * <p>The {@link #processState()} field tracks the injection lifecycle: from
 * {@link ProcessState#NOT_INJECTED} (not yet processed) through
 * {@link ProcessState#INJECTED} (fully processed) or one of the failure states.
 * The deprecated bundle reference is maintained for backward compatibility with
 * older history bundle formats.
 *
 * @see MessageHistoryBundle
 */
@ProtobufMessage(name = "GroupHistoryBundleInfo")
public final class GroupHistoryBundleInfo {
    /**
     * The deprecated message history bundle payload, or {@code null} if not
     * present. Retained for backward compatibility with older bundle formats.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageHistoryBundle deprecatedMessageHistoryBundle;

    /**
     * The current processing state of the history bundle injection, or
     * {@code null} if the state has not been set.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    ProcessState processState;


    /**
     * Constructs a new {@code GroupHistoryBundleInfo} with the specified bundle
     * and processing state.
     *
     * @param deprecatedMessageHistoryBundle the deprecated history bundle, or
     *                                       {@code null} if not available
     * @param processState                   the current processing state, or
     *                                       {@code null} if not yet determined
     */
    GroupHistoryBundleInfo(MessageHistoryBundle deprecatedMessageHistoryBundle, ProcessState processState) {
        this.deprecatedMessageHistoryBundle = deprecatedMessageHistoryBundle;
        this.processState = processState;
    }

    /**
     * Returns the deprecated message history bundle payload, if present.
     *
     * @return an {@code Optional} containing the bundle, or empty if not
     *         available
     */
    public Optional<MessageHistoryBundle> deprecatedMessageHistoryBundle() {
        return Optional.ofNullable(deprecatedMessageHistoryBundle);
    }

    /**
     * Returns the current processing state of the history bundle injection,
     * if set.
     *
     * @return an {@code Optional} containing the process state, or empty if
     *         not yet determined
     */
    public Optional<ProcessState> processState() {
        return Optional.ofNullable(processState);
    }

    /**
     * Sets the deprecated message history bundle payload.
     *
     * @param deprecatedMessageHistoryBundle the bundle to set, or {@code null}
     *                                       to clear
     */
    public void setDeprecatedMessageHistoryBundle(MessageHistoryBundle deprecatedMessageHistoryBundle) {
        this.deprecatedMessageHistoryBundle = deprecatedMessageHistoryBundle;
    }

    /**
     * Sets the processing state of the history bundle injection.
     *
     * @param processState the state to set, or {@code null} to clear
     */
    public void setProcessState(ProcessState processState) {
        this.processState = processState;
    }

    /**
     * Represents the processing lifecycle states for a group history bundle
     * injection.
     *
     * <p>When a history bundle is received, the client processes (injects)
     * its messages into the local message store. This enum tracks whether
     * the injection has not yet occurred, completed successfully (fully or
     * partially), or failed.
     */
    @ProtobufEnum(name = "GroupHistoryBundleInfo.ProcessState")
    public static enum ProcessState {
        /**
         * The history bundle has not yet been injected into the local message
         * store.
         */
        NOT_INJECTED(0),

        /**
         * The history bundle has been fully injected into the local message
         * store.
         */
        INJECTED(1),

        /**
         * The history bundle was only partially injected. Some messages from
         * the bundle were processed, but not all.
         */
        INJECTED_PARTIAL(2),

        /**
         * The injection failed, but a retry may succeed.
         */
        INJECTION_FAILED(3),

        /**
         * The injection failed permanently and should not be retried.
         */
        INJECTION_FAILED_NO_RETRY(4);

        /**
         * Constructs a {@code ProcessState} with the given protobuf index.
         *
         * @param index the protobuf enum index
         */
        ProcessState(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf-assigned numeric index for this process state.
         */
        final int index;

        /**
         * Returns the protobuf-assigned numeric index for this process state.
         *
         * @return the protobuf enum index
         */
        public int index() {
            return this.index;
        }
    }
}
