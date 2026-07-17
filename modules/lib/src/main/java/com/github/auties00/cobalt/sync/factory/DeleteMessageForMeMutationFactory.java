package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.DeleteMessageForMeAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.DeleteMessageForMeActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds outgoing app-state mutations that perform a one-sided message delete (only on the current user's devices).
 *
 * Drives the "delete for me" affordance: the mutation describes one or more messages that should
 * disappear from the current user's devices without touching the original sender or peer. This
 * factory is the outgoing-mutation counterpart of
 * {@link com.github.auties00.cobalt.sync.handler.DeleteMessageForMeHandler}.
 *
 * @implNote
 * This implementation takes pre-resolved {@link MessageKey} instances and parallel lists of message
 * timestamps and group flags. WA Web's {@code getDeleteForMeMutations} reads those fields off the
 * live message model via {@code WAWebMsgGetters.getSender}, {@code getT}, and {@code getIsGroupMsg};
 * Cobalt does not run that live-message model so the caller hands the data in already projected.
 */
public final class DeleteMessageForMeMutationFactory {
    /**
     * The logger for {@link DeleteMessageForMeMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(DeleteMessageForMeMutationFactory.class);

    /**
     * Creates an instance with no collaborators.
     *
     * The factory is stateless, so a single instance may be shared across the lifetime of the
     * client.
     */
    public DeleteMessageForMeMutationFactory() {

    }

    /**
     * Returns a SET mutation that deletes a single message for the current user.
     *
     * Targets one specific {@code (remoteJid, id, fromMe, participant)} tuple. The mutation index
     * follows
     * {@snippet :
     *     ["deleteMessageForMe", remoteJid.toString(), id, fromMe ? "1" : "0", participant != null && !fromMe ? participant.toString() : "0"]
     * }
     * and the {@link DeleteMessageForMeAction} sub-message carries the {@code deleteMedia} flag plus
     * the original message timestamp.
     *
     * @implNote
     * This implementation builds the index inline instead of delegating to a
     * {@code WAWebSyncdActionUtils.buildMessageKey} helper; the wire shape is identical to WA Web's.
     * The participant segment is written as {@code "0"} when the message is either non-group or sent
     * by the current user, matching the receive-side parser's expectations in
     * {@code WAWebSyncdIndexUtils.syncKeyToMsgKey}.
     *
     * @param timestamp        the mutation timestamp
     * @param deleteMedia      {@code true} if the on-disk media must be deleted as well
     * @param messageTimestamp the timestamp of the message being deleted (carried in the action body for receive-side conflict resolution)
     * @param remoteJid        the remote {@link Jid} of the chat the message belongs to
     * @param id               the message identifier
     * @param fromMe           {@code true} if the current user sent the message
     * @param participant      the participant {@link Jid} for group messages received from someone else, or {@code null}
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebDeleteMessageForMeSync", exports = "buildDeleteForMeMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation buildDeleteForMeMutation(
            Instant timestamp,
            boolean deleteMedia,
            Instant messageTimestamp,
            Jid remoteJid,
            String id,
            boolean fromMe,
            Jid participant
    ) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building delete message for me mutation chat={0} id={1} fromMe={2}", remoteJid, id, fromMe);
        var action = new DeleteMessageForMeActionBuilder()
                .deleteMedia(deleteMedia)
                .messageTimestamp(messageTimestamp)
                .build();

        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .deleteMessageForMeAction(action)
                .build();

        var fromMeStr = fromMe ? "1" : "0";
        var participantStr = participant != null && !fromMe
                ? participant.toString()
                : "0";

        var index = JSON.toJSONString(List.of(
                DeleteMessageForMeAction.ACTION_NAME,
                remoteJid.toString(),
                id,
                fromMeStr,
                participantStr
        ));

        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                DeleteMessageForMeAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }

    /**
     * Returns one SET mutation per message in the supplied batch, sharing a single timestamp.
     *
     * Backs the multi-select "delete for me" path; the three lists are parallel arrays indexed
     * together. Messages whose {@link MessageKey#parentJid()} is empty are skipped silently.
     *
     * @implNote
     * This implementation derives the participant segment as {@link MessageKey#senderJid()} converted
     * to its user-JID form via {@link Jid#toUserJid()}, then includes it only when the message is a
     * group message that the current user did not author. WA Web reads {@code getSender(msg)} from
     * the live message model and runs the same {@code widToUserJid} conversion. A single timestamp is
     * reused across the batch so the mutations land together on the wire, matching WA Web's
     * {@code getDeleteForMeMutations} which calls {@code WATimeUtils.unixTimeMs()} once outside the
     * loop.
     *
     * @param keys              the per-message {@link MessageKey}s; {@link MessageKey#parentJid()} is required
     * @param deleteMedia       {@code true} if the on-disk media must be deleted as well
     * @param messageTimestamps parallel array of original message timestamps
     * @param isGroupMessages   parallel array of group-message flags
     * @return the ordered list of pending mutations, with one entry per non-skipped input
     */
    @WhatsAppWebExport(moduleName = "WAWebDeleteMessageForMeSync", exports = "getDeleteForMeMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public List<SyncPendingMutation> getDeleteForMeMutations(
            List<MessageKey> keys,
            boolean deleteMedia,
            List<Instant> messageTimestamps,
            List<Boolean> isGroupMessages
    ) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building batch delete for me mutations count={0}", keys.size());
        var now = Instant.now();
        var results = new ArrayList<SyncPendingMutation>(keys.size());
        for (var i = 0; i < keys.size(); i++) {
            var key = keys.get(i);
            var messageTimestamp = messageTimestamps.get(i);
            var isGroup = isGroupMessages.get(i);

            var senderJid = key.senderJid().map(Jid::toUserJid).orElse(null);

            var participant = isGroup && !key.fromMe() ? senderJid : null;

            var remoteJid = key.parentJid().orElse(null);
            if (remoteJid == null) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "skipping delete for me mutation with no parent jid id={0}", key.id().orElse(""));
                continue;
            }

            results.add(buildDeleteForMeMutation(
                    now,
                    deleteMedia,
                    messageTimestamp,
                    remoteJid,
                    key.id().orElse(""),
                    key.fromMe(),
                    participant
            ));
        }
        return results;
    }
}
