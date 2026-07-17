package com.github.auties00.cobalt.wire.linked.sync.action.privacy;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Records the user's preference to disable automatic link previews in chats.
 *
 * <p>Link previews generate an inline summary of any URL shared in a
 * conversation by fetching the target page's metadata. When a user disables
 * this feature from the privacy settings, WhatsApp propagates the change to
 * every linked device via this sync action so that no linked device fetches
 * link metadata on the user's behalf.
 *
 * <p>The action carries a single boolean flag. The raw flag can be read via
 * {@link #rawIsPreviewsDisabled()} when callers need to distinguish between
 * an absent value and an explicit {@code false}; otherwise
 * {@link #isPreviewsDisabled()} returns the effective boolean preference.
 */
@ProtobufMessage(name = "SyncActionValue.PrivacySettingDisableLinkPreviewsAction")
public final class PrivacySettingDisableLinkPreviewsAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * The canonical action name used to identify this sync action on the
     * wire.
     */
    public static final String ACTION_NAME = "setting_disableLinkPreviews";

    /**
     * The canonical protocol version of this sync action.
     */
    public static final int ACTION_VERSION = 8;

    /**
     * The sync collection this action is stored in.
     *
     * <p>Link preview preferences are persisted in the regular sync patch
     * collection alongside other account-wide privacy settings.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR;

    /**
     * Returns the canonical action name used to identify this sync action on
     * the wire.
     *
     * @return the action name {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the canonical protocol version of this sync action.
     *
     * @return the action version {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Flag indicating whether link previews are disabled for the account.
     *
     * <p>A {@code null} value means the field was absent on the wire, which
     * WhatsApp treats as "previews enabled".
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean isPreviewsDisabled;


    /**
     * Constructs a new action carrying the supplied link previews flag.
     *
     * @param isPreviewsDisabled the preview disable flag to persist, or
     *                           {@code null} if the field is absent
     */
    PrivacySettingDisableLinkPreviewsAction(Boolean isPreviewsDisabled) {
        this.isPreviewsDisabled = isPreviewsDisabled;
    }

    /**
     * Returns whether link previews are disabled for the account, coalescing
     * an absent value to {@code false}.
     *
     * <p>Both an absent value and an explicit {@code false} are treated as
     * "previews enabled", which matches the default behaviour of every
     * WhatsApp client. Callers that need to distinguish the two cases should
     * use {@link #rawIsPreviewsDisabled()} instead.
     *
     * @return {@code true} if link previews are disabled, otherwise
     *         {@code false}
     */
    public boolean isPreviewsDisabled() {
        return isPreviewsDisabled != null && isPreviewsDisabled;
    }

    /**
     * Returns the raw nullable link previews flag as delivered by the sync
     * action, preserving the distinction between an absent field and an
     * explicitly set value.
     *
     * @return an {@link Optional} containing the raw {@link Boolean} value,
     *         or an empty {@code Optional} if the field was not present on
     *         the wire
     */
    public Optional<Boolean> rawIsPreviewsDisabled() {
        return Optional.ofNullable(isPreviewsDisabled);
    }

    /**
     * Sets the link previews flag, which indicates whether link previews
     * should be suppressed for the account.
     *
     * @param isPreviewsDisabled the new flag value, or {@code null} to clear
     *                           the field
     */
    public void setPreviewsDisabled(Boolean isPreviewsDisabled) {
        this.isPreviewsDisabled = isPreviewsDisabled;
    }
}
