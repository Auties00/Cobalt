package com.github.auties00.cobalt.wire.linked.sync.action.privacy;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Records the user's preference to relay all WhatsApp calls through
 * WhatsApp servers.
 *
 * <p>By default, a voice or video call between two users uses a direct
 * peer-to-peer connection whenever possible, which exposes each participant's
 * IP address to the other. When the "relay all calls" option is enabled, the
 * client routes the media stream through WhatsApp servers so the two
 * endpoints never learn each other's addresses.
 *
 * <p>This sync action propagates the preference across the user's linked
 * devices so that every device uses the same relay policy. The action
 * carries a single boolean flag indicating whether relaying is enabled.
 */
@ProtobufMessage(name = "SyncActionValue.PrivacySettingRelayAllCalls")
public final class PrivacySettingRelayAllCalls implements SyncAction<SyncActionEmptyArgs> {
    /**
     * The canonical action name used to identify this sync action on the
     * wire.
     */
    public static final String ACTION_NAME = "setting_relayAllCalls";

    /**
     * The canonical protocol version of this sync action.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * The sync collection this action is stored in.
     *
     * <p>Call relay preferences are persisted in the regular sync patch
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
     * Flag indicating whether call relaying through WhatsApp servers is
     * enabled.
     *
     * <p>A {@code null} value means the field was absent on the wire.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean isEnabled;


    /**
     * Constructs a new action carrying the supplied relay flag.
     *
     * @param isEnabled the relay flag to persist, or {@code null} if the
     *                  field is absent
     */
    PrivacySettingRelayAllCalls(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    /**
     * Returns whether call relaying through WhatsApp servers is enabled.
     *
     * <p>An absent value on the wire is coalesced to {@code false}, which
     * matches the WhatsApp default where calls fall back to direct
     * peer-to-peer connections when possible.
     *
     * @return {@code true} if relaying is enabled, {@code false} otherwise
     *         including when the field is unset
     */
    public boolean isEnabled() {
        return isEnabled != null && isEnabled;
    }

    /**
     * Sets whether call relaying through WhatsApp servers is enabled.
     *
     * @param isEnabled the new relay flag value, or {@code null} to clear
     *                  the field
     */
    public void setEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
}
