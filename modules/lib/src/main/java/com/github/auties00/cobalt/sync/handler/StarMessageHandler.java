package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.StarAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Toggles the starred flag on a single message in response to a cross-device
 * {@code star} mutation.
 *
 * <p>The sync dispatcher routes incoming {@code star} mutations here whenever
 * the user stars or unstars a message on another linked device. The handler
 * locates the matching message in the unified store and sets the starred flag
 * on it.
 */
@WhatsAppWebModule(moduleName = "WAWebStarMessageSync")
public final class StarMessageHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link StarMessageHandler}.
     */
    private static final System.Logger LOGGER = Log.get(StarMessageHandler.class);

    /**
     * Constructs the handler.
     *
     * <p>The handler is stateless; Cobalt's sync registry holds a single
     * instance per client.
     */
    @WhatsAppWebExport(moduleName = "WAWebStarMessageSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public StarMessageHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebStarMessageSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return StarAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebStarMessageSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return StarAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebStarMessageSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return StarAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A non-{@link SyncdOperation#SET} operation is reported as
     * {@link MutationApplicationResult#unsupported()}. The mutation index is
     * {@code ["star", chatJid, messageId, fromMe, participant]}; each of the four
     * trailing slots is required, and a missing slot, an unparseable chat JID, or
     * an absent {@link StarAction} value is reported as malformed. The message key
     * is rebuilt via {@link SyncdIndexUtils#syncKeyToMsgKey} for the orphan branch;
     * the message itself is otherwise looked up directly by chat JID and message
     * id, returning {@link MutationApplicationResult#orphan(String, String)} when
     * absent. The starred flag is then propagated through
     * {@link #starMessage(LinkedMessageInfo, boolean)}.
     *
     * @implNote
     * WA Web's two-tier message resolution (a
     * {@code WAWebSyncdResolveMessages.resolveMessagesForMutations} batch pre-pass
     * plus the {@code WAWebStarredMsgCollection} snapshot plus the
     * {@code WAWebDBProcessMessage.starMessages} persistence batch) is collapsed
     * into a single per-mutation store update because Cobalt's flattened store
     * keeps starred state directly on each message record; WA Web's
     * {@code WAWebAssociationProcessor.detachAssociatedMsg} call is dropped because
     * its in-memory reactive-collection bookkeeping has no Cobalt analogue. Any
     * thrown exception maps to {@link MutationApplicationResult#failed()}, mirroring
     * WA Web's inner {@code try/catch}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebStarMessageSync", exports = {"applyMutations", "getMessageKey"}, adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        try {
            var indexArray = JSON.parseArray(mutation.index());
            if (indexArray.size() < 5) {
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            var chatJidString = indexArray.getString(1);
            var messageId = indexArray.getString(2);
            var fromMeString = indexArray.getString(3);
            var participantString = indexArray.getString(4);
            if (chatJidString == null || chatJidString.isEmpty()
                    || messageId == null || messageId.isEmpty()
                    || fromMeString == null || fromMeString.isEmpty()
                    || participantString == null || participantString.isEmpty()) {
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof StarAction action)) {
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }

            var starred = action.starred();

            var msgKeyOpt = SyncdIndexUtils.syncKeyToMsgKey(client.store(), chatJidString, messageId, fromMeString, participantString);
            if (msgKeyOpt.isEmpty()) {
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }
            var msgKey = msgKeyOpt.get();

            Jid chatJid;
            try {
                chatJid = Jid.of(chatJidString);
            } catch (Exception e) {
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            var message = client.store().chatStore().findMessageById(chatJid, messageId);
            if (message.isEmpty()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "star message: orphan message chat={0} id={1}", chatJid, messageId);
                return MutationApplicationResult.orphan(
                        SyncdIndexUtils.serializeMessageKey(msgKey),
                        "Msg"
                );
            }

            var found = message.get();
            starMessage(found, starred);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "star message: set starred={0} for chat={1} id={2}", starred, chatJid, messageId);
            return MutationApplicationResult.success();
        } catch (Exception e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "star message mutation failed", e);
            return MutationApplicationResult.failed();
        }
    }

    /**
     * Sets the starred flag on a {@link LinkedMessageInfo} regardless of its concrete
     * subtype.
     *
     * <p>Dispatches the resolved {@code starred} flag onto either a chat message
     * or a newsletter message, keeping the concrete-subtype detail inside this
     * helper so the per-mutation pipeline stays uniform.
     *
     * @implNote
     * This implementation accepts the type-system asymmetry between
     * {@link ChatMessageInfo#setStarred(Boolean)} (boxed) and
     * {@link NewsletterMessageInfo#setStarred(boolean)} (primitive); both
     * accessors perform the same logical assignment, the boxed signature just
     * predates the primitive one.
     *
     * @param message the message whose flag is being updated
     * @param starred {@code true} to star, {@code false} to unstar
     */
    @WhatsAppWebExport(moduleName = "WAWebStarMessageSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    private static void starMessage(LinkedMessageInfo message, boolean starred) {
        switch (message) {
            case ChatMessageInfo chatMessageInfo -> chatMessageInfo.setStarred(starred);
            case NewsletterMessageInfo newsletterMessageInfo -> newsletterMessageInfo.setStarred(starred);
        }
    }
}
