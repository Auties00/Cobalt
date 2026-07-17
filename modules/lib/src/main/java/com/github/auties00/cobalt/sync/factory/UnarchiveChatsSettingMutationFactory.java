package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.UnarchiveChatsSetting;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.UnarchiveChatsSettingBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Builds outgoing unarchive-chats-setting sync mutations.
 *
 * <p>This factory backs the "keep chats archived" toggle; one call produces a single
 * {@link SyncPendingMutation} that propagates the chosen auto-unarchive behaviour to every linked
 * device and is consumed on receiving devices by
 * {@link com.github.auties00.cobalt.sync.handler.UnarchiveChatsSettingHandler}.
 *
 * @implNote
 * This factory has no direct WA Web counterpart: the {@code setting_unarchiveChats} action is
 * declared only as a protobuf shape and a user-prefs key on WA Web; outgoing changes there are
 * wrapped via the generic {@code WAWebSyncdActionUtils.buildPendingMutation} pathway shared by every
 * account-scoped syncd action.
 */
public final class UnarchiveChatsSettingMutationFactory {
    /**
     * The logger for {@link UnarchiveChatsSettingMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(UnarchiveChatsSettingMutationFactory.class);

    /**
     * Constructs an unarchive-chats-setting mutation factory.
     *
     * <p>The factory keeps no state, so a single instance is sufficient per client.
     */
    public UnarchiveChatsSettingMutationFactory() {

    }

    /**
     * Builds a pending {@code setting_unarchiveChats} mutation that broadcasts the given
     * auto-unarchive preference to every linked device.
     *
     * <p>When {@code unarchiveChats} is {@code true} every new incoming message to an archived chat
     * pops that chat back out of the archive folder on every linked device; when {@code false}
     * archived chats remain archived regardless of new messages. The index carries only the action
     * name because the preference is a singleton per account.
     *
     * @implNote
     * This implementation models the {@code SyncActionValue.unarchiveChatsSetting} protobuf shape as
     * used by WA Web's generic pending-mutation builder.
     *
     * @param timestamp      the mutation timestamp recorded on both the outer mutation and the inner
     *                       {@code SyncActionValue}
     * @param unarchiveChats {@code true} to enable auto-unarchive on new message, {@code false}
     *                       otherwise
     * @return a pending mutation carrying the {@code setting_unarchiveChats} action
     * @throws NullPointerException if {@code timestamp} is {@code null}
     */
    public SyncPendingMutation getUnarchiveChatsMutation(Instant timestamp, boolean unarchiveChats) {
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building unarchive-chats-setting mutation, unarchiveChats={0}", unarchiveChats);
        var setting = new UnarchiveChatsSettingBuilder()
                .unarchiveChats(unarchiveChats)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .unarchiveChatsSetting(setting)
                .build();
        var index = JSON.toJSONString(List.of(UnarchiveChatsSetting.ACTION_NAME));
        var pending = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                UnarchiveChatsSetting.ACTION_VERSION
        );
        return new SyncPendingMutation(pending, 0);
    }
}
