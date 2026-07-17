package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageSecretAllowedType;
import com.github.auties00.cobalt.wire.wam.type.MessageSecretErrorType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebMessageSecretErrorsWamEvent")
@WamEvent(id = 3686)
public interface MessageSecretErrorsEvent extends WamEventSpec {
    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<MediaType> messageMediaType();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<MessageSecretAllowedType> messageSecretAllowedList();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<MessageSecretErrorType> messageSecretError();
}
