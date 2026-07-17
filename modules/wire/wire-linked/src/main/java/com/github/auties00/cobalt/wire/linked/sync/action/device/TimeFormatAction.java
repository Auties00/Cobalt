package com.github.auties00.cobalt.wire.linked.sync.action.device;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Represents a sync action that replicates the user's preferred time display
 * format (12 hour versus 24 hour) across every linked device.
 *
 * <p>When enabled, timestamps in chat lists, message bubbles, and
 * notifications are rendered in 24 hour form; when disabled, the client falls
 * back to 12 hour form with an AM or PM indicator. The mutation is singleton,
 * so the sync index is composed solely of {@link #ACTION_NAME} with no
 * trailing arguments.
 */
@ProtobufMessage(name = "SyncActionValue.TimeFormatAction")
public final class TimeFormatAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used as the sole component of the singleton mutation
     * index for this action type.
     */
    public static final String ACTION_NAME = "time_format";

    /**
     * Schema version advertised by this action, used by sync handlers to gate
     * deserialisation and handling of newer payload shapes.
     */
    public static final int ACTION_VERSION = 7;

    /**
     * Collection this action belongs to, used by the sync protocol to route the
     * mutation into the correct replication stream.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

    /**
     * Returns the canonical action name for every {@code TimeFormatAction}.
     *
     * @return the constant {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version for every {@code TimeFormatAction}.
     *
     * @return the constant {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Flag that enables 24 hour time display when {@code true} and 12 hour
     * display when {@code false}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean isTwentyFourHourFormatEnabled;


    /**
     * Constructs a new {@code TimeFormatAction} from raw protobuf field values.
     *
     * @param isTwentyFourHourFormatEnabled whether 24 hour display is enabled,
     *                                      possibly {@code null}
     */
    TimeFormatAction(Boolean isTwentyFourHourFormatEnabled) {
        this.isTwentyFourHourFormatEnabled = isTwentyFourHourFormatEnabled;
    }

    /**
     * Returns whether the user has selected the 24 hour time display format.
     *
     * @return {@code true} if 24 hour display is enabled, {@code false}
     *         otherwise (including when the field was unset on the wire)
     */
    public boolean isTwentyFourHourFormatEnabled() {
        return isTwentyFourHourFormatEnabled != null && isTwentyFourHourFormatEnabled;
    }

    /**
     * Sets the 24 hour time display flag.
     *
     * @param isTwentyFourHourFormatEnabled {@code true} to enable 24 hour
     *                                      display, {@code false} to use 12
     *                                      hour display, or {@code null} to
     *                                      clear
     */
    public void setTwentyFourHourFormatEnabled(Boolean isTwentyFourHourFormatEnabled) {
        this.isTwentyFourHourFormatEnabled = isTwentyFourHourFormatEnabled;
    }
}
