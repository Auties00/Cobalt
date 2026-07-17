package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.DialogEventType;
import com.github.auties00.cobalt.wire.wam.type.DialogNameType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebDialogEventWamEvent")
@WamEvent(id = 7068)
public interface DialogEventEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> dialogEventSource();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<DialogEventType> dialogEventType();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<DialogNameType> dialogName();
}
