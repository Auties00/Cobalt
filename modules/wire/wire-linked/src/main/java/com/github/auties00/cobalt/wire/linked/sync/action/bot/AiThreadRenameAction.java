package com.github.auties00.cobalt.wire.linked.sync.action.bot;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A sync action that renames an existing AI conversation thread.
 *
 * <p>AI threads are the individual chat sessions that a user maintains with a
 * WhatsApp AI bot such as Meta AI. Each thread can be given a custom title
 * that replaces the default auto-generated name. When the user renames a
 * thread on one device, this action is replicated to every linked device so
 * that the new title appears consistently across the account.
 *
 * <p>The action is scoped to a specific bot and thread through its
 * {@link AiThreadRenameActionArgs companion index arguments}, and the new
 * title is carried by the {@link #newTitle()} property. An empty or missing
 * title is interpreted as a request to revert the thread to its default name.
 */
@ProtobufMessage(name = "SyncActionValue.AiThreadRenameAction")
public final class AiThreadRenameAction implements SyncAction<AiThreadRenameActionArgs> {
    /**
     * The canonical action name used to identify this sync action on the wire.
     */
    public static final String ACTION_NAME = "ai_thread_rename";

    /**
     * The canonical action version negotiated by the WhatsApp sync protocol
     * for this action type.
     */
    public static final int ACTION_VERSION = 7;

    /**
     * The sync patch collection that stores this action on the server.
     *
     * <p>AI thread renames are persisted in the low-priority regular
     * collection, since they are user-visible but not latency-sensitive.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

    /**
     * Returns the canonical action name used to identify this sync action.
     *
     * @return the action name string
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the canonical action version negotiated for this sync action.
     *
     * @return the action version integer
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * The new title to apply to the AI thread, or {@code null} to reset the
     * thread back to its default auto-generated name.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String newTitle;


    /**
     * Constructs a new {@code AiThreadRenameAction} carrying the supplied
     * title.
     *
     * @param newTitle the new thread title, or {@code null} to reset the
     *                 title to its default value
     */
    AiThreadRenameAction(String newTitle) {
        this.newTitle = newTitle;
    }

    /**
     * Returns the new title that this action requests to apply to the AI
     * thread.
     *
     * <p>An empty {@link Optional} indicates that the action does not carry a
     * title, which is typically interpreted as a request to revert the thread
     * to its default auto-generated name.
     *
     * @return an {@link Optional} containing the new thread title, or
     *         {@link Optional#empty()} if no title was provided
     */
    public Optional<String> newTitle() {
        return Optional.ofNullable(newTitle);
    }

    /**
     * Updates the thread title carried by this action.
     *
     * @param newTitle the new thread title, or {@code null} to clear the
     *                 field and request a reset to the default name
     */
    public void setNewTitle(String newTitle) {
        this.newTitle = newTitle;
    }


}
