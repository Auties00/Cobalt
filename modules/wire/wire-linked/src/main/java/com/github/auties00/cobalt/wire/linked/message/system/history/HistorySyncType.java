package com.github.auties00.cobalt.wire.linked.message.system.history;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Enumerates the flavours of history sync that the primary device can deliver
 * to a companion.
 *
 * <p>Every {@link HistorySyncNotification} carries one of these values to tell
 * the companion which slice of user data is contained in the attached chunk.
 * The companion then dispatches the chunk to the correct ingestion pipeline:
 * initial bootstrap is applied only on first linking, recent and full are used
 * to back-fill conversations, status updates populate the status tray, and so
 * on.
 */
@ProtobufEnum(name = "Message.HistorySyncType")
public enum HistorySyncType {
    /**
     * The first sync delivered immediately after a companion is linked,
     * containing the minimal set of chats and messages needed to render the
     * main UI.
     */
    INITIAL_BOOTSTRAP(0),
    /**
     * An early sync that delivers the most recent status updates so that the
     * status tray can be populated quickly after linking.
     */
    INITIAL_STATUS_V3(1),
    /**
     * A sync that delivers the complete available message history, typically
     * sent after the initial bootstrap when the primary device has finished
     * collecting older messages.
     */
    FULL(2),
    /**
     * A sync that delivers only the most recent messages, used to fill short
     * gaps caused by transient offline periods.
     */
    RECENT(3),
    /**
     * A sync that delivers the mapping of phone numbers to push-notification
     * display names for the user's contacts.
     */
    PUSH_NAME(4),
    /**
     * A sync that delivers ancillary data (such as labels, quick replies or
     * archived state) which is not required for message rendering and can be
     * processed without blocking the main UI.
     */
    NON_BLOCKING_DATA(5),
    /**
     * A sync that was explicitly requested by the companion to retrieve a
     * specific older slice of history on demand.
     */
    ON_DEMAND(6),
    /**
     * A sentinel value indicating that the primary device has no history to
     * share with the companion.
     */
    NO_HISTORY(7),
    /**
     * A sync that carries only a {@link HistorySyncMessageAccessStatus} payload
     * informing the companion about whether it has full or restricted access
     * to the message history.
     */
    MESSAGE_ACCESS_STATUS(8);

    /**
     * Constructs a new constant with the given protobuf wire index.
     *
     * @param index the numeric index used on the wire
     */
    HistorySyncType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * The numeric index used to represent this constant on the protobuf wire.
     */
    final int index;

    /**
     * Returns the protobuf wire index associated with this constant.
     *
     * @return the numeric index used on the wire
     */
    public int index() {
        return this.index;
    }
}
