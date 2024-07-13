package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import static it.auties.protobuf.model.ProtobufType.BOOL;

@ProtobufMessage(name = "Message.InitialSecurityNotificationSettingSync")
public record InitialSecurityNotificationSettingSync(
        @ProtobufProperty(index = 1, type = BOOL) boolean securityNotificationEnabled) {
}
