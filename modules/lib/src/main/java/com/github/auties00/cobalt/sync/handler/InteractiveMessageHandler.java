package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.InteractiveMessageState;
import com.github.auties00.cobalt.wire.linked.chat.InteractiveMessageStateBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.InteractiveMessageAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.InteractiveMessageAction.InteractiveMessageActionMode;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.InteractiveMessageActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppChatStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Collection;

/**
 * Applies the {@code interactive_message_action} app-state sync action that
 * disables the call-to-action on a Galaxy interactive message.
 *
 * <p>The action fans out across the {@link SyncPatchType#REGULAR_LOW}
 * collection so companions stop offering the action once the primary device
 * hides a CTA. The mutation index is the standard message-keyed shape with an
 * extra {@code subId} slot for the per-CTA discriminator, formatted as
 * {@snippet :
 *     ["interactive_message_action", chatJid, messageId, fromMe, participant, subId]
 * }
 *
 * @implNote
 * This implementation persists the per-CTA state through
 * {@link LinkedWhatsAppBusinessStore#putInteractiveMessageState(InteractiveMessageState)}
 * keyed by {@code agmId|<id>}, {@code messageId|<id>} and the full composite
 * index, resolving the chat directly via
 * {@link LinkedWhatsAppChatStore#findChatByJid(com.github.auties00.cobalt.wire.core.jid.JidProvider)}
 * and the message via
 * {@link LinkedWhatsAppChatStore#findMessageById(com.github.auties00.cobalt.wire.linked.chat.Chat, String)}
 * rather than through a batched resolution cache.
 */
