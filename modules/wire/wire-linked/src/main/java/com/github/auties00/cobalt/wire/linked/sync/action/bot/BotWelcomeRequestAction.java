package com.github.auties00.cobalt.wire.linked.sync.action.bot;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A sync action that tracks whether a welcome message has been sent to a
 * newly added bot chat.
 *
 * <p>When a user starts a conversation with a WhatsApp bot for the first
 * time, the client dispatches an initial welcome message on behalf of the
 * user. This action records, per chat, whether that welcome has already
 * been delivered so that linked devices do not redundantly send another
 * welcome when they learn about the same chat.
 *
 * <p>The chat is identified by its companion
 * {@link BotWelcomeRequestActionArgs} index, and the delivery status is
 * carried by the nullable {@code isSent} flag. An absent flag is treated
 * equivalently to an explicit {@code false} by most consumers; callers that
 * need to distinguish "never set" from "explicitly pending" can read the
 * raw value via {@link #rawIsSent()}.
 */
@ProtobufMessage(name = "SyncActionValue.BotWelcomeRequestAction")
public final class BotWelcomeRequestAction implements SyncAction<BotWelcomeRequestActionArgs> {
    /**
     * The canonical action name used to identify this sync action on the wire.
     */
    public static final String ACTION_NAME = "bot_welcome_request";

    /**
     * The canonical action version negotiated by the WhatsApp sync protocol
     * for this action type.
     */
    public static final int ACTION_VERSION = 2;

    /**
     * The sync patch collection that stores this action on the server.
     *
     * <p>Bot welcome tracking lives in the low-priority regular collection
     * because it is a bookkeeping flag rather than a user-facing setting.
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
     * The raw nullable flag indicating whether the welcome message has been
     * sent to the bot chat.
     *
     * <p>Three states are possible on the wire: the field may be absent
     * ({@code null}), explicitly {@code false}, or explicitly {@code true}.
     * Most handlers collapse the first two into a single "not sent" state.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean isSent;


    /**
     * Constructs a new {@code BotWelcomeRequestAction} with the supplied
     * delivery status.
     *
     * @param isSent the delivery status, or {@code null} if the flag was not
     *               set on the wire
     */
    BotWelcomeRequestAction(Boolean isSent) {
        this.isSent = isSent;
    }

    /**
     * Returns whether the welcome message has already been delivered to the
     * bot chat, coalescing an absent value to {@code false}.
     *
     * <p>This is the accessor that most consumers want: it treats "field
     * missing on the wire" as equivalent to "welcome still pending", which
     * matches how WhatsApp clients interpret the flag. Callers that need to
     * distinguish absent from an explicit {@code false} should instead use
     * {@link #rawIsSent()}.
     *
     * @return {@code true} if the welcome message has been sent,
     *         {@code false} otherwise or if the flag is absent
     */
    public boolean isSent() {
        return isSent != null && isSent;
    }

    /**
     * Returns the raw nullable delivery flag exactly as it was received on
     * the wire, preserving the distinction between an absent field and an
     * explicitly set value.
     *
     * <p>This accessor is intended for code paths that need to detect
     * malformed or legacy payloads in which the field is missing entirely.
     *
     * @return an {@link Optional} containing the raw {@link Boolean} value,
     *         or {@link Optional#empty()} if the field was not present on
     *         the wire
     */
    public Optional<Boolean> rawIsSent() {
        return Optional.ofNullable(isSent);
    }

    /**
     * Updates the delivery flag that indicates whether the welcome message
     * has been sent to the bot chat.
     *
     * @param isSent the new flag value, or {@code null} to clear the field
     */
    public void setSent(Boolean isSent) {
        this.isSent = isSent;
    }


}
