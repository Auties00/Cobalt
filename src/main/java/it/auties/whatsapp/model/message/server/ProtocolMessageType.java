package it.auties.whatsapp.model.message.server;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various type of data that a
 * {@link ProtocolMessage} can wrap
 */
public enum ProtocolMessageType implements ProtobufEnum {
    /**
     * A {@link ProtocolMessage} that notifies that a message was deleted for everyone in a chat
     */
    REVOKE(0),
    /**
     * A {@link ProtocolMessage} that notifies that the ephemeral settings in a chat have changed
     */
    EPHEMERAL_SETTING(3),
    /**
     * A {@link ProtocolMessage} that notifies that a dataSync in an ephemeral chat
     */
    EPHEMERAL_SYNC_RESPONSE(4),
    /**
     * A {@link ProtocolMessage} that notifies that a history dataSync in any chat
     */
    HISTORY_SYNC_NOTIFICATION(5),
    /**
     * App state dataSync key share
     */
    APP_STATE_SYNC_KEY_SHARE(6),
    /**
     * App state dataSync key request
     */
    APP_STATE_SYNC_KEY_REQUEST(7),
    /**
     * Message back-fill request
     */
    MESSAGE_BACK_FILL_REQUEST(8),
    /**
     * Initial security notification setting dataSync
     */
    INITIAL_SECURITY_NOTIFICATION_SETTING_SYNC(9),
    /**
     * App state fatal exception notification
     */
    EXCEPTION_NOTIFICATION(10),
    /**
     * Share phone number
     */
    SHARE_PHONE_NUMBER(11),
    /**
     * Message edit
     */
    MESSAGE_EDIT(14);

    final int index;

    ProtocolMessageType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return this.index;
    }
}
