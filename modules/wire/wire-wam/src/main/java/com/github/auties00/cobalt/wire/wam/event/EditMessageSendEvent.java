package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.EditType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.TypeOfGroupEnum;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebEditMessageSendWamEvent")
@WamEvent(id = 3990)
public interface EditMessageSendEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong editDuration();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<EditType> editType();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> editedMessageId();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<MediaType> mediaType();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> messageSendResultIsTerminal();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<MessageType> messageType();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong resendCount();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong retryCount();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<TypeOfGroupEnum> typeOfGroup();
}