@WhatsAppWebModule(moduleName = "WAWebInteractiveMessageSync")
public final class InteractiveMessageHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link InteractiveMessageHandler}.
     */
    private static final System.Logger LOGGER = Log.get(InteractiveMessageHandler.class);

    /**
     * Constructs a new singleton {@link InteractiveMessageHandler}.
     */
    @WhatsAppWebExport(moduleName = "WAWebInteractiveMessageSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public InteractiveMessageHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebInteractiveMessageSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return InteractiveMessageAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebInteractiveMessageSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return InteractiveMessageAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebInteractiveMessageSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return InteractiveMessageAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Rejects non-{@link SyncdOperation#SET} operations as
     * {@link MutationApplicationResult#unsupported()}, a short or empty index
     * as malformed, and an absent chat as
     * {@link MutationApplicationResult#orphan(String, String)} with model type
     * {@code "Msg"}. When the action carries an {@code agmId} the AGM state is
     * recorded first; when the message is found and the action is
     * {@link InteractiveMessageActionMode#DISABLE_CTA} the per-message and
     * per-composite-index entries are written and a
     * {@link MutationApplicationResult#success()} is returned. A present
     * message with a non-{@link InteractiveMessageActionMode#DISABLE_CTA}
     * action yields {@link MutationApplicationResult#skipped()}.
     *
     * @implNote
     * This implementation records the {@code agmId} state before the message
     * lookup so that an absent message still publishes the AGM mapping; a
     * {@code null} {@code agmId} combined with an absent message yields an
     * orphan result rather than a success.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebInteractiveMessageSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        try {
            if (mutation.operation() != SyncdOperation.SET) {
                return MutationApplicationResult.unsupported();
            }

            var indexArray = JSON.parseArray(mutation.index());
            if (indexArray.size() < 6) {
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            var chatJidString = indexArray.getString(1);
            var messageId = indexArray.getString(2);
            var fromMeString = indexArray.getString(3);
            var participantString = indexArray.getString(4);
            var subIdString = indexArray.getString(5);

            if (isNullOrEmpty(chatJidString) || isNullOrEmpty(messageId)
                    || isNullOrEmpty(fromMeString) || isNullOrEmpty(participantString)
                    || isNullOrEmpty(subIdString)) {
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof InteractiveMessageAction action)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "interactive message mutation has malformed action value, id={0}", messageId);
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }

            var incomingMsgKey = SyncdIndexUtils.syncKeyToMsgKey(
                    client.store(), chatJidString, messageId, fromMeString, participantString
            );
            if (incomingMsgKey.isEmpty()) {
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            var chatJid = Jid.of(chatJidString);
            var localChat = client.store().chatStore().findChatByJid(chatJid);

            var agmId = action.agmId().orElse(null);

            if (localChat.isEmpty()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "interactive message action orphaned, chat {0} not found", chatJid);
                return MutationApplicationResult.orphan(
                        SyncdIndexUtils.serializeMessageKey(incomingMsgKey.get()),
                        "Msg"
                );
            }

            var maybeMessage = client.store().chatStore().findMessageById(localChat.get(), messageId);

            if (agmId != null) {
                client.store().businessStore().putInteractiveMessageState(new InteractiveMessageStateBuilder()
                        .messageId("agmId|" + agmId)
                        .type(action.type())
                        .agmId(action.agmId().orElse(null))
                        .build());
            }

            if (maybeMessage.isEmpty()) {
                if (agmId != null) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "interactive message agm state recorded without message, agmId={0}", agmId);
                    return MutationApplicationResult.success();
                }

                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "interactive message action orphaned, message {0} not found in chat {1}", messageId, chatJid);
                return MutationApplicationResult.orphan(
                        SyncdIndexUtils.serializeMessageKey(incomingMsgKey.get()),
                        "Msg"
                );
            }

            var chatMessage = maybeMessage.get();

            if (action.type() != InteractiveMessageActionMode.DISABLE_CTA) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "interactive message action skipped, mode={0}", action.type());
                return MutationApplicationResult.skipped();
            }

            var messageKeyId = chatMessage.key().id().orElse(messageId);
            client.store().businessStore().putInteractiveMessageState(new InteractiveMessageStateBuilder()
                    .messageId("messageId|" + messageKeyId)
                    .type(action.type())
                    .agmId(action.agmId().orElse(null))
                    .build());
            client.store().businessStore().putInteractiveMessageState(new InteractiveMessageStateBuilder()
                    .messageId("%s|%s|%s|%s|%s".formatted(chatJidString, messageId, fromMeString, participantString, subIdString))
                    .type(action.type())
                    .agmId(action.agmId().orElse(null))
                    .build());
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "interactive message cta disabled for message {0} in chat {1}", messageKeyId, chatJid);
            return MutationApplicationResult.success();
        } catch (Exception e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "interactive message mutation failed", e);
            return MutationApplicationResult.failed();
        }
    }

    /**
     * Builds the {@link InteractiveMessageAction} payload that callers embed
     * inside an outgoing pending mutation.
     *
     * <p>Sets the action {@code type} and, when {@code agmId} is non-{@code
     * null}, the AGM identifier; the chat-jid resolution, value wrapping and
     * signing are performed by the outgoing-mutation pipeline rather than here.
     *
     * @param type  the interactive-message action mode, normally
     *              {@link InteractiveMessageActionMode#DISABLE_CTA}
     * @param agmId the optional Galaxy AGM identifier; pass {@code null} when
     *              the action is not bound to a specific AGM
     * @return a freshly built {@link InteractiveMessageAction}
     */
    @WhatsAppWebExport(moduleName = "WAWebInteractiveMessageSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public InteractiveMessageAction buildDisableCTAAction(InteractiveMessageActionMode type, String agmId) {
        var builder = new InteractiveMessageActionBuilder().type(type);
        if (agmId != null) {
            builder.agmId(agmId);
        }
        return builder.build();
    }

    /**
     * Returns whether the given string is {@code null} or empty.
     *
     * @param value the candidate string
     * @return {@code true} when {@code value} is {@code null} or has length
     *         zero
     */
    private static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

    /**
     * Returns the per-AGM and per-message {@link InteractiveMessageState}
     * entries currently tracked in the store.
     *
     * <p>Reads the live, immutable view recorded by
     * {@link #applyMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)} so test
     * suites can inspect the handler's effect without holding a direct
     * reference to the store.
     *
     * @param client the {@link LinkedWhatsAppClient} whose store should be read
     * @return the live, immutable view of the store's interactive message
     *         states
     */
    public Collection<InteractiveMessageState> interactiveMessageStates(LinkedWhatsAppClient client) {
        return client.store().businessStore().interactiveMessageStates();
    }
}
