package com.github.auties00.cobalt.wire.linked.sync.action.setting;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Represents a synchronised setting that toggles whether detected outcomes
 * are shown in the account status.
 *
 * <p>Detected outcomes are automated classifications that WhatsApp surfaces
 * to the user (for example to indicate that a message was detected as spam
 * or that a contact was flagged). When this setting is enabled, the status
 * of those detections is propagated across the user's linked devices.
 *
 * <p>This action carries no indexing arguments: it is a singleton per
 * account and is replaced in-place whenever the user toggles the preference.
 */
@ProtobufMessage(name = "SyncActionValue.DetectedOutcomesStatusAction")
public final class DetectedOutcomesStatusAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used when this setting is encoded in an app
     * state sync mutation.
     */
    public static final String ACTION_NAME = "detected_outcomes_status_action";

    /**
     * Canonical action version emitted alongside this setting when it is
     * published to the app state sync collection.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * App state collection that stores this setting.
     *
     * <p>Mutations are delivered through the {@code REGULAR} patch type.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR;

    /**
     * Returns the canonical action name for this setting.
     *
     * @return the constant {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the canonical action version for this setting.
     *
     * @return the constant {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }

    /**
     * Whether detected outcomes are currently enabled on the account.
     *
     * <p>Encoded as an optional protobuf {@code bool}; a missing value is
     * treated as {@code false}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean isEnabled;


    /**
     * Constructs a new action carrying the enabled state.
     *
     * @param isEnabled the desired enabled state, or {@code null} to leave
     *                  the field unset on the wire
     */
    DetectedOutcomesStatusAction(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    /**
     * Returns whether detected outcomes are enabled.
     *
     * <p>A missing protobuf field is reported as {@code false}, which
     * matches the default behaviour expected by WhatsApp clients when
     * the setting has never been written.
     *
     * @return {@code true} if detected outcomes are enabled, {@code false}
     *         otherwise
     */
    public boolean isEnabled() {
        return isEnabled != null && isEnabled;
    }

    /**
     * Updates the enabled state of this setting.
     *
     * @param isEnabled the new enabled state, or {@code null} to clear the
     *                  protobuf field
     */
    public void setEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
}
