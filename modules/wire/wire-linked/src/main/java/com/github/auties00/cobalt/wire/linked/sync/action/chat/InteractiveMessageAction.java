package com.github.auties00.cobalt.wire.linked.sync.action.chat;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Sync action that records a user's interaction with an interactive message
 * element, such as disabling a call-to-action button on a template message.
 *
 * <p>Interactive messages (buttons, list pickers, CTAs, ...) can have
 * per-user state that needs to propagate across linked devices: for example
 * a CTA button that has been tapped once should appear disabled everywhere.
 * This action captures the {@link InteractiveMessageActionMode mode} of the
 * interaction together with the agent-generated message identifier that
 * identifies the specific interactive element inside the message.
 */
@ProtobufMessage(name = "SyncActionValue.InteractiveMessageAction")
public final class InteractiveMessageAction implements SyncAction<InteractiveMessageActionArgs> {
    /**
     * Canonical action name written as the first element of the sync index.
     */
    public static final String ACTION_NAME = "interactive_message_action";

    /**
     * Schema version declared by this action.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * Collection this action is stored in when encoded into an app state patch.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

    /**
     * Returns the canonical action name for this action type.
     *
     * @return the string {@code "interactive_message_action"}
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
     * Type of interaction recorded by this action.
     *
     * <p>This field is mandatory; constructing the message with a
     * {@code null} type throws a {@link NullPointerException}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    InteractiveMessageActionMode type;

    /**
     * Agent-generated message identifier of the interactive element within
     * the source message (for example the specific CTA button that was
     * tapped).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String agmId;

    /**
     * Constructs a new {@code InteractiveMessageAction}.
     *
     * @param type  the interaction mode (must not be {@code null})
     * @param agmId the agent-generated message identifier, or {@code null}
     *              if not applicable
     * @throws NullPointerException if {@code type} is {@code null}
     */
    InteractiveMessageAction(InteractiveMessageActionMode type, String agmId) {
        this.type = Objects.requireNonNull(type);
        this.agmId = agmId;
    }

    /**
     * Returns the interaction mode recorded by this action.
     *
     * @return the {@link InteractiveMessageActionMode}
     */
    public InteractiveMessageActionMode type() {
        return type;
    }

    /**
     * Returns the agent-generated message identifier targeted by this
     * interaction.
     *
     * @return an {@link Optional} containing the identifier, or an
     *         empty {@code Optional} when no identifier was set
     */
    public Optional<String> agmId() {
        return Optional.ofNullable(agmId);
    }

    /**
     * Sets the interaction mode recorded by this action.
     *
     * @param type the new interaction mode
     */
    public void setType(InteractiveMessageActionMode type) {
        this.type = type;
    }

    /**
     * Sets the agent-generated message identifier targeted by this
     * interaction.
     *
     * @param agmId the new identifier, or {@code null} to clear it
     */
    public void setAgmId(String agmId) {
        this.agmId = agmId;
    }

    /**
     * Enumeration of the interaction modes that can be recorded inside an
     * {@link InteractiveMessageAction}.
     */
    @ProtobufEnum(name = "SyncActionValue.InteractiveMessageAction.InteractiveMessageActionMode")
    public static enum InteractiveMessageActionMode {
        /**
         * Indicates that a call-to-action button on the source message has
         * been disabled (typically because the user already tapped it).
         */
        DISABLE_CTA(1);

        /**
         * Constructs an enum constant with the given protobuf index.
         *
         * @param index the protobuf wire value for this constant
         */
        InteractiveMessageActionMode(@ProtobufEnumIndex int index) {
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
