package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageType;

import java.time.Instant;
import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebWebcMessageSendWamEvent")
@WamEvent(id = 2072)
public interface WebcMessageSendEvent extends WamEventSpec {
    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> messageIsForward();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<MediaType> messageMediaType();

    @WamProperty(index = 4, type = WamType.TIMER)
    Optional<Instant> messageSendT();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<MessageType> messageType();
}
