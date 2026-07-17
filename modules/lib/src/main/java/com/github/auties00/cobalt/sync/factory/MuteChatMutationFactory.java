package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.MuteAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.MuteActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing mute-chat sync mutations.
 *
 * <p>Backs the chat-mute picker in the chat-info surface, supporting mutes for a fixed duration,
 * indefinitely ({@code muteEndSeconds == -1}), or unmuting ({@code muteEndSeconds == 0}). Group
 * chats additionally support a separate mention-everyone mute window gated by the
 * {@link ABProp#ENABLE_MENTION_EVERYONE_SYNCD_SENDER} AB prop. Mutations produced here are
 * consumed on receiving devices by
 * {@link com.github.auties00.cobalt.sync.handler.MuteChatHandler}.
 *
 * @implNote
 * This implementation mirrors {@code WAWebMuteChatSync.generateMuteMutation} including its
 * three-way sentinel for {@code muteEndTimestamp}: {@code -1} stored verbatim (muted
 * indefinitely), {@code 0} stored verbatim (unmuted), every other value multiplied by 1000 to
 * convert seconds to milliseconds.
 */
public final class MuteChatMutationFactory {
    /**
     * The logger for {@link MuteChatMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(MuteChatMutationFactory.class);

    /**
     * The AB-props service consulted for the {@link ABProp#ENABLE_MENTION_EVERYONE_SYNCD_SENDER}
     * gate when building the outgoing mute mutation.
     *
     * <p>The mention-everyone mute window is only populated when the remote-config gate is
     * enabled; otherwise the field is omitted from the {@link MuteAction} regardless of what the
     * caller passes.
     */
    private final ABPropsService abPropsService;

    /**
     * Constructs a mute-chat mutation factory bound to the given AB-props service.
     *
     * <p>The AB-props service is consulted on every generated mute mutation for the
     * mention-everyone gate.
     *
     * @param abPropsService the AB-props service consulted on every generated mute mutation
     */
    public MuteChatMutationFactory(ABPropsService abPropsService) {
        this.abPropsService = abPropsService;
    }

    /**
     * Builds a pending mutation for muting or unmuting a chat.
     *
     * <p>Receiving devices merge the resulting mute expiration (in seconds) into the chat table
     * and fire a mute-collection-add backend event. For group chats with an active
     * {@link ABProp#ENABLE_MENTION_EVERYONE_SYNCD_SENDER} gate, the mention-everyone window
     * propagates as a separate field.
     *
     * @implNote
     * This implementation preserves the WA Web sentinel for indefinite mutes:
     * {@code muteEndSeconds == -1} is stored verbatim in the protobuf {@code int64} field, all
     * other non-zero values are scaled from seconds to milliseconds via
     * {@link Instant#ofEpochMilli(long)}, and {@code muteEndSeconds == 0} maps to
     * {@code Instant.ofEpochMilli(0)} to match WA Web's {@code 0 * 1000 = 0} branch on the unmute
     * path. The {@code mentionAllSeconds} value follows the same sentinel convention: non-positive
     * values pass through unchanged, positive values are multiplied by 1000. The supplied
     * {@code chatJid} is used verbatim in the index; WA Web's
     * {@code getChatJidMutationIndexForChat} would swap a PN for its paired LID under LID1x1
     * migration, which Cobalt does not yet track at this layer.
     *
     * @param client            the WhatsApp client, reserved for callers that need to thread the
     *                          active session state through the factory
     * @param chatJid           the JID of the chat to mute or unmute
     * @param muteEndSeconds    the mute end time in seconds since the epoch; {@code 0} means
     *                          unmute, {@code -1} means muted indefinitely, any other positive
     *                          value is the expiration timestamp
     * @param mentionAllSeconds the optional mention-everyone mute end time in seconds, or
     *                          {@code null} if the caller does not wish to set a mention-everyone
     *                          mute
     * @return the pending mutation for the mute action
     */
    @WhatsAppWebExport(moduleName = "WAWebMuteChatSync", exports = "generateMuteMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation generateMuteMutation(
            LinkedWhatsAppClient client,
            Jid chatJid,
            long muteEndSeconds,
            Long mentionAllSeconds
    ) {
        var now = Instant.now();
        var muted = muteEndSeconds != 0L;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building mute chat mutation chat={0} muted={1}", chatJid, muted);
        var muteEndInstant = muteEndSeconds == -1L
                ? Instant.ofEpochMilli(-1L)
                : Instant.ofEpochMilli(muteEndSeconds * 1000L);
        var actionBuilder = new MuteActionBuilder()
                .muted(muted)
                .muteEndTimestamp(muteEndInstant);
        if (chatJid.hasGroupOrCommunityServer()
                && mentionAllSeconds != null
                && abPropsService.getBool(ABProp.ENABLE_MENTION_EVERYONE_SYNCD_SENDER)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "applying mention-everyone mute gate chat={0}", chatJid);
            var mentionMillis = mentionAllSeconds > 0
                    ? mentionAllSeconds * 1000L
                    : mentionAllSeconds;
            actionBuilder.muteEveryoneMentionEndTimestamp(Instant.ofEpochMilli(mentionMillis));
        }
        var action = actionBuilder.build();
        var value = new SyncActionValueBuilder()
                .timestamp(now)
                .muteAction(action)
                .build();
        var index = JSON.toJSONString(List.of(MuteAction.ACTION_NAME, chatJid.toString()));
        var trusted = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                now,
                MuteAction.ACTION_VERSION
        );
        return new SyncPendingMutation(trusted, 0);
    }
}
