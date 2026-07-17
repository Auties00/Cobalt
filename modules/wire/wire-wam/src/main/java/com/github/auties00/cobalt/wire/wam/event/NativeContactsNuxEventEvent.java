package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.NativeContactsNuxEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.NativeContactsNuxEventType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebNativeContactsNuxEventWamEvent")
@WamEvent(id = 5788)
public interface NativeContactsNuxEventEvent extends WamEventSpec {
    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<NativeContactsNuxEntryPoint> nativeContactsNuxEntryPoint();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<NativeContactsNuxEventType> nativeContactsNuxEventType();
}
