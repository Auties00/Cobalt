package com.github.auties00.cobalt.wire.linked.message.system;

import com.github.auties00.cobalt.wire.linked.message.Message;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * A system message that communicates the initial value of the user's
 * "Show security notifications" preference to newly linked companion devices.
 *
 * <p>The primary device emits this message during the initial sync so that
 * secondary devices start with the same setting the user configured on the
 * phone. When this preference is enabled, WhatsApp displays system messages
 * in conversations whenever a contact's encryption keys change.
 */
@ProtobufMessage(name = "Message.InitialSecurityNotificationSettingSync")
public final class InitialSecurityNotificationSettingSync implements Message {
    /**
     * Whether security notifications are enabled for the user's account.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean securityNotificationEnabled;


    /**
     * Constructs a new initial security notification sync message.
     *
     * @param securityNotificationEnabled the initial preference value, may be {@code null}
     */
    InitialSecurityNotificationSettingSync(Boolean securityNotificationEnabled) {
        this.securityNotificationEnabled = securityNotificationEnabled;
    }

    /**
     * Indicates whether security notifications are enabled for the account.
     *
     * @return {@code true} if security notifications are enabled,
     *         {@code false} otherwise or when the flag is unset
     */
    public boolean securityNotificationEnabled() {
        return securityNotificationEnabled != null && securityNotificationEnabled;
    }

    /**
     * Sets whether security notifications are enabled for the account.
     *
     * @param securityNotificationEnabled the new preference value, or {@code null} to clear it
     */
    public void setSecurityNotificationEnabled(Boolean securityNotificationEnabled) {
        this.securityNotificationEnabled = securityNotificationEnabled;
    }
}
