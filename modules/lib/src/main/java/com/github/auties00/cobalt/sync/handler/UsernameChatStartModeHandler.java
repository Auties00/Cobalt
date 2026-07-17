package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorHandler;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.UsernameChatStartModeAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Mirrors the user's preferred addressing scheme for chats started from a username (LID versus
 * phone number).
 *
 * <p>The sync dispatcher would route incoming {@code usernameChatStartMode} mutations here if the
 * server ever emits one. The handler persists the
 * {@link UsernameChatStartModeAction.ChatStartMode} value through
 * {@link LinkedWhatsAppStore#setUsernameChatStartMode(UsernameChatStartModeAction.ChatStartMode)}
 * so any chat the user opens from the username discovery surface uses the preferred identifier.
 *
 * @implNote
 * This implementation is forward-looking. WA Web defines the {@code UsernameChatStartModeAction}
 * protobuf at action index 59 with name {@code "usernameChatStartMode"} and the
 * {@code USERNAME_CHAT_START_MODE -> REGULAR} collection-router branch, but the bundle ships no
 * concrete sync module, so WA Web's dispatcher would currently drop any incoming mutation with this
 * action as unsupported. Cobalt's handler ingests it today so the wire payload is not lost.
 */
public final class UsernameChatStartModeHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link UsernameChatStartModeHandler}.
     */
    private static final System.Logger LOGGER = Log.get(UsernameChatStartModeHandler.class);

    /**
     * Constructs the handler.
     *
     * <p>The handler is stateless; Cobalt's sync registry holds a single instance per client.
     */
    public UsernameChatStartModeHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String actionName() {
        return UsernameChatStartModeAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns {@link SyncPatchType#REGULAR} as declared by WA Web's
     * {@code USERNAME_CHAT_START_MODE -> REGULAR} collection-router branch.
     */
    @Override
    public SyncPatchType collectionName() {
        return SyncPatchType.REGULAR;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns {@link UsernameChatStartModeAction#ACTION_VERSION}; WA Web has no
     * concrete sync module, so the value is the initial protobuf version and should be revisited
     * once WA Web ships the matching handler.
     */
    @Override
    public int version() {
        return UsernameChatStartModeAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Non-{@link SyncdOperation#SET} operations are {@link MutationApplicationResult#unsupported()}.
     * The decoded value must be a {@link UsernameChatStartModeAction} with a populated
     * {@link UsernameChatStartModeAction#chatStartMode()}, otherwise the mutation is
     * {@link MutationApplicationResult#malformed()}. The resolved enum is written into
     * {@link LinkedWhatsAppStore#setUsernameChatStartMode(UsernameChatStartModeAction.ChatStartMode)}.
     *
     * @implNote
     * This implementation follows the canonical shape used by sibling handlers because WA Web ships
     * no concrete module to mirror. Sibling WA Web handlers wrap their body in a try/catch that maps
     * any throw to a failed result; Cobalt instead lets exceptions propagate to the configured
     * {@link WhatsAppLinkedClientErrorHandler} per the Cobalt error
     * model.
     */
    @Override
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof UsernameChatStartModeAction action)
                || action.chatStartMode().isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "username chat start mode: malformed mutation");
            return MutationApplicationResult.malformed();
        }

        client.store().settingsStore().setUsernameChatStartMode(action.chatStartMode().get());
        if (Log.DEBUG)
            LOGGER.log(Level.DEBUG, "username chat start mode: set mode={0}", action.chatStartMode().get());

        return MutationApplicationResult.success();
    }
}
