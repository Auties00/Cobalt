package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.model.sync.ActionValueSync;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

/**
 * Interface for handling specific action types in app state sync.
 *
 * <p>Each implementation handles a specific type of mutation (e.g., archive, pin, mute)
 * and is responsible for:
 * <ul>
 *   <li>Applying mutations to local state</li>
 *   <li>Resolving conflicts between local and remote mutations</li>
 *   <li>Handling orphan cases when referenced entities don't exist</li>
 * </ul>
 */
public interface WebAppStateActionHandler {
    /**
     * Gets the action type name this handler processes.
     *
     * <p>This should match the field name in {@link ActionValueSync},
     * for example, "archiveChatAction", "pinAction", "starAction", etc.
     *
     * @return the action type name
     */
    String actionName();

    /**
     * Applies mutation to local state
     *
     * @param client   the WhatsAppClient instance linked to the mutation
     * @param mutation the mutation to apply
     * @return {@code true} if the mutation was applied successfully, {@code false} otherwise
     */
    boolean applyMutation(WhatsAppClient client, DecryptedMutation.Trusted mutation);
}
