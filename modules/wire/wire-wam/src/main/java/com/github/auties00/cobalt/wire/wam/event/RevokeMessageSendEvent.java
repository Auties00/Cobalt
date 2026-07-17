package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.RevokeType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebRevokeMessageSendWamEvent")
@WamEvent(id = 3656)
public interface RevokeMessageSendEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.BOOLEAN)
    Optional<Boolean> messageSendResultIsTerminal();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<MessageType> messageType();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong resendCount();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong retryCount();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong revokeDuration();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<RevokeType> revokeType();
}
