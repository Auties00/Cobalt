package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageRange;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.MarkChatAsReadAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.MarkChatAsReadActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing mark-chat-as-read sync mutations.
 *
 * <p>Backs the swipe-to-read and mark-unread gestures in the chat list. Receiving devices apply
 * the read state and merge the carried message range into their active-message-range map.
 * Mutations produced here are consumed on the inbound side by
 * {@link com.github.auties00.cobalt.sync.handler.MarkChatAsReadHandler}.
 *
 * @implNote
 * This implementation mirrors {@code WAWebMarkChatAsReadSync.getMarkChatAsReadMutation} but takes
 * the pre-constructed {@link SyncActionMessageRange} as a parameter rather than calling
 * {@code WAWebMessageRangeUtils.constructMessageRange} inline; building the range requires store
 * infrastructure that lives at a higher layer in Cobalt.
 */
public final class MarkChatAsReadMutationFactory {
    /**
     * The logger for {@link MarkChatAsReadMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(MarkChatAsReadMutationFactory.class);

    /**
     * Constructs a mark-chat-as-read mutation factory.
     *
     * <p>The factory keeps no state, so a single instance is sufficient per client.
     */
    public MarkChatAsReadMutationFactory() {

    }

    /**
     * Builds a pending mutation for marking a chat as read or unread.
     *
     * <p>Receiving devices match the carried message range against their local range and update
     * the chat-table read counter only when the incoming range encloses the local one. The caller
     * is responsible for supplying a non-null {@link SyncActionMessageRange}; the message-range
     * conflict resolution on the receiver depends on it.
     *
     * @implNote
     * This implementation passes the supplied {@code chatJid} verbatim into the index. WA Web's
     * {@code getChatJidMutationIndexForChat} would swap a PN for its paired LID under LID1x1
     * migration; Cobalt does not yet track the outgoing-mutation LID/PN swap at this layer.
     *
     * @param timestamp    the mutation timestamp
     * @param read         {@code true} to mark the chat as read, {@code false} to mark it as
     *                     unread
     * @param chatJid      the JID of the chat
     * @param messageRange the outgoing message range to embed in the {@code markChatAsReadAction}
     *                     value; receiving devices use it to resolve conflicts with their local
     *                     active range
     * @return the pending mutation for the mark-chat-as-read operation
     */
    @WhatsAppWebExport(moduleName = "WAWebMarkChatAsReadSync", exports = "getMarkChatAsReadMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getMarkChatAsReadMutation(
            Instant timestamp,
            boolean read,
            Jid chatJid,
            SyncActionMessageRange messageRange
    ) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building mark chat as read mutation chat={0} read={1}", chatJid, read);
        var action = new MarkChatAsReadActionBuilder()
                .read(read)
                .messageRange(messageRange)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .markChatAsReadAction(action)
                .build();
        var index = JSON.toJSONString(List.of(MarkChatAsReadAction.ACTION_NAME, chatJid.toString()));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                MarkChatAsReadAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
